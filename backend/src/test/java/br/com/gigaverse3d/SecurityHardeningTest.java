package br.com.gigaverse3d;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.login;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

/** Revisão de segurança da Fase 10 transformada em testes de regressão. */
@ExtendWith(OutputCaptureExtension.class)
class SecurityHardeningTest extends PostgresIntegrationTest {

	/** Rotas abertas de propósito (ver SecurityConfig). Qualquer outra rota da API exige sessão. */
	private static final Pattern PUBLIC = Pattern.compile("^/api/v1/(public/.*|payments/webhooks/.*|"
			+ "auth/(csrf|me|login|register|password/forgot|password/reset))$");

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	RequestMappingHandlerMapping mappings;

	record Route(HttpMethod method, String path) {
	}

	/** Todas as rotas da API registradas no Spring, com variáveis de caminho preenchidas. */
	private List<Route> apiRoutes() {
		List<Route> routes = new ArrayList<>();
		mappings.getHandlerMethods().forEach((info, handler) -> {
			for (String pattern : info.getPatternValues()) {
				if (!pattern.startsWith("/api/")) {
					continue;
				}
				String path = pattern.replaceAll("\\{[^}]+}", "1");
				var methods = info.getMethodsCondition().getMethods();
				for (RequestMethod m : methods.isEmpty() ? List.of(RequestMethod.GET) : methods) {
					routes.add(new Route(HttpMethod.valueOf(m.name()), path));
				}
			}
		});
		assertThat(routes).hasSizeGreaterThan(60);
		return routes;
	}

	@Test
	void everyNonPublicApiRouteRejectsAnonymousRequests() throws Exception {
		List<String> leaks = new ArrayList<>();
		for (Route r : apiRoutes()) {
			if (PUBLIC.matcher(r.path()).matches()) {
				continue;
			}
			int code = mvc.perform(request(r.method(), r.path()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{}")).andReturn().getResponse().getStatus();
			if (code != 401) {
				leaks.add(r + " → " + code);
			}
		}
		assertThat(leaks).as("rotas privadas que não responderam 401 sem sessão").isEmpty();
	}

	@Test
	void customersGet403OnEveryAdminRoute() throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		MockHttpSession customer = loggedIn(mvc, json, email);
		List<String> leaks = new ArrayList<>();
		for (Route r : apiRoutes()) {
			if (!r.path().startsWith("/api/v1/admin/")) {
				continue;
			}
			int code = mvc.perform(request(r.method(), r.path()).session(customer).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getStatus();
			if (code != 403) {
				leaks.add(r + " → " + code);
			}
		}
		assertThat(leaks).as("rotas administrativas acessíveis a cliente").isEmpty();
	}

	@Test
	void apiResponsesCarrySecurityHeadersAndRequestId() throws Exception {
		mvc.perform(get("/api/v1/public/status"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'none'")))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andExpect(header().string("X-Frame-Options", "DENY"))
			.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
			.andExpect(header().string("Permissions-Policy", org.hamcrest.Matchers.containsString("geolocation=()")))
			.andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
		mvc.perform(get("/api/v1/public/status").header("X-Request-Id", "proxy-abc-12345"))
			.andExpect(header().string("X-Request-Id", "proxy-abc-12345"));
		// valor fora do formato (ex.: tentativa de injetar linhas no log) é substituído
		mvc.perform(get("/api/v1/public/status").header("X-Request-Id", "x\nFAKE LOG LINE"))
			.andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
	}

	@Test
	void auditEntriesKeepTheRequestId() throws Exception {
		String email = newEmail();
		long id = createUser(jdbc, encoder, email, "CUSTOMER");
		login(mvc, json, new MockHttpSession(), email, "senha-errada-xyz");
		mvc.perform(post("/api/v1/auth/login").with(csrf()).header("X-Request-Id", "corr-login-0001")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("email", email, "password", "outra-errada-xyz"))));
		assertThat(jdbc.queryForList("SELECT request_id FROM audit_log WHERE entity_id = ? AND request_id IS NOT NULL",
				String.class, String.valueOf(id))).contains("corr-login-0001");
	}

	@Test
	void oversizedJsonBodiesAreRejectedBeforeParsing() throws Exception {
		String big = "{\"email\":\"" + "a".repeat(300 * 1024) + "\"}";
		mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(big))
			.andExpect(status().isPayloadTooLarge());
	}

	@Test
	void credentialsNeverReachTheLogs(CapturedOutput output) throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, "CUSTOMER");
		String secret = "Segredo-Que-Nao-Pode-Vazar-77";
		login(mvc, json, new MockHttpSession(), email, secret);
		mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("fullName", "Fulano", "email", email, "password", secret,
					"acceptTerms", true))));
		mvc.perform(post("/api/v1/auth/password/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("token", "token-invalido-123", "newPassword", secret))));
		assertThat(output.getAll()).doesNotContain(secret).doesNotContain("token-invalido-123");
	}

}
