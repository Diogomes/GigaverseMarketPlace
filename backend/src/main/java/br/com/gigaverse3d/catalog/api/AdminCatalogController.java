package br.com.gigaverse3d.catalog.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminCategoryResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminProductListItem;
import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminProductResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.CategoryRequest;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductRequest;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.service.CategoryAdminService;
import br.com.gigaverse3d.catalog.service.ProductAdminService;
import br.com.gigaverse3d.catalog.service.ProductImageService;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.inventory.service.StockService;
import br.com.gigaverse3d.inventory.service.StockService.StockAdjustmentRequest;
import br.com.gigaverse3d.inventory.service.StockService.StockMovementResponse;

/** Administração do catálogo. Autorização no servidor: ADMIN/OPERATOR (exclusões só ADMIN) — ver SecurityConfig. */
@Tag(name = "Admin — catálogo e estoque")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

	private final CategoryAdminService categories;

	private final ProductAdminService products;

	private final ProductImageService images;

	private final StockService stock;

	public AdminCatalogController(CategoryAdminService categories, ProductAdminService products,
			ProductImageService images, StockService stock) {
		this.categories = categories;
		this.products = products;
		this.images = images;
		this.stock = stock;
	}

	// --- categorias

	@Operation(summary = "Lista todas as categorias (inclui inativas)")
	@GetMapping("/categories")
	public List<AdminCategoryResponse> listCategories() {
		return categories.list();
	}

	@Operation(summary = "Cria categoria")
	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminCategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
		return categories.create(req);
	}

	@Operation(summary = "Atualiza categoria")
	@PutMapping("/categories/{id}")
	public AdminCategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
		return categories.update(id, req);
	}

	@Operation(summary = "Exclui categoria sem produtos (somente ADMIN)")
	@DeleteMapping("/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCategory(@PathVariable Long id) {
		categories.delete(id);
	}

	// --- produtos

	@Operation(summary = "Lista produtos (todos os status) com busca, filtros e paginação")
	@GetMapping("/products")
	public PageResponse<AdminProductListItem> listProducts(
			@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) Product.Status status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(PageResponse.MAX_PAGE_SIZE) int size,
			@RequestParam(defaultValue = "updatedAt") String sort,
			@RequestParam(defaultValue = "DESC") Sort.Direction direction) {
		return products.list(q, categoryId, status, page, size, sort, direction);
	}

	@Operation(summary = "Detalhe administrativo do produto (inclui estoque e versão)")
	@GetMapping("/products/{id}")
	public AdminProductResponse getProduct(@PathVariable Long id) {
		return products.get(id);
	}

	@Operation(summary = "Cria produto (estoque inicia em 0; use movimentações para entradas)")
	@PostMapping("/products")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminProductResponse createProduct(@Valid @RequestBody ProductRequest req) {
		return products.create(req);
	}

	@Operation(summary = "Atualiza produto (exige a versão lida; 409 se outra pessoa alterou)")
	@PutMapping("/products/{id}")
	public AdminProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
		return products.update(id, req);
	}

	@Operation(summary = "Exclui produto sem pedidos (somente ADMIN); com pedidos, arquive")
	@DeleteMapping("/products/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProduct(@PathVariable Long id) {
		products.delete(id);
	}

	// --- imagens

	@Operation(summary = "Envia imagem (JPG/PNG/WebP, até 5 MB, máx. 8 por produto)")
	@PostMapping(path = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public AdminProductResponse uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file,
			@RequestParam("altText") String altText) {
		try {
			return images.upload(id, file.getOriginalFilename(), file.getBytes(), altText);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Operation(summary = "Remove imagem do produto (somente ADMIN)")
	@DeleteMapping("/products/{id}/images/{imageId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
		images.delete(id, imageId);
	}

	// --- estoque

	@Operation(summary = "Histórico de movimentações de estoque")
	@GetMapping("/products/{id}/stock-movements")
	public PageResponse<StockMovementResponse> stockHistory(@PathVariable Long id,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(PageResponse.MAX_PAGE_SIZE) int size) {
		return stock.history(id, page, size);
	}

	@Operation(summary = "Registra entrada, devolução ou ajuste de estoque (auditado)")
	@PostMapping("/products/{id}/stock-movements")
	@ResponseStatus(HttpStatus.CREATED)
	public StockMovementResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest req) {
		return stock.adjust(id, req);
	}

}
