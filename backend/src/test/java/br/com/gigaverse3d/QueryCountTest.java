package br.com.gigaverse3d;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.repository.OrderRepository;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proteção contra N+1: o número de comandos SQL emitidos pelo Hibernate numa listagem não pode crescer com o
 * número de linhas da página. Mede a mesma consulta com poucos e com muitos registros e exige contagens iguais.
 */
class QueryCountTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	CatalogFixtures fixtures;

	@Autowired
	OrderRepository orders;

	@Autowired
	TransactionTemplate tx;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	EntityManagerFactory emf;

	Statistics stats;

	@BeforeEach
	void setUp() {
		stats = emf.unwrap(SessionFactory.class).getStatistics();
	}

	long statementsFor(MockHttpServletRequestBuilder request, int expectedRows) throws Exception {
		stats.clear();
		mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(expectedRows));
		return stats.getPrepareStatementCount();
	}

	void addProducts(Category category, int count) {
		for (int i = 0; i < count; i++) {
			fixtures.activeProduct(category, "Produto N+1 " + i, "10.00", 5);
		}
	}

	@Test
	@WithMockUser(username = "admin@exemplo.com", roles = "ADMIN")
	void productListingsUseConstantNumberOfQueries() throws Exception {
		Category category = fixtures.category(CatalogFixtures.unique("nmais1"));
		addProducts(category, 2);
		long publicFew = statementsFor(
				get("/api/v1/public/products").param("category", category.getSlug()).param("size", "40"), 2);
		long adminFew = statementsFor(
				get("/api/v1/admin/products").param("categoryId", category.getId().toString()).param("size", "40"),
				2);

		addProducts(category, 10);
		long publicMany = statementsFor(
				get("/api/v1/public/products").param("category", category.getSlug()).param("size", "40"), 12);
		long adminMany = statementsFor(
				get("/api/v1/admin/products").param("categoryId", category.getId().toString()).param("size", "40"),
				12);

		assertThat(publicFew).isPositive();
		assertThat(publicMany).as("listagem pública").isEqualTo(publicFew);
		assertThat(adminMany).as("listagem administrativa").isEqualTo(adminFew);
	}

	void addOrders(long userId, int count) {
		tx.executeWithoutResult(s -> {
			for (int i = 0; i < count; i++) {
				Order order = new Order(userId, "Cliente N+1", "nmais1@exemplo.com", Order.PaymentMethod.PIX,
						Order.ShippingMethod.PICKUP, null, null);
				order.addItem(null, "Item A", "GV-A", null, new BigDecimal("10.00"), 1);
				order.addItem(null, "Item B", "GV-B", null, new BigDecimal("5.00"), 2);
				orders.save(order);
			}
		});
	}

	@Test
	void orderListingsUseConstantNumberOfQueries() throws Exception {
		String email = newEmail();
		long userId = createUser(jdbc, encoder, email, "CUSTOMER");
		String admin = newEmail();
		createUser(jdbc, encoder, admin, "ADMIN");
		MockHttpSession customerSession = loggedIn(mvc, json, email);
		MockHttpSession adminSession = loggedIn(mvc, json, admin);

		addOrders(userId, 2);
		long accountFew = statementsFor(get("/api/v1/account/orders").param("size", "40").session(customerSession),
				2);
		long adminFew = statementsFor(get("/api/v1/admin/orders").param("customerId", String.valueOf(userId))
			.param("size", "40")
			.session(adminSession), 2);

		addOrders(userId, 8);
		long accountMany = statementsFor(get("/api/v1/account/orders").param("size", "40").session(customerSession),
				10);
		long adminMany = statementsFor(get("/api/v1/admin/orders").param("customerId", String.valueOf(userId))
			.param("size", "40")
			.session(adminSession), 10);

		assertThat(accountFew).isPositive();
		assertThat(accountMany).as("pedidos do cliente").isEqualTo(accountFew);
		assertThat(adminMany).as("pedidos no painel").isEqualTo(adminFew);
	}

}
