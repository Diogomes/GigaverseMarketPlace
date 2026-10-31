package br.com.gigaverse3d.inventory.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.gigaverse3d.inventory.domain.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

	@Query("SELECT i FROM InventoryItem i WHERE i.quantityOnHand <= i.lowStockThreshold ORDER BY i.quantityOnHand")
	List<InventoryItem> findLowStock();

	List<InventoryItem> findByProductIdIn(Collection<Long> productIds);

}
