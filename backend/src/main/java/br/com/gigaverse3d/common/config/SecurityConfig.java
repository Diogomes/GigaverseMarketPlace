package br.com.gigaverse3d.common.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.com.gigaverse3d.account.security.SecurityStampFilter;

/**
 * Autorização no servidor. Sessão por cookie HttpOnly (sem tokens no navegador) + CSRF para SPA
 * (cookie XSRF-TOKEN → header X-XSRF-TOKEN) em todas as requisições que alteram estado,
 * inclusive login. Esconder botões no frontend não é autorização: as regras ficam aqui.
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository,
			JdbcTemplate jdbc) throws Exception {
		http
			.cors(Customizer.withDefaults())
			// Webhooks de pagamento são servidor→servidor: autenticados por assinatura, não por cookie.
			.csrf(csrf -> csrf.spa().ignoringRequestMatchers("/api/v1/payments/webhooks/**"))
			.httpBasic(basic -> basic.disable())
			.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())
			.securityContext(ctx -> ctx.securityContextRepository(contextRepository))
			.addFilterAfter(new SecurityStampFilter(jdbc), SecurityContextHolderFilter.class)
			.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/api/v1/public/**", "/media/**", "/api/v1/auth/csrf", "/api/v1/auth/me",
						"/sitemap.xml")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register",
						"/api/v1/auth/password/forgot", "/api/v1/auth/password/reset")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/payments/webhooks/**").permitAll()
				// Confirmar pagamento e excluir: somente ADMIN.
				.requestMatchers(HttpMethod.POST, "/api/v1/admin/orders/*/confirm-payment").hasRole("ADMIN")
				// Equipe, auditoria e exportações (dados pessoais em massa): somente ADMIN.
				.requestMatchers("/api/v1/admin/users/**", "/api/v1/admin/audit/**", "/api/v1/admin/*/export")
				.hasRole("ADMIN")
				// Exclusões administrativas: somente ADMIN. Demais operações: ADMIN ou OPERATOR.
				.requestMatchers(HttpMethod.DELETE, "/api/v1/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "OPERATOR")
				.requestMatchers("/api/v1/account/**", "/api/v1/auth/**", "/api/v1/cart/**", "/api/v1/checkout/**")
				.authenticated()
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				// Despacho de erro do container (ex.: URL malformada) deve manter o status original (400),
				// não virar 401. A resposta de /error não expõe stack trace nem mensagens internas.
				.requestMatchers("/error").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.anyRequest().authenticated());
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.cors().allowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
