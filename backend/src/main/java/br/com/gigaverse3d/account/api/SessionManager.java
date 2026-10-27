package br.com.gigaverse3d.account.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import br.com.gigaverse3d.account.security.AccountUserDetails;

/** Cria/encerra a sessão HTTP autenticada (cookie HttpOnly). */
@Component
public class SessionManager {

	private final SecurityContextRepository repository;

	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	public SessionManager(SecurityContextRepository repository) {
		this.repository = repository;
	}

	/** Grava a autenticação na sessão trocando o id da sessão (proteção contra session fixation). */
	public void establish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
		HttpSession existing = request.getSession(false);
		if (existing != null) {
			request.changeSessionId();
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		repository.saveContext(context, request, response);
	}

	/** Atualiza o principal da sessão atual (ex.: após trocar a senha, com o novo carimbo). */
	public void refresh(AccountUserDetails principal, HttpServletRequest request, HttpServletResponse response) {
		establish(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()),
				request, response);
	}

	public void end(HttpServletRequest request, HttpServletResponse response) {
		logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
	}

}
