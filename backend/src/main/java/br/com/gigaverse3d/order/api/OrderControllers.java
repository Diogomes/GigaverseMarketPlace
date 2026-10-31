package br.com.gigaverse3d.order.api;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.AccountUserDetails;
import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.domain.BusinessTime;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.common.web.CsvWriter;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.order.api.OrderDtos.AdminOrderListItem;
import br.com.gigaverse3d.order.api.OrderDtos.AdminOrderView;
import br.com.gigaverse3d.order.api.OrderDtos.AdminStatusChangeView;
import br.com.gigaverse3d.order.api.OrderDtos.ConfirmPaymentRequest;
import br.com.gigaverse3d.order.api.OrderDtos.OrderDetailView;
import br.com.gigaverse3d.order.api.OrderDtos.OrderSummaryView;
import br.com.gigaverse3d.order.api.OrderDtos.StatusChangeRequest;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;
import br.com.gigaverse3d.order.domain.OrderStatusChange;
import br.com.gigaverse3d.order.repository.OrderRepository;
import br.com.gigaverse3d.order.service.OrderService;
import br.com.gigaverse3d.order.service.OrderViews;

/** Pedidos do cliente (sempre filtrados pelo dono) e da equipe. */
public final class OrderControllers {

	private OrderControllers() {
	}

	@Tag(name = "Meus pedidos")
	@RestController
	@RequestMapping("/api/v1/account/orders")
	public static class AccountOrders {

		private final OrderRepository orders;

		private final OrderService service;

		private final OrderViews views;

		public AccountOrders(OrderRepository orders, OrderService service, OrderViews views) {
			this.orders = orders;
			this.service = service;
			this.views = views;
		}

		/** Grupos de status usados nas abas da área do cliente. */
		public enum Group {

			ACTIVE(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.IN_PRODUCTION, OrderStatus.READY_FOR_PICKUP,
					OrderStatus.SHIPPED),
			DELIVERED(OrderStatus.DELIVERED), CANCELLED(OrderStatus.CANCELLED);

			final List<OrderStatus> statuses;

			Group(OrderStatus... statuses) {
				this.statuses = List.of(statuses);
			}

		}

		@Operation(summary = "Meus pedidos (mais recentes primeiro), opcionalmente por grupo de status")
		@GetMapping
		@Transactional(readOnly = true)
		public PageResponse<OrderSummaryView> list(@RequestParam(required = false) Group group,
				@RequestParam(defaultValue = "0") @Min(0) int page,
				@RequestParam(defaultValue = "10") @Min(1) @Max(PageResponse.MAX_PAGE_SIZE) int size) {
			Long userId = CurrentUser.requireId();
			PageRequest pageable = PageRequest.of(page, size);
			return PageResponse.of(group == null ? orders.findByUserIdOrderByCreatedAtDesc(userId, pageable)
					: orders.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, group.statuses, pageable), views::summary);
		}

		@Operation(summary = "Detalhe de um pedido meu (pedido de outra pessoa → 404)")
		@GetMapping("/{number}")
		@Transactional(readOnly = true)
		public OrderDetailView detail(@PathVariable Long number) {
			return orders.findByNumberAndUserId(number, CurrentUser.requireId())
				.map(views::detail)
				.orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
		}

