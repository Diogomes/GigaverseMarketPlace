package br.com.gigaverse3d.catalog.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminCategoryResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminImageResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminProductListItem;
import br.com.gigaverse3d.catalog.api.CatalogDtos.AdminProductResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.Availability;
import br.com.gigaverse3d.catalog.api.CatalogDtos.CategoryResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ImageResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.OptionResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.OptionValueResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductDetailResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.ProductSummaryResponse;
import br.com.gigaverse3d.catalog.api.CatalogDtos.StockResponse;
import br.com.gigaverse3d.catalog.domain.Category;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.domain.ProductImage;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.storage.StorageService;

/** Converte entidades em DTOs (deve ser chamado dentro de transação: relações são LAZY). */
@Component
public class CatalogMapper {

	private final StorageService storage;

	public CatalogMapper(StorageService storage) {
		this.storage = storage;
	}

	public static Availability availability(Product p, InventoryItem inventory) {
		return switch (p.getType()) {
			case CUSTOM -> Availability.ON_REQUEST;
			case MADE_TO_ORDER -> Availability.MADE_TO_ORDER;
			case READY -> inventory != null && inventory.available() > 0 ? Availability.IN_STOCK
					: Availability.OUT_OF_STOCK;
		};
	}

	public CategoryResponse category(Category c) {
		return new CategoryResponse(c.getSlug(), c.getName(), c.getDescription(),
				c.getParent() == null ? null : c.getParent().getSlug());
	}

	public AdminCategoryResponse adminCategory(Category c) {
		return new AdminCategoryResponse(c.getId(), c.getSlug(), c.getName(), c.getDescription(),
				c.getParent() == null ? null : c.getParent().getId(), c.getSortOrder(), c.isActive());
	}

	public ImageResponse image(ProductImage i) {
		return new ImageResponse(i.getId(), storage.publicUrl(i.getStorageKey()), i.getAltText(), i.getWidthPx(),
				i.getHeightPx());
	}

	private ImageResponse primaryImage(Product p) {
		return p.getImages().stream().min(Comparator.comparingInt(ProductImage::getSortOrder)).map(this::image)
			.orElse(null);
	}

	private List<OptionResponse> options(Product p) {
		return p.getOptions()
			.stream()
			.map(o -> new OptionResponse(o.getName(),
					o.getValues()
						.stream()
						.map(v -> new OptionValueResponse(v.getId(), v.getLabel(), v.getPriceDelta()))
						.toList()))
			.toList();
	}

	public ProductSummaryResponse summary(Product p, InventoryItem inventory) {
		return new ProductSummaryResponse(p.getSlug(), p.getName(), p.getCategory().getSlug(),
				p.getCategory().getName(), p.getType(), p.getPrice(), p.getPromoPrice(), p.effectivePrice(),
				p.getCurrency(), availability(p, inventory), p.getProductionDays(), p.isFeatured(), primaryImage(p));
	}

	public ProductDetailResponse detail(Product p, InventoryItem inventory) {
		return new ProductDetailResponse(p.getSlug(), p.getName(), p.getDescription(), p.getCategory().getSlug(),
				p.getCategory().getName(), p.getType(), p.getPrice(), p.getPromoPrice(), p.effectivePrice(),
				p.getCurrency(), availability(p, inventory), p.getProductionDays(), p.getWeightGrams(), p.getLengthMm(),
				p.getWidthMm(), p.getHeightMm(), p.getSeoTitle(), p.getSeoDescription(), options(p),
				p.getImages().stream().map(this::image).toList());
	}

	public StockResponse stock(InventoryItem i) {
		return new StockResponse(i.getQuantityOnHand(), i.getQuantityReserved(), i.available(),
				i.getLowStockThreshold(), i.isLowStock());
	}

	public AdminProductResponse admin(Product p, InventoryItem inventory) {
		List<AdminImageResponse> images = p.getImages()
			.stream()
			.map(i -> new AdminImageResponse(i.getId(), storage.publicUrl(i.getStorageKey()), i.getAltText(),
					i.getContentType(), i.getSizeBytes(), i.getWidthPx(), i.getHeightPx(), i.getSortOrder()))
			.toList();
		return new AdminProductResponse(p.getId(), p.getSlug(), p.getSku(), p.getName(), p.getDescription(),
				p.getCategory().getId(), p.getCategory().getSlug(), p.getType(), p.getStatus(), p.getPrice(),
				p.getPromoPrice(), p.getCurrency(), p.getProductionDays(), p.getWeightGrams(), p.getLengthMm(),
				p.getWidthMm(), p.getHeightMm(), p.isFeatured(), p.getSeoTitle(), p.getSeoDescription(),
				inventory == null ? null : stock(inventory), options(p), images, p.getVersion(), p.getCreatedAt(),
				p.getUpdatedAt());
	}

	public AdminProductListItem adminListItem(Product p, InventoryItem inventory) {
		ImageResponse image = primaryImage(p);
		return new AdminProductListItem(p.getId(), p.getSlug(), p.getSku(), p.getName(), p.getCategory().getName(),
				p.getType(), p.getStatus(), p.getPrice(), p.getPromoPrice(),
				inventory == null ? 0 : inventory.getQuantityOnHand(), inventory != null && inventory.isLowStock(),
				image == null ? null : image.url(), p.getUpdatedAt());
	}

}
