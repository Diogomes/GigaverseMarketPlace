package br.com.gigaverse3d.inventory.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.inventory.domain.InsufficientStockException;
import br.com.gigaverse3d.inventory.domain.InventoryItem;
import br.com.gigaverse3d.inventory.domain.StockMovement;
import br.com.gigaverse3d.inventory.repository.InventoryItemRepository;
import br.com.gigaverse3d.inventory.repository.StockMovementRepository;

/**
 * Alterações manuais de estoque. Cada alteração trava a linha do produto (SELECT … FOR UPDATE),
 * valida o saldo e grava movimento + auditoria na mesma transação — sem estoque negativo
 * mesmo com requisições simultâneas.
 */
@Service
@Transactional
public class StockService {

	public enum ManualType {
		/** Entrada por compra/produção (quantidade positiva). */
		PURCHASE,
		/** Correção de inventário (positiva ou negativa; motivo obrigatório). */
		ADJUSTMENT,
		/** Devolução ao estoque (quantidade positiva). */
		RETURN
	}

	public record StockAdjustmentRequest(
			@NotNull(message = "Informe o tipo.") ManualType type,
			@NotNull(message = "Informe a quantidade.") @Min(value = -100000) @Max(value = 100000) Integer quantity,
			@Size(max = 255, message = "Máximo de 255 caracteres.") String reason) {
	}

	public record StockMovementResponse(Long id, StockMovement.Type type, int quantityDelta, int quantityAfter,
			String reason, Long orderId, Instant createdAt) {

		static StockMovementResponse of(StockMovement m) {
			return new StockMovementResponse(m.getId(), m.getType(), m.getQuantityDelta(), m.getQuantityAfter(),
					m.getReason(), m.getOrderId(), m.getCreatedAt());
		}
	}

	private final InventoryItemRepository inventory;

	private final EntityManager em;

	private final StockMovementRepository movements;

	private final AuditService audit;

	public StockService(InventoryItemRepository inventory, StockMovementRepository movements, AuditService audit,
			EntityManager em) {
		this.inventory = inventory;
		this.em = em;
		this.movements = movements;
		this.audit = audit;
	}

	public StockMovementResponse adjust(Long productId, StockAdjustmentRequest req) {
		int qty = req.quantity();
		if (qty == 0) {
			throw new FieldValidationException("quantity", "A quantidade não pode ser zero.");
		}
		if (req.type() != ManualType.ADJUSTMENT && qty < 0) {
			throw new FieldValidationException("quantity", "Entradas e devoluções devem ser positivas.");
		}
		String reason = req.reason() == null ? null : req.reason().strip();
		if (req.type() == ManualType.ADJUSTMENT && (reason == null || reason.isEmpty())) {
			throw new FieldValidationException("reason", "Informe o motivo do ajuste.");
		}
		InventoryItem item = locked(productId)
			.orElseThrow(() -> new NotFoundException("Produto sem controle de estoque."));
		StockMovement movement = item.apply(StockMovement.Type.valueOf(req.type().name()), qty, reason)
			.byActor(audit.currentUserId());
		movements.save(movement);
		audit.record("STOCK_" + req.type().name(), "product", productId,
				Map.of("delta", qty, "after", movement.getQuantityAfter()));
		return StockMovementResponse.of(movement);
	}

	/**
	 * Baixa de estoque de um pedido (dentro da transação do checkout). Trava a linha do produto;
	 * sem saldo suficiente lança {@link InsufficientStockException}.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void reserveForOrder(Long productId, int quantity, Long orderId) {
		InventoryItem item = locked(productId).orElseThrow(() -> new InsufficientStockException(productId, quantity, 0));
		movements.save(item.apply(StockMovement.Type.SALE, -quantity, null).withOrder(orderId));
	}

	/** Devolve ao estoque o que o pedido baixou (cancelamento). */
	@Transactional(propagation = Propagation.MANDATORY)
	public void returnForOrder(Long productId, int quantity, Long orderId, String reason) {
		locked(productId)
			.ifPresent(item -> movements
				.save(item.apply(StockMovement.Type.CANCELLATION, quantity, reason).withOrder(orderId)));
	}

	/**
	 * Trava a linha (SELECT … FOR UPDATE) e RECARREGA o saldo do banco. O refresh é essencial:
	 * a mesma transação pode já ter lido o item sem trava (ex.: cálculo do carrinho), e decidir
	 * com aquela cópia antiga permitiria vender além do estoque.
	 */
	private Optional<InventoryItem> locked(Long productId) {
		InventoryItem item = em.find(InventoryItem.class, productId);
		if (item == null) {
			return Optional.empty();
		}
		em.refresh(item, LockModeType.PESSIMISTIC_WRITE);
		return Optional.of(item);
	}

	@Transactional(readOnly = true)
	public PageResponse<StockMovementResponse> history(Long productId, int page, int size) {
		if (!inventory.existsById(productId)) {
			throw new NotFoundException("Produto sem controle de estoque.");
		}
		return PageResponse.of(movements.findByProductIdOrderByIdDesc(productId, PageRequest.of(page, size)),
				StockMovementResponse::of);
	}

}
