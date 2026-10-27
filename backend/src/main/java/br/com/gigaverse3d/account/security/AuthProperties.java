package br.com.gigaverse3d.account.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Parâmetros de autenticação ({@code app.auth.*}). */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
		@DefaultValue("5") int maxFailedLogins,
		@DefaultValue("15") int lockMinutes,
		@DefaultValue("30") int resetTokenMinutes,
		@DefaultValue("http://localhost:5173") String frontendBaseUrl,
		@DefaultValue("rascunho-2026-10") String legalDocumentsVersion,
		@DefaultValue RateLimits rateLimits) {

	/** Limites por janela deslizante de {@code windowMinutes}. */
	public record RateLimits(
			@DefaultValue("15") int windowMinutes,
			@DefaultValue("10") int loginPerAccount,
			@DefaultValue("50") int loginPerIp,
			@DefaultValue("10") int registerPerIp,
			@DefaultValue("5") int forgotPerIp,
			@DefaultValue("3") int forgotPerEmail) {
	}

}
