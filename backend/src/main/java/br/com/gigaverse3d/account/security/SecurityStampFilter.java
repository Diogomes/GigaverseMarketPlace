package br.com.gigaverse3d.account.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Encerra sessões cujo usuário trocou a senha, foi desativado ou removido depois do login:
 * compara o carimbo de segurança da sessão com o do banco a cada requisição autenticada.
 */
public class SecurityStampFilter extends OncePerRequestFilter {

	private final JdbcTemplate jdbc;

	public SecurityStampFilter(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof AccountUserDetails principal && !isStillValid(principal)) {
			SecurityContextHolder.clearContext();
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
		}
		chain.doFilter(request, response);
	}

	private boolean isStillValid(AccountUserDetails principal) {
		List<String> stamps = jdbc.queryForList("""
				SELECT security_stamp FROM users
				WHERE id = ? AND status = 'ACTIVE' AND deleted_at IS NULL AND role = ?""", String.class,
				principal.getId(), principal.getRole().name());
		return stamps.size() == 1 && stamps.getFirst().equals(principal.getSecurityStamp());
	}

}
