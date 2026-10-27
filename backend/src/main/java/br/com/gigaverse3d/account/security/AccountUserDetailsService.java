package br.com.gigaverse3d.account.security;

import java.time.Instant;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.UserRepository;

@Service
public class AccountUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public AccountUserDetailsService(UserRepository users) {
		this.users = users;
	}

	/**
	 * Sem @Transactional próprio: a exceção de "não encontrado" não pode marcar a transação
	 * do login como rollback-only (a contagem de falhas precisa ser gravada).
	 */
	@Override
	public UserDetails loadUserByUsername(String email) {
		return users.findByEmailAndDeletedAtIsNull(User.normalizeEmail(email))
			.map(u -> AccountUserDetails.of(u, Instant.now()))
			.orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada"));
	}

}
