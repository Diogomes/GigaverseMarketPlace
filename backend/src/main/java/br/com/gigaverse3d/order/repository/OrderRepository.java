package br.com.gigaverse3d.order.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

	/** Consulta sempre filtrada pelo dono: evita IDOR na área do cliente. */
	Optional<Order> findByNumberAndUserId(Long number, Long userId);

	Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Page<Order> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<OrderStatus> statuses,
			Pageable pageable);

	Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

	Optional<Order> findByNumber(Long number);

	List<Order> findTop100ByStatusAndCreatedAtBeforeOrderByIdAsc(OrderStatus status, Instant before);

}
