package br.com.gigaverse3d.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cada teste viola UMA regra do banco e confere o nome da constraint violada.
 * Rodam em transação com rollback: não deixam dados para os demais testes.
 */
@Transactional
class ConstraintsIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	JdbcTemplate jdbc;

	long userId;

	long categoryId;

	@BeforeEach
	void setUp() {
		userId = jdbc.queryForObject("""
				INSERT INTO users (email, password_hash, full_name, role)
				VALUES ('cliente@exemplo.com', '$argon2id$fake-hash-for-tests', 'Cliente Teste', 'CUSTOMER')
				RETURNING id""", Long.class);
		categoryId = jdbc.queryForObject(
				"INSERT INTO categories (slug, name) VALUES ('teste', 'Teste') RETURNING id", Long.class);
	}

	private long insertProduct(String slug, String sku) {
		return jdbc.queryForObject("""
				INSERT INTO products (slug, sku, name, description, category_id, product_type, price)
				VALUES (?, ?, 'Produto', 'Descrição', ?, 'READY', 100.00) RETURNING id""", Long.class, slug, sku,
				categoryId);
	}

	private void assertViolates(String constraint, String sql, Object... args) {
		assertThatThrownBy(() -> jdbc.update(sql, args)).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining(constraint);
	}

	// --- contas ---

	@Test
	void emailMustBeStoredLowercase() {
		assertViolates("users_email_lowercase", """
				INSERT INTO users (email, password_hash, full_name, role)
				VALUES ('Outro@Exemplo.com', 'x', 'Outro', 'CUSTOMER')""");
	}

	@Test
	void emailIsUniqueAmongActiveUsers() {
		assertViolates("users_email_unique", """
				INSERT INTO users (email, password_hash, full_name, role)
				VALUES ('cliente@exemplo.com', 'x', 'Duplicado', 'CUSTOMER')""");
	}

	@Test
	void roleMustBeKnown() {
		assertViolates("users_role_valid", """
				INSERT INTO users (email, password_hash, full_name, role)
				VALUES ('root@exemplo.com', 'x', 'Root', 'SUPERUSER')""");
	}

	@Test
	void onlyOneDefaultAddressPerUser() {
		String sql = """
				INSERT INTO addresses (user_id, label, recipient_name, postal_code, street, number, district, city, state, is_default)
				VALUES (?, ?, 'Cliente', '53625816', 'Rua A', '1', 'Centro', 'Cidade', 'PE', true)""";
		jdbc.update(sql, userId, "Casa");
		assertViolates("addresses_one_default_per_user", sql, userId, "Trabalho");
	}

	@Test
	void postalCodeMustHaveEightDigits() {
		assertViolates("addresses_postal_code_format", """
				INSERT INTO addresses (user_id, label, recipient_name, postal_code, street, number, district, city, state)
				VALUES (?, 'Casa', 'Cliente', '5362-816', 'Rua A', '1', 'Centro', 'Cidade', 'PE')""", userId);
	}

	@Test
	void resetTokenStoresOnlyAHash() {
		assertViolates("password_reset_tokens_hash_format", """
				INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
				VALUES (?, 'token-em-claro', now() + interval '30 minutes')""", userId);
	}

	// --- catálogo e estoque ---

	@Test
	void skuAndSlugAreUnique() {
		insertProduct("produto-a", "GV-001");
		assertThatThrownBy(() -> insertProduct("produto-b", "GV-001"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("products_sku_key");
	}

	@Test
	void promoPriceMustBeLowerThanPrice() {
		long id = insertProduct("produto-a", "GV-001");
		assertViolates("products_promo_price_valid", "UPDATE products SET promo_price = 100.00 WHERE id = ?", id);
	}

	@Test
	void madeToOrderRequiresProductionDays() {
		long id = insertProduct("produto-a", "GV-001");
		assertViolates("products_production_days_required",
				"UPDATE products SET product_type = 'MADE_TO_ORDER' WHERE id = ?", id);
	}

	@Test
	void onlyBrlIsAccepted() {
		long id = insertProduct("produto-a", "GV-001");
		assertViolates("products_currency_brl", "UPDATE products SET currency = 'USD' WHERE id = ?", id);
	}

	@Test
	void stockCannotBeNegative() {
		long id = insertProduct("produto-a", "GV-001");
		jdbc.update("INSERT INTO inventory_items (product_id, quantity_on_hand) VALUES (?, 2)", id);
		assertViolates("inventory_on_hand_non_negative",
				"UPDATE inventory_items SET quantity_on_hand = quantity_on_hand - 3 WHERE product_id = ?", id);
	}

	@Test
	void reservedCannotExceedOnHand() {
		long id = insertProduct("produto-a", "GV-001");
		jdbc.update("INSERT INTO inventory_items (product_id, quantity_on_hand) VALUES (?, 2)", id);
		assertViolates("inventory_reserved_within_on_hand",
				"UPDATE inventory_items SET quantity_reserved = 3 WHERE product_id = ?", id);
	}

	@Test
	void manualAdjustmentRequiresReason() {
		long id = insertProduct("produto-a", "GV-001");
		assertViolates("stock_movements_adjustment_needs_reason", """
				INSERT INTO stock_movements (product_id, movement_type, quantity_delta, quantity_after)
				VALUES (?, 'ADJUSTMENT', -1, 0)""", id);
	}

	// --- pedidos e pagamentos ---

	private long insertPickupOrder(String total) {
		return jdbc.queryForObject("""
				INSERT INTO orders (user_id, subtotal, shipping_total, total, payment_method, shipping_method,
				                    customer_name, customer_email)
				VALUES (?, 100.00, 0, ?::numeric, 'PIX', 'PICKUP', 'Cliente Teste', 'cliente@exemplo.com')
				RETURNING id""", Long.class, userId, total);
	}

	@Test
	void orderTotalMustMatchItsComponents() {
		assertThatThrownBy(() -> insertPickupOrder("90.00")).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("orders_total_consistent");
	}

	@Test
	void deliveryOrderRequiresAddressSnapshot() {
		assertViolates("orders_delivery_needs_address", """
				INSERT INTO orders (user_id, subtotal, total, payment_method, shipping_method, customer_name, customer_email)
				VALUES (?, 100.00, 100.00, 'PIX', 'DELIVERY', 'Cliente', 'cliente@exemplo.com')""", userId);
	}

	@Test
	void orderItemLineTotalMustMatch() {
		long orderId = insertPickupOrder("100.00");
		assertViolates("order_items_line_total_consistent", """
				INSERT INTO order_items (order_id, product_name, sku, unit_price, quantity, line_total)
				VALUES (?, 'Produto', 'GV-001', 50.00, 2, 99.99)""", orderId);
	}

	@Test
	void sameIdempotencyKeyCannotCreateTwoOrders() {
		String sql = """
				INSERT INTO orders (user_id, subtotal, total, payment_method, shipping_method, customer_name,
				                    customer_email, idempotency_key)
				VALUES (?, 100.00, 100.00, 'PIX', 'PICKUP', 'Cliente', 'cliente@exemplo.com', 'chave-123')""";
		jdbc.update(sql, userId);
		assertViolates("orders_idempotency_unique", sql, userId);
	}

	@Test
	void cardPaymentStoresOnlyLastFourDigits() {
		long orderId = insertPickupOrder("100.00");
		assertViolates("payments_card_last4_format", """
				INSERT INTO payments (order_id, provider, method, amount, card_brand, card_last4)
				VALUES (?, 'MANUAL', 'CARD', 100.00, 'VISA', '41x1')""", orderId);
	}

	@Test
	void pixPaymentCannotCarryCardData() {
		long orderId = insertPickupOrder("100.00");
		assertViolates("payments_card_fields_only_for_card", """
				INSERT INTO payments (order_id, provider, method, amount, card_last4)
				VALUES (?, 'MANUAL', 'PIX', 100.00, '1234')""", orderId);
	}

	@Test
	void webhookEventIsProcessedOnlyOnce() {
		String sql = """
				INSERT INTO payment_webhook_events (provider, event_id, event_type, payload_sha256)
				VALUES ('gateway', 'evt_1', 'payment.paid', 'abc')""";
		jdbc.update(sql);
		assertViolates("payment_webhook_events_unique", sql);
	}

	@Test
	void validOrderIsAccepted() {
		assertThatCode(() -> insertPickupOrder("100.00")).doesNotThrowAnyException();
	}

}
