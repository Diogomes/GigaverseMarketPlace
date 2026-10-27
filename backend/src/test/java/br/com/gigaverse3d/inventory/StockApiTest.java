package br.com.gigaverse3d.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.gigaverse3d.catalog.CatalogFixtures;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.inventory.domain.InsufficientStockException;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;
import br.com.gigaverse3d.inventory.repository.StockMovementRepository;
import br.com.gigaverse3d.inventory.service.StockService;
import br.com.gigaverse3d.inventory.service.StockService.ManualType;
import br.com.gigaverse3d.inventory.service.StockService.StockAdjustmentRequest;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

@WithMockUser(roles = "OPERATOR")
class StockApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	CatalogFixtures fixtures;

	@Autowired
	JsonMapper json;

	@Autowired
	StockService stock;

	@Autowired
	InventoryItemRepository inventory;

	@Autowired
	StockMovementRepository movements;

	Product product;

	@BeforeEach
	void setUp() {
		product = fixtures.activeProduct(fixtures.category(CatalogFixtures.unique("stk")), "Estoque", "10.00", 5);
	}

	private ResultActions move(Map<String, Object> body) throws Exception {
		return mvc.perform(post("/api/v1/admin/products/{id}/stock-movements", product.getId()).with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(body)));
	}

	@Test
	void purchaseAddsStockAndIsRecorded() throws Exception {
		move(Map.of("type", "PURCHASE", "quantity", 10)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.quantityAfter").value(15))
			.andExpect(jsonPath("$.type").value("PURCHASE"));
		mvc.perform(get("/api/v1/admin/products/{id}/stock-movements", product.getId()))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].quantityDelta").value(10));
	}

	@Test
	void adjustmentRequiresReasonAndCannotGoNegative() throws Exception {
		move(Map.of("type", "ADJUSTMENT", "quantity", -1)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("reason"));
		move(Map.of("type", "ADJUSTMENT", "quantity", -6, "reason", "Contagem")).andExpect(status().isConflict());
		move(Map.of("type", "ADJUSTMENT", "quantity", -5, "reason", "Quebra no transporte"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.quantityAfter").value(0));
	}

	@Test
	void purchaseMustBePositive() throws Exception {
		move(Map.of("type", "PURCHASE", "quantity", -3)).andExpect(status().isBadRequest());
		move(Map.of("type", "SALE", "quantity", -1)).andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "CUSTOMER")
	void customersCannotTouchStock() throws Exception {
		move(Map.of("type", "PURCHASE", "quantity", 1)).andExpect(status().isForbidden());
	}

	/** 20 baixas simultâneas de 1 unidade com saldo 5: exatamente 5 passam e o saldo termina em 0. */
	@Test
	void concurrentDecrementsNeverOversell() throws Exception {
		int threads = 20;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			results.add(pool.submit(() -> {
				start.await();
				try {
					stock.adjust(product.getId(), new StockAdjustmentRequest(ManualType.ADJUSTMENT, -1, "Teste concorrência"));
					return true;
				}
				catch (InsufficientStockException e) {
					return false;
				}
			}));
		}
		start.countDown();
		int ok = 0;
		for (Future<Boolean> r : results) {
			if (r.get(30, TimeUnit.SECONDS)) {
				ok++;
			}
		}
		pool.shutdown();

		assertThat(ok).isEqualTo(5);
		assertThat(inventory.findById(product.getId()).orElseThrow().getQuantityOnHand()).isZero();
		assertThat(movements.findByProductIdOrderByCreatedAtDesc(product.getId())).hasSize(1 + 5);
	}

}
