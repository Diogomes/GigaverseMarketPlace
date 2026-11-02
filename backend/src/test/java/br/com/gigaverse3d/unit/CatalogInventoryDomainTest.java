package br.com.gigaverse3d.unit;

import static br.com.gigaverse3d.unit.DomainFixtures.activeReady;
import static br.com.gigaverse3d.unit.DomainFixtures.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductImage;
import br.com.gigaverse3d.inventory.domain.InsufficientStockException;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.inventory.domain.StockMovement;

class CatalogInventoryDomainTest {

	// ---------- Produto ----------

	@Test
	void newProductStartsAsDraftAndInvisible() {
		Product p = new Product("vaso", "GV-1", "Vaso", "Descrição fictícia", DomainFixtures.category(),
				Product.Type.READY, new BigDecimal("59.9"));
		assertThat(p.getStatus()).isEqualTo(Product.Status.DRAFT);
		assertThat(p.isPubliclyVisible()).isFalse();
		assertThat(p.getPrice()).isEqualTo(new BigDecimal("59.90"));
	}

	@Test
	void onlyActiveProductsAreVisible() {
		assertThat(product("10", Product.Type.READY, Product.Status.ACTIVE).isPubliclyVisible()).isTrue();
		assertThat(product("10", Product.Type.READY, Product.Status.ARCHIVED).isPubliclyVisible()).isFalse();
		assertThat(product("10", Product.Type.READY, Product.Status.DRAFT).isPubliclyVisible()).isFalse();
	}

	@Test
	void effectivePriceUsesPromotionWhenPresent() {
		Product p = activeReady("189.90");
		assertThat(p.effectivePrice()).isEqualByComparingTo("189.90");
		p.setPromoPrice(new BigDecimal("149.9"));
		assertThat(p.effectivePrice()).isEqualTo(new BigDecimal("149.90"));
		p.setPromoPrice(null);
		assertThat(p.effectivePrice()).isEqualByComparingTo("189.90");
	}

	@Test
	void updateDetailsNormalizesMoney() {
		Product p = activeReady("10");
		p.updateDetails("novo-slug", "GV-2", "Novo nome", "Nova descrição fictícia", DomainFixtures.category(),
				Product.Type.MADE_TO_ORDER, new BigDecimal("75.5"), new BigDecimal("70"), 5, null, null, null, null,
				true, null, null);
		assertThat(p.getPrice()).isEqualTo(new BigDecimal("75.50"));
		assertThat(p.effectivePrice()).isEqualTo(new BigDecimal("70.00"));
		assertThat(p.getType()).isEqualTo(Product.Type.MADE_TO_ORDER);
		assertThat(p.isFeatured()).isTrue();
	}

	@Test
	void imagesAndOptionsKeepInsertionOrder() {
		Product p = activeReady("10");
		ProductImage first = p.addImage("k1", "Frente", "image/webp", 100, 800, 800);
		ProductImage second = p.addImage("k2", "Verso", "image/webp", 100, 800, 800);
		assertThat(first.getSortOrder()).isZero();
		assertThat(second.getSortOrder()).isEqualTo(1);
		p.removeImage(first);
		assertThat(p.addImage("k3", "Lado", "image/png", 100, null, null).getSortOrder()).isEqualTo(2);

		assertThat(p.addOption("Cor").getSortOrder()).isZero();
		assertThat(p.addOption("Tamanho").getSortOrder()).isEqualTo(1);
		p.clearOptions();
		assertThat(p.getOptions()).isEmpty();
	}

	// ---------- Estoque ----------

	InventoryItem stock(int onHand, int reserved, int lowThreshold) {
		InventoryItem item = new InventoryItem(activeReady("10"), lowThreshold);
		ReflectionTestUtils.setField(item, "quantityOnHand", onHand);
		ReflectionTestUtils.setField(item, "quantityReserved", reserved);
		return item;
	}

	@Test
	void entryIncreasesBalanceAndRecordsQuantityAfter() {
		InventoryItem item = stock(0, 0, 2);
		StockMovement m = item.apply(StockMovement.Type.PURCHASE, 5, "Nota fiscal fictícia 123");
		assertThat(item.getQuantityOnHand()).isEqualTo(5);
		assertThat(m.getQuantityDelta()).isEqualTo(5);
		assertThat(m.getQuantityAfter()).isEqualTo(5);
		assertThat(m.getType()).isEqualTo(StockMovement.Type.PURCHASE);
	}

	@Test
	void availableDiscountsReservations() {
		InventoryItem item = stock(10, 4, 2);
		assertThat(item.available()).isEqualTo(6);
		assertThat(item.isLowStock()).isFalse();
		assertThat(stock(5, 3, 2).isLowStock()).as("disponível 2 = mínimo").isTrue();
		assertThat(stock(0, 0, 0).isLowStock()).isTrue();
	}

	@Test
	void cannotRemoveBelowReservedQuantity() {
		InventoryItem item = stock(10, 4, 2);
		item.apply(StockMovement.Type.ADJUSTMENT, -6, "Avaria");
		assertThat(item.getQuantityOnHand()).isEqualTo(4);
		assertThatThrownBy(() -> item.apply(StockMovement.Type.ADJUSTMENT, -1, "Avaria"))
			.isInstanceOf(InsufficientStockException.class);
		assertThat(item.getQuantityOnHand()).as("saldo intacto após recusa").isEqualTo(4);
	}

	@Test
	void cannotGoNegative() {
		assertThatThrownBy(() -> stock(2, 0, 0).apply(StockMovement.Type.SALE, -3, null))
			.isInstanceOf(InsufficientStockException.class);
	}

	@Test
	void zeroMovementAndNegativeThresholdAreRejected() {
		InventoryItem item = stock(1, 0, 0);
		assertThatThrownBy(() -> item.apply(StockMovement.Type.ADJUSTMENT, 0, "nada"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> item.setLowStockThreshold(-1)).isInstanceOf(IllegalArgumentException.class);
		item.setLowStockThreshold(0);
		assertThat(item.getLowStockThreshold()).isZero();
	}

}
