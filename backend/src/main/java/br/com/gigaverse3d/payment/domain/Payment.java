package br.com.gigaverse3d.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.common.domain.TimestampedEntity;
import br.com.gigaverse3d.order.domain.Order;

/** Registro de pagamento. Nunca guarda número de cartão, CVV ou validade. */
@Entity
@Table(name = "payments")
public class Payment extends TimestampedEntity {

	public enum Status {
		PENDING, AUTHORIZED, PAID, FAILED, CANCELLED, REFUNDED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false, updatable = false)
	private Long orderId;

	@Column(nullable = false, length = 40, updatable = false)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10, updatable = false)
	private Order.PaymentMethod method;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.PENDING;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency = Money.CURRENCY;

	@Column(name = "provider_reference", length = 120)
	private String providerReference;

	@Column(name = "card_brand", length = 20)
	private String cardBrand;

	@Column(name = "card_last4", length = 4)
	private String cardLast4;

	@Column(name = "failure_reason")
	private String failureReason;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Version
	private Long version;

	protected Payment() {
	}

	public Payment(Long orderId, String provider, Order.PaymentMethod method, BigDecimal amount) {
		this.orderId = orderId;
		this.provider = provider;
		this.method = method;
		this.amount = Money.normalize(amount);
	}

	public void markPaid(Instant at, String providerReference) {
		this.status = Status.PAID;
		this.paidAt = at;
		if (providerReference != null) {
			this.providerReference = providerReference;
		}
	}

	public void markFailed(String reason) {
		this.status = Status.FAILED;
		this.failureReason = reason;
	}

	public void cancel() {
		if (status == Status.PENDING || status == Status.AUTHORIZED) {
			this.status = Status.CANCELLED;
		}
	}

	public void setProviderReference(String providerReference) {
		this.providerReference = providerReference;
	}

	public Long getId() {
		return id;
	}

	public Long getOrderId() {
		return orderId;
	}

	public String getProvider() {
		return provider;
	}

	public Order.PaymentMethod getMethod() {
		return method;
	}

	public Status getStatus() {
		return status;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getProviderReference() {
		return providerReference;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

}
