package br.com.gigaverse3d.cart.service;

import static br.com.gigaverse3d.unit.DomainFixtures.activeReady;
import static br.com.gigaverse3d.unit.DomainFixtures.product;
import static br.com.gigaverse3d.unit.DomainFixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.gigaverse3d.cart.service.Pricing.PricedLine;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductOption;
import br.com.gigaverse3d.inventory.domain.InventoryItem;

/** Cálculo de preço e disponibilidade: única fonte de verdade para carrinho e checkout. */
class PricingTest {

	final Pricing pricing = new Pricing();

	Product dragon;

	long white;

	long gold;

	long small;

	long large;

	static InventoryItem stock(Product p, int onHand, int reserved) {
		InventoryItem item = new InventoryItem(p, 2);
		ReflectionTestUtils.setField(item, "quantityOnHand", onHand);
		ReflectionTestUtils.setField(item, "quantityReserved", reserved);
		return item;
	}

	@BeforeEach
	void setUp() {
		dragon = activeReady("100.00");
		ProductOption color = dragon.addOption("Cor");
		white = withId(color.addValue("Branco", BigDecimal.ZERO), 11).getId();
		gold = withId(color.addValue("Dourado", new BigDecimal("15.50")), 12).getId();
		ProductOption size = dragon.addOption("Tamanho");
		small = withId(size.addValue("P", BigDecimal.ZERO), 21).getId();
		large = withId(size.addValue("G", new BigDecimal("30.00")), 22).getId();
	}

	@Test
	void sumsEffectivePriceAndOptionDeltas() {
		dragon.setPromoPrice(new BigDecimal("80.00"));
		PricedLine line = pricing.price(dragon, new Long[] { gold, large }, 3, stock(dragon, 10, 0));
		assertThat(line.ok()).isTrue();
		assertThat(line.unitPrice()).isEqualTo(new BigDecimal("125.50"));
		assertThat(line.lineTotal()).isEqualTo(new BigDecimal("376.50"));
		assertThat(line.optionsSummary()).isEqualTo("Cor: Dourado · Tamanho: G");
	}

	@Test
	void optionOrderDoesNotMatter() {
		PricedLine a = pricing.price(dragon, new Long[] { large, white }, 1, stock(dragon, 10, 0));
		PricedLine b = pricing.price(dragon, new Long[] { white, large }, 1, stock(dragon, 10, 0));
		assertThat(a.unitPrice()).isEqualTo(b.unitPrice()).isEqualTo(new BigDecimal("130.00"));
	}

	@Test
	void requiresExactlyOneValuePerOptionGroup() {
		InventoryItem s = stock(dragon, 10, 0);
		String msg = "Escolha uma opção válida para cada característica do produto.";
		assertThat(pricing.price(dragon, new Long[0], 1, s).problem()).isEqualTo(msg);
		assertThat(pricing.price(dragon, new Long[] { white }, 1, s).problem()).as("falta tamanho").isEqualTo(msg);
		assertThat(pricing.price(dragon, new Long[] { white, gold, small }, 1, s).problem()).as("duas cores")
			.isEqualTo(msg);
		assertThat(pricing.price(dragon, new Long[] { white, small, 999L }, 1, s).problem()).as("id estranho")
			.isEqualTo(msg);
		assertThat(pricing.price(dragon, new Long[] { white, white, small }, 1, s).problem()).as("repetido")
			.isEqualTo(msg);
	}

	@Test
	void productWithoutOptionsAcceptsEmptySelection() {
		Product vase = activeReady("49.90");
		PricedLine line = pricing.price(vase, null, 2, stock(vase, 5, 0));
		assertThat(line.ok()).isTrue();
		assertThat(line.lineTotal()).isEqualTo(new BigDecimal("99.80"));
		assertThat(line.optionsSummary()).isNull();
	}

	@Test
	void readyProductIsLimitedByAvailableStock() {
		Product vase = activeReady("10");
		assertThat(pricing.price(vase, null, 3, stock(vase, 5, 2)).ok()).isTrue();
		PricedLine tooMany = pricing.price(vase, null, 4, stock(vase, 5, 2));
		assertThat(tooMany.problem()).isEqualTo("Quantidade indisponível (máximo 3).");
		assertThat(tooMany.maxQuantity()).isEqualTo(3);
		assertThat(pricing.price(vase, null, 1, stock(vase, 2, 2)).problem()).isEqualTo("Sem estoque.");
		assertThat(pricing.price(vase, null, 1, null).problem()).as("sem registro de estoque").isEqualTo("Sem estoque.");
	}

	@Test
	void maxQuantityPerType() {
		Product ready = activeReady("10");
		assertThat(Pricing.maxQuantity(ready, stock(ready, 500, 0))).as("teto de 99").isEqualTo(99);
		assertThat(Pricing.maxQuantity(product("10", Product.Type.MADE_TO_ORDER, Product.Status.ACTIVE), null))
			.isEqualTo(Pricing.MADE_TO_ORDER_MAX);
		assertThat(Pricing.maxQuantity(product("10", Product.Type.CUSTOM, Product.Status.ACTIVE), null)).isZero();
	}

	@Test
	void madeToOrderDoesNotNeedStock() {
		Product mto = product("200", Product.Type.MADE_TO_ORDER, Product.Status.ACTIVE);
		assertThat(pricing.price(mto, null, 10, null).ok()).isTrue();
		assertThat(pricing.price(mto, null, 11, null).problem()).startsWith("Quantidade indisponível");
	}

	@Test
	void unavailableProductsAreFlagged() {
		Product draft = product("10", Product.Type.READY, Product.Status.DRAFT);
		assertThat(pricing.price(draft, null, 1, stock(draft, 5, 0)).problem())
			.isEqualTo("Produto indisponível no momento.");

		Product hiddenCategory = activeReady("10");
		ReflectionTestUtils.setField(hiddenCategory.getCategory(), "active", false);
		assertThat(pricing.price(hiddenCategory, null, 1, stock(hiddenCategory, 5, 0)).problem())
			.isEqualTo("Produto indisponível no momento.");

		Product custom = product("10", Product.Type.CUSTOM, Product.Status.ACTIVE);
		assertThat(pricing.price(custom, null, 1, null).problem())
			.isEqualTo("Produto personalizado: solicite um orçamento.");
	}

	@Test
	void neverTrustsPriceFromOutside() {
		// o preço vem sempre do produto; alterar o produto muda o resultado
		Product vase = activeReady("10.00");
		InventoryItem s = stock(vase, 5, 0);
		assertThat(pricing.price(vase, null, 1, s).unitPrice()).isEqualByComparingTo("10.00");
		vase.setPromoPrice(new BigDecimal("7.99"));
		assertThat(pricing.price(vase, null, 1, s).unitPrice()).isEqualByComparingTo("7.99");
	}

}
