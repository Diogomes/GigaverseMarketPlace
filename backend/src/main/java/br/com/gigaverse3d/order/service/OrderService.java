package br.com.gigaverse3d.order.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.domain.Address;
import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.AddressRepository;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.cart.service.CartService;
import br.com.gigaverse3d.cart.service.CartService.PricedCart;
import br.com.gigaverse3d.cart.service.Pricing.PricedLine;
import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.inventory.repository.StockMovementRepository;
import br.com.gigaverse3d.inventory.service.StockService;
import br.com.gigaverse3d.order.api.OrderDtos.CheckoutRequest;
import br.com.gigaverse3d.order.api.OrderDtos.OrderDetailView;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;
import br.com.gigaverse3d.order.domain.ShippingAddress;
import br.com.gigaverse3d.order.repository.OrderRepository;
import br.com.gigaverse3d.payment.domain.Payment;
import br.com.gigaverse3d.payment.repository.PaymentRepository;
import br.com.gigaverse3d.payment.service.PaymentProvider;
import br.com.gigaverse3d.payment.service.PaymentProviders;
import br.com.gigaverse3d.shipping.ShippingService;
import br.com.gigaverse3d.shipping.ShippingService.ShippingQuote;

/**
 * Checkout e ciclo de vida do pedido. Tudo que altera pedido, estoque e pagamento ocorre na mesma
 * transação: ou tudo é gravado, ou nada.
 *
 * Quem pode mudar cada estado:
 * - cliente: cancelar o próprio pedido enquanto aguarda pagamento;
 * - sistema: cancelar pedidos não pagos após o prazo;
 * - ADMIN ou webhook verificado: confirmar pagamento (→ PAID);
 * - ADMIN/OPERATOR: produção, pronto para retirada, envio e entrega;
 * - só ADMIN: cancelar pedido já pago (exige motivo; estorno é manual).
 */
@Service
public class OrderService {

	private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

	private final OrderRepository orders;

	private final UserRepository users;

	private final AddressRepository addresses;

	private final CartService carts;

	private final ShippingService shipping;

	private final StockService stock;

	private final StockMovementRepository movements;

	private final PaymentRepository payments;

	private final PaymentProviders providers;

	private final OrderViews views;

	private final OrderNotifier notifier;

	private final AuditService audit;

	private final Clock clock;

	private final Duration pendingExpiration;

	public OrderService(OrderRepository orders, UserRepository users, AddressRepository addresses, CartService carts,
			ShippingService shipping, StockService stock, StockMovementRepository movements, PaymentRepository payments,
			PaymentProviders providers, OrderViews views, OrderNotifier notifier, AuditService audit, Clock clock,
			@Value("${app.orders.pending-expiration:PT72H}") Duration pendingExpiration) {
		this.orders = orders;
		this.users = users;
		this.addresses = addresses;
		this.carts = carts;
		this.shipping = shipping;
		this.stock = stock;
		this.movements = movements;
		this.payments = payments;
		this.providers = providers;
		this.views = views;
		this.notifier = notifier;
		this.audit = audit;
		this.clock = clock;
		this.pendingExpiration = pendingExpiration;
	}

	// ------------------------------------------------------------------ checkout

	@Transactional
	public OrderDetailView checkout(Long userId, CheckoutRequest req, String idempotencyKey) {
		if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
			throw new FieldValidationException("Idempotency-Key",
					"Envie o header Idempotency-Key (8 a 64 caracteres: letras, números e hífen).");
		}
		// Repetição da mesma requisição (duplo clique, retry de rede): devolve o pedido já criado.
		var existing = orders.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
		if (existing.isPresent()) {
			return views.detail(existing.get());
		}

		User user = users.findById(userId).orElseThrow();
		PricedCart cart = carts.priced(userId);
		if (cart.lines().isEmpty()) {
			throw new ConflictException("Seu carrinho está vazio.");
		}
		if (cart.hasProblems()) {
			throw new ConflictException("Revise o carrinho: " + cart.lines().stream().filter(l -> !l.ok())
				.map(l -> l.product().getName() + " — " + l.problem()).findFirst().orElse(""));
		}

