package br.com.gigaverse3d.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** O banco sobe do zero só com migrations; o Hibernate valida o mapeamento (ddl-auto=validate). */
class MigrationIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	Flyway flyway;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void allVersionedMigrationsApplySuccessfully() {
		List<MigrationInfo> applied = Arrays.asList(flyway.info().applied());
		assertThat(applied).extracting(MigrationInfo::getVersion).extracting(Object::toString)
			.containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
		assertThat(applied).allMatch(m -> m.getState() == MigrationState.SUCCESS);
		assertThat(flyway.info().pending()).isEmpty();
	}

	@Test
	void demoSeedIsNotLoadedOutsideDevProfile() {
		assertThat(flyway.info().applied()).noneMatch(m -> m.getScript().contains("demo"));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM categories WHERE slug = 'colecionaveis'", Integer.class))
			.isZero();
	}

	@Test
	void essentialIndexesExist() {
		List<String> indexes = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'",
				String.class);
		assertThat(indexes).contains("users_email_unique", "addresses_one_default_per_user", "products_search_idx",
				"orders_user_created_idx", "orders_status_created_idx", "stock_movements_product_idx",
				"payments_provider_reference_unique", "audit_log_entity_idx");
	}

	@Test
	void noPlainTextPasswordColumnExists() {
		List<String> columns = jdbc.queryForList(
				"SELECT column_name FROM information_schema.columns WHERE table_name = 'users'", String.class);
		assertThat(columns).contains("password_hash").doesNotContain("password");
	}

}
