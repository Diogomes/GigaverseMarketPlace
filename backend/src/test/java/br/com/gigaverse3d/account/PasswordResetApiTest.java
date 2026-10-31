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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.gigaverse3d.account.AuthTestSupport.CapturingEmailSender;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class PasswordResetApiTest extends PostgresIntegrationTest {

	private static final String NEW_PASSWORD = "Outra-Senha-Bem-Longa-42";

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

	private ResultActions forgot(String email) throws Exception {
		return mvc.perform(post("/api/v1/auth/password/forgot").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("email", email))));
	}

	private ResultActions reset(String token, String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/password/reset").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("token", token, "newPassword", password))));
	}

	private static String sha256(String value) throws Exception {
		return HexFormat.of()
			.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void responseIsIdenticalForExistingAndUnknownEmails() throws Exception {
		String existing = newEmail();
		createUser(jdbc, encoder, existing, "CUSTOMER");
		String unknown = newEmail();
		String a = forgot(existing).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
		String b = forgot(unknown).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
		assertThat(a).isEqualTo(b);
		assertThat(emails.to(existing)).hasSize(1);
		assertThat(emails.to(unknown)).isEmpty();
	}

	@Test
	void tokenIsStoredHashedWorksOnceAndChangesPassword() throws Exception {
		String email = newEmail();
		long userId = createUser(jdbc, encoder, email, "CUSTOMER");
		forgot(email).andExpect(status().isAccepted());
		String token = emails.lastResetToken(email).orElseThrow();

		String stored = jdbc.queryForObject(
				"SELECT token_hash FROM password_reset_tokens WHERE user_id = ? ORDER BY id DESC LIMIT 1", String.class,
				userId);
		assertThat(stored).isEqualTo(sha256(token)).doesNotContain(token);

		reset(token, NEW_PASSWORD).andExpect(status().isOk());
		reset(token, "Mais-Uma-Senha-Nova-77").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Link inválido ou expirado. Solicite um novo."));

		login(mvc, json, new MockHttpSession(), email, PASSWORD).andExpect(status().isUnauthorized());
		login(mvc, json, new MockHttpSession(), email, NEW_PASSWORD).andExpect(status().isOk());
		assertThat(emails.to(email)).anyMatch(m -> m.subject().contains("senha foi alterada"));
	}

	@Test
	void expiredTokenIsRejected() throws Exception {
		String email = newEmail();
		long userId = createUser(jdbc, encoder, email, "CUSTOMER");
		String token = "token-expirado-de-teste-1234567890";
		jdbc.update("""
				INSERT INTO password_reset_tokens (user_id, token_hash, created_at, expires_at)
				VALUES (?, ?, now() - interval '2 hours', now() - interval '1 hour')""", userId, sha256(token));
		reset(token, NEW_PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void onlyTheLatestLinkIsValid() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		forgot(email);
		String first = emails.lastResetToken(email).orElseThrow();
		forgot(email);
		String second = emails.lastResetToken(email).orElseThrow();
		reset(first, NEW_PASSWORD).andExpect(status().isBadRequest());
		reset(second, NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void resetEndsExistingSessionsAndEnforcesPolicy() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		MockHttpSession session = loggedIn(mvc, json, email);
		forgot(email);
		String token = emails.lastResetToken(email).orElseThrow();

		reset(token, "curta").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("newPassword"));
		reset(token, NEW_PASSWORD).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isNoContent());
	}

}