		ShippingAddress snapshot = null;
		BigDecimal shippingPrice = BigDecimal.ZERO;
		if (req.shippingMethod() == Order.ShippingMethod.DELIVERY) {
			if (req.addressId() == null) {
				throw new FieldValidationException("addressId", "Escolha o endereço de entrega.");
			}
			Address address = addresses.findByIdAndUserIdAndDeletedAtIsNull(req.addressId(), userId)
				.orElseThrow(() -> new FieldValidationException("addressId", "Endereço não encontrado."));
			ShippingQuote quote = shipping.delivery(address.getPostalCode())
				.orElseThrow(() -> new FieldValidationException("shippingMethod",
						"Ainda não entregamos neste CEP. Escolha a retirada ou fale conosco."));
			shippingPrice = quote.price();
			snapshot = new ShippingAddress(address.getRecipientName(), address.getPostalCode(), address.getStreet(),
					address.getNumber(), address.getComplement(), address.getDistrict(), address.getCity(),
					address.getState());
		}
		else if (!shipping.pickupEnabled()) {
			throw new FieldValidationException("shippingMethod", "Retirada indisponível.");
		}

		Order order = new Order(userId, user.getFullName(), user.getEmail(), req.paymentMethod(), req.shippingMethod(),
				snapshot, idempotencyKey);
		for (PricedLine line : cart.lines()) {
			order.addItem(line.product().getId(), line.product().getName(), line.product().getSku(),
					line.optionsSummary(), line.unitPrice(), line.quantity());
		}
		order.setShippingTotal(shippingPrice);
		order.setCustomerNote(req.customerNote() == null || req.customerNote().isBlank() ? null : req.customerNote().strip());

		if (req.expectedTotal() != null && req.expectedTotal().compareTo(order.getTotal()) != 0) {
			throw new ConflictException("Os preços, a disponibilidade ou o frete mudaram. Revise o pedido: o total agora é R$ "
					+ order.getTotal().toPlainString().replace('.', ',') + ".");
		}

		orders.saveAndFlush(order);

		// Baixa de estoque em ordem de id (evita deadlock entre checkouts simultâneos).
		cart.lines().stream()
			.filter(l -> l.product().getType() == Product.Type.READY)
			.sorted(Comparator.comparing(l -> l.product().getId()))
			.forEach(l -> stock.reserveForOrder(l.product().getId(), l.quantity(), order.getId()));

		PaymentProvider provider = providers.active();
		PaymentProvider.PaymentStart start = provider.start(order);
		Payment payment = new Payment(order.getId(), provider.code(), order.getPaymentMethod(), order.getTotal());
		payment.setProviderReference(start.providerReference());
		payments.save(payment);

