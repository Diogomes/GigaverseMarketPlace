package br.com.gigaverse3d.order.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.gigaverse3d.common.domain.Money;

/** Item do pedido com snapshot de nome, SKU, opções e preço unitário no momento da compra. */
@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	/** Referência ao produto atual (pode virar null se o produto for removido; o snapshot permanece). */
	@Column(name = "product_id")
	private Long productId;

	@Column(name = "product_name", nullable = false, length = 120)
	private String productName;

	@Column(nullable = false, length = 32)
	private String sku;

	@Column(name = "options_summary")
	private String optionsSummary;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	protected OrderItem() {
	}

	OrderItem(Order order, Long productId, String productName, String sku, String optionsSummary,
			BigDecimal unitPrice, int quantity) {
		if (quantity < 1) {
			throw new IllegalArgumentException("Quantidade deve ser >= 1");
		}
		this.order = order;
		this.productId = productId;
		this.productName = productName;
		this.sku = sku;
		this.optionsSummary = optionsSummary;
		this.unitPrice = Money.normalize(unitPrice);
		this.quantity = quantity;
		this.lineTotal = Money.normalize(this.unitPrice.multiply(BigDecimal.valueOf(quantity)));
	}

	public Long getId() {
		return id;
	}

	public Order getOrder() {
		return order;
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public String getSku() {
		return sku;
	}

	public String getOptionsSummary() {
		return optionsSummary;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}

}
