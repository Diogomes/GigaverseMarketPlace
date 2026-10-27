package br.com.gigaverse3d.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.repository.CategoryRepository;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.inventory.domain.InsufficientStockException;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.inventory.domain.StockMovement;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;
import br.com.gigaverse3d.inventory.repository.StockMovementRepository;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;
import br.com.gigaverse3d.order.domain.ShippingAddress;
import br.com.gigaverse3d.order.repository.OrderRepository;

/** Mapeamento JPA ↔ schema: grava e relê os agregados principais. */
@Transactional
class PersistenceIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	EntityManager em;

	@Autowired
	UserRepository users;

	@Autowired
	CategoryRepository categories;

	@Autowired
	ProductRepository products;

	@Autowired
	InventoryItemRepository inventory;

	@Autowired
	StockMovementRepository movements;

	@Autowired
	OrderRepository orders;

	private Product newProduct(String slug, String sku, String name) {
		Category category = categories.findBySlug("decoracao-teste")
			.orElseGet(() -> categories.save(new Category("decoracao-teste", "Decoração", null)));
		Product product = new Product(slug, sku, name, "Peça impressa em 3D para decorar o ambiente.", category,
				Product.Type.READY, new BigDecimal("159.9"));
		product.setStatus(Product.Status.ACTIVE);
		return products.save(product);
	}

	private User newCustomer() {
		return users.save(new User("  Ana@Exemplo.com ", "$argon2id$fake-hash", "Ana Teste", User.Role.CUSTOMER));
	}

	@Test
	void productWithOptionsAndInventoryRoundTrips() {
		Product product = newProduct("luminaria-teste", "GV-T-001", "Luminária Teste");
		product.addOption("Tamanho").addValue("15 cm", new BigDecimal("30"));
		InventoryItem item = inventory.save(new InventoryItem(product, 4));
		movements.save(item.apply(StockMovement.Type.INITIAL, 10, "Saldo inicial"));
		em.flush();
		em.clear();

		Product loaded = products.findBySlugAndStatus("luminaria-teste", Product.Status.ACTIVE).orElseThrow();
		assertThat(loaded.getPrice()).isEqualByComparingTo("159.90");
		assertThat(loaded.getPrice().scale()).isEqualTo(Money.SCALE);
		assertThat(loaded.getOptions()).singleElement()
			.satisfies(o -> assertThat(o.getValues()).extracting("label").containsExactly("15 cm"));
		assertThat(inventory.findById(loaded.getId()).orElseThrow().getQuantityOnHand()).isEqualTo(10);
		assertThat(movements.findByProductIdOrderByCreatedAtDesc(loaded.getId())).hasSize(1);
	}

	@Test
	void inventoryRefusesToGoNegative() {
		InventoryItem item = inventory.save(new InventoryItem(newProduct("vaso-teste", "GV-T-002", "Vaso"), 1));
		item.apply(StockMovement.Type.INITIAL, 2, null);
		assertThatThrownBy(() -> item.apply(StockMovement.Type.SALE, -3, null))
			.isInstanceOf(InsufficientStockException.class);
		assertThat(item.getQuantityOnHand()).isEqualTo(2);
	}

	@Test
	void fullTextSearchFindsActiveProductsInPortuguese() {
		Product active = newProduct("luminaria-lua-teste", "GV-T-003", "Luminária Lua");
		Product draft = newProduct("luminaria-rascunho", "GV-T-004", "Luminária Rascunho");
		draft.setStatus(Product.Status.DRAFT);
		em.flush();

		// o banco é compartilhado com outras classes de teste: verifica só os próprios registros
		assertThat(products.searchActiveIds("luminárias", 500)).contains(active.getId()).doesNotContain(draft.getId());
	}

	@Test
	void userEmailIsNormalized() {
		User saved = newCustomer();
		em.flush();
		assertThat(users.findByEmailAndDeletedAtIsNull("ana@exemplo.com")).contains(saved);
	}

	@Test
	void orderKeepsSnapshotsTotalsAndHistory() {
		User customer = newCustomer();
		Product product = newProduct("dragao-teste", "GV-T-005", "Dragão");
		ShippingAddress address = new ShippingAddress("Ana Teste", "53625816", "Rua Exemplo", "123", null, "Centro",
				"Cidade Exemplo", "PE");
		Order order = new Order(customer.getId(), customer.getFullName(), customer.getEmail(), Order.PaymentMethod.PIX,
				Order.ShippingMethod.DELIVERY, address, "idem-1");
		order.addItem(product.getId(), product.getName(), product.getSku(), "Tamanho: 15 cm",
				product.effectivePrice().add(new BigDecimal("30")), 2);
		order.setShippingTotal(new BigDecimal("24.90"));
		orders.save(order);
		em.flush();
		em.clear();

		Order loaded = orders.findByUserIdAndIdempotencyKey(customer.getId(), "idem-1").orElseThrow();
		assertThat(loaded.getNumber()).isGreaterThanOrEqualTo(1000L);
		assertThat(loaded.getSubtotal()).isEqualByComparingTo("379.80");
		assertThat(loaded.getTotal()).isEqualByComparingTo("404.70");
		assertThat(loaded.getItems()).singleElement().satisfies(i -> {
			assertThat(i.getProductName()).isEqualTo("Dragão");
			assertThat(i.getLineTotal()).isEqualByComparingTo("379.80");
		});
		assertThat(loaded.getShippingAddress().postalCode()).isEqualTo("53625816");
		assertThat(loaded.getHistory()).extracting("toStatus").containsExactly(OrderStatus.PENDING_PAYMENT);

		// Snapshot: mudar o produto depois não altera o pedido.
		Product changed = products.findById(product.getId()).orElseThrow();
		changed.setPromoPrice(new BigDecimal("99.90"));
		em.flush();
		em.clear();
		assertThat(orders.findById(loaded.getId()).orElseThrow().getItems().getFirst().getUnitPrice())
			.isEqualByComparingTo("189.90");
	}

	@Test
	void ownershipFilterPreventsReadingAnotherCustomersOrder() {
		User owner = newCustomer();
		Order order = new Order(owner.getId(), owner.getFullName(), owner.getEmail(), Order.PaymentMethod.CARD,
				Order.ShippingMethod.PICKUP, null, null);
		order.addItem(null, "Item", "GV-T-006", null, new BigDecimal("10"), 1);
		orders.save(order);
		em.flush();
		em.refresh(order);

		assertThat(orders.findByNumberAndUserId(order.getNumber(), owner.getId())).isPresent();
		assertThat(orders.findByNumberAndUserId(order.getNumber(), owner.getId() + 999)).isEmpty();
	}

	@Test
	void statusTransitionsArePersistedInHistory() {
		User owner = newCustomer();
		Order order = new Order(owner.getId(), owner.getFullName(), owner.getEmail(), Order.PaymentMethod.PIX,
				Order.ShippingMethod.PICKUP, null, null);
		order.addItem(null, "Item", "GV-T-007", null, new BigDecimal("10"), 1);
		order.transitionTo(OrderStatus.PAID, null, "Confirmado pelo provedor");
		order.transitionTo(OrderStatus.READY_FOR_PICKUP, null, null);
		orders.save(order);
		em.flush();
		em.clear();

		Order loaded = orders.findById(order.getId()).orElseThrow();
		assertThat(loaded.getPaidAt()).isNotNull();
		assertThat(loaded.getHistory()).extracting("toStatus")
			.containsExactly(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.READY_FOR_PICKUP);
	}

}
