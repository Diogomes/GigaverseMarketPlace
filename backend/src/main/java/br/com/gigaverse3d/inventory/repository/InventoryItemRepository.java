package br.com.gigaverse3d.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.inventory.domain.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

	@Query("SELECT i FROM InventoryItem i WHERE i.quantityOnHand <= i.lowStockThreshold ORDER BY i.quantityOnHand")
	List<InventoryItem> findLowStock();

	/** SELECT … FOR UPDATE: serializa alterações concorrentes de estoque do mesmo produto. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId")
	Optional<InventoryItem> findForUpdate(@Param("productId") Long productId);

	List<InventoryItem> findByProductIdIn(Collection<Long> productIds);

}