		@Operation(summary = "Cancela meu pedido enquanto aguarda pagamento")
		@PostMapping("/{number}/cancel")
		public OrderDetailView cancel(@PathVariable Long number) {
			return service.cancelByCustomer(CurrentUser.requireId(), number);
		}

	}

	@Tag(name = "Admin — pedidos")
	@RestController
	@RequestMapping("/api/v1/admin/orders")
	public static class AdminOrders {

		static final int MAX_EXPORT_ROWS = 5000;

		private final OrderRepository orders;

		private final OrderService service;

		private final OrderViews views;

		private final UserRepository users;

		private final AuditService audit;

		private final Clock clock;

		public AdminOrders(OrderRepository orders, OrderService service, OrderViews views, UserRepository users,
				AuditService audit, Clock clock) {
			this.orders = orders;
			this.service = service;
			this.views = views;
			this.users = users;
			this.audit = audit;
			this.clock = clock;
		}

		@Operation(summary = "Lista pedidos com filtros (status, período de criação, cliente, número/nome/e-mail)")
		@GetMapping
		@Transactional(readOnly = true)
		public PageResponse<AdminOrderListItem> list(@RequestParam(required = false) OrderStatus status,
				@RequestParam(required = false) @Size(max = 100) String q,
				@RequestParam(required = false) Long customerId,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
				@RequestParam(defaultValue = "0") @Min(0) int page,
				@RequestParam(defaultValue = "20") @Min(1) @Max(PageResponse.MAX_PAGE_SIZE) int size) {
			return PageResponse.of(orders.findAll(filter(status, q, customerId, from, to),
					PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))), views::adminListItem);
		}

		@Operation(summary = "Exporta pedidos em CSV (somente ADMIN; máx. 5000 linhas; sem endereço; auditado)")
		@GetMapping("/export")
		@Transactional
		public ResponseEntity<byte[]> export(@RequestParam(required = false) OrderStatus status,
				@RequestParam(required = false) @Size(max = 100) String q,
				@RequestParam(required = false) Long customerId,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
			Page<Order> page = orders.findAll(filter(status, q, customerId, from, to),
					PageRequest.of(0, MAX_EXPORT_ROWS + 1, Sort.by(Sort.Direction.ASC, "id")));
			if (page.getNumberOfElements() > MAX_EXPORT_ROWS) {
				throw new ConflictException(
						"A exportação está limitada a " + MAX_EXPORT_ROWS + " pedidos. Refine os filtros e tente novamente.");
			}
			CsvWriter csv = new CsvWriter().row(List.of("numero", "criado_em", "status", "cliente", "email",
					"pagamento", "entrega", "subtotal", "frete", "desconto", "total", "pago_em"));
			page.forEach(o -> csv.row(Arrays.asList(o.getNumber(), o.getPlacedAt(), o.getStatus(), o.getCustomerName(),
					o.getCustomerEmail(), o.getPaymentMethod(), o.getShippingMethod(), o.getSubtotal(),
					o.getShippingTotal(), o.getDiscountTotal(), o.getTotal(), o.getPaidAt())));
			// Filtros usados, sem o texto livre da busca (pode ser um e-mail).
			Map<String, Object> details = new LinkedHashMap<>();
			details.put("rows", page.getNumberOfElements());
			details.put("search", StringUtils.hasText(q));
			if (status != null) {
				details.put("status", status.name());
			}
			if (customerId != null) {
				details.put("customerId", customerId);
			}
			if (from != null) {
				details.put("from", from.toString());
			}
			if (to != null) {
				details.put("to", to.toString());
			}
			audit.record("ORDERS_EXPORTED", "order", null, details);
			return csv.toResponse("pedidos-" + BusinessTime.today(clock) + ".csv");
		}

		private static Specification<Order> filter(OrderStatus status, String q, Long customerId, LocalDate from,
				LocalDate to) {
			return (root, query, cb) -> {
				List<Predicate> p = new ArrayList<>();
				if (status != null) {
					p.add(cb.equal(root.get("status"), status));
				}
				if (customerId != null) {
					p.add(cb.equal(root.get("userId"), customerId));
				}
				if (from != null) {
					p.add(cb.greaterThanOrEqualTo(root.get("placedAt"), BusinessTime.startOf(from)));
				}
				if (to != null) {
					p.add(cb.lessThan(root.get("placedAt"), BusinessTime.endOf(to)));
				}
				if (StringUtils.hasText(q)) {
					String term = q.strip().toLowerCase();
					List<Predicate> or = new ArrayList<>();
					String like = "%" + term.replace("%", "\\%").replace("_", "\\_") + "%";
					or.add(cb.like(cb.lower(root.get("customerEmail")), like, '\\'));
					or.add(cb.like(cb.lower(root.get("customerName")), like, '\\'));
					if (term.matches("[0-9]{1,18}")) {
						or.add(cb.equal(root.get("number"), Long.valueOf(term)));
					}
					p.add(cb.or(or.toArray(Predicate[]::new)));
				}
				return cb.and(p.toArray(Predicate[]::new));
			};
		}

		@Operation(summary = "Detalhe administrativo (cliente, autor de cada mudança e próximas transições)")
		@GetMapping("/{number}")
		@Transactional(readOnly = true)
		public AdminOrderView detail(@PathVariable Long number) {
			Order order = orders.findByNumber(number).orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
			Map<Long, String> actors = users
				.findAllById(order.getHistory().stream().map(OrderStatusChange::getChangedByUserId)
					.filter(Objects::nonNull).distinct().toList())
				.stream()
				.collect(Collectors.toMap(User::getId, User::getFullName));
			List<AdminStatusChangeView> history = order.getHistory().stream()
				.map(h -> new AdminStatusChangeView(h.getFromStatus(), h.getToStatus(), h.getCreatedAt(), h.getReason(),
						h.getChangedByUserId(), h.getChangedByUserId() == null ? null : actors.get(h.getChangedByUserId())))
				.toList();
			List<OrderStatus> next = order.getStatus().allowedNext().stream()
				.filter(s -> s != OrderStatus.PAID)
				.filter(s -> s != OrderStatus.SHIPPED || order.getShippingMethod() == Order.ShippingMethod.DELIVERY)
				.filter(s -> s != OrderStatus.READY_FOR_PICKUP || order.getShippingMethod() == Order.ShippingMethod.PICKUP)
				.toList();
			return new AdminOrderView(views.detail(order), order.getUserId(), order.getCustomerName(),
					order.getCustomerEmail(), order.getPaidAt(), order.getCancellationReason(), history, next,
					order.getStatus() == OrderStatus.PENDING_PAYMENT);
		}

		@Operation(summary = "Muda o status (transições válidas; cancelar pedido pago só ADMIN)")
		@PostMapping("/{number}/status")
		public OrderDetailView changeStatus(@PathVariable Long number, @Valid @RequestBody StatusChangeRequest req) {
			AccountUserDetails actor = CurrentUser.require();
			return service.changeStatus(number, req.status(), req.reason(), req.trackingCode(), actor.getId(),
					actor.getRole() == User.Role.ADMIN);
		}

		@Operation(summary = "Confirma pagamento recebido (baixa manual; somente ADMIN)")
		@PostMapping("/{number}/confirm-payment")
		public OrderDetailView confirmPayment(@PathVariable Long number, @Valid @RequestBody ConfirmPaymentRequest req) {
			return service.confirmPayment(number, CurrentUser.requireId(), req.reference(), req.note());
		}

	}

}
