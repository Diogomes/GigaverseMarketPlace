package br.com.gigaverse3d.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigValidatorTest {

	static MockEnvironment valid() {
		return new MockEnvironment().withProperty("app.auth.frontend-base-url", "https://loja.exemplo.com.br")
			.withProperty("app.cors.allowed-origins", "https://loja.exemplo.com.br")
			.withProperty("server.servlet.session.cookie.secure", "true")
			.withProperty("app.storage.type", "local")
			.withProperty("spring.mail.host", "smtp.provedor.com")
			.withProperty("app.mail.from", "Gigaverse3D <nao-responda@exemplo.com.br>");
	}

	@Test
	void acceptsCompleteConfiguration() {
		assertThat(ProductionConfigValidator.validate(valid())).isEmpty();
	}

	@Test
	void rejectsLocalhostFrontendAndInsecureOrigins() {
		MockEnvironment env = valid().withProperty("app.auth.frontend-base-url", "http://localhost:5173")
			.withProperty("app.cors.allowed-origins", "https://loja.exemplo.com.br,http://localhost:5173");
		assertThat(ProductionConfigValidator.validate(env)).hasSize(2)
			.anyMatch(p -> p.startsWith("APP_FRONTEND_BASE_URL"))
			.anyMatch(p -> p.startsWith("APP_CORS_ALLOWED_ORIGINS"));
	}

	@Test
	void rejectsInsecureCookieAndMissingMail() {
		MockEnvironment env = valid().withProperty("server.servlet.session.cookie.secure", "false")
			.withProperty("spring.mail.host", "");
		assertThat(ProductionConfigValidator.validate(env)).anyMatch(p -> p.startsWith("SESSION_COOKIE_SECURE"))
			.anyMatch(p -> p.startsWith("SMTP_HOST"));
	}

	@Test
	void mailCanBeDisabledOnlyExplicitly() {
		MockEnvironment env = valid().withProperty("spring.mail.host", "")
			.withProperty("app.mail.allow-disabled", "true");
		assertThat(ProductionConfigValidator.validate(env)).isEmpty();
		assertThat(ProductionConfigValidator.warnings(env)).anyMatch(w -> w.contains("DESATIVADO"));
	}

	@Test
	void s3RequiresBucketRegionAndPublicUrl() {
		MockEnvironment env = valid().withProperty("app.storage.type", "s3");
		assertThat(ProductionConfigValidator.validate(env)).containsExactly(
				"APP_STORAGE_S3_BUCKET é obrigatório com APP_STORAGE_TYPE=s3",
				"APP_STORAGE_S3_REGION é obrigatório com APP_STORAGE_TYPE=s3",
				"APP_STORAGE_PUBLIC_BASE_URL é obrigatório com APP_STORAGE_TYPE=s3");
	}

	@Test
	void failureMessageNamesVariablesButNeverValues() {
		MockEnvironment env = valid().withProperty("app.auth.frontend-base-url", "http://segredo-interno:9999")
			.withProperty("spring.mail.host", "");
		assertThatThrownBy(() -> ProductionConfigValidator.check(env))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("APP_FRONTEND_BASE_URL")
			.hasMessageContaining("SMTP_HOST")
			.hasMessageNotContaining("segredo-interno");
	}

	@Test
	void recognisesPublicHttpsUrls() {
		assertThat(ProductionConfigValidator.isPublicHttps("https://gigaverse3d.com.br")).isTrue();
		assertThat(ProductionConfigValidator.isPublicHttps("https://127.0.0.1")).isFalse();
		assertThat(ProductionConfigValidator.isPublicHttps("https://loja.local")).isFalse();
		assertThat(ProductionConfigValidator.isPublicHttps("http://gigaverse3d.com.br")).isFalse();
		assertThat(ProductionConfigValidator.isPublicHttps("nada")).isFalse();
	}

	@Test
	void ignoresNonProductionProfiles() {
		MockEnvironment env = new MockEnvironment().withProperty("app.auth.frontend-base-url", "http://localhost:5173");
		env.setActiveProfiles("dev");
		new ProductionConfigValidator().onApplicationEvent(
				new org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent(
						new org.springframework.boot.bootstrap.DefaultBootstrapContext(),
						new org.springframework.boot.SpringApplication(), new String[0], env));
	}

	@Test
	void rejectsInvalidConfigurationWhenProdProfileIsActive() {
		MockEnvironment env = new MockEnvironment().withProperty("app.auth.frontend-base-url", "http://localhost:5173");
		env.setActiveProfiles("prod");
		assertThatThrownBy(() -> new ProductionConfigValidator().onApplicationEvent(
				new org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent(
						new org.springframework.boot.bootstrap.DefaultBootstrapContext(),
						new org.springframework.boot.SpringApplication(), new String[0], env)))
			.isInstanceOf(IllegalStateException.class);
	}

}
