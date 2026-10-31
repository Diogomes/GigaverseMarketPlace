package br.com.gigaverse3d.payment.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import br.com.gigaverse3d.order.domain.Order;

/**
 * Integração de pagamento desacoplada do fornecedor. Regra central: o pedido só vira PAGO por
 * confirmação do provedor (webhook com assinatura verificada) ou baixa manual de um administrador —
 * nunca pelo retorno do navegador.
 */
public interface PaymentProvider {

	/** Evento de webhook já autenticado (assinatura conferida pelo provedor). */
	record WebhookEvent(String eventId, String type, String providerReference, BigDecimal amount) {
	}

	String code();

	/** Inicia a cobrança e devolve a referência no provedor (ou null) e as instruções ao cliente. */
	PaymentStart start(Order order);

	record PaymentStart(String providerReference, String customerInstructions) {
	}

	/** Instruções exibidas ao cliente enquanto o pagamento estiver pendente. */
	String instructions(Order order);

	/**
	 * Valida a assinatura e interpreta o webhook. Provedores sem webhook retornam vazio.
	 * @throws InvalidWebhookSignatureException se a assinatura não conferir
	 */
	default Optional<WebhookEvent> verifyWebhook(Map<String, String> headers, byte[] body) {
		return Optional.empty();
	}

	class InvalidWebhookSignatureException extends RuntimeException {

		public InvalidWebhookSignatureException() {
			super("Assinatura de webhook inválida.");
		}

	}

}
