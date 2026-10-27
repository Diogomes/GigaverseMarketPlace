package br.com.gigaverse3d.account.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Registro imutável de concessão/revogação de consentimento (prova para LGPD). */
@Entity
@Table(name = "user_consents")
public class UserConsent {

	public enum Type {
		TERMS, PRIVACY_POLICY, MARKETING_EMAIL
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "consent_type", nullable = false, length = 30, updatable = false)
	private Type type;

	@Column(nullable = false, updatable = false)
	private boolean granted;

	@Column(name = "document_version", length = 20, updatable = false)
	private String documentVersion;

	@Column(name = "recorded_at", nullable = false, updatable = false)
	private Instant recordedAt;

	protected UserConsent() {
	}

	public UserConsent(Long userId, Type type, boolean granted, String documentVersion) {
		this.userId = userId;
		this.type = type;
		this.granted = granted;
		this.documentVersion = documentVersion;
	}

	@PrePersist
	void onCreate() {
		recordedAt = Instant.now();
	}

	public Long getUserId() {
		return userId;
	}

	public Type getType() {
		return type;
	}

	public boolean isGranted() {
		return granted;
	}

	public String getDocumentVersion() {
		return documentVersion;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

}
