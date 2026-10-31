package br.com.gigaverse3d.cart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductOption;
import br.com.gigaverse3d.catalog.domain.ProductOptionValue;
import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.inventory.domain.InventoryItem;

/**
 * Cálculo de preço e disponibilidade — ÚNICA fonte de verdade para carrinho e checkout.
 * Preço = preço efetivo do produto (promo, se houver) + acréscimos das opções escolhidas.
 * Nunca usa valores enviados pelo cliente.
 */
@Component
public class Pricing {

	/** Limite por linha para itens sob encomenda (sem estoque físico). */
	public static final int MADE_TO_ORDER_MAX = 10;

	public record ChosenOption(String group, String label, BigDecimal priceDelta) {
	}

	public record PricedLine(Product product, List<ChosenOption> options, BigDecimal unitPrice, int quantity,
			int maxQuantity, String problem) {

		public BigDecimal lineTotal() {
			return Money.normalize(unitPrice.multiply(BigDecimal.valueOf(quantity)));
		}

		public boolean ok() {
			return problem == null;
		}

		public String optionsSummary() {
			return options.isEmpty() ? null
					: options.stream().map(o -> o.group() + ": " + o.label()).collect(Collectors.joining(" · "));
		}
	}

	public PricedLine price(Product product, Long[] optionValueIds, int quantity, InventoryItem inventory) {
		Optional<List<ChosenOption>> options = resolveOptions(product, optionValueIds);
		BigDecimal unit = product.effectivePrice()
			.add(options.orElse(List.of()).stream().map(ChosenOption::priceDelta).reduce(BigDecimal.ZERO, BigDecimal::add));
		int max = maxQuantity(product, inventory);
		String problem = null;
		if (!product.isPubliclyVisible() || !product.getCategory().isActive()) {
			problem = "Produto indisponível no momento.";
		}
		else if (product.getType() == Product.Type.CUSTOM) {
			problem = "Produto personalizado: solicite um orçamento.";
		}
		else if (options.isEmpty()) {
			problem = "Escolha uma opção válida para cada característica do produto.";
		}
		else if (max == 0) {
			problem = "Sem estoque.";
		}
		else if (quantity > max) {
			problem = "Quantidade indisponível (máximo " + max + ").";
		}
		return new PricedLine(product, options.orElse(List.of()), Money.normalize(unit), quantity, max, problem);
	}

	static int maxQuantity(Product product, InventoryItem inventory) {
		return switch (product.getType()) {
			case READY -> inventory == null ? 0 : Math.min(inventory.available(), 99);
			case MADE_TO_ORDER -> MADE_TO_ORDER_MAX;
			case CUSTOM -> 0;
		};
	}

	/** Exige exatamente um valor por grupo de opção do produto; ids de outros produtos são recusados. */
	static Optional<List<ChosenOption>> resolveOptions(Product product, Long[] optionValueIds) {
		Set<Long> ids = new HashSet<>(Arrays.asList(optionValueIds == null ? new Long[0] : optionValueIds));
		if (ids.size() != (optionValueIds == null ? 0 : optionValueIds.length)) {
			return Optional.empty();
		}
		Map<Long, ProductOptionValue> byId = product.getOptions()
			.stream()
			.flatMap(o -> o.getValues().stream())
			.collect(Collectors.toMap(ProductOptionValue::getId, v -> v));
		if (!byId.keySet().containsAll(ids)) {
			return Optional.empty();
		}
		List<ChosenOption> chosen = new ArrayList<>();
		for (ProductOption group : product.getOptions()) {
			List<ProductOptionValue> picked = group.getValues().stream().filter(v -> ids.contains(v.getId())).toList();
			if (picked.size() != 1) {
				return Optional.empty();
			}
			ProductOptionValue v = picked.getFirst();
			chosen.add(new ChosenOption(group.getName(), v.getLabel(), v.getPriceDelta()));
		}
		return Optional.of(chosen);
	}

}
