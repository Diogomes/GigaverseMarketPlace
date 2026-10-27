package br.com.gigaverse3d.account.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.PasswordPolicy;
import br.com.gigaverse3d.common.audit.AuditService;

/**
 * Cria o PRIMEIRO administrador a partir de variáveis de ambiente, sem credenciais no código:
 * APP_BOOTSTRAP_ADMIN_EMAIL, APP_BOOTSTRAP_ADMIN_PASSWORD e (opcional) APP_BOOTSTRAP_ADMIN_NAME.
 * Só age se ainda não existir nenhum ADMIN; depois disso as variáveis devem ser removidas.
 * A senha nunca é registrada em log.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final PasswordPolicy passwordPolicy;

	private final AuditService audit;

	private final String email;

	private final String password;

	private final String name;

	public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
			AuditService audit, @Value("${app.bootstrap-admin.email:}") String email,
			@Value("${app.bootstrap-admin.password:}") String password,
			@Value("${app.bootstrap-admin.name:Administrador}") String name) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.audit = audit;
		this.email = email;
		this.password = password;
		this.name = name;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(email)) {
			return;
		}
		if (users.existsByRoleAndDeletedAtIsNull(User.Role.ADMIN)) {
			log.info("Bootstrap de administrador ignorado: já existe um ADMIN. Remova APP_BOOTSTRAP_ADMIN_* do ambiente.");
			return;
		}
		String normalized = User.normalizeEmail(email);
		if (users.existsByEmailAndDeletedAtIsNull(normalized)) {
			throw new IllegalStateException(
					"Bootstrap de administrador: o e-mail informado já pertence a uma conta. Use outro e-mail.");
		}
		passwordPolicy.validate(password, normalized, "APP_BOOTSTRAP_ADMIN_PASSWORD");
		User admin = users.save(new User(normalized, passwordEncoder.encode(password), name, User.Role.ADMIN));
		audit.recordAs(admin.getId(), "ADMIN_BOOTSTRAPPED", "user", admin.getId(), Map.of());
		log.warn("Administrador inicial criado para {}. Remova APP_BOOTSTRAP_ADMIN_* do ambiente agora.", normalized);
	}

}
