package br.com.gigaverse3d.order.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Linha imutável do histórico de status do pedido. */
@Entity
@Table(name = "order_status_history")
public class OrderStatusChange {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, updatable = false)
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 30, updatable = false)
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 30, updatable = false)
	private OrderStatus toStatus;

	@Column(name = "changed_by_user_id", updatable = false)
	private Long changedByUserId;

	@Column(length = 500, updatable = false)
	private String reason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected OrderStatusChange() {
	}

	OrderStatusChange(Order order, OrderStatus fromStatus, OrderStatus toStatus, Long changedByUserId, String reason) {
		this.order = order;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.changedByUserId = changedByUserId;
		this.reason = reason;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public OrderStatus getFromStatus() {
		return fromStatus;
	}

	public OrderStatus getToStatus() {
		return toStatus;
	}

	public Long getChangedByUserId() {
		return changedByUserId;
	}

	public String getReason() {
		return reason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
