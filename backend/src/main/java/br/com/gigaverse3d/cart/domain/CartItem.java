package br.com.gigaverse3d.cart.domain;

import java.time.Instant;
import java.util.Arrays;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cart_items")
public class CartItem {

	public static final int MAX_QUANTITY = 99;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	/** Valores de opção escolhidos (ordenados, para comparar linhas iguais). */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "option_value_ids", nullable = false, columnDefinition = "bigint[]")
	private Long[] optionValueIds = new Long[0];

	@Column(nullable = false)
	private int quantity;

	@Column(name = "added_at", nullable = false, updatable = false)
	private Instant addedAt;

	protected CartItem() {
	}

	CartItem(Cart cart, Long productId, Long[] optionValueIds, int quantity) {
		this.cart = cart;
		this.productId = productId;
		this.optionValueIds = normalize(optionValueIds);
		setQuantity(quantity);
	}

	static Long[] normalize(Long[] ids) {
		Long[] copy = ids == null ? new Long[0] : ids.clone();
		Arrays.sort(copy);
		return copy;
	}

	@PrePersist
	void onCreate() {
		addedAt = Instant.now();
	}

	public void setQuantity(int quantity) {
		if (quantity < 1 || quantity > MAX_QUANTITY) {
			throw new IllegalArgumentException("Quantidade fora do intervalo");
		}
		this.quantity = quantity;
	}

	public Long getId() {
		return id;
	}

	public Long getProductId() {
		return productId;
	}

	public Long[] getOptionValueIds() {
		return optionValueIds.clone();
	}

	public int getQuantity() {
		return quantity;
	}

	public Instant getAddedAt() {
		return addedAt;
	}

}
