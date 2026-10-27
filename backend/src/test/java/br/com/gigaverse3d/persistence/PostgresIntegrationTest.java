package br.com.gigaverse3d.persistence;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import br.com.gigaverse3d.TestcontainersConfiguration;
import br.com.gigaverse3d.account.AuthTestSupport;
import br.com.gigaverse3d.catalog.CatalogFixtures;

/** Base compartilhada: um único contexto Spring + PostgreSQL (Testcontainers) para todas as subclasses. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, CatalogFixtures.class, AuthTestSupport.Config.class })
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {
}
