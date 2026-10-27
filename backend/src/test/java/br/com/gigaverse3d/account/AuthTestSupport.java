package br.com.gigaverse3d.account;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.mail.EmailSender;
import tools.jackson.databind.json.JsonMapper;

/** Utilitários de autenticação para testes de integração. */
public final class AuthTestSupport {

	private AuthTestSupport() {
	}

	/** Substitui o envio real: guarda os e-mails em memória para inspeção. */
	public static class CapturingEmailSender implements EmailSender {

		private final List<EmailMessage> sent = new ArrayList<>();

		@Override
		public synchronized void send(EmailMessage message) {
			sent.add(message);
		}

		public synchronized List<EmailMessage> to(String email) {
			return sent.stream().filter(m -> m.to().equals(email)).toList();
		}

		/** Extrai o token do último link de redefinição enviado ao e-mail. */
		public Optional<String> lastResetToken(String email) {
			List<EmailMessage> messages = to(email);
			for (int i = messages.size() - 1; i >= 0; i--) {
				Matcher m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(messages.get(i).textBody());
				if (m.find()) {
					return Optional.of(m.group(1));
				}
			}
			return Optional.empty();
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		@Primary
		CapturingEmailSender capturingEmailSender() {
			return new CapturingEmailSender();
		}

	}

	public static String newEmail() {
		return CatalogFixtures.unique("u") + "@exemplo.com";
	}

	public static final String PASSWORD = "Correta-Cavalo-Bateria-9";

	/** Cria usuário direto no banco (para papéis que o cadastro público não concede). */
	public static long createUser(JdbcTemplate jdbc, PasswordEncoder encoder, String email, String role) {
		return jdbc.queryForObject("""
				INSERT INTO users (email, password_hash, full_name, role) VALUES (?, ?, 'Usuário Teste', ?)
				RETURNING id""", Long.class, email, encoder.encode(PASSWORD), role);
	}

	public static ResultActions login(MockMvc mvc, JsonMapper json, MockHttpSession session, String email,
			String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/login").session(session)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("email", email, "password", password))));
	}

	/** Faz login e devolve a sessão autenticada. */
	public static MockHttpSession loggedIn(MockMvc mvc, JsonMapper json, String email) throws Exception {
		MockHttpSession session = new MockHttpSession();
		login(mvc, json, session, email, PASSWORD).andReturn();
		return session;
	}

}
