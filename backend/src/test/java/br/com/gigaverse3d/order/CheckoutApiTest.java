package br.com.gigaverse3d.order;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.gigaverse3d.account.AuthTestSupport.CapturingEmailSender;
import br.com.gigaverse3d.cart.api.CartDtos.AddItemRequest;
import br.com.gigaverse3d.cart.service.CartService;
import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.inventory.domain.InsufficientStockException;
import br.com.gigaverse3d.order.api.OrderDtos.CheckoutRequest;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.service.OrderService;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class CheckoutApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	CatalogFixtures fixtures;

	@Autowired
	CartService carts;

	@Autowired
	OrderService orderService;

	@Autowired
	CapturingEmailSender emails;

	Category category;

	String email;

	long userId;

	MockHttpSession session;

	@BeforeEach
	void setUp() throws Exception {
		category = fixtures.category(CatalogFixtures.unique("chk"));
		email = newEmail();
		userId = createUser(jdbc, encoder, email, "CUSTOMER");
		session = loggedIn(mvc, json, email);
	}

	private ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
		return mvc.perform(req.session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	private void addToCart(MockHttpSession s, Product p, int qty) throws Exception {
		Map<String, Object> body = new HashMap<>(Map.of("productSlug", p.getSlug(), "quantity", qty));
		body.put("unitPrice", 0.01); // ignorado: o servidor não aceita preço do cliente
		send(post("/api/v1/cart/items"), s, body).andExpect(status().isOk());
	}

	private ResultActions checkout(MockHttpSession s, Map<String, Object> body, String key) throws Exception {
		return mvc.perform(post("/api/v1/checkout").session(s).with(csrf()).header("Idempotency-Key", key)
			.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
	}

	private static Map<String, Object> pickupPix() {
		return new HashMap<>(Map.of("shippingMethod", "PICKUP", "paymentMethod", "PIX"));
	}

	private int stockOf(Product p) {
		return jdbc.queryForObject("SELECT quantity_on_hand FROM inventory_items WHERE product_id = ?", Integer.class,
				p.getId());
	}

	private long addressId(MockHttpSession s, String cep) throws Exception {
		String body = send(post("/api/v1/account/addresses"), s, Map.of("label", "Casa", "recipientName", "Cliente Teste",
				"postalCode", cep, "street", "Rua A", "number", "10", "district", "Centro", "city", "Cidade", "state", "PE"))
			.andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("id").asLong();
	}

	// --- carrinho

	@Test
	void cartRecalculatesPricesOnServerAndRejectsCustomProducts() throws Exception {
		Product p = fixtures.activeProduct(category, "Vaso", "50.00", 5);
		addToCart(session, p, 2);
		mvc.perform(get("/api/v1/cart").session(session))
			.andExpect(jsonPath("$.subtotal").value(100.00))
			.andExpect(jsonPath("$.items[0].unitPrice").value(50.00))
			.andExpect(jsonPath("$.items[0].maxQuantity").value(5));

		Product custom = fixtures.product(category, "Sob medida", "80.00", Product.Status.ACTIVE, Product.Type.CUSTOM, 0);
		send(post("/api/v1/cart/items"), session, Map.of("productSlug", custom.getSlug(), "quantity", 1))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Produto personalizado: solicite um orçamento."));
	}

	@Test
	void mergingTheSameVisitorCartTwiceNeverDuplicates() throws Exception {
		Product p = fixtures.activeProduct(category, "Vaso", "50.00", 9);
		Map<String, Object> body = Map.of("items", List.of(Map.of("productSlug", p.getSlug(), "quantity", 2)));
		send(post("/api/v1/cart/merge"), session, body).andExpect(status().isOk());
		send(post("/api/v1/cart/merge"), session, body).andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].quantity").value(2))
			.andExpect(jsonPath("$.itemCount").value(2));
	}

	@Test
	void cartRequiresLogin() throws Exception {
		mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
	}

	// --- checkout

	@Test
	void checkoutComputesTotalsOnServerDecrementsStockAndSendsInstructions() throws Exception {
		Product p = fixtures.activeProduct(category, "Dragão", "100.00", 3);
		addToCart(session, p, 2);
		checkout(session, pickupPix(), UUID.randomUUID().toString()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.subtotal").value(200.00))
			.andExpect(jsonPath("$.shippingTotal").value(0))
			.andExpect(jsonPath("$.total").value(200.00))
			.andExpect(jsonPath("$.paymentStatus").value("PENDING"))
			.andExpect(jsonPath("$.paymentInstructions").value(org.hamcrest.Matchers.containsString("PIX")))
			.andExpect(jsonPath("$.canCancel").value(true));
		assertThat(stockOf(p)).isEqualTo(1);
		mvc.perform(get("/api/v1/cart").session(session)).andExpect(jsonPath("$.items.length()").value(0));
		assertThat(emails.to(email)).anyMatch(m -> m.subject().contains("recebido") && m.textBody().contains("PIX"));
		assertThat(jdbc.queryForObject("""
				SELECT count(*) FROM payments p JOIN orders o ON o.id = p.order_id
				WHERE o.user_id = ? AND p.card_last4 IS NULL AND p.provider = 'MANUAL'""", Integer.class, userId))
			.isEqualTo(1);
	}

	@Test
	void deliveryUsesConfiguredZoneAndKeepsAddressSnapshot() throws Exception {
		Product p = fixtures.activeProduct(category, "Luminária", "80.00", 2);
		addToCart(session, p, 1);
		long address = addressId(session, "53625816");
		mvc.perform(get("/api/v1/checkout/shipping-options").param("addressId", String.valueOf(address)).session(session))
			.andExpect(jsonPath("$.originPostalCode").value("53625816"))
			.andExpect(jsonPath("$.options[?(@.method == 'DELIVERY')].price").value(15.00));

		Map<String, Object> body = new HashMap<>(Map.of("shippingMethod", "DELIVERY", "addressId", address,
				"paymentMethod", "CARD", "expectedTotal", new BigDecimal("95.00")));
		String created = checkout(session, body, UUID.randomUUID().toString()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.total").value(95.00))
			.andExpect(jsonPath("$.shippingAddress.postalCode").value("53625816"))
			.andReturn().getResponse().getContentAsString();
		long number = json.readTree(created).get("number").asLong();

		send(put("/api/v1/account/addresses/{id}", address), session, Map.of("label", "Casa", "recipientName",
				"Outra Pessoa", "postalCode", "53625816", "street", "Rua Nova", "number", "99", "district", "Centro",
				"city", "Cidade", "state", "PE")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/account/orders/{n}", number).session(session))
			.andExpect(jsonPath("$.shippingAddress.street").value("Rua A"));
	}

	@Test
	void deliveryOutsideConfiguredZonesIsRejected() throws Exception {
		addToCart(session, fixtures.activeProduct(category, "Item", "10.00", 2), 1);
		long address = addressId(session, "01001000");
		checkout(session, Map.of("shippingMethod", "DELIVERY", "addressId", address, "paymentMethod", "PIX"),
				UUID.randomUUID().toString())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("shippingMethod"));
	}

	@Test
	void sameIdempotencyKeyCreatesOneOrder() throws Exception {
		addToCart(session, fixtures.activeProduct(category, "Item", "10.00", 5), 1);
		String key = UUID.randomUUID().toString();
		String first = checkout(session, pickupPix(), key).andExpect(status().isCreated()).andReturn().getResponse()
			.getContentAsString();
		String second = checkout(session, pickupPix(), key).andExpect(status().isCreated()).andReturn().getResponse()
			.getContentAsString();
		assertThat(json.readTree(second).get("number").asLong()).isEqualTo(json.readTree(first).get("number").asLong());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orders WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);
	}

	@Test
	void missingIdempotencyKeyIsRejected() throws Exception {
		addToCart(session, fixtures.activeProduct(category, "Item", "10.00", 5), 1);
		mvc.perform(post("/api/v1/checkout").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(pickupPix()))).andExpect(status().isBadRequest());
	}

	@Test
	void changedPriceWithExpectedTotalIsConflictAndNothingIsCharged() throws Exception {
		Product p = fixtures.activeProduct(category, "Item", "10.00", 5);
		addToCart(session, p, 1);
		jdbc.update("UPDATE products SET price = 12.00 WHERE id = ?", p.getId());
		Map<String, Object> body = pickupPix();
		body.put("expectedTotal", new BigDecimal("10.00"));
		checkout(session, body, UUID.randomUUID().toString()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("R$ 12,00")));
		assertThat(stockOf(p)).isEqualTo(5);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orders WHERE user_id = ?", Integer.class, userId)).isZero();
	}

	@Test
	void inactiveProductBlocksCheckout() throws Exception {
		Product p = fixtures.activeProduct(category, "Item", "10.00", 5);
		addToCart(session, p, 1);
		jdbc.update("UPDATE products SET status = 'ARCHIVED' WHERE id = ?", p.getId());
		checkout(session, pickupPix(), UUID.randomUUID().toString()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("indisponível")));
	}

	@Test
	void emptyCartIsConflict() throws Exception {
		checkout(session, pickupPix(), UUID.randomUUID().toString()).andExpect(status().isConflict());
	}

	/** 8 clientes disputam a última unidade ao mesmo tempo: só 1 pedido, estoque 0, nenhum pedido órfão. */
	@Test
	void concurrentCheckoutsForLastUnitNeverOversell() throws Exception {
		Product p = fixtures.activeProduct(category, "Última unidade", "10.00", 1);
		List<Long> buyers = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			long id = createUser(jdbc, encoder, newEmail(), "CUSTOMER");
			carts.add(id, new AddItemRequest(p.getSlug(), List.of(), 1));
			buyers.add(id);
		}
		ExecutorService pool = Executors.newFixedThreadPool(buyers.size());
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		for (Long buyer : buyers) {
			results.add(pool.submit(() -> {
				start.await();
				try {
					orderService.checkout(buyer, new CheckoutRequest(Order.ShippingMethod.PICKUP, null,
							Order.PaymentMethod.PIX, null, null), UUID.randomUUID().toString());
					return true;
				}
				catch (InsufficientStockException | ConflictException e) {
					return false;
				}
			}));
		}
		start.countDown();
		int ok = 0;
		for (Future<Boolean> r : results) {
			if (r.get(60, TimeUnit.SECONDS)) {
				ok++;
			}
		}
		pool.shutdown();
		assertThat(ok).isEqualTo(1);
		assertThat(stockOf(p)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM order_items WHERE product_id = ?", Integer.class, p.getId()))
			.isEqualTo(1);
	}

	// --- ciclo de vida

	@Test
	void customerCancelReturnsStockAndCannotCancelAfterPayment() throws Exception {
		Product p = fixtures.activeProduct(category, "Item", "10.00", 3);
		addToCart(session, p, 2);
		long number = json.readTree(checkout(session, pickupPix(), UUID.randomUUID().toString()).andReturn()
			.getResponse().getContentAsString()).get("number").asLong();
		assertThat(stockOf(p)).isEqualTo(1);

		mvc.perform(post("/api/v1/account/orders/{n}/cancel", number).session(session).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"))
			.andExpect(jsonPath("$.paymentStatus").value("CANCELLED"));
		assertThat(stockOf(p)).isEqualTo(3);
		mvc.perform(post("/api/v1/account/orders/{n}/cancel", number).session(session).with(csrf()))
			.andExpect(status().isConflict());
	}

	@Test
	void customersOnlySeeTheirOwnOrders() throws Exception {
		addToCart(session, fixtures.activeProduct(category, "Item", "10.00", 3), 1);
		long number = json.readTree(checkout(session, pickupPix(), UUID.randomUUID().toString()).andReturn()
			.getResponse().getContentAsString()).get("number").asLong();
		String other = newEmail();
		createUser(jdbc, encoder, other, "CUSTOMER");
		MockHttpSession otherSession = loggedIn(mvc, json, other);
		mvc.perform(get("/api/v1/account/orders/{n}", number).session(otherSession)).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/account/orders/{n}/cancel", number).session(otherSession).with(csrf()))
			.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/account/orders").session(otherSession)).andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/v1/account/orders").session(session)).andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void customerOrdersCanBeFilteredByStatusGroup() throws Exception {
		Product p = fixtures.activeProduct(category, "Item", "10.00", 5);
		addToCart(session, p, 1);
		long first = json.readTree(checkout(session, pickupPix(), UUID.randomUUID().toString()).andReturn()
			.getResponse().getContentAsString()).get("number").asLong();
		addToCart(session, p, 1);
		checkout(session, pickupPix(), UUID.randomUUID().toString()).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/account/orders/{n}/cancel", first).session(session).with(csrf()));

		mvc.perform(get("/api/v1/account/orders").param("group", "ACTIVE").session(session))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/account/orders").param("group", "CANCELLED").session(session))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].number").value(first));
		mvc.perform(get("/api/v1/account/orders").param("group", "INVALIDO").session(session))
			.andExpect(status().isBadRequest());
	}

	@Test
	void staffTransitionsFollowTheRules() throws Exception {
		Product p = fixtures.activeProduct(category, "Item", "10.00", 5);
		addToCart(session, p, 1);
		long number = json.readTree(checkout(session, pickupPix(), UUID.randomUUID().toString()).andReturn()
			.getResponse().getContentAsString()).get("number").asLong();
		String opEmail = newEmail();
		String adminEmail = newEmail();
		createUser(jdbc, encoder, opEmail, "OPERATOR");
		createUser(jdbc, encoder, adminEmail, "ADMIN");
		MockHttpSession operator = loggedIn(mvc, json, opEmail);
		MockHttpSession admin = loggedIn(mvc, json, adminEmail);
		String base = "/api/v1/admin/orders/" + number;

		send(post(base + "/status"), operator, Map.of("status", "IN_PRODUCTION")).andExpect(status().isConflict());
		send(post(base + "/status"), operator, Map.of("status", "PAID")).andExpect(status().isBadRequest());
		send(post(base + "/confirm-payment"), operator, Map.of()).andExpect(status().isForbidden());
		send(post(base + "/confirm-payment"), session, Map.of()).andExpect(status().isForbidden());
		send(post(base + "/confirm-payment"), admin, Map.of("reference", "PIX-123"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"))
			.andExpect(jsonPath("$.paymentStatus").value("PAID"));

		mvc.perform(post("/api/v1/account/orders/{n}/cancel", number).session(session).with(csrf()))
			.andExpect(status().isConflict());
		send(post(base + "/status"), operator, Map.of("status", "SHIPPED")).andExpect(status().isBadRequest());
		send(post(base + "/status"), operator, Map.of("status", "IN_PRODUCTION")).andExpect(status().isOk());
		send(post(base + "/status"), operator, Map.of("status", "CANCELLED", "reason", "Erro"))
			.andExpect(status().isConflict());
		send(post(base + "/status"), admin, Map.of("status", "CANCELLED")).andExpect(status().isBadRequest());
		send(post(base + "/status"), admin, Map.of("status", "CANCELLED", "reason", "Cliente desistiu"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.history[-1].note").value(org.hamcrest.Matchers.containsString("Estorno")));
		assertThat(stockOf(p)).isEqualTo(5);
		assertThat(jdbc.queryForList("SELECT action FROM audit_log WHERE entity_type = 'order' AND entity_id = ?",
				String.class, String.valueOf(number))).contains("ORDER_PLACED", "PAYMENT_CONFIRMED", "ORDER_CANCELLED");
	}

	@Test
	void adminDetailShowsWhoChangedAndOnlyValidNextSteps() throws Exception {
		Product p = fixtures.activeProduct(category, "Item", "10.00", 5);
		addToCart(session, p, 1);
		long number = json.readTree(checkout(session, pickupPix(), UUID.randomUUID().toString()).andReturn()
			.getResponse().getContentAsString()).get("number").asLong();
		String adminEmail = newEmail();
		createUser(jdbc, encoder, adminEmail, "ADMIN");
		jdbc.update("UPDATE users SET full_name = 'Admin Conferente' WHERE email = ?", adminEmail);
		MockHttpSession admin = loggedIn(mvc, json, adminEmail);
		String base = "/api/v1/admin/orders/" + number;

		mvc.perform(get(base).session(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.canConfirmPayment").value(true))
			.andExpect(jsonPath("$.allowedNext").value(org.hamcrest.Matchers.contains("CANCELLED")));
		send(post(base + "/confirm-payment"), admin, Map.of("reference", "PIX-9")).andExpect(status().isOk());
		mvc.perform(get(base).session(admin))
			.andExpect(jsonPath("$.canConfirmPayment").value(false))
			.andExpect(jsonPath("$.paidAt").isNotEmpty())
			// retirada: sem "enviado"
			.andExpect(jsonPath("$.allowedNext").value(
					org.hamcrest.Matchers.contains("IN_PRODUCTION", "READY_FOR_PICKUP", "CANCELLED")))
			.andExpect(jsonPath("$.history[-1].to").value("PAID"))
			.andExpect(jsonPath("$.history[-1].actorName").value("Admin Conferente"));

		mvc.perform(get("/api/v1/admin/orders").param("customerId", String.valueOf(userId)).session(admin))
			.andExpect(jsonPath("$.content[*].number").value(org.hamcrest.Matchers.hasItem((int) number)));
		mvc.perform(get("/api/v1/admin/orders").param("customerId", "999999999").session(admin))
			.andExpect(jsonPath("$.totalElements").value(0));

		String csv = mvc.perform(get("/api/v1/admin/orders/export").param("customerId", String.valueOf(userId))
			.session(admin)).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
		assertThat(csv).contains("numero;criado_em;status").contains(number + ";").doesNotContain("Rua");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'ORDERS_EXPORTED'", Integer.class))
			.isPositive();
	}

	@Test
	void unpaidOrdersExpireAndReturnStock() throws Exception {
		Product p = fixtures.activeProduct(category, "Item", "10.00", 2);
		addToCart(session, p, 2);
		long number = json.readTree(checkout(session, pickupPix(), UUID.randomUUID().toString()).andReturn()
			.getResponse().getContentAsString()).get("number").asLong();
		jdbc.update("UPDATE orders SET created_at = now() - interval '4 days' WHERE number = ?", number);
		assertThat(orderService.expireUnpaid()).isGreaterThanOrEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE number = ?", String.class, number))
			.isEqualTo("CANCELLED");
		assertThat(stockOf(p)).isEqualTo(2);
	}

}
