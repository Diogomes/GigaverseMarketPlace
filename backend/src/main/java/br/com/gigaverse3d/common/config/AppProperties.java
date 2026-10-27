package br.com.gigaverse3d.common.config;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração da aplicação lida de {@code app.*}. Validada na inicialização:
 * a aplicação falha ao subir se um valor obrigatório estiver ausente ou inválido.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotNull @Valid Cors cors, @NotNull @Valid Store store) {

	public record Cors(@NotEmpty List<@NotBlank @Pattern(regexp = "^https?://[^*\\s]+$",
			message = "origem CORS deve ser explícita (sem curinga)") String> allowedOrigins) {
	}

	public record Store(
			@NotBlank String currency,
			@NotBlank @Pattern(regexp = "^\\d{8}$", message = "CEP de origem deve ter 8 dígitos") String shippingOriginPostalCode,
			@NotEmpty List<PaymentMethod> paymentMethods) {
	}

	public enum PaymentMethod {
		PIX, CARD
	}

}
