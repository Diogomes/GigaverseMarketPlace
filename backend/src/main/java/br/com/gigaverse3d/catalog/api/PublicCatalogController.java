package br.com.gigaverse3d.catalog.api;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.catalog.api.CatalogDtos.CategoryResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductDetailResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductSummaryResponse;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.service.CatalogQueryService;
import br.com.gigaverse3d.common.web.PageResponse;

@Tag(name = "Catálogo público", description = "Somente produtos ativos em categorias ativas")
@RestController
@RequestMapping("/api/v1/public")
public class PublicCatalogController {

	private final CatalogQueryService catalog;

	public PublicCatalogController(CatalogQueryService catalog) {
		this.catalog = catalog;
	}

	@Operation(summary = "Lista categorias ativas")
	@GetMapping("/categories")
	public List<CategoryResponse> categories() {
		return catalog.categories();
	}

	@Operation(summary = "Busca/lista produtos com filtros, ordenação e paginação")
	@GetMapping("/products")
	public PageResponse<ProductSummaryResponse> products(
			@Parameter(description = "Busca textual em português") @RequestParam(required = false)
			@Size(max = 100, message = "Busca com no máximo 100 caracteres.") String q,
			@Parameter(description = "Slug da categoria", example = "colecionaveis") @RequestParam(required = false)
			@Size(max = 80) String category,
			@RequestParam(required = false) Product.Type type,
			@Parameter(description = "true = só destaques") @RequestParam(required = false) Boolean featured,
			@Parameter(description = "relevance | price_asc | price_desc | name | newest")
			@RequestParam(defaultValue = "relevance")
			@Pattern(regexp = "relevance|price_asc|price_desc|name|newest", message = "Ordenação inválida.") String sort,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "Página inválida.") int page,
			@RequestParam(defaultValue = "12") @Min(value = 1, message = "Tamanho mínimo 1.")
			@Max(value = PageResponse.MAX_PAGE_SIZE, message = "Tamanho máximo 48.") int size) {
		return catalog.search(q, category, type, featured, sort, page, size);
	}

	@Operation(summary = "Detalhe de um produto ativo pelo slug")
	@GetMapping("/products/{slug}")
	public ProductDetailResponse product(@PathVariable @Size(max = 120) String slug) {
		return catalog.bySlug(slug);
	}

}
