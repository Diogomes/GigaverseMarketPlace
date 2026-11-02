package br.com.gigaverse3d.order;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jornada completa só pela API HTTP, com dados fictícios: o admin cadastra o produto (com opções e promoção)
 * e lança estoque; o cliente compra com entrega e cartão; o admin confirma o pagamento e leva o pedido até a
 * entrega. Verifica que todos os módulos (catálogo, estoque, carrinho, checkout, frete, pedidos, e-mail,
 * auditoria) conversam entre si — cada um já tem testes próprios.
 */
class PurchaseJourneyApiTest extends PostgresIntegrationTest {

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
	CapturingEmailSender emails;

	MockHttpSession admin;

	MockHttpSession customer;

	String customerEmail;

	Category category;

	@BeforeEach
	void setUp() throws Exception {
		String adminEmail = newEmail();
		createUser(jdbc, encoder, adminEmail, "ADMIN");
		admin = loggedIn(mvc, json, adminEmail);
		customerEmail = newEmail();
		createUser(jdbc, encoder, customerEmail, "CUSTOMER");
		customer = loggedIn(mvc, json, customerEmail);
		category = fixtures.category(CatalogFixtures.unique("jornada"));
	}

	ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
		return mvc.perform(req.session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	JsonNode body(ResultActions r) throws Exception {
		return json.readTree(r.andReturn().getResponse().getContentAsString());
	}

	/** Admin cadastra "Dragão Fictício" R$ 200 (promo 180) com Tamanho P (+0) / G (+40) e 4 unidades. */
	JsonNode adminCreatesProduct() throws Exception {
		JsonNode product = body(send(post("/api/v1/admin/products"), admin,
				Map.of("name", "Dragão Fictício " + CatalogFixtures.unique("j"), "sku", CatalogFixtures.uniqueSku().replace("T-", "GV-"),
						"description", "Produto fictício para o teste da jornada completa.", "categoryId",
						category.getId(), "type", "READY", "status", "ACTIVE", "price", new BigDecimal("200.00"),
						"promoPrice", new BigDecimal("180.00"), "lowStockThreshold", 1, "options",
						List.of(Map.of("name", "Tamanho", "values",
								List.of(Map.of("label", "P", "priceDelta", 0),
										Map.of("label", "G", "priceDelta", 40))))))
			.andExpect(status().isCreated()));
		long id = product.get("id").asLong();
		send(post("/api/v1/admin/products/{id}/stock-movements", id), admin,
				Map.of("type", "PURCHASE", "quantity", 4, "reason", "Nota fiscal fictícia 0001"))
			.andExpect(status().isCreated());
		return body(mvc.perform(get("/api/v1/admin/products/{id}", id).session(admin)).andExpect(status().isOk()));
	}

	@Test
	void fullPurchaseFromCatalogToDelivery() throws Exception {
		JsonNode product = adminCreatesProduct();
		String slug = product.get("slug").asText();
		long productId = product.get("id").asLong();
		long large = product.get("options").get(0).get("values").get(1).get("id").asLong();
		assertThat(product.get("stock").get("available").asInt()).isEqualTo(4);

		// vitrine: produto público com preço promocional e sem expor o estoque exato
		mvc.perform(get("/api/v1/public/products/{slug}", slug))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.price").value(200.00))
			.andExpect(jsonPath("$.promoPrice").value(180.00))
			.andExpect(jsonPath("$.availability").value("IN_STOCK"))
			.andExpect(jsonPath("$.stock").doesNotExist());

		// carrinho: 2 × (180 + 40) = 440
		send(post("/api/v1/cart/items"), customer,
				Map.of("productSlug", slug, "quantity", 2, "optionValueIds", List.of(large)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].unitPrice").value(220.00))
			.andExpect(jsonPath("$.subtotal").value(440.00));

		// endereço em Pernambuco → frete da faixa de teste (15,00)
		long addressId = body(send(post("/api/v1/account/addresses"), customer,
				Map.of("label", "Casa", "recipientName", "Cliente Fictício", "postalCode", "50010000", "street",
						"Rua da Jornada", "number", "42", "district", "Centro", "city", "Recife", "state", "PE")))
			.get("id").asLong();
		mvc.perform(get("/api/v1/checkout/shipping-options").param("addressId", String.valueOf(addressId))
			.session(customer)).andExpect(jsonPath("$.options[?(@.method == 'DELIVERY')].price").value(15.00));

		JsonNode order = body(mvc.perform(post("/api/v1/checkout").session(customer).with(csrf())
			.header("Idempotency-Key", UUID.randomUUID().toString())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("shippingMethod", "DELIVERY", "addressId", addressId,
					"paymentMethod", "CARD", "expectedTotal", new BigDecimal("455.00")))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.subtotal").value(440.00))
			.andExpect(jsonPath("$.shippingTotal").value(15.00))
			.andExpect(jsonPath("$.total").value(455.00))
			.andExpect(jsonPath("$.items[0].optionsSummary").value("Tamanho: G")));
		long number = order.get("number").asLong();

		// estoque: 4 − 2 = 2, com movimento de venda ligado ao pedido
		assertThat(body(mvc.perform(get("/api/v1/admin/products/{id}", productId).session(admin))).get("stock")
			.get("available").asInt()).isEqualTo(2);
		assertThat(jdbc.queryForObject("""
				SELECT count(*) FROM stock_movements m JOIN orders o ON o.id = m.order_id
				WHERE m.product_id = ? AND o.number = ? AND m.movement_type = 'SALE' AND m.quantity_delta = -2""",
				Integer.class, productId, number)).isEqualTo(1);

		// o admin enxerga o pedido na lista filtrada e confirma o pagamento
		mvc.perform(get("/api/v1/admin/orders").param("status", "PENDING_PAYMENT").param("q", String.valueOf(number))
			.session(admin)).andExpect(jsonPath("$.content[0].number").value(number));
		String base = "/api/v1/admin/orders/" + number;
		send(post(base + "/confirm-payment"), admin, Map.of("reference", "CARTAO-FICTICIO-123"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PAID"));

		// produção → envio com rastreio → entrega
		send(post(base + "/status"), admin, Map.of("status", "IN_PRODUCTION")).andExpect(status().isOk());
		send(post(base + "/status"), admin, Map.of("status", "SHIPPED", "trackingCode", "BR123456789TS"))
			.andExpect(status().isOk());
		send(post(base + "/status"), admin, Map.of("status", "DELIVERED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("DELIVERED"));
		mvc.perform(get(base).session(admin))
			.andExpect(jsonPath("$.allowedNext").isEmpty())
			.andExpect(jsonPath("$.history[*].to").value(
					contains("PENDING_PAYMENT", "PAID", "IN_PRODUCTION", "SHIPPED", "DELIVERED")));

		// o cliente acompanha a mesma história
		mvc.perform(get("/api/v1/account/orders/{n}", number).session(customer))
			.andExpect(jsonPath("$.status").value("DELIVERED"))
			.andExpect(jsonPath("$.canCancel").value(false))
			.andExpect(jsonPath("$.history[*].status").value(
					contains("PENDING_PAYMENT", "PAID", "IN_PRODUCTION", "SHIPPED", "DELIVERED")));

		// e-mails ao cliente e trilha de auditoria
		assertThat(emails.to(customerEmail)).extracting(m -> m.subject()).anyMatch(s -> s.contains("recebido"));
		assertThat(jdbc.queryForList("SELECT action FROM audit_log WHERE entity_type = 'order' AND entity_id = ?",
				String.class, String.valueOf(number))).contains("ORDER_PLACED", "PAYMENT_CONFIRMED");
		assertThat(jdbc.queryForList("SELECT action FROM audit_log WHERE entity_type = 'product' AND entity_id = ?",
				String.class, String.valueOf(productId))).isNotEmpty();
	}

	@Test
	void stockRunsOutAcrossCustomersAndArchivedProductLeavesTheStore() throws Exception {
		JsonNode product = adminCreatesProduct();
		String slug = product.get("slug").asText();
		long productId = product.get("id").asLong();
		long small = product.get("options").get(0).get("values").get(0).get("id").asLong();

		// cliente 1 leva todas as 4 unidades
		send(post("/api/v1/cart/items"), customer,
				Map.of("productSlug", slug, "quantity", 4, "optionValueIds", List.of(small)))
			.andExpect(status().isOk());
		mvc.perform(post("/api/v1/checkout").session(customer).with(csrf())
			.header("Idempotency-Key", UUID.randomUUID().toString())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("shippingMethod", "PICKUP", "paymentMethod", "PIX"))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.total").value(720.00));

		// vitrine passa a "esgotado" e o cliente 2 não consegue adicionar
		mvc.perform(get("/api/v1/public/products/{slug}", slug)).andExpect(jsonPath("$.availability")
			.value("OUT_OF_STOCK"));
		String other = newEmail();
		createUser(jdbc, encoder, other, "CUSTOMER");
		MockHttpSession second = loggedIn(mvc, json, other);
		send(post("/api/v1/cart/items"), second,
				Map.of("productSlug", slug, "quantity", 1, "optionValueIds", List.of(small)))
			.andExpect(status().isBadRequest());

		// admin repõe 1 unidade → volta a vender; depois arquiva → some da loja
		send(post("/api/v1/admin/products/{id}/stock-movements", productId), admin,
				Map.of("type", "PURCHASE", "quantity", 1, "reason", "Reposição fictícia"))
			.andExpect(status().isCreated());
		mvc.perform(get("/api/v1/public/products/{slug}", slug)).andExpect(jsonPath("$.availability")
			.value("IN_STOCK"));
		long version = body(mvc.perform(get("/api/v1/admin/products/{id}", productId).session(admin))).get("version")
			.asLong();
		send(post("/api/v1/admin/products/{id}/status", productId), admin,
				Map.of("status", "ARCHIVED", "version", version))
			.andExpect(status().isOk());
		mvc.perform(get("/api/v1/public/products/{slug}", slug)).andExpect(status().isNotFound());
	}

}
