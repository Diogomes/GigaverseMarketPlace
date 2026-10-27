package br.com.gigaverse3d.catalog.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.common.domain.TimestampedEntity;

@Entity
@Table(name = "products")
public class Product extends TimestampedEntity {

	public enum Type {
		/** Pronta entrega: depende de estoque. */
		READY,
		/** Produzido após o pedido; exige prazo de produção. */
		MADE_TO_ORDER,
		/** Personalizado: preço de referência, valor final por orçamento. */
		CUSTOM
	}

	public enum Status {
		DRAFT, ACTIVE, ARCHIVED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Column(nullable = false, unique = true, length = 32)
	private String sku;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(name = "product_type", nullable = false, length = 20)
	private Type type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.DRAFT;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "promo_price", precision = 12, scale = 2)
	private BigDecimal promoPrice;

	@Column(nullable = false, length = 3)
	private String currency = Money.CURRENCY;

	@Column(name = "production_days")
	private Integer productionDays;

	@Column(name = "weight_grams")
	private Integer weightGrams;

	@Column(name = "length_mm")
	private Integer lengthMm;

	@Column(name = "width_mm")
	private Integer widthMm;

	@Column(name = "height_mm")
	private Integer heightMm;

	@Column(nullable = false)
	private boolean featured;

	@Column(name = "seo_title", length = 70)
	private String seoTitle;

	@Column(name = "seo_description", length = 160)
	private String seoDescription;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<ProductOption> options = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<ProductImage> images = new ArrayList<>();

	@Version
	private Long version;

	protected Product() {
	}

	public Product(String slug, String sku, String name, String description, Category category, Type type,
			BigDecimal price) {
		this.slug = slug;
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.category = Objects.requireNonNull(category);
		this.type = Objects.requireNonNull(type);
		this.price = Money.normalize(price);
	}

	/** Preço cobrado (promocional, se houver). Base para o cálculo no servidor — nunca o do cliente. */
	public BigDecimal effectivePrice() {
		return promoPrice != null ? promoPrice : price;
	}

	public boolean isPubliclyVisible() {
		return status == Status.ACTIVE;
	}

	/** Atualiza os dados editáveis. Regras entre campos são validadas no serviço e reforçadas pelo banco. */
	public void updateDetails(String slug, String sku, String name, String description, Category category, Type type,
			BigDecimal price, BigDecimal promoPrice, Integer productionDays, Integer weightGrams, Integer lengthMm,
			Integer widthMm, Integer heightMm, boolean featured, String seoTitle, String seoDescription) {
		this.slug = slug;
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.category = Objects.requireNonNull(category);
		this.type = Objects.requireNonNull(type);
		this.price = Money.normalize(price);
		setPromoPrice(promoPrice);
		this.productionDays = productionDays;
		this.weightGrams = weightGrams;
		this.lengthMm = lengthMm;
		this.widthMm = widthMm;
		this.heightMm = heightMm;
		this.featured = featured;
		this.seoTitle = seoTitle;
		this.seoDescription = seoDescription;
	}

	public void clearOptions() {
		options.clear();
	}

	public ProductImage addImage(String storageKey, String altText, String contentType, int sizeBytes, Integer widthPx,
			Integer heightPx) {
		int nextOrder = images.stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1;
		ProductImage image = new ProductImage(this, storageKey, altText, contentType, sizeBytes, widthPx, heightPx,
				nextOrder);
		images.add(image);
		return image;
	}

	public void removeImage(ProductImage image) {
		images.remove(image);
	}

	public Long getVersion() {
		return version;
	}

	public void setStatus(Status status) {
		this.status = Objects.requireNonNull(status);
	}

	public void setPromoPrice(BigDecimal promoPrice) {
		this.promoPrice = promoPrice == null ? null : Money.normalize(promoPrice);
	}

	public void setProductionDays(Integer productionDays) {
		this.productionDays = productionDays;
	}

	public void setFeatured(boolean featured) {
		this.featured = featured;
	}

	public ProductOption addOption(String name) {
		ProductOption option = new ProductOption(this, name, options.size());
		options.add(option);
		return option;
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public Type getType() {
		return type;
	}

	public Status getStatus() {
		return status;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BigDecimal getPromoPrice() {
		return promoPrice;
	}

	public String getCurrency() {
		return currency;
	}

	public Integer getProductionDays() {
		return productionDays;
	}

	public Integer getWeightGrams() {
		return weightGrams;
	}

	public Integer getLengthMm() {
		return lengthMm;
	}

	public Integer getWidthMm() {
		return widthMm;
	}

	public Integer getHeightMm() {
		return heightMm;
	}

	public boolean isFeatured() {
		return featured;
	}

	public String getSeoTitle() {
		return seoTitle;
	}

	public String getSeoDescription() {
		return seoDescription;
	}

	public List<ProductOption> getOptions() {
		return Collections.unmodifiableList(options);
	}

	public List<ProductImage> getImages() {
		return Collections.unmodifiableList(images);
	}

}
