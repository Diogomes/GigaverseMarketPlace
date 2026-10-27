package br.com.gigaverse3d.account.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.com.gigaverse3d.common.domain.TimestampedEntity;

@Entity
@Table(name = "users")
public class User extends TimestampedEntity {

	public enum Role {
		CUSTOMER, OPERATOR, ADMIN
	}

	public enum Status {
		ACTIVE, DISABLED, ANONYMIZED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 254)
	private String email;

	/** Hash (Argon2id/BCrypt, Fase 05). Nunca a senha em claro. */
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.ACTIVE;

	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	/** Muda quando a senha é trocada ou a conta desativada: invalida sessões existentes. */
	@Column(name = "security_stamp", nullable = false, length = 36)
	private String securityStamp = UUID.randomUUID().toString();

	@Version
	private Long version;

	protected User() {
	}

	public User(String email, String passwordHash, String fullName, Role role) {
		this.email = normalizeEmail(email);
		this.passwordHash = Objects.requireNonNull(passwordHash);
		this.fullName = Objects.requireNonNull(fullName).strip();
		this.role = Objects.requireNonNull(role);
	}

	public static String normalizeEmail(String email) {
		return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
	}

	public boolean isLocked(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	public boolean canLogIn(Instant now) {
		return status == Status.ACTIVE && deletedAt == null && !isLocked(now);
	}

	/** Conta a falha; ao atingir o limite, bloqueia temporariamente e zera o contador. */
	public void recordFailedLogin(int maxAttempts, Duration lockDuration, Instant now) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			lockedUntil = now.plus(lockDuration);
			failedLoginAttempts = 0;
		}
	}

	public void recordSuccessfulLogin(Instant now) {
		failedLoginAttempts = 0;
		lockedUntil = null;
		lastLoginAt = now;
	}

	/** Troca o hash da senha e renova o carimbo (derruba as outras sessões). */
	public void changePassword(String newPasswordHash) {
		this.passwordHash = Objects.requireNonNull(newPasswordHash);
		this.failedLoginAttempts = 0;
		this.lockedUntil = null;
		rotateSecurityStamp();
	}

	public void rotateSecurityStamp() {
		this.securityStamp = UUID.randomUUID().toString();
	}

	public void updateName(String fullName) {
		this.fullName = Objects.requireNonNull(fullName).strip();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public Role getRole() {
		return role;
	}

	public Status getStatus() {
		return status;
	}

	public Instant getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public String getSecurityStamp() {
		return securityStamp;
	}

}
