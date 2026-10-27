package br.com.gigaverse3d;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Servidor real (Tomcat): requisições rejeitadas pelo container passam pelo despacho /error
 * e devem manter o status original, sem vazar detalhes internos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class HttpErrorDispatchTest {

	@LocalServerPort
	int port;

	private HttpResponse<String> get(String pathAndQuery) throws Exception {
		return HttpClient.newHttpClient()
			.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery)).build(),
					HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void malformedUtf8InQueryIsBadRequestNotUnauthorized() throws Exception {
		HttpResponse<String> res = get("/api/v1/public/products?q=Drag%E3o");
		assertThat(res.statusCode()).isEqualTo(400);
		assertThat(res.body()).doesNotContain("Exception").doesNotContain("at org.");
	}

	/** Servidor real: o token CSRF vem no cookie XSRF-TOKEN (legível pelo SPA) e vale para X-XSRF-TOKEN. */
	@Test
	void csrfTokenIsIssuedAsCookieForTheSpa() throws Exception {
		HttpResponse<String> res = get("/api/v1/auth/csrf");
		assertThat(res.statusCode()).isEqualTo(200);
		assertThat(res.body()).contains("\"headerName\":\"X-XSRF-TOKEN\"");
		assertThat(res.headers().allValues("Set-Cookie")).anyMatch(c -> c.startsWith("XSRF-TOKEN=") && !c.contains("HttpOnly"));
	}

	@Test
	void unknownRouteStaysProtected() throws Exception {
		assertThat(get("/api/v1/admin/inexistente").statusCode()).isEqualTo(401);
	}

}
