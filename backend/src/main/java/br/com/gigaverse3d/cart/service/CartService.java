package br.com.gigaverse3d.cart.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.cart.api.CartDtos.AddItemRequest;
import br.com.gigaverse3d.cart.api.CartDtos.CartItemView;
import br.com.gigaverse3d.cart.api.CartDtos.CartView;
import br.com.gigaverse3d.cart.api.CartDtos.OptionView;
import br.com.gigaverse3d.cart.domain.Cart;
import br.com.gigaverse3d.cart.domain.CartItem;
import br.com.gigaverse3d.cart.repository.CartRepository;
import br.com.gigaverse3d.cart.service.Pricing.PricedLine;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductImage;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;
import br.com.gigaverse3d.storage.StorageService;

/** Carrinho do usuário da sessão. Toda leitura recalcula preço e disponibilidade. */
@Service
@Transactional
public class CartService {

	private final CartRepository carts;

	private final ProductRepository products;

	private final InventoryItemRepository inventory;

	private final Pricing pricing;

	private final StorageService storage;

	public CartService(CartRepository carts, ProductRepository products, InventoryItemRepository inventory,
			Pricing pricing, StorageService storage) {
		this.carts = carts;
		this.products = products;
		this.inventory = inventory;
		this.pricing = pricing;
		this.storage = storage;
	}

	public record PricedCart(Cart cart, List<PricedLine> lines, List<CartItem> items) {

		public BigDecimal subtotal() {
			return Money.normalize(lines.stream().map(PricedLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
		}

		public boolean hasProblems() {
			return lines.stream().anyMatch(l -> !l.ok());
		}
	}

	Cart cartOf(Long userId) {
		return carts.findByUserId(userId).orElseGet(() -> carts.save(new Cart(userId)));
	}

	public CartView view(Long userId) {
		return toView(priced(userId));
	}

	public CartView add(Long userId, AddItemRequest req) {
		Cart cart = cartOf(userId);
		addInternal(cart, req, false, false);
		carts.flush();
		return view(userId);
	}

	public CartView updateQuantity(Long userId, Long itemId, int quantity) {
		CartItem item = itemOf(userId, itemId);
		item.setQuantity(quantity);
		carts.flush();
		return view(userId);
	}

	public CartView remove(Long userId, Long itemId) {
		Cart cart = cartOf(userId);
		cart.remove(itemOf(userId, itemId));
		carts.flush();
		return view(userId);
	}

	/**
	 * Junta o carrinho guardado no navegador (visitante) ao carrinho da conta após o login.
	 * Idempotente: cada linha fica com a MAIOR quantidade entre conta e navegador (não soma), então
	 * reenvios — p.ex. requisição abortada por recarregamento e repetida — nunca duplicam itens.
	 */
	public CartView merge(Long userId, List<AddItemRequest> items) {
		Cart cart = cartOf(userId);
		for (AddItemRequest req : items == null ? List.<AddItemRequest>of() : items) {
			try {
				addInternal(cart, req, true, true);
			}
			catch (NotFoundException | FieldValidationException | ConflictException ignored) {
				// item do navegador que deixou de existir ou ficou inválido: descartado
			}
		}
		carts.flush();
		return view(userId);
	}

	private void addInternal(Cart cart, AddItemRequest req, boolean lenient, boolean keepMax) {
		Product product = products.findBySlugAndStatus(req.productSlug(), Product.Status.ACTIVE)
			.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
		Long[] options = req.optionValueIds() == null ? new Long[0] : req.optionValueIds().toArray(Long[]::new);
		PricedLine check = pricing.price(product, options, 1, inventory.findById(product.getId()).orElse(null));
		if (!check.ok() && !(lenient && check.problem().startsWith("Quantidade"))) {
			throw new FieldValidationException("productSlug", check.problem());
		}
		CartItem existing = cart.find(product.getId(), options).orElse(null);
		int current = existing == null ? 0 : existing.getQuantity();
		int quantity = Math.min(CartItem.MAX_QUANTITY, keepMax ? Math.max(current, req.quantity()) : current + req.quantity());
		if (existing != null) {
			existing.setQuantity(quantity);
		}
		else {
			if (cart.getItems().size() >= Cart.MAX_LINES) {
				throw new ConflictException("Limite de " + Cart.MAX_LINES + " itens diferentes no carrinho.");
			}
			cart.add(product.getId(), options, quantity);
		}
	}

	private CartItem itemOf(Long userId, Long itemId) {
		return cartOf(userId).getItems()
			.stream()
			.filter(i -> i.getId().equals(itemId))
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Item não encontrado no carrinho."));
	}

	/** Carrinho com preços recalculados agora (usado também pelo checkout). */
	public PricedCart priced(Long userId) {
		Cart cart = cartOf(userId);
		List<CartItem> items = cart.getItems();
		List<Long> ids = items.stream().map(CartItem::getProductId).distinct().toList();
		Map<Long, Product> byId = products.findAllById(ids).stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));
		Map<Long, InventoryItem> stock = inventory.findByProductIdIn(ids).stream()
			.collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
		// produto excluído do catálogo some do carrinho
		items.stream().filter(i -> !byId.containsKey(i.getProductId())).toList().forEach(cart::remove);
		List<CartItem> remaining = cart.getItems();
		List<PricedLine> lines = remaining.stream()
			.map(i -> pricing.price(byId.get(i.getProductId()), i.getOptionValueIds(), i.getQuantity(),
					stock.get(i.getProductId())))
			.toList();
		return new PricedCart(cart, lines, remaining);
	}

	private CartView toView(PricedCart priced) {
		List<CartItemView> views = new java.util.ArrayList<>();
		for (int i = 0; i < priced.items().size(); i++) {
			CartItem item = priced.items().get(i);
			PricedLine line = priced.lines().get(i);
			String image = line.product().getImages().stream()
				.min(Comparator.comparingInt(ProductImage::getSortOrder))
				.map(img -> storage.publicUrl(img.getStorageKey()))
				.orElse(null);
			views.add(new CartItemView(item.getId(), line.product().getSlug(), line.product().getName(), image,
					line.options().stream().map(o -> new OptionView(o.group(), o.label(), o.priceDelta())).toList(),
					line.unitPrice(), item.getQuantity(), line.lineTotal(), line.maxQuantity(), line.problem()));
		}
		int count = priced.items().stream().mapToInt(CartItem::getQuantity).sum();
		return new CartView(views, count, priced.subtotal(), Money.CURRENCY, priced.hasProblems());
	}

}
