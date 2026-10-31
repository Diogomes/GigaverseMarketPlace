package br.com.gigaverse3d.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Generated;

import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.common.domain.TimestampedEntity;

/**
 * Pedido. Totais são sempre calculados aqui a partir dos itens (nunca recebidos do cliente);
 * o banco reforça {@code total = subtotal + frete - desconto}.
 */
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class Order extends TimestampedEntity {

	public enum PaymentMethod {
		PIX, CARD
	}

	public enum ShippingMethod {
		DELIVERY, PICKUP
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Número público do pedido, gerado pela sequence do banco (a partir de 1000). */
	@Generated
	@Column(nullable = false, unique = true, insertable = false, updatable = false)
	private Long number;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status = OrderStatus.PENDING_PAYMENT;

	@Column(nullable = false, length = 3)
	private String currency = Money.CURRENCY;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal = Money.of("0");

	@Column(name = "shipping_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal shippingTotal = Money.of("0");

	@Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountTotal = Money.of("0");

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total = Money.of("0");

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 10)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "shipping_method", nullable = false, length = 20)
	private ShippingMethod shippingMethod;

	@Column(name = "customer_name", nullable = false, length = 120)
	private String customerName;

	@Column(name = "customer_email", nullable = false, length = 254)
	private String customerEmail;

	@Embedded
	private ShippingAddress shippingAddress;

	@Column(name = "customer_note", length = 500)
	private String customerNote;

	@Column(name = "idempotency_key", length = 64, updatable = false)
	private String idempotencyKey;

	@Column(name = "cancellation_reason", length = 500)
	private String cancellationReason;

	@Column(name = "placed_at", nullable = false, updatable = false)
	private Instant placedAt;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	@OrderBy("id ASC")
	private List<OrderStatusChange> history = new ArrayList<>();

	@Version
	private Long version;

	protected Order() {
	}

	public Order(Long userId, String customerName, String customerEmail, PaymentMethod paymentMethod,
			ShippingMethod shippingMethod, ShippingAddress shippingAddress, String idempotencyKey) {
		if (shippingMethod == ShippingMethod.DELIVERY && shippingAddress == null) {
			throw new IllegalArgumentException("Entrega exige endereço");
		}
		this.userId = Objects.requireNonNull(userId);
		this.customerName = customerName;
		this.customerEmail = customerEmail;
		this.paymentMethod = Objects.requireNonNull(paymentMethod);
		this.shippingMethod = Objects.requireNonNull(shippingMethod);
		this.shippingAddress = shippingAddress;
		this.idempotencyKey = idempotencyKey;
		this.placedAt = Instant.now();
		this.history.add(new OrderStatusChange(this, null, OrderStatus.PENDING_PAYMENT, userId, null));
	}

	public OrderItem addItem(Long productId, String productName, String sku, String optionsSummary,
			BigDecimal unitPrice, int quantity) {
		requireEditable();
		OrderItem item = new OrderItem(this, productId, productName, sku, optionsSummary, unitPrice, quantity);
		items.add(item);
		recalculateTotals();
		return item;
	}

	public void setCustomerNote(String customerNote) {
		requireEditable();
		this.customerNote = customerNote;
	}

	public void setShippingTotal(BigDecimal shippingTotal) {
		requireEditable();
		this.shippingTotal = Money.normalize(shippingTotal);
		recalculateTotals();
	}

	private void requireEditable() {
		if (status != OrderStatus.PENDING_PAYMENT) {
			throw new IllegalStateException("Itens e valores só podem mudar antes do pagamento");
		}
	}

	private void recalculateTotals() {
		subtotal = Money.normalize(items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
		total = Money.normalize(subtotal.add(shippingTotal).subtract(discountTotal));
	}

	/** Aplica uma transição validada pela máquina de estados e registra no histórico. */
	public void transitionTo(OrderStatus next, Long actorUserId, String reason) {
		if (!status.canTransitionTo(next)) {
			throw new InvalidOrderTransitionException(status, next);
		}
		Instant now = Instant.now();
		if (next == OrderStatus.PAID) {
			paidAt = now;
		}
		if (next == OrderStatus.CANCELLED) {
			cancelledAt = now;
			cancellationReason = reason;
		}
		history.add(new OrderStatusChange(this, status, next, actorUserId, reason));
		status = next;
	}

	public Long getId() {
		return id;
	}

	public Long getNumber() {
		return number;
	}

	public Long getUserId() {
		return userId;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getShippingTotal() {
		return shippingTotal;
	}

	public BigDecimal getDiscountTotal() {
		return discountTotal;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public ShippingMethod getShippingMethod() {
		return shippingMethod;
	}

	public String getCustomerName() {
		return customerName;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public ShippingAddress getShippingAddress() {
		return shippingAddress;
	}

	public String getCustomerNote() {
		return customerNote;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getCancellationReason() {
		return cancellationReason;
	}

	public Instant getPlacedAt() {
		return placedAt;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public List<OrderItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public List<OrderStatusChange> getHistory() {
		return Collections.unmodifiableList(history);
	}

}
