package br.com.gigaverse3d.cart.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import br.com.gigaverse3d.common.domain.TimestampedEntity;

/** Carrinho do cliente logado. Não guarda preços: o servidor recalcula a cada leitura e no checkout. */
@Entity
@Table(name = "carts")
public class Cart extends TimestampedEntity {

	public static final int MAX_LINES = 30;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", unique = true, updatable = false)
	private Long userId;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<CartItem> items = new ArrayList<>();

	protected Cart() {
	}

	public Cart(Long userId) {
		this.userId = userId;
	}

	public Optional<CartItem> find(Long productId, Long[] optionValueIds) {
		Long[] sorted = CartItem.normalize(optionValueIds);
		return items.stream()
			.filter(i -> i.getProductId().equals(productId) && Arrays.equals(i.getOptionValueIds(), sorted))
			.findFirst();
	}

	public CartItem add(Long productId, Long[] optionValueIds, int quantity) {
		CartItem item = new CartItem(this, productId, optionValueIds, quantity);
		items.add(item);
		return item;
	}

	public void remove(CartItem item) {
		items.remove(item);
	}

	public void clear() {
		items.clear();
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public List<CartItem> getItems() {
		return Collections.unmodifiableList(items);
	}

}
