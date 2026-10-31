package br.com.gigaverse3d.payment.api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.common.web.ApiExceptions.UnauthorizedException;
import br.com.gigaverse3d.order.service.OrderService;
import br.com.gigaverse3d.payment.repository.PaymentRepository;
import br.com.gigaverse3d.payment.service.PaymentProvider;
import br.com.gigaverse3d.payment.service.PaymentProvider.InvalidWebhookSignatureException;
import br.com.gigaverse3d.payment.service.PaymentProvider.WebhookEvent;
import br.com.gigaverse3d.payment.service.PaymentProviders;

/**
 * Webhooks de provedores de pagamento: assinatura verificada pelo provedor, idempotência por
 * id do evento, e validação de valor/estado antes de confirmar. Sem sessão nem CSRF (servidor→servidor).
 */
@Tag(name = "Pagamentos — webhooks")
@RestController
public class PaymentWebhookController {

	private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

	private final PaymentProviders providers;

	private final PaymentRepository payments;

	private final OrderService orders;

	public PaymentWebhookController(PaymentProviders providers, PaymentRepository payments, OrderService orders) {
		this.providers = providers;
		this.payments = payments;
		this.orders = orders;
	}

	@Operation(summary = "Recebe notificação do provedor (corpo bruto; assinatura obrigatória)")
	@PostMapping("/api/v1/payments/webhooks/{provider}")
	@Transactional
	public ResponseEntity<Void> receive(@PathVariable String provider, @RequestBody byte[] body,
			HttpServletRequest request) {
		PaymentProvider p = providers.byCode(provider.toUpperCase(Locale.ROOT))
			.orElseThrow(() -> new NotFoundException("Provedor desconhecido."));
		Map<String, String> headers = Collections.list(request.getHeaderNames())
			.stream()
			.collect(Collectors.toMap(h -> h.toLowerCase(Locale.ROOT), request::getHeader, (a, b) -> a));
		WebhookEvent event;
		try {
			event = p.verifyWebhook(headers, body).orElseThrow(() -> new NotFoundException("Provedor sem webhooks."));
		}
		catch (InvalidWebhookSignatureException e) {
			log.warn("Webhook de pagamento com assinatura inválida (provedor {})", p.code());
			throw new UnauthorizedException("Assinatura inválida.");
		}
		if (payments.registerWebhookEvent(p.code(), event.eventId(), event.type(), sha256(body)) == 0) {
			return ResponseEntity.ok().build(); // já processado: idempotente
		}
		if ("payment.paid".equals(event.type())) {
			payments.findByProviderAndProviderReference(p.code(), event.providerReference())
				.ifPresentOrElse(payment -> orders.confirmFromProvider(payment, event.amount(), event.providerReference()),
						() -> log.warn("Webhook para pagamento desconhecido (provedor {})", p.code()));
		}
		return ResponseEntity.status(HttpStatus.OK).build();
	}

	static String sha256(byte[] body) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
