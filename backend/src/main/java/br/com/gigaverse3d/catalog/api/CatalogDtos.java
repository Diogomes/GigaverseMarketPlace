package br.com.gigaverse3d.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.common.text.Slugs;

/** DTOs da API de catálogo. Entidades JPA nunca são expostas diretamente. */
public final class CatalogDtos {

	private CatalogDtos() {
	}

	// ---------------------------------------------------------------- público

	@Schema(description = "Disponibilidade exibida ao cliente (a quantidade exata em estoque não é exposta)")
	public enum Availability {
		IN_STOCK, OUT_OF_STOCK, MADE_TO_ORDER, ON_REQUEST
	}

	public record CategoryResponse(
			@Schema(example = "colecionaveis") String slug,
			@Schema(example = "Colecionáveis") String name,
			String description,
			String parentSlug) {
	}

	public record ImageResponse(Long id, @Schema(example = "/media/products/12/0b6c…e1.jpg") String url,
			String alt, Integer width, Integer height) {
	}

	public record ProductSummaryResponse(
			@Schema(example = "dragao-mecanico-azul") String slug,
			@Schema(example = "Dragão Mecânico Azul") String name,
			String categorySlug,
			String categoryName,
			Product.Type type,
			@Schema(example = "349.90") BigDecimal price,
			@Schema(example = "319.90") BigDecimal promoPrice,
			@Schema(description = "Preço cobrado (promocional, se houver)", example = "319.90") BigDecimal effectivePrice,
			@Schema(example = "BRL") String currency,
			Availability availability,
			Integer productionDays,
			boolean featured,
			ImageResponse image) {
	}

	public record OptionValueResponse(Long id, String label, BigDecimal priceDelta) {
	}

	public record OptionResponse(String name, List<OptionValueResponse> values) {
	}

	public record ProductDetailResponse(String slug, String name, String description, String categorySlug,
			String categoryName, Product.Type type, BigDecimal price, BigDecimal promoPrice, BigDecimal effectivePrice,
			String currency, Availability availability, Integer productionDays, Integer weightGrams, Integer lengthMm,
			Integer widthMm, Integer heightMm, String seoTitle, String seoDescription, List<OptionResponse> options,
			List<ImageResponse> images) {
	}

	// ---------------------------------------------------------------- admin: categorias

	public record CategoryRequest(
			@NotBlank(message = "Informe o nome.") @Size(max = 80, message = "Máximo de 80 caracteres.") String name,
			@Pattern(regexp = Slugs.PATTERN, message = "Slug: letras minúsculas, números e hífen.")
			@Size(max = 80) String slug,
			@Size(max = 300, message = "Máximo de 300 caracteres.") String description,
			Long parentId,
			@Min(value = 0, message = "Ordem não pode ser negativa.") Integer sortOrder,
			Boolean active) {
	}

	public record AdminCategoryResponse(Long id, String slug, String name, String description, Long parentId,
			int sortOrder, boolean active) {
	}

	// ---------------------------------------------------------------- admin: produtos

	public record OptionValueRequest(
			@NotBlank(message = "Informe o rótulo.") @Size(max = 40) String label,
			@NotNull(message = "Informe o acréscimo (0 se não houver).")
			@DecimalMin(value = "0.00", message = "Acréscimo não pode ser negativo.")
			@Digits(integer = 10, fraction = 2, message = "Use até 2 casas decimais.") BigDecimal priceDelta) {
	}

	public record OptionRequest(
			@NotBlank(message = "Informe o nome da opção.") @Size(max = 40) String name,
			@NotEmpty(message = "Informe ao menos um valor.") @Size(max = 10, message = "Máximo de 10 valores.")
			List<@Valid OptionValueRequest> values) {
	}

	public record ProductRequest(
			@NotBlank(message = "Informe o nome.") @Size(min = 3, max = 120, message = "Entre 3 e 120 caracteres.")
			@Schema(example = "Dragão Mecânico Azul") String name,
			@Pattern(regexp = Slugs.PATTERN, message = "Slug: letras minúsculas, números e hífen.") @Size(max = 120)
			@Schema(description = "Opcional; gerado a partir do nome", example = "dragao-mecanico-azul") String slug,
			@NotBlank(message = "Informe o SKU.")
			@Pattern(regexp = "^[A-Z0-9-]{3,32}$", message = "SKU: 3 a 32 caracteres, maiúsculas, números e hífen.")
			@Schema(example = "GV-COL-001") String sku,
			@NotBlank(message = "Informe a descrição.")
			@Size(min = 10, max = 4000, message = "Entre 10 e 4000 caracteres.") String description,
			@NotNull(message = "Selecione a categoria.") Long categoryId,
			@NotNull(message = "Informe o tipo.") Product.Type type,
			@NotNull(message = "Informe o status.") Product.Status status,
			@NotNull(message = "Informe o preço.") @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero.")
			@Digits(integer = 10, fraction = 2, message = "Use até 2 casas decimais.")
			@Schema(example = "349.90") BigDecimal price,
			@DecimalMin(value = "0.01", message = "Preço promocional deve ser maior que zero.")
			@Digits(integer = 10, fraction = 2, message = "Use até 2 casas decimais.")
			@Schema(example = "319.90") BigDecimal promoPrice,
			@Min(value = 1, message = "Mínimo de 1 dia.") @Max(value = 120, message = "Máximo de 120 dias.")
			Integer productionDays,
			@Positive Integer weightGrams,
			@Positive Integer lengthMm,
			@Positive Integer widthMm,
			@Positive Integer heightMm,
			Boolean featured,
			@Size(max = 70, message = "Máximo de 70 caracteres.") String seoTitle,
			@Size(max = 160, message = "Máximo de 160 caracteres.") String seoDescription,
			@Min(value = 0, message = "Não pode ser negativo.") Integer lowStockThreshold,
			@Size(max = 5, message = "Máximo de 5 grupos de opções.") List<@Valid OptionRequest> options,
			@Schema(description = "Obrigatório na edição: versão lida (controle de concorrência)") Long version) {
	}

	public record StockResponse(int onHand, int reserved, int available, int lowStockThreshold, boolean low) {
	}

	public record AdminImageResponse(Long id, String url, String alt, String contentType, int sizeBytes,
			Integer width, Integer height, int sortOrder) {
	}

	public record AdminProductResponse(Long id, String slug, String sku, String name, String description,
			Long categoryId, String categorySlug, Product.Type type, Product.Status status, BigDecimal price,
			BigDecimal promoPrice, String currency, Integer productionDays, Integer weightGrams, Integer lengthMm,
			Integer widthMm, Integer heightMm, boolean featured, String seoTitle, String seoDescription,
			StockResponse stock, List<OptionResponse> options, List<AdminImageResponse> images, Long version,
			Instant createdAt, Instant updatedAt) {
	}

	public record AdminProductListItem(Long id, String slug, String sku, String name, String categoryName,
			Product.Type type, Product.Status status, BigDecimal price, BigDecimal promoPrice, int stockOnHand,
			boolean lowStock, String imageUrl, Instant updatedAt) {
	}

}
