package br.com.gigaverse3d.order.domain;

public class InvalidOrderTransitionException extends RuntimeException {

	public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
		super("Transição de status inválida: " + from + " → " + to + ".");
	}

}
