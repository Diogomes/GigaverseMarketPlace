package br.com.gigaverse3d.catalog.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminCategoryResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.CategoryRequest;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.repository.CategoryRepository;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.text.Slugs;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;

@Service
@Transactional
public class CategoryAdminService {

	private final CategoryRepository categories;

	private final CatalogMapper mapper;

	private final AuditService audit;

	public CategoryAdminService(CategoryRepository categories, CatalogMapper mapper, AuditService audit) {
		this.categories = categories;
		this.mapper = mapper;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public List<AdminCategoryResponse> list() {
		return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(mapper::adminCategory).toList();
	}

	public AdminCategoryResponse create(CategoryRequest req) {
		String slug = slugOf(req);
		if (categories.existsBySlug(slug)) {
			throw new ConflictException("Já existe uma categoria com o slug '" + slug + "'.");
		}
		Category category = new Category(slug, req.name().strip(), req.description());
		category.update(slug, req.name().strip(), req.description(), parentOf(req, null), sortOf(req), activeOf(req));
		categories.save(category);
		audit.record("CATEGORY_CREATED", "category", category.getId(), Map.of("slug", slug));
		return mapper.adminCategory(category);
	}

	public AdminCategoryResponse update(Long id, CategoryRequest req) {
		Category category = find(id);
		String slug = slugOf(req);
		if (categories.existsBySlugAndIdNot(slug, id)) {
			throw new ConflictException("Já existe uma categoria com o slug '" + slug + "'.");
		}
		category.update(slug, req.name().strip(), req.description(), parentOf(req, id), sortOf(req), activeOf(req));
		audit.record("CATEGORY_UPDATED", "category", id, Map.of("slug", slug, "active", category.isActive()));
		return mapper.adminCategory(category);
	}

	public void delete(Long id) {
		Category category = find(id);
		if (categories.hasProducts(id)) {
			throw new ConflictException("A categoria possui produtos. Mova-os ou desative a categoria.");
		}
		if (categories.existsByParentId(id)) {
			throw new ConflictException("A categoria possui subcategorias.");
		}
		categories.delete(category);
		audit.record("CATEGORY_DELETED", "category", id, Map.of("slug", category.getSlug()));
	}

	private Category find(Long id) {
		return categories.findById(id).orElseThrow(() -> new NotFoundException("Categoria não encontrada."));
	}

	private static String slugOf(CategoryRequest req) {
		String slug = StringUtils.hasText(req.slug()) ? req.slug() : Slugs.from(req.name());
		if (slug.isEmpty()) {
			throw new FieldValidationException("slug", "Não foi possível gerar o slug a partir do nome.");
		}
		return slug;
	}

	private Category parentOf(CategoryRequest req, Long selfId) {
		if (req.parentId() == null) {
			return null;
		}
		if (req.parentId().equals(selfId)) {
			throw new FieldValidationException("parentId", "A categoria não pode ser pai de si mesma.");
		}
		return categories.findById(req.parentId())
			.orElseThrow(() -> new FieldValidationException("parentId", "Categoria pai inexistente."));
	}

	private static int sortOf(CategoryRequest req) {
		return req.sortOrder() == null ? 0 : req.sortOrder();
	}

	private static boolean activeOf(CategoryRequest req) {
		return req.active() == null || req.active();
	}

}
