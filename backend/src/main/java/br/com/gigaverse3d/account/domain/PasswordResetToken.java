package br.com.gigaverse3d.account.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Token de redefinição de senha: só o hash SHA-256 é persistido; uso único e com expiração. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected PasswordResetToken() {
	}

	public PasswordResetToken(Long userId, String tokenHash, Instant createdAt, Instant expiresAt) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public boolean isUsable(Instant now) {
		return usedAt == null && expiresAt.isAfter(now);
	}

	/** Marca como usado (ou invalida). Respeita a constraint used_at <= expires_at. */
	public void consume(Instant now) {
		this.usedAt = now.isAfter(expiresAt) ? expiresAt : now;
	}

	public Long getUserId() {
		return userId;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

}
