package br.com.gigaverse3d.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.gigaverse3d.cart.domain.Cart;
import br.com.gigaverse3d.cart.domain.CartItem;
import br.com.gigaverse3d.order.domain.InvalidOrderTransitionException;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;
import br.com.gigaverse3d.order.domain.ShippingAddress;
import br.com.gigaverse3d.payment.domain.Payment;
import br.com.gigaverse3d.shipping.ShippingService;
import br.com.gigaverse3d.shipping.ShippingService.ShippingProperties;
import br.com.gigaverse3d.shipping.ShippingService.ShippingProperties.Pickup;
import br.com.gigaverse3d.shipping.ShippingService.ShippingProperties.Zone;

class OrderCartShippingDomainTest {

	static final ShippingAddress ADDRESS = new ShippingAddress("Cliente Fictício", "50010000", "Rua de Teste", "100",
			"Apto 1", "Centro", "Recife", "PE");

	static Order pixPickup() {
		return new Order(7L, "Cliente Fictício", "cliente@exemplo.com", Order.PaymentMethod.PIX,
				Order.ShippingMethod.PICKUP, null, "chave-idem-1");
	}

	// ---------- Pedido ----------

	@Test
	void totalsAreSubtotalPlusShipping() {
		Order order = new Order(7L, "Cliente", "c@exemplo.com", Order.PaymentMethod.CARD, Order.ShippingMethod.DELIVERY,
				ADDRESS, null);
		order.addItem(1L, "Dragão", "GV-1", "Cor: Dourado", new BigDecimal("125.50"), 2);
		order.addItem(2L, "Vaso", "GV-2", null, new BigDecimal("49.90"), 1);
		assertThat(order.getSubtotal()).isEqualTo(new BigDecimal("300.90"));
		order.setShippingTotal(new BigDecimal("15"));
		assertThat(order.getShippingTotal()).isEqualTo(new BigDecimal("15.00"));
		assertThat(order.getTotal()).isEqualTo(new BigDecimal("315.90"));
		assertThat(order.getItems()).hasSize(2);
		assertThat(order.getItems().getFirst().getLineTotal()).isEqualTo(new BigDecimal("251.00"));
	}

	@Test
	void newOrderIsPendingWithInitialHistory() {
		Order order = pixPickup();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(order.getHistory()).hasSize(1);
		assertThat(order.getPlacedAt()).isNotNull();
		assertThat(order.getPaidAt()).isNull();
	}

