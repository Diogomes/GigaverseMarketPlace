package br.com.gigaverse3d.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.catalog.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findBySlug(String slug);

	List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();

	List<Category> findAllByOrderBySortOrderAscNameAsc();

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

	boolean existsByParentId(Long parentId);

	@Query("SELECT count(p) > 0 FROM Product p WHERE p.category.id = :categoryId")
	boolean hasProducts(@Param("categoryId") Long categoryId);

}
