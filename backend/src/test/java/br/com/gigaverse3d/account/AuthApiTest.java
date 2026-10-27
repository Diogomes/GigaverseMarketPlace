package br.com.gigaverse3d.account;

import static br.com.gigaverse3d.account.AuthTestSupport.PASSWORD;
import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.login;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class AuthApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	private ResultActions register(MockHttpSession session, Map<String, Object> body) throws Exception {
		return mvc.perform(post("/api/v1/auth/register").session(session)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	private Map<String, Object> registration(String email) {
		return new HashMap<>(Map.of("fullName", "Cliente Teste", "email", email, "password", PASSWORD, "acceptTerms",
				true, "marketingOptIn", false));
	}

	// --- cadastro

	@Test
	void registerCreatesCustomerWithArgon2HashAndStartsSessionIgnoringRoleInPayload() throws Exception {
		String email = newEmail();
		Map<String, Object> body = registration(email.toUpperCase());
		body.put("role", "ADMIN");
		body.put("status", "ACTIVE");
		MockHttpSession session = new MockHttpSession();
		register(session, body).andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.role").value("CUSTOMER"))
			.andExpect(jsonPath("$.password").doesNotExist());

		assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email = ?", String.class, email))
			.isEqualTo("CUSTOMER");
		assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email))
			.startsWith("{argon2}$argon2id$")
			.doesNotContain(PASSWORD);
		assertThat(jdbc.queryForObject("""
				SELECT count(*) FROM user_consents c JOIN users u ON u.id = c.user_id WHERE u.email = ?""",
				Integer.class, email)).isEqualTo(3);
		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void registerValidatesPasswordTermsAndDuplicates() throws Exception {
		Map<String, Object> weak = registration(newEmail());
		weak.put("password", "1234567890");
		register(new MockHttpSession(), weak).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("password"));

		Map<String, Object> noTerms = registration(newEmail());
		noTerms.put("acceptTerms", false);
		register(new MockHttpSession(), noTerms).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("acceptTerms"));

		String email = newEmail();
		register(new MockHttpSession(), registration(email)).andExpect(status().isCreated());
		register(new MockHttpSession(), registration(email)).andExpect(status().isConflict());
	}

	// --- login

	@Test
	void loginReturnsGenericMessageForWrongPasswordOrUnknownEmail() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		login(mvc, json, new MockHttpSession(), email, "senha-errada-123").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
		login(mvc, json, new MockHttpSession(), newEmail(), PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
	}

	@Test
	void loginWithoutCsrfTokenIsRejected() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("email", newEmail(), "password", PASSWORD))))
			.andExpect(status().isForbidden());
	}

	@Test
	void accountLocksAfterFiveFailuresEvenWithCorrectPasswordAfterwards() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		for (int i = 0; i < 5; i++) {
			login(mvc, json, new MockHttpSession(), email, "errada-" + i + "-xyz").andExpect(status().isUnauthorized());
		}
		login(mvc, json, new MockHttpSession(), email, PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
		assertThat(jdbc.queryForObject("SELECT locked_until > now() FROM users WHERE email = ?", Boolean.class, email))
			.isTrue();
	}

	@Test
	void tooManyAttemptsFromSameClientGets429() throws Exception {
		String email = newEmail();
		for (int i = 0; i < 10; i++) {
			mvc.perform(post("/api/v1/auth/login").with(csrf()).with(r -> {
				r.setRemoteAddr("10.9.9.9");
				return r;
			}).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("email", email, "password", "x".repeat(12)))))
				.andExpect(status().isUnauthorized());
		}
		mvc.perform(post("/api/v1/auth/login").with(csrf()).with(r -> {
			r.setRemoteAddr("10.9.9.9");
			return r;
		}).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("email", email, "password", "x".repeat(12)))))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"));
	}

	@Test
	void loginRotatesSessionIdAndLogoutEndsSession() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		MockHttpSession session = new MockHttpSession();
		String before = session.getId();
		login(mvc, json, session, email, PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("CUSTOMER"));
		assertThat(session.getId()).isNotEqualTo(before);

		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
		assertThat(session.isInvalid()).isTrue();
		mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
	}

	// --- autorização por papel

	@Test
	void customerCannotUseAdminApiButAdminAndOperatorCan() throws Exception {
		String customer = newEmail();
		String admin = newEmail();
		String operator = newEmail();
		createUser(jdbc, encoder, customer, "CUSTOMER");
		createUser(jdbc, encoder, admin, "ADMIN");
		createUser(jdbc, encoder, operator, "OPERATOR");

		mvc.perform(get("/api/v1/admin/products").session(loggedIn(mvc, json, customer)))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/admin/products").session(loggedIn(mvc, json, admin))).andExpect(status().isOk());
		mvc.perform(get("/api/v1/admin/products").session(loggedIn(mvc, json, operator))).andExpect(status().isOk());
	}

	@Test
	void disablingAccountEndsExistingSessionsAndBlocksLogin() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		MockHttpSession session = loggedIn(mvc, json, email);
		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());

		jdbc.update("UPDATE users SET status = 'DISABLED' WHERE email = ?", email);
		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
		login(mvc, json, new MockHttpSession(), email, PASSWORD).andExpect(status().isUnauthorized());
	}

	@Test
	void roleChangeInDatabaseInvalidatesOldSessionPrivileges() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "ADMIN");
		MockHttpSession session = loggedIn(mvc, json, email);
		mvc.perform(get("/api/v1/admin/products").session(session)).andExpect(status().isOk());

		jdbc.update("UPDATE users SET role = 'CUSTOMER' WHERE email = ?", email);
		mvc.perform(get("/api/v1/admin/products").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void loginIsAudited() throws Exception {
		String email = newEmail();
		long id = createUser(jdbc, encoder, email, "CUSTOMER");
		login(mvc, json, new MockHttpSession(), email, PASSWORD).andExpect(status().isOk());
		assertThat(jdbc.queryForList("SELECT action FROM audit_log WHERE actor_user_id = ?", String.class, id))
			.contains("LOGIN_SUCCESS");
		assertThat(jdbc.queryForList("SELECT details::text FROM audit_log WHERE actor_user_id = ?", String.class, id))
			.noneMatch(d -> d.contains(PASSWORD));
	}

}
