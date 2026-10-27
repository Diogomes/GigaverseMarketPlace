package br.com.gigaverse3d.account.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.api.AccountDtos.RegisterRequest;
import br.com.gigaverse3d.account.domain.CustomerProfile;
import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.domain.UserConsent;
import br.com.gigaverse3d.account.repository.CustomerProfileRepository;
import br.com.gigaverse3d.account.repository.UserConsentRepository;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.AuthProperties;
import br.com.gigaverse3d.account.security.PasswordPolicy;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.UnauthorizedException;

@Service
public class AuthService {

	/** Mesma mensagem para senha errada, conta inexistente, bloqueada ou desativada (não revela qual). */
	public static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos.";

	private final AuthenticationManager authenticationManager;

	private final UserRepository users;

	private final CustomerProfileRepository profiles;

	private final UserConsentRepository consents;

	private final PasswordEncoder passwordEncoder;

	private final PasswordPolicy passwordPolicy;

	private final AuthProperties properties;

	private final AuditService audit;

	private final Clock clock;

	public AuthService(AuthenticationManager authenticationManager, UserRepository users,
			CustomerProfileRepository profiles, UserConsentRepository consents, PasswordEncoder passwordEncoder,
			PasswordPolicy passwordPolicy, AuthProperties properties, AuditService audit, Clock clock) {
		this.authenticationManager = authenticationManager;
		this.users = users;
		this.profiles = profiles;
		this.consents = consents;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.properties = properties;
		this.audit = audit;
		this.clock = clock;
	}

	/** Cadastro público: sempre cria CUSTOMER, independentemente do que vier no corpo. */
	@Transactional
	public User register(RegisterRequest req) {
		String email = User.normalizeEmail(req.email());
		passwordPolicy.validate(req.password(), email, "password");
		if (users.existsByEmailAndDeletedAtIsNull(email)) {
			throw new ConflictException("Não foi possível criar a conta com este e-mail. Se ele já é seu, faça login ou recupere a senha.");
		}
		User user = users.save(new User(email, passwordEncoder.encode(req.password()), req.fullName(),
				User.Role.CUSTOMER));
		profiles.save(new CustomerProfile(user.getId()));
		String version = properties.legalDocumentsVersion();
		consents.save(new UserConsent(user.getId(), UserConsent.Type.TERMS, true, version));
		consents.save(new UserConsent(user.getId(), UserConsent.Type.PRIVACY_POLICY, true, version));
		consents.save(new UserConsent(user.getId(), UserConsent.Type.MARKETING_EMAIL,
				Boolean.TRUE.equals(req.marketingOptIn()), version));
		audit.recordAs(user.getId(), "ACCOUNT_CREATED", "user", user.getId(), Map.of("role", "CUSTOMER"));
		return user;
	}

	/**
	 * Verifica credenciais com contagem de falhas e bloqueio temporário.
	 * Não faz rollback no 401 para que a contagem de falhas seja gravada.
	 */
	@Transactional(noRollbackFor = UnauthorizedException.class)
	public Authentication authenticate(String rawEmail, String password) {
		String email = User.normalizeEmail(rawEmail);
		Instant now = clock.instant();
		Optional<User> found = users.findByEmailAndDeletedAtIsNull(email);
		try {
			// Executa o provedor mesmo para e-mail inexistente (tempo de resposta semelhante).
			Authentication auth = authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
			User user = found.orElseThrow();
			user.recordSuccessfulLogin(now);
			audit.recordAs(user.getId(), "LOGIN_SUCCESS", "user", user.getId(), Map.of());
			return auth;
		}
		catch (AuthenticationException e) {
			found.filter(u -> !u.isLocked(now)).ifPresent(u -> {
				u.recordFailedLogin(properties.maxFailedLogins(), Duration.ofMinutes(properties.lockMinutes()), now);
				audit.recordAs(u.getId(), u.isLocked(now) ? "LOGIN_LOCKED" : "LOGIN_FAILED", "user", u.getId(),
						Map.of());
			});
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}
	}

}
