package br.com.gigaverse3d.catalog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminProductListItem;
import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminProductResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.OptionRequest;
import br.com.gigaverse3d.catalog.api.CatalogDtos.OptionValueRequest;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductRequest;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductImage;
import br.com.gigaverse3d.catalog.domain.ProductOption;
import br.com.gigaverse3d.catalog.repository.CategoryRepository;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.domain.AfterTransaction;
import br.com.gigaverse3d.common.text.Slugs;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;
import br.com.gigaverse3d.storage.StorageService;

@Service
@Transactional
public class ProductAdminService {

	/** Campos aceitos para ordenação na listagem administrativa (lista branca). */
	public static final Set<String> SORTABLE = Set.of("name", "sku", "price", "createdAt", "updatedAt");

	private final ProductRepository products;

	private final CategoryRepository categories;

	private final InventoryItemRepository inventory;

	private final CatalogMapper mapper;

	private final AuditService audit;

	private final StorageService storage;

	private final EntityManager em;

	public ProductAdminService(ProductRepository products, CategoryRepository categories,
			InventoryItemRepository inventory, CatalogMapper mapper, AuditService audit, StorageService storage,
			EntityManager em) {
		this.products = products;
		this.categories = categories;
		this.inventory = inventory;
		this.mapper = mapper;
		this.audit = audit;
		this.storage = storage;
		this.em = em;
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminProductListItem> list(String q, Long categoryId, Product.Status status, Product.Type type,
			boolean lowStock, int page, int size, String sortField, Sort.Direction direction) {
		Specification<Product> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (StringUtils.hasText(q)) {
				String like = "%" + q.strip().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("name")), like, '\\'),
						cb.like(cb.lower(root.get("sku")), like, '\\')));
			}
			if (categoryId != null) {
				predicates.add(cb.equal(root.get("category").get("id"), categoryId));
			}
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (type != null) {
				predicates.add(cb.equal(root.get("type"), type));
			}
			if (lowStock) {
				// pronta entrega com disponível (em estoque − reservado) ≤ mínimo
				Subquery<Long> low = query.subquery(Long.class);
				Root<InventoryItem> item = low.from(InventoryItem.class);
				low.select(item.get("productId"))
					.where(cb.le(cb.diff(item.<Integer>get("quantityOnHand"), item.<Integer>get("quantityReserved")),
							item.<Integer>get("lowStockThreshold")));
				predicates.add(cb.equal(root.get("type"), Product.Type.READY));
				predicates.add(root.get("id").in(low));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		String field = SORTABLE.contains(sortField) ? sortField : "updatedAt";
		Page<Product> result = products.findAll(spec,
				PageRequest.of(page, size, Sort.by(direction, field).and(Sort.by("id"))));
		Map<Long, InventoryItem> stock = stockFor(result.getContent());
		return PageResponse.of(result, p -> mapper.adminListItem(p, stock.get(p.getId())));
	}

	@Transactional(readOnly = true)
	public AdminProductResponse get(Long id) {
		Product product = find(id);
		return mapper.admin(product, inventory.findById(id).orElse(null));
	}

	public AdminProductResponse create(ProductRequest req) {
		validateRules(req);
		Category category = category(req.categoryId());
		String slug = slugOf(req);
		if (products.existsBySlug(slug)) {
			throw new ConflictException("Já existe um produto com o slug '" + slug + "'.");
		}
		if (products.existsBySku(req.sku())) {
			throw new ConflictException("Já existe um produto com o SKU '" + req.sku() + "'.");
		}
		Product product = new Product(slug, req.sku(), req.name().strip(), req.description().strip(), category,
				req.type(), req.price());
		apply(product, req, slug, category);
		products.save(product);
		InventoryItem stock = inventory.save(new InventoryItem(product, thresholdOf(req)));
		audit.record("PRODUCT_CREATED", "product", product.getId(),
				Map.of("sku", product.getSku(), "status", product.getStatus().name()));
		return mapper.admin(product, stock);
	}

	public AdminProductResponse update(Long id, ProductRequest req) {
		Product product = find(id);
		if (req.version() == null) {
			throw new FieldValidationException("version", "Informe a versão lida do produto.");
		}
		if (!req.version().equals(product.getVersion())) {
			throw new ConflictException("O produto foi alterado por outra pessoa. Recarregue e tente novamente.");
		}
		validateRules(req);
		Category category = category(req.categoryId());
		String slug = slugOf(req);
		if (products.existsBySlugAndIdNot(slug, id)) {
			throw new ConflictException("Já existe um produto com o slug '" + slug + "'.");
		}
		if (products.existsBySkuAndIdNot(req.sku(), id)) {
			throw new ConflictException("Já existe um produto com o SKU '" + req.sku() + "'.");
		}
		Product.Status previous = product.getStatus();
		apply(product, req, slug, category);
		InventoryItem stock = inventory.findById(id).orElseGet(() -> inventory.save(new InventoryItem(product, 0)));
		stock.setLowStockThreshold(thresholdOf(req));
		// produto já gerenciado: flush (e não save/merge) para persistir opções novas sem copiá-las
		products.flush();
		audit.record("PRODUCT_UPDATED", "product", id,
				Map.of("sku", product.getSku(), "statusFrom", previous.name(), "statusTo", product.getStatus().name()));
		return mapper.admin(product, stock);
	}

	/** Arquivar/ativar/voltar a rascunho sem reenviar o produto inteiro (confere a versão lida). */
	public AdminProductResponse changeStatus(Long id, Product.Status status, Long version) {
		Product product = find(id);
		if (!version.equals(product.getVersion())) {
			throw new ConflictException("O produto foi alterado por outra pessoa. Recarregue e tente novamente.");
		}
		Product.Status previous = product.getStatus();
		if (previous != status) {
			product.setStatus(status);
			products.flush();
			audit.record("PRODUCT_STATUS_CHANGED", "product", id,
					Map.of("sku", product.getSku(), "statusFrom", previous.name(), "statusTo", status.name()));
		}
		return mapper.admin(product, inventory.findById(id).orElse(null));
	}

	/** Exclusão física só sem histórico de pedidos; caso contrário, arquivar. */
	public void delete(Long id) {
		Product product = find(id);
		if (products.hasOrderItems(id)) {
			throw new ConflictException("O produto possui pedidos. Arquive-o em vez de excluir.");
		}
		List<String> keys = product.getImages().stream().map(ProductImage::getStorageKey).toList();
		inventory.findById(id).ifPresent(inventory::delete);
		products.delete(product);
		audit.record("PRODUCT_DELETED", "product", id, Map.of("sku", product.getSku()));
		AfterTransaction.onCommit(() -> keys.forEach(storage::delete));
	}

	Product find(Long id) {
		return products.findById(id).orElseThrow(() -> new NotFoundException("Produto não encontrado."));
	}

	private Category category(Long id) {
		return categories.findById(id)
			.orElseThrow(() -> new FieldValidationException("categoryId", "Categoria inexistente."));
	}

	private void apply(Product product, ProductRequest req, String slug, Category category) {
		product.updateDetails(slug, req.sku(), req.name().strip(), req.description().strip(), category, req.type(),
				req.price(), req.promoPrice(), req.productionDays(), req.weightGrams(), req.lengthMm(), req.widthMm(), req.heightMm(), Boolean.TRUE.equals(req.featured()),
				req.seoTitle(),
				req.seoDescription());
		product.setStatus(req.status());
		// Substituir opções: remove as antigas e grava antes de inserir (evita conflito de unicidade por nome).
		if (!product.getOptions().isEmpty()) {
			product.clearOptions();
			em.flush();
		}
		for (OptionRequest o : req.options() == null ? List.<OptionRequest>of() : req.options()) {
			ProductOption option = product.addOption(o.name().strip());
			for (OptionValueRequest v : o.values()) {
				option.addValue(v.label().strip(), v.priceDelta());
			}
		}
	}

	/** Regras entre campos (o banco também as garante; aqui viram 400 com mensagem útil). */
	private static void validateRules(ProductRequest req) {
		if (req.promoPrice() != null && req.promoPrice().compareTo(req.price()) >= 0) {
			throw new FieldValidationException("promoPrice", "O preço promocional deve ser menor que o preço.");
		}
		if (req.type() != Product.Type.READY && req.productionDays() == null) {
			throw new FieldValidationException("productionDays",
					"Informe o prazo de produção para itens sob encomenda ou personalizados.");
		}
		if (req.options() != null) {
			long distinct = req.options().stream().map(o -> o.name().strip().toLowerCase()).distinct().count();
			if (distinct != req.options().size()) {
				throw new FieldValidationException("options", "Há grupos de opções com o mesmo nome.");
			}
			for (OptionRequest o : req.options()) {
				long labels = o.values().stream().map(v -> v.label().strip().toLowerCase()).distinct().count();
				if (labels != o.values().size()) {
					throw new FieldValidationException("options", "A opção '" + o.name() + "' tem valores repetidos.");
				}
			}
		}
	}

	private static String slugOf(ProductRequest req) {
		String slug = StringUtils.hasText(req.slug()) ? req.slug() : Slugs.from(req.name());
		if (slug.isEmpty()) {
			throw new FieldValidationException("slug", "Não foi possível gerar o slug a partir do nome.");
		}
		return slug;
	}

	private static int thresholdOf(ProductRequest req) {
		return req.lowStockThreshold() == null ? 0 : req.lowStockThreshold();
	}

	private Map<Long, InventoryItem> stockFor(List<Product> list) {
		return inventory.findByProductIdIn(list.stream().map(Product::getId).toList())
			.stream()
			.collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
	}

}
