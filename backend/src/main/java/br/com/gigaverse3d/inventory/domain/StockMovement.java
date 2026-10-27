package br.com.gigaverse3d.inventory.domain;

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

import br.com.gigaverse3d.catalog.domain.Product;

/** Registro imutável de cada alteração de estoque (trilha auditável). */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

	public enum Type {
		INITIAL, PURCHASE, ADJUSTMENT, SALE, RETURN, CANCELLATION
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, updatable = false)
	private Product product;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 20, updatable = false)
	private Type type;

	@Column(name = "quantity_delta", nullable = false, updatable = false)
	private int quantityDelta;

	@Column(name = "quantity_after", nullable = false, updatable = false)
	private int quantityAfter;

	@Column(length = 255, updatable = false)
	private String reason;

	@Column(name = "order_id", updatable = false)
	private Long orderId;

	@Column(name = "actor_user_id", updatable = false)
	private Long actorUserId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected StockMovement() {
	}

	StockMovement(Product product, Type type, int quantityDelta, int quantityAfter, String reason) {
		this.product = product;
		this.type = type;
		this.quantityDelta = quantityDelta;
		this.quantityAfter = quantityAfter;
		this.reason = reason;
	}

	public StockMovement withOrder(Long orderId) {
		this.orderId = orderId;
		return this;
	}

	public StockMovement byActor(Long actorUserId) {
		this.actorUserId = actorUserId;
		return this;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public Type getType() {
		return type;
	}

	public int getQuantityDelta() {
		return quantityDelta;
	}

	public int getQuantityAfter() {
		return quantityAfter;
	}

	public String getReason() {
		return reason;
	}

	public Long getOrderId() {
		return orderId;
	}

	public Long getActorUserId() {
		return actorUserId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
