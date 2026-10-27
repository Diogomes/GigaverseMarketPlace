package br.com.gigaverse3d.common.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Registro imutável de ação relevante. Nunca guardar senhas, tokens ou dados de pagamento em {@code details}. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_user_id", updatable = false)
	private Long actorUserId;

	@Column(nullable = false, length = 80, updatable = false)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 60, updatable = false)
	private String entityType;

	@Column(name = "entity_id", length = 60, updatable = false)
	private String entityId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(updatable = false)
	private String details;

	@Column(name = "request_id", length = 64, updatable = false)
	private String requestId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AuditLog() {
	}

	AuditLog(Long actorUserId, String action, String entityType, String entityId, String details) {
		this.actorUserId = actorUserId;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.details = details;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getActorUserId() {
		return actorUserId;
	}

	public String getAction() {
		return action;
	}

	public String getEntityType() {
		return entityType;
	}

	public String getEntityId() {
		return entityId;
	}

	public String getDetails() {
		return details;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
