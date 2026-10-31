package br.com.gigaverse3d.inventory.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.com.gigaverse3d.catalog.domain.Product;

/**
 * Saldo de estoque do produto. O banco garante saldo >= 0 e reservado <= saldo;
 * o {@code @Version} evita atualizações perdidas em alterações concorrentes.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

	@Id
	@Column(name = "product_id")
	private Long productId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "quantity_on_hand", nullable = false)
	private int quantityOnHand;

	@Column(name = "quantity_reserved", nullable = false)
	private int quantityReserved;

	@Column(name = "low_stock_threshold", nullable = false)
	private int lowStockThreshold;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private Long version;

	protected InventoryItem() {
	}

	public InventoryItem(Product product, int lowStockThreshold) {
		this.product = product;
		this.lowStockThreshold = lowStockThreshold;
	}

	@PrePersist
	@PreUpdate
	void touch() {
		updatedAt = Instant.now();
	}

	public void setLowStockThreshold(int lowStockThreshold) {
		if (lowStockThreshold < 0) {
			throw new IllegalArgumentException("Limite de estoque baixo não pode ser negativo");
		}
		this.lowStockThreshold = lowStockThreshold;
	}

	public int available() {
		return quantityOnHand - quantityReserved;
	}

	/** Mesmo critério do dashboard: disponível (em estoque − reservado) no mínimo ou abaixo. */
	public boolean isLowStock() {
		return available() <= lowStockThreshold;
	}

	/**
	 * Aplica uma variação de saldo e devolve o movimento correspondente.
	 * @throws InsufficientStockException se o saldo ficaria negativo ou abaixo do reservado
	 */
	public StockMovement apply(StockMovement.Type type, int delta, String reason) {
		if (delta == 0) {
			throw new IllegalArgumentException("A movimentação deve alterar o saldo");
		}
		int after = quantityOnHand + delta;
		if (after < quantityReserved) {
			throw new InsufficientStockException(productId, -delta, available());
		}
		quantityOnHand = after;
		return new StockMovement(product, type, delta, after, reason);
	}

	public Long getProductId() {
		return productId;
	}

	public Product getProduct() {
		return product;
	}

	public int getQuantityOnHand() {
		return quantityOnHand;
	}

	public int getQuantityReserved() {
		return quantityReserved;
	}

	public int getLowStockThreshold() {
		return lowStockThreshold;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
