package br.com.gigaverse3d.admin;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Métricas com valores conhecidos. Os pedidos ficam em 2020 (o banco é compartilhado com os
 * outros testes, que criam pedidos "agora"), com horários na virada do dia para testar o fuso
 * de Recife (UTC−3).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardMetricsTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	CatalogFixtures fixtures;

	MockHttpSession operator;

	Product p1;

	Product p2;

	Product lowStockProduct;

	@BeforeAll
	void seed() throws Exception {
		String opEmail = newEmail();
		createUser(jdbc, encoder, opEmail, "OPERATOR");
		operator = loggedIn(mvc, json, opEmail);

		Category category = fixtures.category(CatalogFixtures.unique("dash"));
		p1 = fixtures.activeProduct(category, "Produto Um", "45.00", 50);
		p2 = fixtures.activeProduct(category, "Produto Dois", "200.00", 50);
		lowStockProduct = fixtures.activeProduct(category, "Quase Esgotado", "10.00", 1); // mínimo 2

		long a = customer("2020-03-02T12:00:00Z"); // cadastro no período → 1 novo cadastro
		long b = customer(null);
		long c = customer(null);
		long d = customer(null);
		long e = customer(null);
		long f = customer(null);

		// A: dois pedidos pagos no período (primeira compra em março → comprador novo)
		item(order(a, "DELIVERED", "2020-03-05T15:00:00Z", "2020-03-05T15:10:00Z", "90.00", "10.00"), p1, "45.00", 2);
		item(order(a, "PAID", "2020-03-20T15:00:00Z", "2020-03-20T15:10:00Z", "200.00", "0.00"), p2, "200.00", 1);
		// B: já tinha comprado em fevereiro → comprador recorrente
		item(order(b, "DELIVERED", "2020-02-10T15:00:00Z", "2020-02-10T15:10:00Z", "50.00", "0.00"), p1, "50.00", 1);
		item(order(b, "SHIPPED", "2020-03-10T15:00:00Z", "2020-03-10T15:10:00Z", "60.00", "15.00"), p1, "60.00", 1);
		// C: pago e cancelado depois → entra nas brutas, sai das líquidas; não é comprador
		item(order(c, "CANCELLED", "2020-03-12T15:00:00Z", "2020-03-12T15:10:00Z", "80.00", "0.00"), p2, "80.00", 1);
		// D: aguardando pagamento → conta como pedido criado, não como venda
		item(order(d, "PENDING_PAYMENT", "2020-03-15T15:00:00Z", null, "30.00", "0.00"), p1, "30.00", 1);
		// E: 01/04 02:30 UTC = 31/03 23:30 em Recife → pertence a março
		item(order(e, "IN_PRODUCTION", "2020-04-01T02:00:00Z", "2020-04-01T02:30:00Z", "10.00", "0.00"), p1, "10.00", 1);
		// F: 01/03 02:00 UTC = 29/02 23:00 em Recife → pertence a fevereiro
		item(order(f, "DELIVERED", "2020-03-01T01:00:00Z", "2020-03-01T02:00:00Z", "1000.00", "0.00"), p2, "1000.00", 1);
	}

	private long customer(String createdAt) {
		long id = createUser(jdbc, encoder, newEmail(), "CUSTOMER");
		if (createdAt != null) {
			jdbc.update("UPDATE users SET created_at = ? WHERE id = ?", Timestamp.from(Instant.parse(createdAt)), id);
		}
		return id;
	}

	private long order(long userId, String status, String placedAt, String paidAt, String subtotal, String shipping) {
		BigDecimal total = new BigDecimal(subtotal).add(new BigDecimal(shipping));
		return jdbc.queryForObject("""
				INSERT INTO orders (user_id, status, subtotal, shipping_total, total, payment_method, shipping_method,
				                    customer_name, customer_email, placed_at, created_at, paid_at, cancelled_at)
				VALUES (?, ?, ?, ?, ?, 'PIX', 'PICKUP', 'Cliente Métrica', 'metrica@exemplo.com', ?, ?, ?, ?)
				RETURNING id""", Long.class, userId, status, new BigDecimal(subtotal), new BigDecimal(shipping), total,
				ts(placedAt), ts(placedAt), ts(paidAt), "CANCELLED".equals(status) ? ts("2020-03-13T12:00:00Z") : null);
	}

	private void item(long orderId, Product product, String unitPrice, int quantity) {
		BigDecimal unit = new BigDecimal(unitPrice);
		jdbc.update("""
				INSERT INTO order_items (order_id, product_id, product_name, sku, unit_price, quantity, line_total)
				VALUES (?, ?, ?, ?, ?, ?, ?)""", orderId, product.getId(), product.getName(), product.getSku(), unit,
				quantity, unit.multiply(BigDecimal.valueOf(quantity)));
	}

	private static Timestamp ts(String iso) {
		return iso == null ? null : Timestamp.from(Instant.parse(iso));
	}

	@Test
	void salesKpisFollowTheDocumentedDefinitions() throws Exception {
		mvc.perform(get("/api/v1/admin/dashboard?from=2020-03-01&to=2020-03-31").session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.timezone").value("America/Recife"))
			// brutas = 100 + 200 + 75 + 80 (cancelado após pagar) + 10 (fuso) — sem o pedido de 29/02 local
			.andExpect(jsonPath("$.kpis.grossSales").value(465.00))
			.andExpect(jsonPath("$.kpis.cancelledAfterPayment").value(80.00))
			.andExpect(jsonPath("$.kpis.shipping").value(25.00))
			// líquidas = (100 + 200 + 75 + 10) − frete 25
			.andExpect(jsonPath("$.kpis.netSales").value(360.00))
			.andExpect(jsonPath("$.kpis.paidOrders").value(4))
			// ticket = 385 / 4
			.andExpect(jsonPath("$.kpis.averageTicket").value(96.25))
			// criados em março (horário local), qualquer status
			.andExpect(jsonPath("$.kpis.ordersPlaced").value(6));
	}

	@Test
	void previousPeriodHasTheSameLength() throws Exception {
		mvc.perform(get("/api/v1/admin/dashboard?from=2020-03-01&to=2020-03-31").session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.previousFrom").value("2020-01-30"))
			.andExpect(jsonPath("$.previousTo").value("2020-02-29"))
			.andExpect(jsonPath("$.previous.grossSales").value(1050.00))
			.andExpect(jsonPath("$.previous.paidOrders").value(2));
	}

	@Test
	void dailySeriesCoversEveryDayInLocalTime() throws Exception {
		mvc.perform(get("/api/v1/admin/dashboard?from=2020-03-01&to=2020-03-31").session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sales.length()").value(31))
			.andExpect(jsonPath("$.sales[0].day").value("2020-03-01"))
			.andExpect(jsonPath("$.sales[0].grossSales").value(0))
			.andExpect(jsonPath("$.sales[11].day").value("2020-03-12"))
			.andExpect(jsonPath("$.sales[11].grossSales").value(80.00))
			.andExpect(jsonPath("$.sales[11].netSales").value(0))
			.andExpect(jsonPath("$.sales[30].day").value("2020-03-31"))
			.andExpect(jsonPath("$.sales[30].grossSales").value(10.00));
	}

	@Test
	void ordersByStatusTopProductsAndCustomers() throws Exception {
		mvc.perform(get("/api/v1/admin/dashboard?from=2020-03-01&to=2020-03-31").session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ordersByStatus[?(@.status == 'PENDING_PAYMENT')].count").value(1))
			.andExpect(jsonPath("$.ordersByStatus[?(@.status == 'CANCELLED')].count").value(1))
			.andExpect(jsonPath("$.ordersByStatus[?(@.status == 'READY_FOR_PICKUP')].count").value(0))
			// P1: 2 + 1 + 1 (fuso) unidades; P2: só o pedido não cancelado
			.andExpect(jsonPath("$.topProducts[0].productId").value(p1.getId()))
			.andExpect(jsonPath("$.topProducts[0].quantity").value(4))
			.andExpect(jsonPath("$.topProducts[0].revenue").value(160.00))
			.andExpect(jsonPath("$.topProducts[1].productId").value(p2.getId()))
			.andExpect(jsonPath("$.topProducts[1].quantity").value(1))
			.andExpect(jsonPath("$.customers.buyers").value(3))
			.andExpect(jsonPath("$.customers.newBuyers").value(2))
			.andExpect(jsonPath("$.customers.returningBuyers").value(1))
			.andExpect(jsonPath("$.customers.signups").value(1));
	}

	@Test
	void lowStockAndAwaitingActionReflectCurrentState() throws Exception {
		mvc.perform(get("/api/v1/admin/dashboard?from=2020-03-01&to=2020-03-31").session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lowStockTotal").value(greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.awaitingAction[?(@.status == 'PENDING_PAYMENT')].count")
				.value(org.hamcrest.Matchers.everyItem(greaterThanOrEqualTo(1))));
		// o mesmo critério de estoque baixo na lista de produtos
		mvc.perform(get("/api/v1/admin/products?lowStock=true&q=" + lowStockProduct.getSku()).session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].lowStock").value(true));
		mvc.perform(get("/api/v1/admin/products?lowStock=true&q=" + p1.getSku()).session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void rejectsInvalidPeriods() throws Exception {
		mvc.perform(get("/api/v1/admin/dashboard?from=2020-03-31&to=2020-03-01").session(operator))
			.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/admin/dashboard?from=2019-01-01&to=2020-03-01").session(operator))
			.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/admin/dashboard").session(operator))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sales.length()").value(30));
	}

}
