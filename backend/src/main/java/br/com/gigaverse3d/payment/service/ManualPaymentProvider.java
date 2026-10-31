package br.com.gigaverse3d.payment.service;

import org.springframework.stereotype.Component;

import br.com.gigaverse3d.order.domain.Order;

/**
 * Pagamento MANUAL (enquanto o gateway não é escolhido — D-P01): não cobra, não gera QR Code
 * nem coleta cartão. A equipe envia a cobrança (PIX ou link de cartão) e um ADMIN confirma o
 * recebimento no painel. Claramente identificado ao cliente.
 */
@Component
public class ManualPaymentProvider implements PaymentProvider {

	public static final String CODE = "MANUAL";

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public PaymentStart start(Order order) {
		return new PaymentStart(null, instructions(order));
	}

	@Override
	public String instructions(Order order) {
		return switch (order.getPaymentMethod()) {
			case PIX -> "Pagamento via PIX combinado com a equipe: enviaremos a chave PIX pelo e-mail cadastrado. "
					+ "O pedido é confirmado somente depois que o pagamento for conferido por nós.";
			case CARD -> "Pagamento com cartão: enviaremos um link de pagamento seguro pelo e-mail cadastrado. "
					+ "Nenhum dado de cartão é digitado ou armazenado neste site.";
		};
	}

}
