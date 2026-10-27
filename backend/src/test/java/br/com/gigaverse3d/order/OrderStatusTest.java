package br.com.gigaverse3d.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.gigaverse3d.order.domain.InvalidOrderTransitionException;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;

class OrderStatusTest {

	@ParameterizedTest
	@CsvSource({ "PENDING_PAYMENT,PAID", "PENDING_PAYMENT,CANCELLED", "PAID,IN_PRODUCTION", "PAID,SHIPPED",
			"IN_PRODUCTION,READY_FOR_PICKUP", "IN_PRODUCTION,SHIPPED", "READY_FOR_PICKUP,DELIVERED",
			"SHIPPED,DELIVERED" })
	void allowsValidTransitions(OrderStatus from, OrderStatus to) {
		assertThat(from.canTransitionTo(to)).isTrue();
	}

	@ParameterizedTest
	@CsvSource({ "PENDING_PAYMENT,SHIPPED", "PENDING_PAYMENT,DELIVERED", "PAID,PENDING_PAYMENT", "SHIPPED,CANCELLED",
			"DELIVERED,CANCELLED", "CANCELLED,PAID", "DELIVERED,SHIPPED" })
	void rejectsInvalidTransitions(OrderStatus from, OrderStatus to) {
		assertThat(from.canTransitionTo(to)).isFalse();
	}

	@Test
	void finalStatesHaveNoExit() {
		assertThat(OrderStatus.DELIVERED.isFinal()).isTrue();
		assertThat(OrderStatus.CANCELLED.isFinal()).isTrue();
	}

	@Test
	void customerMayCancelOnlyBeforePayment() {
		assertThat(OrderStatus.PENDING_PAYMENT.customerMayCancel()).isTrue();
		assertThat(OrderStatus.PAID.customerMayCancel()).isFalse();
	}

	@Test
	void orderRejectsInvalidTransitionAndKeepsState() {
		Order order = new Order(1L, "Cliente", "c@exemplo.com", Order.PaymentMethod.PIX, Order.ShippingMethod.PICKUP,
				null, null);
		assertThatThrownBy(() -> order.transitionTo(OrderStatus.SHIPPED, null, null))
			.isInstanceOf(InvalidOrderTransitionException.class);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
	}

	@Test
	void itemsCannotChangeAfterPayment() {
		Order order = new Order(1L, "Cliente", "c@exemplo.com", Order.PaymentMethod.PIX, Order.ShippingMethod.PICKUP,
				null, null);
		order.addItem(null, "Item", "GV-1", null, new BigDecimal("10.00"), 1);
		order.transitionTo(OrderStatus.PAID, null, null);
		assertThatThrownBy(() -> order.addItem(null, "Outro", "GV-2", null, new BigDecimal("5.00"), 1))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void deliveryRequiresAddress() {
		assertThatThrownBy(() -> new Order(1L, "Cliente", "c@exemplo.com", Order.PaymentMethod.CARD,
				Order.ShippingMethod.DELIVERY, null, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
