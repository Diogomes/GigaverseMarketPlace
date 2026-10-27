package br.com.gigaverse3d.inventory.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gigaverse3d.inventory.domain.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

	Page<StockMovement> findByProductIdOrderByIdDesc(Long productId, Pageable pageable);

}
