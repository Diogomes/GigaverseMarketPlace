package br.com.gigaverse3d.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	Optional<Product> findBySlugAndStatus(String slug, Product.Status status);

	boolean existsBySlug(String slug);

	boolean existsBySku(String sku);

	boolean existsBySlugAndIdNot(String slug, Long id);

	boolean existsBySkuAndIdNot(String sku, Long id);

	@Query("SELECT count(oi) > 0 FROM OrderItem oi WHERE oi.productId = :productId")
	boolean hasOrderItems(@Param("productId") Long productId);

	/** Busca textual (português) apenas entre produtos ativos, ordenada por relevância. */
	@Query(value = """
			SELECT p.id FROM products p
			WHERE p.status = 'ACTIVE' AND p.search_vector @@ plainto_tsquery('portuguese_unaccent', :term)
			ORDER BY ts_rank(p.search_vector, plainto_tsquery('portuguese_unaccent', :term)) DESC, p.id
			LIMIT :limit
			""", nativeQuery = true)
	List<Long> searchActiveIds(@Param("term") String term, @Param("limit") int limit);

	/**
	 * Catálogo público: só ACTIVE; filtros opcionais; ordenação por lista branca ({@code sort}).
	 * Parâmetros sempre ligados (sem concatenação de SQL).
	 */
	@Query(value = """
			SELECT p.* FROM products p JOIN categories c ON c.id = p.category_id
			WHERE p.status = 'ACTIVE' AND c.active
			  AND (CAST(:category AS text) IS NULL OR c.slug = CAST(:category AS text))
			  AND (CAST(:type AS text) IS NULL OR p.product_type = CAST(:type AS text))
			  AND (CAST(:q AS text) IS NULL OR p.search_vector @@ plainto_tsquery('portuguese_unaccent', CAST(:q AS text)))
			  AND (CAST(:featured AS boolean) IS NULL OR p.featured = CAST(:featured AS boolean))
			ORDER BY
			  CASE WHEN CAST(:sort AS text) = 'price_asc' THEN COALESCE(p.promo_price, p.price) END ASC,
			  CASE WHEN CAST(:sort AS text) = 'price_desc' THEN COALESCE(p.promo_price, p.price) END DESC,
			  CASE WHEN CAST(:sort AS text) = 'name' THEN p.name END ASC,
			  CASE WHEN CAST(:sort AS text) = 'newest' THEN p.created_at END DESC,
			  CASE WHEN CAST(:q AS text) IS NOT NULL
			       THEN ts_rank(p.search_vector, plainto_tsquery('portuguese_unaccent', CAST(:q AS text))) END DESC,
			  p.featured DESC, p.id DESC
			""", countQuery = """
			SELECT count(*) FROM products p JOIN categories c ON c.id = p.category_id
			WHERE p.status = 'ACTIVE' AND c.active
			  AND (CAST(:category AS text) IS NULL OR c.slug = CAST(:category AS text))
			  AND (CAST(:type AS text) IS NULL OR p.product_type = CAST(:type AS text))
			  AND (CAST(:q AS text) IS NULL OR p.search_vector @@ plainto_tsquery('portuguese_unaccent', CAST(:q AS text)))
			  AND (CAST(:featured AS boolean) IS NULL OR p.featured = CAST(:featured AS boolean))
			""", nativeQuery = true)
	Page<Product> searchPublic(@Param("q") String q, @Param("category") String category, @Param("type") String type,
			@Param("featured") Boolean featured, @Param("sort") String sort, Pageable pageable);

	@Query("SELECT p.slug FROM Product p WHERE p.status = br.com.gigaverse3d.catalog.domain.Product.Status.ACTIVE "
			+ "AND p.category.active = true ORDER BY p.id")
	List<String> findActiveSlugs();

}
