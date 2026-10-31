package br.com.gigaverse3d.crm;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class CustomerCrmApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	MockHttpSession operator;

	MockHttpSession admin;

	String customerEmail;

	long customerId;

	@BeforeEach
	void setUp() throws Exception {
		String op = newEmail();
		createUser(jdbc, encoder, op, "OPERATOR");
		operator = loggedIn(mvc, json, op);
		String ad = newEmail();
		createUser(jdbc, encoder, ad, "ADMIN");
		admin = loggedIn(mvc, json, ad);
		customerEmail = newEmail();
		customerId = createUser(jdbc, encoder, customerEmail, "CUSTOMER");
	}

	private ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body) throws Exception {
		return mvc.perform(req.session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	private void paidOrder(long userId, String total, Instant paidAt, String status) {
		jdbc.update("""
				INSERT INTO orders (user_id, status, subtotal, total, payment_method, shipping_method,
				                    customer_name, customer_email, paid_at, cancelled_at)
				VALUES (?, ?, ?, ?, 'PIX', 'PICKUP', 'Cliente', 'c@exemplo.com', ?, ?)""", userId, status,
				new BigDecimal(total), new BigDecimal(total), Timestamp.from(paidAt),
				"CANCELLED".equals(status) ? Timestamp.from(paidAt) : null);
	}

	@Test
	void searchShowsPurchaseSummaryCountingOnlyValidPaidOrders() throws Exception {
		Instant now = Instant.now();
		paidOrder(customerId, "100.00", now.minus(10, ChronoUnit.DAYS), "DELIVERED");
		paidOrder(customerId, "50.00", now.minus(2, ChronoUnit.DAYS), "PAID");
		paidOrder(customerId, "999.00", now.minus(1, ChronoUnit.DAYS), "CANCELLED");

		mvc.perform(get("/api/v1/admin/customers").param("q", customerEmail.toUpperCase()).session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(customerId))
			.andExpect(jsonPath("$.content[0].paidOrders").value(2))
			.andExpect(jsonPath("$.content[0].totalSpent").value(150.00))
			.andExpect(jsonPath("$.content[0].lastPurchaseAt").isNotEmpty())
			// dados mínimos: nada de senha, telefone ou endereço na listagem
			.andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
			.andExpect(jsonPath("$.content[0].phone").doesNotExist());

		mvc.perform(get("/api/v1/admin/customers/" + customerId).session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.orders").value(3))
			.andExpect(jsonPath("$.paidOrders").value(2))
			.andExpect(jsonPath("$.averageTicket").value(75.00))
			.andExpect(jsonPath("$.marketingOptIn").value(false));

		// busca pelo id também funciona; equipe e % na busca não quebram a consulta
		mvc.perform(get("/api/v1/admin/customers").param("q", String.valueOf(customerId)).session(operator))
			.andExpect(jsonPath("$.content[?(@.id == " + customerId + ")]").exists());
		mvc.perform(get("/api/v1/admin/customers").param("q", "%_\\").session(operator)).andExpect(status().isOk());
	}

	@Test
	void staffAccountsAreNotCustomers() throws Exception {
		long staffId = createUser(jdbc, encoder, newEmail(), "OPERATOR");
		mvc.perform(get("/api/v1/admin/customers/" + staffId).session(operator)).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/admin/customers/999999999").session(operator)).andExpect(status().isNotFound());
	}

	@Test
	void segmentsAreComputedFromOrders() throws Exception {
		String tag = CatalogFixtures.unique("seg");
		long tagId = json.readTree(send(post("/api/v1/admin/customer-tags"), operator, Map.of("name", tag))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
		long repeat = createUser(jdbc, encoder, newEmail(), "CUSTOMER");
		long lapsed = createUser(jdbc, encoder, newEmail(), "CUSTOMER");
		long none = createUser(jdbc, encoder, newEmail(), "CUSTOMER");
		Instant now = Instant.now();
		paidOrder(repeat, "10.00", now.minus(5, ChronoUnit.DAYS), "DELIVERED");
		paidOrder(repeat, "10.00", now.minus(1, ChronoUnit.DAYS), "PAID");
		paidOrder(lapsed, "10.00", now.minus(120, ChronoUnit.DAYS), "DELIVERED");
		for (long id : List.of(repeat, lapsed, none)) {
			send(put("/api/v1/admin/customers/" + id + "/tags"), operator, Map.of("tagIds", List.of(tagId)))
				.andExpect(status().isOk());
		}
		String base = "/api/v1/admin/customers?tagId=" + tagId + "&segment=";
		mvc.perform(get(base + "REPEAT").session(operator))
			.andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(repeat));
		mvc.perform(get(base + "LAPSED").session(operator))
			.andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(lapsed));
		mvc.perform(get(base + "NO_PURCHASE").session(operator))
			.andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(none));
	}

	@Test
	void notesAreInternalAuditedWithoutContentAndOnlyAdminDeletes() throws Exception {
		String secret = "Prefere retirar no sábado " + CatalogFixtures.unique("n");
		String body = send(post("/api/v1/admin/customers/" + customerId + "/notes"), operator, Map.of("body", secret))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.body").value(secret))
			.andExpect(jsonPath("$.authorName").value("Usuário Teste"))
			.andReturn().getResponse().getContentAsString();
		long noteId = json.readTree(body).get("id").asLong();

		String audit = jdbc.queryForObject("""
				SELECT CAST(details AS text) FROM audit_log
				WHERE action = 'CUSTOMER_NOTE_ADDED' AND entity_id = ? ORDER BY id DESC LIMIT 1""", String.class,
				String.valueOf(customerId));
		assertThat(audit).contains("length").doesNotContain("sábado");

		// o próprio cliente não enxerga notas (nem a API administrativa)
		MockHttpSession customer = loggedIn(mvc, json, customerEmail);
		mvc.perform(get("/api/v1/admin/customers/" + customerId + "/notes").session(customer))
			.andExpect(status().isForbidden());
		assertThat(mvc.perform(get("/api/v1/account/profile").session(customer)).andReturn().getResponse()
			.getContentAsString()).doesNotContain(secret);

		mvc.perform(delete("/api/v1/admin/customers/" + customerId + "/notes/" + noteId).session(operator).with(csrf()))
			.andExpect(status().isForbidden());
		mvc.perform(delete("/api/v1/admin/customers/" + customerId + "/notes/" + noteId).session(admin).with(csrf()))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/admin/customers/" + customerId + "/notes").session(operator))
			.andExpect(jsonPath("$.totalElements").value(0));
		// nota de outro cliente não pode ser apagada por este caminho
		mvc.perform(delete("/api/v1/admin/customers/" + customerId + "/notes/" + noteId).session(admin).with(csrf()))
			.andExpect(status().isNotFound());

		send(post("/api/v1/admin/customers/" + customerId + "/notes"), operator, Map.of("body", "   "))
			.andExpect(status().isBadRequest());
	}

	@Test
	void tagsAreSetAsAWholeAndAudited() throws Exception {
		String name = CatalogFixtures.unique("VIP");
		long vip = json.readTree(send(post("/api/v1/admin/customer-tags"), operator, Map.of("name", name))
			.andReturn().getResponse().getContentAsString()).get("id").asLong();
		long other = json.readTree(send(post("/api/v1/admin/customer-tags"), operator,
				Map.of("name", CatalogFixtures.unique("Atacado"))).andReturn().getResponse().getContentAsString())
			.get("id").asLong();
		send(post("/api/v1/admin/customer-tags"), operator, Map.of("name", name.toLowerCase()))
			.andExpect(status().isConflict());

		send(put("/api/v1/admin/customers/" + customerId + "/tags"), operator, Map.of("tagIds", List.of(vip, other)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
		send(put("/api/v1/admin/customers/" + customerId + "/tags"), operator, Map.of("tagIds", List.of(vip)))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value(name));
		send(put("/api/v1/admin/customers/" + customerId + "/tags"), operator, Map.of("tagIds", List.of(-1)))
			.andExpect(status().isBadRequest());

		mvc.perform(get("/api/v1/admin/customers?tagId=" + vip).session(operator))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].tags[0].name").value(name));
		Integer changes = jdbc.queryForObject(
				"SELECT COUNT(*) FROM audit_log WHERE action = 'CUSTOMER_TAGS_CHANGED' AND entity_id = ?", Integer.class,
				String.valueOf(customerId));
		assertThat(changes).isEqualTo(2);

		mvc.perform(delete("/api/v1/admin/customer-tags/" + vip).session(admin).with(csrf()))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/admin/customers/" + customerId).session(operator))
			.andExpect(jsonPath("$.tags.length()").value(0));
	}

	@Test
	void exportIsCsvWithFormulaProtectionAndIsAudited() throws Exception {
		String marker = CatalogFixtures.unique("exp");
		long evil = createUser(jdbc, encoder, marker + "@exemplo.com", "CUSTOMER");
		jdbc.update("UPDATE users SET full_name = ? WHERE id = ?", "=HYPERLINK(\"http://x\";\"y\")", evil);

		var response = mvc.perform(get("/api/v1/admin/customers/export").param("q", marker).session(admin))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
			.andReturn().getResponse();
		String csv = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
		assertThat(csv).startsWith("﻿id;nome;email");
		assertThat(csv).contains(marker + "@exemplo.com");
		// fórmula neutralizada com apóstrofo e campo entre aspas (contém ; e ")
		assertThat(csv).contains("\"'=HYPERLINK(\"\"http://x\"\";\"\"y\"\")\"");
		assertThat(csv.lines().count()).isEqualTo(2);

		String audit = jdbc.queryForObject(
				"SELECT CAST(details AS text) FROM audit_log WHERE action = 'CUSTOMERS_EXPORTED' ORDER BY id DESC LIMIT 1",
				String.class);
		assertThat(audit).contains("\"rows\": 1").doesNotContain(marker);
	}

}
