package br.com.gigaverse3d.order.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados do pedido e transições válidas. Quem pode disparar cada transição é regra da
 * camada de serviço (Fase 06): o cliente só cancela pedidos aguardando pagamento; PAID só
 * via confirmação do provedor (webhook verificado) ou baixa manual por administrador.
 */
public enum OrderStatus {

	PENDING_PAYMENT, PAID, IN_PRODUCTION, READY_FOR_PICKUP, SHIPPED, DELIVERED, CANCELLED;

	public Set<OrderStatus> allowedNext() {
		return switch (this) {
			case PENDING_PAYMENT -> EnumSet.of(PAID, CANCELLED);
			case PAID -> EnumSet.of(IN_PRODUCTION, READY_FOR_PICKUP, SHIPPED, CANCELLED);
			case IN_PRODUCTION -> EnumSet.of(READY_FOR_PICKUP, SHIPPED, CANCELLED);
			case READY_FOR_PICKUP -> EnumSet.of(DELIVERED, CANCELLED);
			case SHIPPED -> EnumSet.of(DELIVERED);
			case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
		};
	}

	public boolean canTransitionTo(OrderStatus next) {
		return allowedNext().contains(next);
	}

	public boolean isFinal() {
		return allowedNext().isEmpty();
	}

	/** Pedido conta como venda (métricas) a partir do pagamento confirmado. */
	public boolean isPaid() {
		return this != PENDING_PAYMENT && this != CANCELLED;
	}

	/** Cancelamento pelo próprio cliente (sem estorno envolvido). */
	public boolean customerMayCancel() {
		return this == PENDING_PAYMENT;
	}

}
