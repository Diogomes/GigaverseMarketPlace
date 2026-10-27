package br.com.gigaverse3d.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findBySlugAndStatus(String slug, Product.Status status);

	/** Busca textual (português) apenas entre produtos ativos, ordenada por relevância. */
	@Query(value = """
			SELECT p.id FROM products p
			WHERE p.status = 'ACTIVE' AND p.search_vector @@ plainto_tsquery('portuguese', :term)
			ORDER BY ts_rank(p.search_vector, plainto_tsquery('portuguese', :term)) DESC, p.id
			LIMIT :limit
			""", nativeQuery = true)
	List<Long> searchActiveIds(@Param("term") String term, @Param("limit") int limit);

}
