package br.com.gigaverse3d.cart.api;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CartDtos {

	private CartDtos() {
	}

	public record AddItemRequest(
			@NotBlank(message = "Informe o produto.") @Size(max = 120) String productSlug,
			@Size(max = 10) List<Long> optionValueIds,
			@NotNull @Min(value = 1, message = "Quantidade mínima 1.") @Max(value = 99, message = "Quantidade máxima 99.")
			Integer quantity) {
	}

	public record UpdateItemRequest(
			@NotNull @Min(value = 1, message = "Quantidade mínima 1.") @Max(value = 99, message = "Quantidade máxima 99.")
			Integer quantity) {
	}

	public record MergeRequest(@Size(max = 30) List<@Valid AddItemRequest> items) {
	}

	public record OptionView(String group, String label, BigDecimal priceDelta) {
	}

	public record CartItemView(Long id, String productSlug, String productName, String imageUrl,
			List<OptionView> options, BigDecimal unitPrice, int quantity, BigDecimal lineTotal, int maxQuantity,
			String problem) {
	}

	/** Valores calculados no servidor no momento da leitura. */
	public record CartView(List<CartItemView> items, int itemCount, BigDecimal subtotal, String currency,
			boolean hasProblems) {
	}

}
