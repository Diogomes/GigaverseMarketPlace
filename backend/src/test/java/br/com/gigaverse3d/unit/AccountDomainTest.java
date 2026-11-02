package br.com.gigaverse3d.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.com.gigaverse3d.account.domain.PasswordResetToken;
import br.com.gigaverse3d.account.domain.User;

class AccountDomainTest {

	static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");

	static User customer() {
		return new User("cliente@exemplo.com", "{hash}", "Cliente Fictício", User.Role.CUSTOMER);
	}

	@Test
	void emailIsNormalized() {
		assertThat(User.normalizeEmail("  Cliente.Teste@Exemplo.COM ")).isEqualTo("cliente.teste@exemplo.com");
	}

	@Test
	void locksAfterMaxFailedAttemptsAndUnlocksAfterWindow() {
		User u = customer();
		for (int i = 0; i < 4; i++) {
			u.recordFailedLogin(5, Duration.ofMinutes(15), NOW);
		}
		assertThat(u.isLocked(NOW)).isFalse();
		assertThat(u.getFailedLoginAttempts()).isEqualTo(4);
		u.recordFailedLogin(5, Duration.ofMinutes(15), NOW);
		assertThat(u.isLocked(NOW)).isTrue();
		assertThat(u.canLogIn(NOW.plusSeconds(60))).isFalse();
		assertThat(u.getFailedLoginAttempts()).as("contador zera ao bloquear").isZero();
		assertThat(u.canLogIn(NOW.plus(Duration.ofMinutes(15)).plusSeconds(1))).isTrue();
	}

	@Test
	void successfulLoginResetsCounters() {
		User u = customer();
		u.recordFailedLogin(5, Duration.ofMinutes(15), NOW);
		u.recordSuccessfulLogin(NOW);
		assertThat(u.getFailedLoginAttempts()).isZero();
		assertThat(u.getLockedUntil()).isNull();
		assertThat(u.getLastLoginAt()).isEqualTo(NOW);
	}

	@Test
	void sensitiveChangesRotateSecurityStamp() {
		User u = customer();
		String s0 = u.getSecurityStamp();
		u.changePassword("{novo-hash}");
		String s1 = u.getSecurityStamp();
		assertThat(s1).isNotEqualTo(s0);
		u.changeRole(User.Role.OPERATOR);
		String s2 = u.getSecurityStamp();
		assertThat(s2).isNotEqualTo(s1);
		u.disable();
		assertThat(u.getSecurityStamp()).isNotEqualTo(s2);
		assertThat(u.canLogIn(NOW)).isFalse();
		u.enable();
		assertThat(u.canLogIn(NOW)).isTrue();
	}

	@Test
	void anonymizationRemovesPersonalDataAndBlocksLogin() {
		User u = customer();
		u.anonymize("removido-123@anonimo.invalid", "{inutilizavel}", NOW);
		assertThat(u.getEmail()).isEqualTo("removido-123@anonimo.invalid");
		assertThat(u.getFullName()).isEqualTo("Conta removida");
		assertThat(u.getStatus()).isEqualTo(User.Status.ANONYMIZED);
		assertThat(u.canLogIn(NOW)).isFalse();
	}

	@Test
	void resetTokenIsSingleUseAndExpires() {
		PasswordResetToken t = new PasswordResetToken(1L, "hash", NOW, NOW.plus(Duration.ofMinutes(30)));
		assertThat(t.isUsable(NOW.plusSeconds(60))).isTrue();
		assertThat(t.isUsable(NOW.plus(Duration.ofMinutes(31)))).isFalse();
		t.consume(NOW.plusSeconds(120));
		assertThat(t.isUsable(NOW.plusSeconds(130))).as("já usado").isFalse();

		PasswordResetToken late = new PasswordResetToken(1L, "hash2", NOW, NOW.plusSeconds(10));
		late.consume(NOW.plusSeconds(3600));
		assertThat(late.getUsedAt()).as("nunca depois da expiração").isEqualTo(NOW.plusSeconds(10));
	}

}
