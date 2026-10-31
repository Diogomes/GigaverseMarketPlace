package br.com.gigaverse3d.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.gigaverse3d.account.AuthTestSupport;
import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;

/** Webhook de pagamento: assinatura, idempotência, valor e estado do pedido. */
class PaymentWebhookTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	long number;

	String reference;

	@BeforeEach
	void pendingOrderWithTestProvider() {
		long userId = AuthTestSupport.createUser(jdbc, encoder, AuthTestSupport.newEmail(), "CUSTOMER");
		long orderId = jdbc.queryForObject("""
				INSERT INTO orders (user_id, subtotal, total, payment_method, shipping_method, customer_name, customer_email)
				VALUES (?, 50.00, 50.00, 'PIX', 'PICKUP', 'Cliente', 'c@exemplo.com') RETURNING id""", Long.class, userId);
		jdbc.update("""
				INSERT INTO order_status_history (order_id, to_status) VALUES (?, 'PENDING_PAYMENT')""", orderId);
		number = jdbc.queryForObject("SELECT number FROM orders WHERE id = ?", Long.class, orderId);
		reference = "ref-" + CatalogFixtures.unique("pay");
		jdbc.update("""
				INSERT INTO payments (order_id, provider, method, amount, provider_reference)
				VALUES (?, 'TESTPAY', 'PIX', 50.00, ?)""", orderId, reference);
	}

	private ResultActions webhook(String body, String signature) throws Exception {
		return mvc.perform(post("/api/v1/payments/webhooks/testpay").contentType(MediaType.APPLICATION_JSON)
			.header("X-Signature", signature).content(body));
	}

	private String event(String id, String amount) {
		return """
				{"id":"%s","type":"payment.paid","reference":"%s","amount":"%s"}""".formatted(id, reference, amount);
	}

	private static String sign(String body) {
		return TestHmacPaymentProvider.sign(body.getBytes(StandardCharsets.UTF_8));
	}

	private String orderStatus() {
		return jdbc.queryForObject("SELECT status FROM orders WHERE number = ?", String.class, number);
	}

	@Test
	void validSignedEventConfirmsPaymentOnceWithoutCsrfOrSession() throws Exception {
		String body = event(CatalogFixtures.unique("evt"), "50.00");
		webhook(body, sign(body)).andExpect(status().isOk());
		assertThat(orderStatus()).isEqualTo("PAID");
		assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE provider_reference = ?", String.class, reference))
			.isEqualTo("PAID");

		// reenvio do mesmo evento: idempotente
		webhook(body, sign(body)).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM order_status_history h JOIN orders o ON o.id = h.order_id "
				+ "WHERE o.number = ? AND h.to_status = 'PAID'", Integer.class, number)).isEqualTo(1);
	}

	@Test
	void invalidSignatureIsRejectedAndChangesNothing() throws Exception {
		String body = event(CatalogFixtures.unique("evt"), "50.00");
		webhook(body, "assinatura-falsa").andExpect(status().isUnauthorized());
		webhook(body, sign(body.replace("50.00", "5.00"))).andExpect(status().isUnauthorized());
		assertThat(orderStatus()).isEqualTo("PENDING_PAYMENT");
	}

	@Test
	void amountMismatchIsRejected() throws Exception {
		String body = event(CatalogFixtures.unique("evt"), "1.00");
		webhook(body, sign(body)).andExpect(status().isConflict());
		assertThat(orderStatus()).isEqualTo("PENDING_PAYMENT");
	}

	@Test
	void paymentForCancelledOrderDoesNotReviveIt() throws Exception {
		jdbc.update("UPDATE orders SET status = 'CANCELLED', cancelled_at = now() WHERE number = ?", number);
		String body = event(CatalogFixtures.unique("evt"), "50.00");
		webhook(body, sign(body)).andExpect(status().isOk());
		assertThat(orderStatus()).isEqualTo("CANCELLED");
		assertThat(jdbc.queryForList("SELECT action FROM audit_log WHERE entity_type = 'order' AND entity_id = ?",
				String.class, String.valueOf(number))).contains("PAYMENT_FOR_NON_PENDING_ORDER");
	}

	@Test
	void unknownProviderIs404AndManualHasNoWebhook() throws Exception {
		webhook("{}", "x").andReturn();
		mvc.perform(post("/api/v1/payments/webhooks/inexistente").content("{}")).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/payments/webhooks/manual").content("{}")).andExpect(status().isNotFound());
	}

}