	@Test
	void fullHappyPathRecordsEveryStep() {
		Order order = pixPickup();
		order.addItem(1L, "Item", "GV-1", null, new BigDecimal("10"), 1);
		order.transitionTo(OrderStatus.PAID, 99L, "PIX conferido");
		assertThat(order.getPaidAt()).isNotNull();
		order.transitionTo(OrderStatus.IN_PRODUCTION, 99L, null);
		order.transitionTo(OrderStatus.READY_FOR_PICKUP, 99L, null);
		order.transitionTo(OrderStatus.DELIVERED, 99L, null);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
		assertThat(order.getHistory()).hasSize(5);
		assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED, 99L, "tarde demais"))
			.isInstanceOf(InvalidOrderTransitionException.class);
	}

	@Test
	void cancellationKeepsReasonAndTimestamp() {
		Order order = pixPickup();
		order.transitionTo(OrderStatus.CANCELLED, 7L, "Desisti da compra");
		assertThat(order.getCancellationReason()).isEqualTo("Desisti da compra");
		assertThat(order.getCancelledAt()).isNotNull();
		assertThat(OrderStatus.CANCELLED.allowedNext()).isEmpty();
	}

	@Test
	void valuesFreezeAfterPayment() {
		Order order = pixPickup();
		order.addItem(1L, "Item", "GV-1", null, new BigDecimal("10"), 1);
		order.transitionTo(OrderStatus.PAID, null, null);
		assertThatThrownBy(() -> order.setShippingTotal(new BigDecimal("5"))).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> order.setCustomerNote("mudança")).isInstanceOf(IllegalStateException.class);
		assertThat(order.getTotal()).isEqualTo(new BigDecimal("10.00"));
	}

	@Test
	void everyNonFinalStatusHasAnExitAndFinalOnesDoNot() {
		for (OrderStatus s : OrderStatus.values()) {
			assertThat(s.allowedNext().isEmpty()).as(s.name()).isEqualTo(s.isFinal());
		}
	}

	// ---------- Pagamento ----------

	@Test
	void paymentLifecycle() {
		Payment p = new Payment(1L, "MANUAL", Order.PaymentMethod.PIX, new BigDecimal("99.9"));
		assertThat(p.getAmount()).isEqualTo(new BigDecimal("99.90"));
		assertThat(p.getStatus()).isEqualTo(Payment.Status.PENDING);
		p.markPaid(java.time.Instant.now(), "E2E-PIX-123");
		assertThat(p.getStatus()).isEqualTo(Payment.Status.PAID);
		assertThat(p.getProviderReference()).isEqualTo("E2E-PIX-123");
		p.cancel();
		assertThat(p.getStatus()).as("pago não volta a cancelado").isEqualTo(Payment.Status.PAID);

		Payment pending = new Payment(2L, "MANUAL", Order.PaymentMethod.CARD, BigDecimal.TEN);
		pending.cancel();
		assertThat(pending.getStatus()).isEqualTo(Payment.Status.CANCELLED);
		Payment failed = new Payment(3L, "MANUAL", Order.PaymentMethod.CARD, BigDecimal.TEN);
		failed.markFailed("Cartão recusado (fictício)");
		assertThat(failed.getStatus()).isEqualTo(Payment.Status.FAILED);
		assertThat(org.springframework.test.util.ReflectionTestUtils.getField(failed, "failureReason"))
			.isEqualTo("Cartão recusado (fictício)");
	}

	// ---------- Carrinho ----------

	@Test
	void cartFindsSameLineRegardlessOfOptionOrder() {
		Cart cart = new Cart(7L);
		CartItem item = cart.add(1L, new Long[] { 22L, 11L }, 2);
		assertThat(cart.find(1L, new Long[] { 11L, 22L })).containsSame(item);
		assertThat(cart.find(1L, new Long[] { 11L })).isEmpty();
		assertThat(cart.find(2L, new Long[] { 11L, 22L })).isEmpty();
		assertThat(item.getOptionValueIds()).containsExactly(11L, 22L);
	}

	@Test
	void cartItemQuantityBounds() {
		Cart cart = new Cart(7L);
		CartItem item = cart.add(1L, null, 1);
		item.setQuantity(CartItem.MAX_QUANTITY);
		assertThatThrownBy(() -> item.setQuantity(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> item.setQuantity(100)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> cart.add(2L, null, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThat(item.getQuantity()).isEqualTo(99);
	}

	@Test
	void cartRemoveAndClear() {
		Cart cart = new Cart(7L);
		CartItem a = cart.add(1L, null, 1);
		cart.add(2L, null, 3);
		cart.remove(a);
		assertThat(cart.getItems()).extracting(CartItem::getProductId).containsExactly(2L);
		cart.clear();
		assertThat(cart.getItems()).isEmpty();
	}

	// ---------- Frete ----------

	static ShippingService shipping(boolean pickup, List<Zone> zones) {
		return new ShippingService(new ShippingProperties(new Pickup(pickup, "Retirada"), zones, false));
	}

	static final List<Zone> ZONES = List.of(
			new Zone("Pernambuco", "50000000", "56999999", new BigDecimal("15"), 2, 4),
			new Zone("Nordeste", "40000000", "65999999", new BigDecimal("25"), 4, 8));

	@Test
	void firstMatchingZoneWins() {
		ShippingService s = shipping(true, ZONES);
		assertThat(s.delivery("53625816")).get()
			.satisfies(q -> {
				assertThat(q.label()).isEqualTo("Entrega — Pernambuco");
				assertThat(q.price()).isEqualTo(new BigDecimal("15.00"));
			});
		assertThat(s.delivery("40010000")).get().extracting(q -> q.label()).isEqualTo("Entrega — Nordeste");
		assertThat(s.delivery("56999999")).as("limite superior inclusivo").isPresent();
		assertThat(s.delivery("01001000")).as("SP fora das faixas").isEmpty();
	}

	@Test
	void invalidPostalCodesNeverQuote() {
		ShippingService s = shipping(true, ZONES);
		assertThat(s.delivery(null)).isEmpty();
		assertThat(s.delivery("5362-816")).isEmpty();
		assertThat(s.delivery("53625-816")).isEmpty();
		assertThat(s.delivery("ABCDEFGH")).isEmpty();
	}

	@Test
	void pickupIsFreeAndOptional() {
		assertThat(shipping(true, ZONES).quote("53625816")).hasSize(2).first().satisfies(q -> {
			assertThat(q.method()).isEqualTo(Order.ShippingMethod.PICKUP);
			assertThat(q.price()).isEqualByComparingTo("0");
		});
		assertThat(shipping(false, ZONES).quote("53625816")).hasSize(1);
		assertThat(shipping(true, null).quote("53625816")).as("sem faixas: só retirada").hasSize(1);
		assertThat(shipping(false, null).quote("53625816")).isEmpty();
	}

}
