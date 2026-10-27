package br.com.gigaverse3d.catalog.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Grupo de opções do produto (ex.: Tamanho, Acabamento). */
@Entity
@Table(name = "product_options")
public class ProductOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, length = 40)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<ProductOptionValue> values = new ArrayList<>();

	protected ProductOption() {
	}

	ProductOption(Product product, String name, int sortOrder) {
		this.product = product;
		this.name = name;
		this.sortOrder = sortOrder;
	}

	public ProductOptionValue addValue(String label, BigDecimal priceDelta) {
		ProductOptionValue value = new ProductOptionValue(this, label, priceDelta, values.size());
		values.add(value);
		return value;
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public String getName() {
		return name;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public List<ProductOptionValue> getValues() {
		return Collections.unmodifiableList(values);
	}

}
