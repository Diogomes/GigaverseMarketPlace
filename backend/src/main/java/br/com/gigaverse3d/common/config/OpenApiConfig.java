package br.com.gigaverse3d.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Documentação OpenAPI (Swagger UI disponível apenas no perfil dev). */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI gigaverseOpenApi() {
		return new OpenAPI().info(new Info().title("Gigaverse3D API")
			.version("v1")
			.description("""
					API da loja Gigaverse3D. Erros seguem RFC 9457 (application/problem+json).
					Rotas /api/v1/admin/** exigem sessão de ADMIN ou OPERATOR (exclusões: só ADMIN) e
					token CSRF (cookie XSRF-TOKEN → header X-XSRF-TOKEN). Exemplos usam dados fictícios."""));
	}

}
