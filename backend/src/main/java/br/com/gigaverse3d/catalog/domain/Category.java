package br.com.gigaverse3d.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.gigaverse3d.common.domain.TimestampedEntity;

@Entity
@Table(name = "categories")
public class Category extends TimestampedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String slug;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(length = 300)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active = true;

	protected Category() {
	}

	public Category(String slug, String name, String description) {
		this.slug = slug;
		this.name = name;
		this.description = description;
	}

	public void update(String slug, String name, String description, Category parent, int sortOrder, boolean active) {
		this.slug = slug;
		this.name = name;
		this.description = description;
		this.parent = parent;
		this.sortOrder = sortOrder;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getParent() {
		return parent;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}

}