		cart.cart().clear();
		audit.recordAs(userId, "ORDER_PLACED", "order", order.getNumber(),
				Map.of("total", order.getTotal(), "items", order.getItems().size(), "provider", provider.code()));
		notifier.orderPlaced(order, start.customerInstructions());
		return views.detail(order);
	}

	// ------------------------------------------------------------------ cliente

	@Transactional
	public OrderDetailView cancelByCustomer(Long userId, Long number) {
		Order order = orders.findByNumberAndUserId(number, userId)
			.orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
		if (!order.getStatus().customerMayCancel()) {
			throw new ConflictException("Este pedido não pode mais ser cancelado pelo site. Fale conosco.");
		}
		cancel(order, userId, "Cancelado pelo cliente.");
		return views.detail(order);
	}

	// ------------------------------------------------------------------ equipe

	/** Confirmação de pagamento por ADMIN (baixa manual) — nunca pelo navegador do cliente. */
	@Transactional
	public OrderDetailView confirmPayment(Long number, Long actorId, String reference, String note) {
		Order order = byNumber(number);
		confirm(order, actorId, reference, note == null || note.isBlank() ? "Pagamento conferido pela equipe." : note);
		return views.detail(order);
	}

	/** Confirmação vinda de webhook verificado: valida valor e estado antes de aceitar. */
	@Transactional
	public void confirmFromProvider(Payment payment, BigDecimal amount, String reference) {
		Order order = orders.findById(payment.getOrderId()).orElseThrow();
		if (amount == null || amount.compareTo(payment.getAmount()) != 0) {
			audit.recordAs(null, "PAYMENT_AMOUNT_MISMATCH", "order", order.getNumber(),
					Map.of("expected", payment.getAmount(), "received", String.valueOf(amount)));
			throw new ConflictException("Valor pago diferente do valor do pedido.");
		}
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			// pago após cancelamento/expiração: exige ação humana (estorno ou reativação)
			audit.recordAs(null, "PAYMENT_FOR_NON_PENDING_ORDER", "order", order.getNumber(),
					Map.of("status", order.getStatus().name()));
			return;
		}
		confirm(order, null, reference, "Pagamento confirmado pelo provedor.");
	}

	private void confirm(Order order, Long actorId, String reference, String note) {
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new ConflictException("Só pedidos aguardando pagamento podem ser confirmados.");
		}
		Instant now = clock.instant();
		Payment payment = payments.findByOrderIdOrderByIdDesc(order.getId()).stream().findFirst()
			.orElseThrow(() -> new ConflictException("Pedido sem registro de pagamento."));
		payment.markPaid(now, reference);
		order.transitionTo(OrderStatus.PAID, actorId, note);
		audit.recordAs(actorId, "PAYMENT_CONFIRMED", "order", order.getNumber(),
				Map.of("provider", payment.getProvider()));
		notifier.statusChanged(order, null);
	}

	/** Mudanças operacionais pela equipe. PAID só via confirmação de pagamento. */
	@Transactional
	public OrderDetailView changeStatus(Long number, OrderStatus target, String reason, String trackingCode,
			Long actorId, boolean actorIsAdmin) {
		Order order = byNumber(number);
		if (target == OrderStatus.PAID) {
			throw new FieldValidationException("status", "Use a confirmação de pagamento para marcar como pago.");
		}
		if (target == OrderStatus.SHIPPED && order.getShippingMethod() != Order.ShippingMethod.DELIVERY) {
			throw new FieldValidationException("status", "Pedido para retirada não é enviado.");
		}
		if (target == OrderStatus.READY_FOR_PICKUP && order.getShippingMethod() != Order.ShippingMethod.PICKUP) {
			throw new FieldValidationException("status", "Pedido com entrega não fica pronto para retirada.");
		}
		String note = reason == null || reason.isBlank() ? null : reason.strip();
		if (target == OrderStatus.SHIPPED && trackingCode != null && !trackingCode.isBlank()) {
			note = (note == null ? "" : note + " · ") + "Código de rastreio: " + trackingCode.strip();
		}
		if (target == OrderStatus.CANCELLED) {
			if (note == null) {
				throw new FieldValidationException("reason", "Informe o motivo do cancelamento.");
			}
			if (order.getStatus().isPaid() && !actorIsAdmin) {
				throw new ConflictException("Somente um administrador pode cancelar pedido já pago.");
			}
			if (order.getStatus().isPaid()) {
				note = note + " · Estorno ao cliente deve ser feito manualmente.";
			}
			cancel(order, actorId, note);
			return views.detail(order);
		}
		order.transitionTo(target, actorId, note);
		audit.recordAs(actorId, "ORDER_STATUS_CHANGED", "order", number, Map.of("to", target.name()));
		notifier.statusChanged(order, note);
		return views.detail(order);
	}

	/** Cancela pedidos não pagos após o prazo e devolve o estoque. Retorna quantos expiraram. */
	@Transactional
	public int expireUnpaid() {
		List<Order> stale = orders.findTop100ByStatusAndCreatedAtBeforeOrderByIdAsc(OrderStatus.PENDING_PAYMENT,
				clock.instant().minus(pendingExpiration));
		stale.forEach(o -> cancel(o, null, "Cancelado automaticamente: pagamento não identificado no prazo."));
		return stale.size();
	}

	private void cancel(Order order, Long actorId, String reason) {
		order.transitionTo(OrderStatus.CANCELLED, actorId, reason);
		// devolve exatamente o que este pedido baixou (líquido das movimentações do pedido)
		movements.netByOrder(order.getId()).stream()
			.filter(n -> n.getNet() < 0)
			.sorted(Comparator.comparing(StockMovementRepository.ProductNet::getProductId))
			.forEach(n -> stock.returnForOrder(n.getProductId(), (int) -n.getNet(), order.getId(), "Pedido cancelado"));
		payments.findByOrderIdOrderByIdDesc(order.getId()).forEach(Payment::cancel);
		audit.recordAs(actorId, "ORDER_CANCELLED", "order", order.getNumber(), Map.of("reason", reason));
		notifier.statusChanged(order, reason);
	}

	private Order byNumber(Long number) {
		return orders.findByNumber(number).orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
	}

}
