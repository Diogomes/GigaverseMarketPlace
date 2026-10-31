package br.com.gigaverse3d.order;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.payment.service.PaymentProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Provedor fictício SÓ PARA TESTES: simula um gateway que assina webhooks com HMAC-SHA256
 * (header X-Signature). Permite testar assinatura, idempotência e validação de estado.
 */
public class TestHmacPaymentProvider implements PaymentProvider {

	public static final String CODE = "TESTPAY";

	public static final String SECRET = "segredo-de-teste-nao-usar-em-producao";

	private final JsonMapper json = JsonMapper.builder().build();

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public PaymentStart start(Order order) {
		return new PaymentStart("ref-" + order.getNumber(), "teste");
	}

	@Override
	public String instructions(Order order) {
		return "teste";
	}

	@Override
	public Optional<WebhookEvent> verifyWebhook(Map<String, String> headers, byte[] body) {
		String received = headers.getOrDefault("x-signature", "");
		if (!MessageDigest.isEqual(sign(body).getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8))) {
			throw new InvalidWebhookSignatureException();
		}
		JsonNode node = json.readTree(body);
		return Optional.of(new WebhookEvent(node.get("id").asString(), node.get("type").asString(),
				node.get("reference").asString(), new BigDecimal(node.get("amount").asString())));
	}

	public static String sign(byte[] body) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(body));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		TestHmacPaymentProvider testHmacPaymentProvider() {
			return new TestHmacPaymentProvider();
		}

	}

}
