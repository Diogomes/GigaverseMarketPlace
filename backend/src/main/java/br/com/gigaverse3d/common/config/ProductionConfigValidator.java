package br.com.gigaverse3d.common.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Recusa subir em produção com configuração insegura ou incompleta. A mensagem cita só NOMES de variáveis,
 * nunca valores (que podem ser segredos).
 * <p>
 * Roda no evento {@link ApplicationEnvironmentPreparedEvent} — antes de criar qualquer bean, portanto antes de
 * abrir conexão com o banco ou de o Flyway aplicar migrations. Registrado em {@code BackendApplication.main}.
 */
public class ProductionConfigValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private static final Logger log = LoggerFactory.getLogger(ProductionConfigValidator.class);

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment env = event.getEnvironment();
		if (env.acceptsProfiles(Profiles.of("prod"))) {
			check(env);
		}
	}

	static void check(Environment env) {
		List<String> problems = validate(env);
		if (!problems.isEmpty()) {
			throw new IllegalStateException("Configuração de produção inválida:\n - " + String.join("\n - ", problems)
					+ "\nVeja docs/ENVIRONMENT_VARIABLES.md e docs/DEPLOYMENT.md.");
		}
		warnings(env).forEach(w -> log.warn("Configuração de produção: {}", w));
	}

	static List<String> validate(Environment env) {
		List<String> problems = new ArrayList<>();

		String frontend = env.getProperty("app.auth.frontend-base-url", "");
		if (!isPublicHttps(frontend)) {
			problems.add("APP_FRONTEND_BASE_URL deve ser a URL pública HTTPS da loja (usada em links de e-mail e "
					+ "no sitemap); não pode ser localhost");
		}

		String origins = env.getProperty("app.cors.allowed-origins", "");
		List<String> originList = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
		if (originList.isEmpty() || !originList.stream().allMatch(ProductionConfigValidator::isPublicHttps)) {
			problems.add("APP_CORS_ALLOWED_ORIGINS deve listar apenas origens HTTPS públicas (sem localhost nem *)");
		}

		if (!env.getProperty("server.servlet.session.cookie.secure", Boolean.class, true)) {
			problems.add("SESSION_COOKIE_SECURE não pode ser false em produção");
		}

		String storage = env.getProperty("app.storage.type", "local");
		if ("s3".equals(storage)) {
			for (String[] p : new String[][] { { "app.storage.s3.bucket", "APP_STORAGE_S3_BUCKET" },
					{ "app.storage.s3.region", "APP_STORAGE_S3_REGION" },
					{ "app.storage.s3.public-base-url", "APP_STORAGE_PUBLIC_BASE_URL" } }) {
				if (env.getProperty(p[0], "").isBlank()) {
					problems.add(p[1] + " é obrigatório com APP_STORAGE_TYPE=s3");
				}
			}
		}
		else if (!"local".equals(storage)) {
			problems.add("APP_STORAGE_TYPE deve ser local ou s3");
		}

		boolean mailDisabled = env.getProperty("app.mail.allow-disabled", Boolean.class, false);
		if (env.getProperty("spring.mail.host", "").isBlank() && !mailDisabled) {
			problems.add("SMTP_HOST (e-mail transacional) é obrigatório: sem ele a recuperação de senha e os avisos "
					+ "de pedido não chegam. Para subir mesmo assim, defina APP_MAIL_ALLOW_DISABLED=true conscientemente");
		}
		String from = env.getProperty("app.mail.from", "");
		if (!mailDisabled && from.toLowerCase(Locale.ROOT).contains(".local")) {
			problems.add("APP_MAIL_FROM deve usar o domínio real verificado no provedor de e-mail");
		}
		return problems;
	}

	static List<String> warnings(Environment env) {
		List<String> warnings = new ArrayList<>();
		if ("local".equals(env.getProperty("app.storage.type", "local"))) {
			warnings.add("imagens em disco local (APP_STORAGE_TYPE=local): exige volume persistente com backup e "
					+ "uma única instância");
		}
		if (env.getProperty("app.mail.allow-disabled", Boolean.class, false)) {
			warnings.add("e-mail DESATIVADO (APP_MAIL_ALLOW_DISABLED=true): clientes não recebem e-mails");
		}
		if ("MANUAL".equals(env.getProperty("app.payments.provider", "MANUAL"))) {
			warnings.add("pagamento MANUAL: pedidos aguardam confirmação de um administrador (D-P01)");
		}
		if (!env.getProperty("app.bootstrap-admin.password", "").isBlank()) {
			warnings.add("APP_BOOTSTRAP_ADMIN_PASSWORD ainda definido: remova-o do ambiente após criar o primeiro ADMIN");
		}
		return warnings;
	}

	static boolean isPublicHttps(String url) {
		try {
			URI uri = URI.create(url.trim());
			String host = uri.getHost();
			return "https".equals(uri.getScheme()) && host != null && !host.equals("localhost")
					&& !host.startsWith("127.") && !host.endsWith(".local") && !url.contains("*");
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

}
