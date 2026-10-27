package br.com.gigaverse3d.catalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;

class PublicCatalogApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	CatalogFixtures fixtures;

	String cat;

	Product cheap;

	Product expensive;

	Product draft;

	@BeforeEach
	void setUp() {
		cat = CatalogFixtures.unique("pub");
		Category category = fixtures.category(cat);
		cheap = fixtures.activeProduct(category, "Vaso Geométrico", "50.00", 3);
		expensive = fixtures.activeProduct(category, "Luminária Lunar", "150.00", 0);
		draft = fixtures.product(category, "Rascunho Secreto", "10.00", Product.Status.DRAFT, Product.Type.READY, 1);
		fixtures.product(category, "Arquivado Antigo", "10.00", Product.Status.ARCHIVED, Product.Type.READY, 1);
	}

	@Test
	void listsOnlyActiveProductsWithAvailabilityAndWithoutStockQuantity() throws Exception {
		mvc.perform(get("/api/v1/public/products").param("category", cat).param("sort", "price_asc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[*].slug").value(contains(cheap.getSlug(), expensive.getSlug())))
			.andExpect(jsonPath("$.content[0].availability").value("IN_STOCK"))
			.andExpect(jsonPath("$.content[1].availability").value("OUT_OF_STOCK"))
			.andExpect(jsonPath("$.content[0].price").value(50.00))
			.andExpect(jsonPath("$.content[0].stock").doesNotExist());
	}

	@Test
	void sortsByPriceDescending() throws Exception {
		mvc.perform(get("/api/v1/public/products").param("category", cat).param("sort", "price_desc"))
			.andExpect(jsonPath("$.content[*].slug").value(contains(expensive.getSlug(), cheap.getSlug())));
	}

	@Test
	void searchesInPortugueseWithinFilters() throws Exception {
		mvc.perform(get("/api/v1/public/products").param("category", cat).param("q", "luminárias"))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].slug").value(expensive.getSlug()));
	}

	@Test
	void searchIgnoresAccents() throws Exception {
		mvc.perform(get("/api/v1/public/products").param("category", cat).param("q", "luminarias"))
			.andExpect(jsonPath("$.content[*].slug").value(contains(expensive.getSlug())));
		mvc.perform(get("/api/v1/public/products").param("category", cat).param("q", "GEOMETRICO"))
			.andExpect(jsonPath("$.content[*].slug").value(contains(cheap.getSlug())));
	}

	@Test
	void paginates() throws Exception {
		mvc.perform(get("/api/v1/public/products").param("category", cat).param("size", "1").param("page", "1")
			.param("sort", "price_asc"))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].slug").value(expensive.getSlug()))
			.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void rejectsOversizedPagesAndUnknownSort() throws Exception {
		mvc.perform(get("/api/v1/public/products").param("size", "500"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("Tamanho máximo 48."));
		mvc.perform(get("/api/v1/public/products").param("sort", "price; DROP TABLE products"))
			.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/public/products").param("type", "INVALIDO")).andExpect(status().isBadRequest());
	}

	@Test
	void detailReturnsActiveProductAndHidesDrafts() throws Exception {
		mvc.perform(get("/api/v1/public/products/{slug}", cheap.getSlug()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Vaso Geométrico"))
			.andExpect(jsonPath("$.effectivePrice").value(50.00))
			.andExpect(jsonPath("$.currency").value("BRL"));
		mvc.perform(get("/api/v1/public/products/{slug}", draft.getSlug()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Produto não encontrado."));
	}

	@Test
	void draftNeverAppearsInListings() throws Exception {
		mvc.perform(get("/api/v1/public/products").param("q", "Rascunho Secreto"))
			.andExpect(jsonPath("$.content[*].slug").value(not(hasItem(draft.getSlug()))));
	}

	@Test
	void listsActiveCategories() throws Exception {
		mvc.perform(get("/api/v1/public/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].slug").value(hasItem(cat)));
	}

}
