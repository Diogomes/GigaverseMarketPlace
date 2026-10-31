package br.com.gigaverse3d.order.api;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.account.repository.AddressRepository;
import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.order.api.OrderDtos.CheckoutRequest;
import br.com.gigaverse3d.order.api.OrderDtos.OrderDetailView;
import br.com.gigaverse3d.order.api.OrderDtos.ShippingQuotesResponse;
import br.com.gigaverse3d.order.service.OrderService;
import br.com.gigaverse3d.shipping.ShippingService;

@Tag(name = "Checkout", description = "Totais calculados no servidor; pagamento nunca confirmado pelo navegador")
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

	private final OrderService orders;

	private final ShippingService shipping;

	private final AddressRepository addresses;

	private final String originPostalCode;

	public CheckoutController(OrderService orders, ShippingService shipping, AddressRepository addresses,
			@Value("${app.store.shipping-origin-postal-code}") String originPostalCode) {
		this.orders = orders;
		this.shipping = shipping;
		this.addresses = addresses;
		this.originPostalCode = originPostalCode;
	}

	@Operation(summary = "Opções de frete para um endereço da conta (retirada sempre listada se habilitada)")
	@GetMapping("/shipping-options")
	public ShippingQuotesResponse shippingOptions(@RequestParam(required = false) Long addressId) {
		String postalCode = null;
		if (addressId != null) {
			postalCode = addresses.findByIdAndUserIdAndDeletedAtIsNull(addressId, CurrentUser.requireId())
				.orElseThrow(() -> new FieldValidationException("addressId", "Endereço não encontrado."))
				.getPostalCode();
		}
		return new ShippingQuotesResponse(originPostalCode, shipping.quote(postalCode));
	}

	@Operation(summary = "Cria o pedido a partir do carrinho (header Idempotency-Key obrigatório)")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderDetailView checkout(@Valid @RequestBody CheckoutRequest req,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		return orders.checkout(CurrentUser.requireId(), req, idempotencyKey);
	}

}
