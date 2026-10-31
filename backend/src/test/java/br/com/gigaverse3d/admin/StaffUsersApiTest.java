package br.com.gigaverse3d.admin;

import static br.com.gigaverse3d.account.AuthTestSupport.PASSWORD;
import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.login;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import br.com.gigaverse3d.account.AuthTestSupport.CapturingEmailSender;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class StaffUsersApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	CapturingEmailSender emails;

	MockHttpSession admin;

	long adminId;

	@BeforeEach
	void setUp() throws Exception {
		String email = newEmail();
		adminId = createUser(jdbc, encoder, email, "ADMIN");
		admin = loggedIn(mvc, json, email);
	}

	private ResultActions send(MockHttpServletRequestBuilder req, Object body) throws Exception {
		return mvc.perform(req.session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	@Test
	void invitedOperatorDefinesOwnPasswordAndLogsIn() throws Exception {
		String email = newEmail();
		long id = json.readTree(send(post("/api/v1/admin/users"),
				Map.of("email", email.toUpperCase(), "fullName", "Operadora Nova", "role", "OPERATOR"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.role").value("OPERATOR"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andReturn().getResponse().getContentAsString()).get("id").asLong();

		assertThat(emails.to(email)).singleElement()
			.satisfies(m -> assertThat(m.subject()).contains("convite"));
		String token = emails.lastResetToken(email).orElseThrow();
		mvc.perform(post("/api/v1/auth/password/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("token", token, "newPassword", PASSWORD))))
			.andExpect(status().is2xxSuccessful());

		MockHttpSession operator = new MockHttpSession();
		login(mvc, json, operator, email, PASSWORD).andExpect(status().isOk());
		mvc.perform(get("/api/v1/admin/dashboard").session(operator)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/admin/users").session(operator)).andExpect(status().isForbidden());

		mvc.perform(get("/api/v1/admin/users").session(admin))
			.andExpect(jsonPath("$[?(@.id == " + id + ")].role").value("OPERATOR"));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'STAFF_USER_CREATED' AND entity_id = ?",
				Integer.class, String.valueOf(id))).isEqualTo(1);
	}

	@Test
	void rejectsDuplicateEmailAndCustomerRole() throws Exception {
		String customer = newEmail();
		createUser(jdbc, encoder, customer, "CUSTOMER");
		send(post("/api/v1/admin/users"), Map.of("email", customer, "fullName", "Cliente", "role", "OPERATOR"))
			.andExpect(status().isConflict());
		send(post("/api/v1/admin/users"), Map.of("email", newEmail(), "fullName", "Alguém", "role", "CUSTOMER"))
			.andExpect(status().isBadRequest());
		send(post("/api/v1/admin/users"), Map.of("email", "invalido", "fullName", "Alguém", "role", "OPERATOR"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void cannotChangeOwnRoleOrDisableSelf() throws Exception {
		send(put("/api/v1/admin/users/" + adminId + "/role"), Map.of("role", "OPERATOR")).andExpect(status().isConflict());
		mvc.perform(post("/api/v1/admin/users/" + adminId + "/disable").session(admin).with(csrf()))
			.andExpect(status().isConflict());
	}

	@Test
	void roleChangeAndDisableEndExistingSessions() throws Exception {
		String email = newEmail();
		long operatorId = createUser(jdbc, encoder, email, "OPERATOR");
		MockHttpSession operator = loggedIn(mvc, json, email);
		mvc.perform(get("/api/v1/admin/dashboard").session(operator)).andExpect(status().isOk());

		send(put("/api/v1/admin/users/" + operatorId + "/role"), Map.of("role", "ADMIN"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
		mvc.perform(get("/api/v1/auth/me").session(operator)).andExpect(status().isNoContent());

		MockHttpSession again = loggedIn(mvc, json, email);
		mvc.perform(get("/api/v1/admin/users").session(again)).andExpect(status().isOk());
		mvc.perform(post("/api/v1/admin/users/" + operatorId + "/disable").session(admin).with(csrf()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISABLED"));
		mvc.perform(get("/api/v1/auth/me").session(again)).andExpect(status().isNoContent());
		login(mvc, json, new MockHttpSession(), email, PASSWORD).andExpect(status().isUnauthorized());

		mvc.perform(post("/api/v1/admin/users/" + operatorId + "/enable").session(admin).with(csrf()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
		login(mvc, json, new MockHttpSession(), email, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void customersCannotBeManagedAsStaff() throws Exception {
		long customerId = createUser(jdbc, encoder, newEmail(), "CUSTOMER");
		send(put("/api/v1/admin/users/" + customerId + "/role"), Map.of("role", "ADMIN")).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/admin/users/" + customerId + "/disable").session(admin).with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void auditTrailIsFilterableAndShowsActor() throws Exception {
		long operatorId = createUser(jdbc, encoder, newEmail(), "OPERATOR");
		mvc.perform(post("/api/v1/admin/users/" + operatorId + "/disable").session(admin).with(csrf()))
			.andExpect(status().isOk());
		mvc.perform(get("/api/v1/admin/audit").param("action", "STAFF_USER_DISABLED")
			.param("entityId", String.valueOf(operatorId)).session(admin))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].actorId").value(adminId))
			.andExpect(jsonPath("$.content[0].details.role").value("OPERATOR"));
		mvc.perform(get("/api/v1/admin/audit").param("action", "drop table").session(admin))
			.andExpect(status().isBadRequest());
	}

}
