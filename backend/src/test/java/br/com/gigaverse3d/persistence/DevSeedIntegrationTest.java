package br.com.gigaverse3d.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import br.com.gigaverse3d.TestcontainersConfiguration;

/** No perfil dev o catálogo de demonstração é carregado — sem usuários — e pode ser reaplicado. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class DevSeedIntegrationTest {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	Flyway flyway;

	private int count(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}

	@Test
	void loadsDemoCatalogWithoutUsersAndIsIdempotent() {
		assertThat(count("SELECT count(*) FROM categories")).isEqualTo(6);
		assertThat(count("SELECT count(*) FROM products")).isEqualTo(10);
		assertThat(count("SELECT count(*) FROM products WHERE status = 'ACTIVE'")).isEqualTo(9);
		assertThat(count("SELECT count(*) FROM users")).isZero();

		// Reaplicar o seed não duplica nada.
		jdbc.execute(new String(readSeed()));
		assertThat(count("SELECT count(*) FROM products")).isEqualTo(10);
		assertThat(count("SELECT count(*) FROM stock_movements")).isEqualTo(7);
	}

	private byte[] readSeed() {
		try (var in = getClass().getResourceAsStream("/db/seed/R__demo_catalog.sql")) {
			return in.readAllBytes();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
