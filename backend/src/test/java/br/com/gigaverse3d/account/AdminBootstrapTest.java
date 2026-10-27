package br.com.gigaverse3d.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.PasswordPolicy;
import br.com.gigaverse3d.account.service.AdminBootstrap;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;

/** Executa em transação com rollback: rebaixa admins de outros testes só durante o teste. */
@Transactional
class AdminBootstrapTest extends PostgresIntegrationTest {

	@Autowired
	UserRepository users;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	PasswordPolicy policy;

	@Autowired
	AuditService audit;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void noAdminsYet() {
		jdbc.update("UPDATE users SET role = 'OPERATOR' WHERE role = 'ADMIN'");
	}

	private AdminBootstrap bootstrap(String email, String password) {
		return new AdminBootstrap(users, encoder, policy, audit, email, password, "Dono da Loja");
	}

	@Test
	void createsFirstAdminOnlyOnceAndNeverStoresPlainPassword() throws Exception {
		String email = AuthTestSupport.newEmail();
		bootstrap(email, "Senha-Do-Dono-Muito-Forte-1").run(null);
		assertThat(jdbc.queryForObject("SELECT role FROM users WHERE email = ?", String.class, email)).isEqualTo("ADMIN");
		assertThat(jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email))
			.startsWith("{argon2}")
			.doesNotContain("Senha-Do-Dono");

		String second = AuthTestSupport.newEmail();
		bootstrap(second, "Outra-Senha-Muito-Forte-2").run(null);
		assertThat(users.findByEmailAndDeletedAtIsNull(second)).isEmpty();
	}

	@Test
	void rejectsWeakPassword() {
		assertThatThrownBy(() -> bootstrap(AuthTestSupport.newEmail(), "admin").run(null))
			.isInstanceOf(FieldValidationException.class);
	}

	@Test
	void doesNothingWithoutConfiguration() throws Exception {
		long before = users.count();
		bootstrap("", "").run(null);
		assertThat(users.count()).isEqualTo(before);
	}

}
