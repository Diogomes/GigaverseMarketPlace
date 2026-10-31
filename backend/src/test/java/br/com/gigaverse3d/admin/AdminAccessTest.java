package br.com.gigaverse3d.admin;

import static br.com.gigaverse3d.account.AuthTestSupport.createUser;
import static br.com.gigaverse3d.account.AuthTestSupport.loggedIn;
import static br.com.gigaverse3d.account.AuthTestSupport.newEmail;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

/** Papéis no servidor: cliente nunca acessa a área administrativa; OPERATOR não acessa o que é só de ADMIN. */
class AdminAccessTest extends PostgresIntegrationTest {

	/** Leituras da equipe (ADMIN e OPERATOR). */
	static final List<String> STAFF_READS = List.of("/api/v1/admin/dashboard", "/api/v1/admin/customers",
			"/api/v1/admin/customer-tags", "/api/v1/admin/orders", "/api/v1/admin/products", "/api/v1/admin/settings");

	/** Somente ADMIN: equipe, auditoria e exportações de dados pessoais. */
	static final List<String> ADMIN_ONLY_READS = List.of("/api/v1/admin/users", "/api/v1/admin/audit",
			"/api/v1/admin/customers/export", "/api/v1/admin/orders/export");

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper json;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder encoder;

	MockHttpSession customer;

	MockHttpSession operator;

	MockHttpSession admin;

	@BeforeEach
	void setUp() throws Exception {
		customer = session("CUSTOMER");
		operator = session("OPERATOR");
		admin = session("ADMIN");
	}

	private MockHttpSession session(String role) throws Exception {
		String email = newEmail();
		createUser(jdbc, encoder, email, role);
		return loggedIn(mvc, json, email);
	}

	@Test
	void anonymousGets401() throws Exception {
		for (String path : concat()) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
		}
	}

	@Test
	void customerGets403OnEveryAdminEndpoint() throws Exception {
		for (String path : concat()) {
			mvc.perform(get(path).session(customer)).andExpect(status().isForbidden());
		}
		mvc.perform(post("/api/v1/admin/customer-tags").session(customer).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"VIP\"}")).andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/admin/users").session(customer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"x@exemplo.com\",\"fullName\":\"X\",\"role\":\"ADMIN\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void operatorReadsStaffAreasButNotAdminOnlyOnes() throws Exception {
		for (String path : STAFF_READS) {
			mvc.perform(get(path).session(operator)).andExpect(status().isOk());
		}
		for (String path : ADMIN_ONLY_READS) {
			mvc.perform(get(path).session(operator)).andExpect(status().isForbidden());
		}
		mvc.perform(post("/api/v1/admin/users").session(operator).with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + newEmail() + "\",\"fullName\":\"Novo\",\"role\":\"ADMIN\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(delete("/api/v1/admin/customer-tags/1").session(operator).with(csrf()))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/admin/orders/1000/confirm-payment").session(operator).with(csrf())
			.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
	}

	@Test
	void adminReadsEverything() throws Exception {
		for (String path : concat()) {
			mvc.perform(get(path).session(admin)).andExpect(status().isOk());
		}
	}

	@Test
	void writesWithoutCsrfAreRejected() throws Exception {
		mvc.perform(post("/api/v1/admin/customer-tags").session(admin).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Sem CSRF\"}")).andExpect(status().isForbidden());
	}

	private static List<String> concat() {
		return java.util.stream.Stream.concat(STAFF_READS.stream(), ADMIN_ONLY_READS.stream()).toList();
	}

}
