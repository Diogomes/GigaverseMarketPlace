package br.com.gigaverse3d.account.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.gigaverse3d.common.web.ApiExceptions.UnauthorizedException;

/** Acesso ao usuário autenticado da requisição atual. */
public final class CurrentUser {

	private CurrentUser() {
	}

	public static Optional<AccountUserDetails> get() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getPrincipal() instanceof AccountUserDetails p ? Optional.of(p) : Optional.empty();
	}

	public static AccountUserDetails require() {
		return get().orElseThrow(() -> new UnauthorizedException("Faça login para continuar."));
	}

	public static Long requireId() {
		return require().getId();
	}

}
