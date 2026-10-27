package br.com.gigaverse3d.catalog.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Metadados de imagem; o binário fica no storage (chave gerada pelo servidor, nunca o nome enviado). */
@Entity
@Table(name = "product_images")
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "storage_key", nullable = false, unique = true)
	private String storageKey;

	@Column(name = "alt_text", nullable = false, length = 160)
	private String altText;

	@Column(name = "content_type", nullable = false, length = 20)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private int sizeBytes;

	@Column(name = "width_px")
	private Integer widthPx;

	@Column(name = "height_px")
	private Integer heightPx;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ProductImage() {
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public String getAltText() {
		return altText;
	}

	public String getContentType() {
		return contentType;
	}

	public int getSizeBytes() {
		return sizeBytes;
	}

	public Integer getWidthPx() {
		return widthPx;
	}

	public Integer getHeightPx() {
		return heightPx;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
