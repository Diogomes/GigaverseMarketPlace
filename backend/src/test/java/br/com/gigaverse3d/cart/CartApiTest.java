package br.com.gigaverse3d.cart;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductOption;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Carrinho: preços sempre do servidor, opções com acréscimo, limites de quantidade e isolamento entre contas. */
class CartApiTest extends PostgresIntegrationTest {

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
	ProductRepository products;

	@Autowired
	TransactionTemplate tx;

	MockHttpSession session;

	Category category;

	@BeforeEach
	void setUp() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		session = loggedIn(mvc, json, email);
		category = fixtures.category(CatalogFixtures.unique("cart"));
	}

	ResultActions add(MockHttpSession s, String slug, int quantity, List<Long> options) throws Exception {
		Map<String, Object> body = options == null ? Map.of("productSlug", slug, "quantity", quantity)
				: Map.of("productSlug", slug, "quantity", quantity, "optionValueIds", options);
		return mvc.perform(post("/api/v1/cart/items").session(s)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	long firstItemId(ResultActions result) throws Exception {
		JsonNode node = json.readTree(result.andReturn().getResponse().getContentAsString());
		return node.get("items").get(0).get("id").asLong();
	}

	@Test
	void requiresAuthentication() throws Exception {
		mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
	}

	@Test
	void addsSameLineTwiceSummingQuantityAndUsesServerPrice() throws Exception {
		Product p = fixtures.activeProduct(category, "Vaso Carrinho", "40.00", 10);
		add(session, p.getSlug(), 2, null).andExpect(status().isOk());
		add(session, p.getSlug(), 1, null).andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].quantity").value(3))
			.andExpect(jsonPath("$.items[0].unitPrice").value(40.00))
			.andExpect(jsonPath("$.subtotal").value(120.00))
			.andExpect(jsonPath("$.hasProblems").value(false));
	}

	@Test
	void usesPromotionalPriceAndOptionDeltas() throws Exception {
		Product p = fixtures.activeProduct(category, "Luminária Opções", "100.00", 10);
		List<Long> ids = tx.execute(s -> {
			Product managed = products.findById(p.getId()).orElseThrow();
			managed.setPromoPrice(new BigDecimal("80.00"));
			ProductOption color = managed.addOption("Cor");
			color.addValue("Branco", BigDecimal.ZERO);
			color.addValue("Dourado", new BigDecimal("15.50"));
			products.saveAndFlush(managed);
			return managed.getOptions().getFirst().getValues().stream().map(v -> v.getId()).toList();
		});

		// sem escolher a opção obrigatória → recusado
		add(session, p.getSlug(), 1, null).andExpect(status().isBadRequest());
		// opção de outro produto/inexistente → recusado
		add(session, p.getSlug(), 1, List.of(987_654_321L)).andExpect(status().isBadRequest());
		// duas opções do mesmo grupo → recusado
		add(session, p.getSlug(), 1, ids).andExpect(status().isBadRequest());

		add(session, p.getSlug(), 2, List.of(ids.get(1))).andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].unitPrice").value(95.50))
			.andExpect(jsonPath("$.items[0].lineTotal").value(191.00))
			.andExpect(jsonPath("$.items[0].options[0].label").value("Dourado"));
		// a mesma peça com outra opção vira outra linha
		add(session, p.getSlug(), 1, List.of(ids.get(0))).andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.subtotal").value(271.00));
	}

	@Test
	void updatesQuantityAndFlagsWhenAboveStock() throws Exception {
		Product p = fixtures.activeProduct(category, "Estoque Curto", "10.00", 3);
		long itemId = firstItemId(add(session, p.getSlug(), 1, null));

		mvc.perform(put("/api/v1/cart/items/{id}", itemId).session(session)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"quantity\":3}")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(3))
			.andExpect(jsonPath("$.hasProblems").value(false));

		mvc.perform(put("/api/v1/cart/items/{id}", itemId).session(session)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"quantity\":5}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].maxQuantity").value(3))
			.andExpect(jsonPath("$.items[0].problem").value(startsWith("Quantidade indisponível")))
			.andExpect(jsonPath("$.hasProblems").value(true));

		mvc.perform(put("/api/v1/cart/items/{id}", itemId).session(session)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"quantity\":0}")).andExpect(status().isBadRequest());
	}

	@Test
	void rejectsOutOfStockDraftAndCustomProducts() throws Exception {
		Product none = fixtures.activeProduct(category, "Esgotado", "10.00", 0);
		Product draft = fixtures.product(category, "Rascunho", "10.00", Product.Status.DRAFT, Product.Type.READY, 5);
		Product custom = fixtures.product(category, "Sob Medida", "10.00", Product.Status.ACTIVE, Product.Type.CUSTOM,
				0);
		add(session, none.getSlug(), 1, null).andExpect(status().isBadRequest());
		add(session, draft.getSlug(), 1, null).andExpect(status().isNotFound());
		add(session, custom.getSlug(), 1, null).andExpect(status().isBadRequest());
	}

	@Test
	void anotherCustomerCannotTouchMyCartItem() throws Exception {
		Product p = fixtures.activeProduct(category, "Item Privado", "10.00", 5);
		long itemId = firstItemId(add(session, p.getSlug(), 1, null));

		String other = newEmail();
		createUser(jdbc, encoder, other, "CUSTOMER");
		MockHttpSession otherSession = loggedIn(mvc, json, other);
		mvc.perform(put("/api/v1/cart/items/{id}", itemId).session(otherSession)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"quantity\":2}")).andExpect(status().isNotFound());
		mvc.perform(delete("/api/v1/cart/items/{id}", itemId).session(otherSession).with(csrf()))
			.andExpect(status().isNotFound());

		mvc.perform(get("/api/v1/cart").session(session)).andExpect(jsonPath("$.items[0].quantity").value(1));
		mvc.perform(delete("/api/v1/cart/items/{id}", itemId).session(session).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void mergeKeepsLargestQuantityAndDropsInvalidLines() throws Exception {
		Product p = fixtures.activeProduct(category, "Junta Carrinho", "10.00", 5);
		add(session, p.getSlug(), 2, null);
		String body = json.writeValueAsString(Map.of("items",
				List.of(Map.of("productSlug", p.getSlug(), "quantity", 1),
						Map.of("productSlug", "nao-existe-" + System.nanoTime(), "quantity", 1))));
		for (int i = 0; i < 2; i++) { // idempotente
			mvc.perform(post("/api/v1/cart/merge").session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].quantity").value(2));
		}
	}

	@Test
	void concurrentFirstAccessCreatesASingleCart() throws Exception {
		String email = newEmail();
		long userId = createUser(jdbc, encoder, email, "CUSTOMER");
		MockHttpSession fresh = loggedIn(mvc, json, email);
		int threads = 6;
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
		java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
		try {
			List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();
			for (int i = 0; i < threads; i++) {
				results.add(pool.submit(() -> {
					start.await();
					return mvc.perform(get("/api/v1/cart").session(fresh)).andReturn().getResponse().getStatus();
				}));
			}
			start.countDown();
			for (java.util.concurrent.Future<Integer> r : results) {
				org.assertj.core.api.Assertions.assertThat(r.get()).isEqualTo(200);
			}
		}
		finally {
			pool.shutdownNow();
		}
		org.assertj.core.api.Assertions
			.assertThat(jdbc.queryForObject("SELECT count(*) FROM carts WHERE user_id = ?", Integer.class, userId))
			.isEqualTo(1);
	}

	@Test
	void rejectsMutationWithoutCsrfToken() throws Exception {
		Product p = fixtures.activeProduct(category, "Sem CSRF", "10.00", 5);
		mvc.perform(post("/api/v1/cart/items").session(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("productSlug", p.getSlug(), "quantity", 1))))
			.andExpect(status().isForbidden());
	}

}
