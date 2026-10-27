package br.com.gigaverse3d.catalog.domain;

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

@Entity
@Table(name = "product_option_values")
public class ProductOptionValue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "option_id", nullable = false)
	private ProductOption option;

	@Column(nullable = false, length = 40)
	private String label;

	/** Acréscimo ao preço do produto (>= 0). */
	@Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
	private BigDecimal priceDelta;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected ProductOptionValue() {
	}

	ProductOptionValue(ProductOption option, String label, BigDecimal priceDelta, int sortOrder) {
		this.option = option;
		this.label = label;
		this.priceDelta = Money.normalize(priceDelta);
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public ProductOption getOption() {
		return option;
	}

	public String getLabel() {
		return label;
	}

	public BigDecimal getPriceDelta() {
		return priceDelta;
	}

	public int getSortOrder() {
		return sortOrder;
	}

}
