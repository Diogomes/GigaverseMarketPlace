package br.com.gigaverse3d.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gigaverse3d.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	/** Consulta sempre filtrada pelo dono: evita IDOR na área do cliente. */
	Optional<Order> findByNumberAndUserId(Long number, Long userId);

	Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

}
