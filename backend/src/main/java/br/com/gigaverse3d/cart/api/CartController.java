package br.com.gigaverse3d.cart.api;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.cart.api.CartDtos.AddItemRequest;
import br.com.gigaverse3d.cart.api.CartDtos.CartView;
import br.com.gigaverse3d.cart.api.CartDtos.MergeRequest;
import br.com.gigaverse3d.cart.api.CartDtos.UpdateItemRequest;
import br.com.gigaverse3d.cart.service.CartService;

@Tag(name = "Carrinho", description = "Preços e disponibilidade sempre recalculados no servidor")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

	private final CartService cart;

	public CartController(CartService cart) {
		this.cart = cart;
	}

	@Operation(summary = "Carrinho atual com preços recalculados")
	@GetMapping
	public CartView get() {
		return cart.view(CurrentUser.requireId());
	}

	@Operation(summary = "Adiciona item (soma à linha igual, se existir)")
	@PostMapping("/items")
	public CartView add(@Valid @RequestBody AddItemRequest req) {
		return cart.add(CurrentUser.requireId(), req);
	}

	@Operation(summary = "Altera a quantidade de uma linha")
	@PutMapping("/items/{itemId}")
	public CartView update(@PathVariable Long itemId, @Valid @RequestBody UpdateItemRequest req) {
		return cart.updateQuantity(CurrentUser.requireId(), itemId, req.quantity());
	}

	@Operation(summary = "Remove uma linha")
	@DeleteMapping("/items/{itemId}")
	public CartView remove(@PathVariable Long itemId) {
		return cart.remove(CurrentUser.requireId(), itemId);
	}

	@Operation(summary = "Junta o carrinho do visitante (navegador) ao da conta após o login")
	@PostMapping("/merge")
	public CartView merge(@Valid @RequestBody MergeRequest req) {
		return cart.merge(CurrentUser.requireId(), req.items());
	}

}
