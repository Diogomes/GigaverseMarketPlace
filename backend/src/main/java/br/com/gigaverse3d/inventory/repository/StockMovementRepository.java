package br.com.gigaverse3d.inventory.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.inventory.domain.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

	Page<StockMovement> findByProductIdOrderByIdDesc(Long productId, Pageable pageable);

	/** Saldo líquido movimentado por produto num pedido (baixas negativas, devoluções positivas). */
	@Query("SELECT m.product.id AS productId, SUM(m.quantityDelta) AS net FROM StockMovement m "
			+ "WHERE m.orderId = :orderId GROUP BY m.product.id")
	List<ProductNet> netByOrder(@Param("orderId") Long orderId);

	interface ProductNet {

		Long getProductId();

		Long getNet();

	}

}
