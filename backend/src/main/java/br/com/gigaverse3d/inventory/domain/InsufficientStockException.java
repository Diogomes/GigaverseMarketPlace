package br.com.gigaverse3d.inventory.domain;

public class InsufficientStockException extends RuntimeException {

	private final Long productId;

	private final int requested;

	private final int available;

	public InsufficientStockException(Long productId, int requested, int available) {
		super("Estoque insuficiente para o produto " + productId + ": solicitado " + requested + ", disponível "
				+ available);
		this.productId = productId;
		this.requested = requested;
		this.available = available;
	}

	public Long getProductId() {
		return productId;
	}

	public int getRequested() {
		return requested;
	}

	public int getAvailable() {
		return available;
	}

}
