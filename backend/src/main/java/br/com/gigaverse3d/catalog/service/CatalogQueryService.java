package br.com.gigaverse3d.catalog.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.gigaverse3d.catalog.api.CatalogDtos.CategoryResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductDetailResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductSummaryResponse;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.repository.CategoryRepository;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;

/** Leitura do catálogo público: só produtos ACTIVE em categorias ativas. */
@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

	private final ProductRepository products;

	private final CategoryRepository categories;

	private final InventoryItemRepository inventory;

	private final CatalogMapper mapper;

	public CatalogQueryService(ProductRepository products, CategoryRepository categories,
			InventoryItemRepository inventory, CatalogMapper mapper) {
		this.products = products;
		this.categories = categories;
		this.inventory = inventory;
		this.mapper = mapper;
	}

	public List<CategoryResponse> categories() {
		return categories.findByActiveTrueOrderBySortOrderAscNameAsc().stream().map(mapper::category).toList();
	}

	public PageResponse<ProductSummaryResponse> search(String q, String category, Product.Type type, String sort,
			int page, int size) {
		Page<Product> result = products.searchPublic(StringUtils.hasText(q) ? q.strip() : null,
				StringUtils.hasText(category) ? category : null, type == null ? null : type.name(),
				StringUtils.hasText(sort) ? sort : "relevance", PageRequest.of(page, size));
		// Estoque da página inteira em uma consulta (evita N+1).
		Map<Long, InventoryItem> stock = inventory
			.findByProductIdIn(result.getContent().stream().map(Product::getId).toList())
			.stream()
			.collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
		return PageResponse.of(result, p -> mapper.summary(p, stock.get(p.getId())));
	}

	public ProductDetailResponse bySlug(String slug) {
		Product product = products.findBySlugAndStatus(slug, Product.Status.ACTIVE)
			.filter(p -> p.getCategory().isActive())
			.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
		return mapper.detail(product, inventory.findById(product.getId()).orElse(null));
	}

}
