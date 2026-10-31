package br.com.gigaverse3d.catalog;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.common.audit.AuditLogRepository;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class AdminCatalogApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	CatalogFixtures fixtures;

	@Autowired
	JsonMapper json;

	@Autowired
	AuditLogRepository auditLog;

	@Autowired
	JdbcTemplate jdbc;

	Category category;

	@BeforeEach
	void setUp() {
		category = fixtures.category(CatalogFixtures.unique("adm"));
	}

	private Map<String, Object> validProduct(String sku) {
		Map<String, Object> body = productWithoutSlug(sku);
		body.put("slug", CatalogFixtures.unique("dragao"));
		return body;
	}

	private Map<String, Object> productWithoutSlug(String sku) {
		return new java.util.HashMap<>(Map.of("name", "Dragão Mecânico Azul", "sku", sku, "description",
				"Escultura de dragão mecânico com placas sobrepostas.", "categoryId", category.getId(), "type", "READY",
				"status", "ACTIVE", "price", new BigDecimal("349.90"), "promoPrice", new BigDecimal("319.90"),
				"lowStockThreshold", 3, "options",
				List.of(Map.of("name", "Tamanho", "values",
						List.of(Map.of("label", "10 cm", "priceDelta", 0), Map.of("label", "20 cm", "priceDelta", 60))))));
	}

	private ResultActions postProduct(Map<String, Object> body) throws Exception {
		return mvc.perform(post("/api/v1/admin/products").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	// --- autorização

	@Test
	void anonymousGets401() throws Exception {
		mvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "CUSTOMER")
	void customerGets403() throws Exception {
		mvc.perform(get("/api/v1/admin/products")).andExpect(status().isForbidden());
		postProduct(validProduct(CatalogFixtures.uniqueSku())).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void writesWithoutCsrfTokenAreRejected() throws Exception {
		mvc.perform(post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(validProduct(CatalogFixtures.uniqueSku()))))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "operador@exemplo.com", roles = "OPERATOR")
	void operatorCanCreateButNotDelete() throws Exception {
		String body = postProduct(validProduct(CatalogFixtures.uniqueSku())).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		long id = json.readTree(body).get("id").asLong();
		mvc.perform(delete("/api/v1/admin/products/{id}", id).with(csrf())).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "operador@exemplo.com", roles = "OPERATOR")
	void archiveAndReactivateChecksVersionAndIsAudited() throws Exception {
		Product p = fixtures.activeProduct(category, "Arquivável", "20.00", 1);
		String path = "/api/v1/admin/products/" + p.getId() + "/status";
		mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"ARCHIVED\",\"version\":" + p.getVersion() + "}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ARCHIVED"))
			.andExpect(jsonPath("$.version").value(p.getVersion() + 1));
		// versão antiga: outra pessoa já alterou
		mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"ACTIVE\",\"version\":" + p.getVersion() + "}"))
			.andExpect(status().isConflict());
		mvc.perform(get("/api/v1/public/products/" + p.getSlug())).andExpect(status().isNotFound());
		mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"ACTIVE\",\"version\":" + (p.getVersion() + 1) + "}"))
			.andExpect(status().isOk());
		assertThat(auditLog.findByEntityTypeAndEntityIdOrderByIdDesc("product", String.valueOf(p.getId())))
			.filteredOn(a -> a.getAction().equals("PRODUCT_STATUS_CHANGED"))
			.hasSize(2);
	}

	// --- criação e validação

	@Test
	@WithMockUser(username = "admin@exemplo.com", roles = "ADMIN")
	void createsProductWithGeneratedSlugOptionsZeroStockAndAudit() throws Exception {
		String sku = CatalogFixtures.uniqueSku();
		String body = postProduct(productWithoutSlug(sku)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("dragao-mecanico-azul"))
			.andExpect(jsonPath("$.price").value(349.90))
			.andExpect(jsonPath("$.stock.onHand").value(0))
			.andExpect(jsonPath("$.options[0].values[1].priceDelta").value(60))
			.andExpect(jsonPath("$.version").value(0))
			.andReturn().getResponse().getContentAsString();
		String id = json.readTree(body).get("id").asString();
		assertThat(auditLog.findByEntityTypeAndEntityIdOrderByIdDesc("product", id))
			.anySatisfy(a -> {
				assertThat(a.getAction()).isEqualTo("PRODUCT_CREATED");
				assertThat(a.getDetails()).contains("admin@exemplo.com");
			});
		// limpa o slug fixo para não colidir com outros testes
		mvc.perform(delete("/api/v1/admin/products/{id}", id).with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void rejectsInvalidPayloadWithFieldErrors() throws Exception {
		Map<String, Object> body = validProduct("sku minusculo");
		body.put("price", -1);
		body.remove("name");
		postProduct(body).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
			.andExpect(jsonPath("$.errors[?(@.field == 'price')]").exists())
			.andExpect(jsonPath("$.errors[?(@.field == 'sku')]").exists());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void promoPriceMustBeLowerThanPrice() throws Exception {
		Map<String, Object> body = validProduct(CatalogFixtures.uniqueSku());
		body.put("promoPrice", new BigDecimal("349.90"));
		postProduct(body).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("promoPrice"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void madeToOrderRequiresProductionDays() throws Exception {
		Map<String, Object> body = validProduct(CatalogFixtures.uniqueSku());
		body.put("type", "MADE_TO_ORDER");
		postProduct(body).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("productionDays"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void duplicateSkuOrSlugIsConflict() throws Exception {
		Product existing = fixtures.activeProduct(category, "Existente", "10.00", 0);
		postProduct(validProduct(existing.getSku())).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Já existe um produto com o SKU '" + existing.getSku() + "'."));
		Map<String, Object> sameSlug = validProduct(CatalogFixtures.uniqueSku());
		sameSlug.put("slug", existing.getSlug());
		postProduct(sameSlug).andExpect(status().isConflict());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void unknownCategoryIsFieldError() throws Exception {
		Map<String, Object> body = validProduct(CatalogFixtures.uniqueSku());
		body.put("categoryId", 999_999_999);
		postProduct(body).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("categoryId"));
	}

	// --- atualização e concorrência

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateRequiresCurrentVersion() throws Exception {
		Product product = fixtures.activeProduct(category, "Editável", "10.00", 0);
		Map<String, Object> body = validProduct(product.getSku());
		body.put("slug", product.getSlug());
		body.put("version", 0);
		mvc.perform(put("/api/v1/admin/products/{id}", product.getId()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Dragão Mecânico Azul"))
			.andExpect(jsonPath("$.version").value(1));
		// segunda edição com a versão antiga (outra aba/pessoa) → 409
		mvc.perform(put("/api/v1/admin/products/{id}", product.getId()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)))
			.andExpect(status().isConflict());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void replacingOptionsWithSameNamesWorks() throws Exception {
		String created = postProduct(Map.of("name", "Raptor " + CatalogFixtures.unique("x"), "sku",
				CatalogFixtures.uniqueSku(), "description", "Raptor articulado de teste.", "categoryId",
				category.getId(), "type", "READY", "status", "DRAFT", "price", 99, "options",
				List.of(Map.of("name", "Tamanho", "values", List.of(Map.of("label", "10 cm", "priceDelta", 0))))))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		var node = json.readTree(created);
		Map<String, Object> update = new java.util.HashMap<>(Map.of("name", node.get("name").asString(), "sku",
				node.get("sku").asString(), "description", "Raptor articulado de teste.", "categoryId",
				category.getId(), "type", "READY", "status", "DRAFT", "price", 99, "version", 0, "options",
				List.of(Map.of("name", "Tamanho", "values",
						List.of(Map.of("label", "10 cm", "priceDelta", 0), Map.of("label", "15 cm", "priceDelta", 30))))));
		mvc.perform(put("/api/v1/admin/products/{id}", node.get("id").asLong()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(update)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.options[0].values.length()").value(2));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void notFoundFor404() throws Exception {
		mvc.perform(get("/api/v1/admin/products/{id}", 987_654_321)).andExpect(status().isNotFound());
	}

	// --- exclusão

	@Test
	@WithMockUser(roles = "ADMIN")
	void productWithOrdersCannotBeDeleted() throws Exception {
		Product product = fixtures.activeProduct(category, "Vendido", "10.00", 0);
		Long userId = jdbc.queryForObject("""
				INSERT INTO users (email, password_hash, full_name, role)
				VALUES (?, 'x', 'Cliente', 'CUSTOMER') RETURNING id""", Long.class,
				CatalogFixtures.unique("c") + "@exemplo.com");
		Long orderId = jdbc.queryForObject("""
				INSERT INTO orders (user_id, subtotal, total, payment_method, shipping_method, customer_name, customer_email)
				VALUES (?, 10, 10, 'PIX', 'PICKUP', 'Cliente', 'c@exemplo.com') RETURNING id""", Long.class, userId);
		jdbc.update("""
				INSERT INTO order_items (order_id, product_id, product_name, sku, unit_price, quantity, line_total)
				VALUES (?, ?, 'Vendido', ?, 10, 1, 10)""", orderId, product.getId(), product.getSku());

		mvc.perform(delete("/api/v1/admin/products/{id}", product.getId()).with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("O produto possui pedidos. Arquive-o em vez de excluir."));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void categoryWithProductsCannotBeDeleted() throws Exception {
		fixtures.activeProduct(category, "Qualquer", "10.00", 0);
		mvc.perform(delete("/api/v1/admin/categories/{id}", category.getId()).with(csrf()))
			.andExpect(status().isConflict());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void categoryCrud() throws Exception {
		String slug = CatalogFixtures.unique("nova");
		String body = mvc.perform(post("/api/v1/admin/categories").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("name", "Nova", "slug", slug))))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		long id = json.readTree(body).get("id").asLong();
		mvc.perform(post("/api/v1/admin/categories").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("name", "Duplicada", "slug", slug))))
			.andExpect(status().isConflict());
		mvc.perform(delete("/api/v1/admin/categories/{id}", id).with(csrf())).andExpect(status().isNoContent());
	}

}
