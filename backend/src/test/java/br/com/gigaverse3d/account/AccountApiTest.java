package br.com.gigaverse3d.account;

import static br.com.gigaverse3d.account.AuthTestSupport.PASSWORD;
import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
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

import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class AccountApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	String emailA;

	MockHttpSession userA;

	MockHttpSession userB;

	@BeforeEach
	void setUp() throws Exception {
		emailA = newEmail();
		String emailB = newEmail();
		createUser(jdbc, encoder, emailA, "CUSTOMER");
		createUser(jdbc, encoder, emailB, "CUSTOMER");
		userA = loggedIn(mvc, json, emailA);
		userB = loggedIn(mvc, json, emailB);
	}

	private ResultActions send(MockHttpServletRequestBuilder req, MockHttpSession session, Object body)
			throws Exception {
		return mvc.perform(req.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	private Map<String, Object> address(String label) {
		return new HashMap<>(Map.of("label", label, "recipientName", "Cliente Teste", "postalCode", "53625-816",
				"street", "Rua Exemplo", "number", "100", "district", "Centro", "city", "Cidade Exemplo", "state", "PE"));
	}

	private long createAddress(MockHttpSession session, String label) throws Exception {
		String body = send(post("/api/v1/account/addresses"), session, address(label)).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("id").asLong();
	}

	// --- perfil

	@Test
	void profileUpdateIgnoresRoleAndEmailInPayload() throws Exception {
		Map<String, Object> body = new HashMap<>(Map.of("fullName", "Nome Novo", "phone", "81999998888",
				"marketingOptIn", true));
		body.put("role", "ADMIN");
		body.put("email", "outro@exemplo.com");
		send(put("/api/v1/account/profile"), userA, body).andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Nome Novo"))
			.andExpect(jsonPath("$.email").value(emailA))
			.andExpect(jsonPath("$.marketingOptIn").value(true));
		assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email = ?", String.class, emailA))
			.isEqualTo("CUSTOMER");
		mvc.perform(get("/api/v1/admin/products").session(userA)).andExpect(status().isForbidden());
	}

	@Test
	void profileRequiresLogin() throws Exception {
		mvc.perform(get("/api/v1/account/profile")).andExpect(status().isUnauthorized());
	}

	// --- senha

	@Test
	void changePasswordRequiresCurrentAndKeepsOnlyThisSession() throws Exception {
		MockHttpSession otherDevice = loggedIn(mvc, json, emailA);
		send(put("/api/v1/account/password"), userA, Map.of("currentPassword", "errada-123456", "newPassword",
				"Nova-Senha-Segura-31")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("currentPassword"));

		send(put("/api/v1/account/password"), userA, Map.of("currentPassword", PASSWORD, "newPassword",
				"Nova-Senha-Segura-31")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/me").session(userA)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/me").session(otherDevice)).andExpect(status().isNoContent());
	}

	// --- endereços

	@Test
	void addressCrudWithSingleDefault() throws Exception {
		long home = createAddress(userA, "Casa");
		long work = createAddress(userA, "Trabalho");
		mvc.perform(get("/api/v1/account/addresses").session(userA))
			.andExpect(jsonPath("$[0].id").value(home))
			.andExpect(jsonPath("$[0].isDefault").value(true))
			.andExpect(jsonPath("$[0].postalCode").value("53625816"))
			.andExpect(jsonPath("$[1].isDefault").value(false));

		mvc.perform(post("/api/v1/account/addresses/{id}/default", work).session(userA).with(csrf()))
			.andExpect(jsonPath("$.isDefault").value(true));
		mvc.perform(delete("/api/v1/account/addresses/{id}", work).session(userA).with(csrf()))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/account/addresses").session(userA))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].isDefault").value(true));
	}

	@Test
	void customerCannotSeeOrChangeAnotherCustomersAddress() throws Exception {
		long addressOfA = createAddress(userA, "Casa");
		mvc.perform(get("/api/v1/account/addresses").session(userB)).andExpect(jsonPath("$.length()").value(0));
		send(put("/api/v1/account/addresses/{id}", addressOfA), userB, address("Invadido"))
			.andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/account/addresses/{id}/default", addressOfA).session(userB).with(csrf()))
			.andExpect(status().isNotFound());
		mvc.perform(delete("/api/v1/account/addresses/{id}", addressOfA).session(userB).with(csrf()))
			.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/account/addresses").session(userA))
			.andExpect(jsonPath("$[0].label").value("Casa"));
	}

	@Test
	void addressValidation() throws Exception {
		Map<String, Object> invalid = address("Casa");
		invalid.put("postalCode", "123");
		invalid.put("state", "XX");
		send(post("/api/v1/account/addresses"), userA, invalid).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'postalCode')]").exists())
			.andExpect(jsonPath("$.errors[?(@.field == 'state')]").exists());
	}

	@Test
	void addressLimit() throws Exception {
		for (int i = 0; i < 10; i++) {
			createAddress(userA, "E" + i);
		}
		send(post("/api/v1/account/addresses"), userA, address("Extra")).andExpect(status().isConflict());
	}

}
