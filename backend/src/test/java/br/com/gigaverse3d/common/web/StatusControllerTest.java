package br.com.gigaverse3d.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.gigaverse3d.common.config.SecurityConfig;

@WebMvcTest(StatusController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class StatusControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void publicStatusIsAccessibleWithoutAuthentication() throws Exception {
		mvc.perform(get("/api/v1/public/status"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.service").value("gigaverse3d-api"))
			.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void nonPublicRoutesRequireAuthentication() throws Exception {
		mvc.perform(get("/api/v1/admin/anything")).andExpect(status().isUnauthorized());
	}

	@Test
	void corsAllowsConfiguredOriginWithCredentials() throws Exception {
		mvc.perform(options("/api/v1/public/status")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
			.andExpect(header().string("Access-Control-Allow-Credentials", "true"));
	}

	@Test
	void corsRejectsUnknownOrigin() throws Exception {
		mvc.perform(options("/api/v1/public/status")
				.header("Origin", "https://evil.example")
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isForbidden());
	}

}
