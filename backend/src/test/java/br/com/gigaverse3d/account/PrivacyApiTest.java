package br.com.gigaverse3d.account;

import static br.com.gigaverse3d.account.AuthTestSupport.PASSWORD;
import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.login;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

import br.com.gigaverse3d.account.service.DataRetentionJob;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class PrivacyApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	DataRetentionJob retention;

	String email;

	long userId;

	MockHttpSession session;

	@BeforeEach
	void setUp() throws Exception {
		email = newEmail();
		userId = createUser(jdbc, encoder, email, "CUSTOMER");
		session = loggedIn(mvc, json, email);
		jdbc.update("""
				INSERT INTO addresses (user_id, label, recipient_name, postal_code, street, number, district, city, state)
				VALUES (?, 'Casa', 'Cliente Teste', '53625816', 'Rua Privada', '10', 'Centro', 'Igarassu', 'PE')""", userId);
	}

	private long order(String status) {
		return jdbc.queryForObject("""
				INSERT INTO orders (user_id, status, subtotal, total, payment_method, shipping_method, customer_name,
				                    customer_email, customer_note, paid_at, cancelled_at)
				VALUES (?, ?, 50, 50, 'PIX', 'PICKUP', 'Cliente Teste', ?, 'deixar com o porteiro', now(), ?)
				RETURNING number""", Long.class, userId, status, email,
				"CANCELLED".equals(status) ? Timestamp.from(Instant.now()) : null);
	}

	private ResultActions deleteAccount(MockHttpSession s, String password) throws Exception {
		return mvc.perform(post("/api/v1/account/privacy/delete-account").session(s).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("password", password))));
	}

	@Test
	void exportContainsOnlyMyDataAndNoSecrets() throws Exception {
		order("DELIVERED");
		String other = newEmail();
		createUser(jdbc, encoder, other, "CUSTOMER");
		String body = mvc.perform(get("/api/v1/account/privacy/export").session(session))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.conta.email").value(email))
			.andExpect(jsonPath("$.enderecos[0].rua").value("Rua Privada"))
			.andExpect(jsonPath("$.pedidos.length()").value(1))
			.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain("password").doesNotContain("argon2").doesNotContain(other)
			.doesNotContain("security_stamp");
		mvc.perform(get("/api/v1/account/privacy/export")).andExpect(status().isUnauthorized());
	}

	@Test
	void deletionRequiresPasswordAndNoOpenOrders() throws Exception {
		deleteAccount(session, "senha-errada-123").andExpect(status().isBadRequest());
		order("PENDING_PAYMENT");
		deleteAccount(session, PASSWORD).andExpect(status().isConflict());
		assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, userId)).isEqualTo("ACTIVE");
	}

	@Test
	void deletionAnonymizesAccountKeepsOrdersAndEndsSessions() throws Exception {
		long number = order("DELIVERED");
		MockHttpSession otherDevice = loggedIn(mvc, json, email);

		deleteAccount(session, PASSWORD).andExpect(status().isOk());

		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/auth/me").session(otherDevice)).andExpect(status().isNoContent());
		login(mvc, json, new MockHttpSession(), email, PASSWORD).andExpect(status().isUnauthorized());

		Map<String, Object> user = jdbc.queryForMap("SELECT email, full_name, status, deleted_at FROM users WHERE id = ?",
				userId);
		assertThat(user.get("email")).isEqualTo("removido-" + userId + "@anonimizado.invalid");
		assertThat(user.get("full_name")).isEqualTo("Conta removida");
		assertThat(user.get("status")).isEqualTo("ANONYMIZED");
		assertThat(user.get("deleted_at")).isNotNull();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM addresses WHERE user_id = ?", Integer.class, userId))
			.isZero();
		Map<String, Object> kept = jdbc.queryForMap("SELECT customer_email, customer_note FROM orders WHERE number = ?",
				number);
		assertThat(kept.get("customer_email")).isEqualTo(user.get("email"));
		assertThat(kept.get("customer_note")).isNull();
		assertThat(jdbc.queryForList("SELECT action FROM audit_log WHERE entity_type = 'user' AND entity_id = ?",
				String.class, String.valueOf(userId))).contains("ACCOUNT_ANONYMIZED");

		// o e-mail original fica livre para um novo cadastro
		mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("fullName", "Nova Conta", "email", email, "password", PASSWORD,
					"acceptTerms", true))))
			.andExpect(status().is2xxSuccessful());
	}

	@Test
	void retentionPurgesExpiredTokensAndGuestCarts() {
		Instant old = Instant.now().minus(40, ChronoUnit.DAYS);
		jdbc.update("""
				INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?)""",
				userId, "a".repeat(64), Timestamp.from(old.plusSeconds(1800)), Timestamp.from(old));
		jdbc.update("INSERT INTO carts (anonymous_token_hash, expires_at) VALUES (?, ?)", "b".repeat(64),
				Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
		jdbc.update("INSERT INTO carts (anonymous_token_hash, expires_at) VALUES (?, ?)", "c".repeat(64),
				Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)));

		DataRetentionJob.PurgeResult result = retention.purge();

		assertThat(result.resetTokens()).isGreaterThanOrEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = ?", Integer.class,
				"a".repeat(64))).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carts WHERE anonymous_token_hash = ?", Integer.class,
				"b".repeat(64))).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carts WHERE anonymous_token_hash = ?", Integer.class,
				"c".repeat(64))).isOne();
	}

}
