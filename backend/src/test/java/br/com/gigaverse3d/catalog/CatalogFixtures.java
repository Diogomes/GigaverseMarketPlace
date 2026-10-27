package br.com.gigaverse3d.catalog;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.support.TransactionTemplate;

import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.repository.CategoryRepository;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.inventory.domain.StockMovement;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;
import br.com.gigaverse3d.inventory.repository.StockMovementRepository;

/**
 * Cria dados de teste com identificadores únicos (o banco é compartilhado entre classes
 * de teste e os dados gravados via API são commitados).
 */
public class CatalogFixtures {

	private static final AtomicInteger SEQ = new AtomicInteger();

	private final CategoryRepository categories;

	private final ProductRepository products;

	private final InventoryItemRepository inventory;

	private final StockMovementRepository movements;

	private final TransactionTemplate tx;

	public CatalogFixtures(CategoryRepository categories, ProductRepository products, InventoryItemRepository inventory,
			StockMovementRepository movements, TransactionTemplate tx) {
		this.categories = categories;
		this.products = products;
		this.inventory = inventory;
		this.movements = movements;
		this.tx = tx;
	}

	public static String unique(String prefix) {
		return prefix + "-" + System.nanoTime() % 1_000_000 + "-" + SEQ.incrementAndGet();
	}

	public static String uniqueSku() {
		return "T-" + (System.nanoTime() % 1_000_000_000) + "-" + SEQ.incrementAndGet();
	}

	public Category category(String slug) {
		return tx.execute(s -> categories.save(new Category(slug, "Categoria " + slug, null)));
	}

	public Product product(Category category, String name, String price, Product.Status status, Product.Type type,
			int stock) {
		return tx.execute(s -> {
			Product p = new Product(unique("p"), uniqueSku(), name, "Descrição do produto " + name, category, type,
					new BigDecimal(price));
			p.setStatus(status);
			if (type != Product.Type.READY) {
				p.setProductionDays(7);
			}
			products.save(p);
			InventoryItem item = inventory.save(new InventoryItem(p, 2));
			if (stock > 0) {
				movements.save(item.apply(StockMovement.Type.INITIAL, stock, null));
			}
			return p;
		});
	}

	public Product activeProduct(Category category, String name, String price, int stock) {
		return product(category, name, price, Product.Status.ACTIVE, Product.Type.READY, stock);
	}

}
