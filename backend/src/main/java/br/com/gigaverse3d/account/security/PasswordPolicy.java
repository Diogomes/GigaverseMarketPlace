package br.com.gigaverse3d.account.security;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;

/**
 * Política de senha (alinhada ao NIST 800-63B): tamanho mínimo, sem regras de composição
 * forçada, recusando senhas óbvias e senhas iguais ao e-mail.
 */
@Component
public class PasswordPolicy {

	public static final int MIN_LENGTH = 10;

	public static final int MAX_LENGTH = 128;

	private static final Set<String> COMMON = Set.of("1234567890", "12345678910", "0123456789", "senha12345",
			"senhasenha", "password12", "qwertyuiop", "abcdefghij", "gigaverse3d", "gigaverse123", "minhasenha1",
			"1111111111", "aaaaaaaaaa", "iloveyou12", "123456789a", "a123456789");

	public void validate(String password, String email, String field) {
		if (password == null || password.length() < MIN_LENGTH) {
			throw new FieldValidationException(field, "A senha deve ter pelo menos " + MIN_LENGTH + " caracteres.");
		}
		if (password.length() > MAX_LENGTH) {
			throw new FieldValidationException(field, "A senha deve ter no máximo " + MAX_LENGTH + " caracteres.");
		}
		String lower = password.toLowerCase(Locale.ROOT);
		if (COMMON.contains(lower) || lower.chars().distinct().count() <= 2) {
			throw new FieldValidationException(field, "Essa senha é muito comum. Escolha outra.");
		}
		if (email != null && (lower.equals(email.toLowerCase(Locale.ROOT))
				|| lower.equals(email.toLowerCase(Locale.ROOT).split("@")[0]))) {
			throw new FieldValidationException(field, "A senha não pode ser igual ao e-mail.");
		}
	}

}
