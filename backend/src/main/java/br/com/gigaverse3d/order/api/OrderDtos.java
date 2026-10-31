package br.com.gigaverse3d.order.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;
import br.com.gigaverse3d.payment.domain.Payment;
import br.com.gigaverse3d.shipping.ShippingService.ShippingQuote;

public final class OrderDtos {

	private OrderDtos() {
	}

	public record CheckoutRequest(
			@NotNull(message = "Escolha entrega ou retirada.") Order.ShippingMethod shippingMethod,
			@Schema(description = "Obrigatório para entrega: um endereço da própria conta") Long addressId,
			@NotNull(message = "Escolha PIX ou cartão.") Order.PaymentMethod paymentMethod,
			@Size(max = 500, message = "Máximo de 500 caracteres.") String customerNote,
			@Schema(description = "Total exibido ao cliente; se o servidor calcular outro valor, responde 409")
			@DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal expectedTotal) {
	}

	public record ShippingQuotesResponse(String originPostalCode, List<ShippingQuote> options) {
	}

	public record OrderItemView(String productSlug, String productName, String sku, String optionsSummary,
			BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
	}

	public record StatusChangeView(OrderStatus status, Instant at, String note) {
	}

	public record ShippingAddressView(String recipientName, String postalCode, String street, String number,
			String complement, String district, String city, String state) {
	}

	public record OrderSummaryView(Long number, Instant createdAt, OrderStatus status, BigDecimal total, int itemCount,
			String firstItemName, String firstItemSlug) {
	}

	public record OrderDetailView(Long number, Instant createdAt, OrderStatus status, List<OrderItemView> items,
			BigDecimal subtotal, BigDecimal shippingTotal, BigDecimal discountTotal, BigDecimal total, String currency,
			Order.PaymentMethod paymentMethod, Payment.Status paymentStatus, String paymentInstructions,
			Order.ShippingMethod shippingMethod, ShippingAddressView shippingAddress, String customerNote,
			List<StatusChangeView> history, boolean canCancel, Integer productionDays) {
	}

	/** Mudança de status com autor (só na visão da equipe). */
	public record AdminStatusChangeView(OrderStatus from, OrderStatus to, Instant at, String note, Long actorId,
			String actorName) {
	}

	/**
	 * Visão administrativa: inclui cliente, autor de cada mudança e as próximas transições
	 * permitidas (PAID fica de fora: usa a confirmação de pagamento, somente ADMIN).
	 */
	public record AdminOrderView(OrderDetailView order, Long customerId, String customerName, String customerEmail,
			Instant paidAt, String cancellationReason, List<AdminStatusChangeView> history,
			List<OrderStatus> allowedNext, boolean canConfirmPayment) {
	}

	public record AdminOrderListItem(Long number, Instant createdAt, OrderStatus status, BigDecimal total,
			String customerName, String customerEmail, Order.PaymentMethod paymentMethod,
			Order.ShippingMethod shippingMethod, int itemCount) {
	}

	public record StatusChangeRequest(@NotNull(message = "Informe o novo status.") OrderStatus status,
			@Size(max = 500) String reason, @Size(max = 60) String trackingCode) {
	}

	public record ConfirmPaymentRequest(@Size(max = 120) String reference, @Size(max = 500) String note) {
	}

}
