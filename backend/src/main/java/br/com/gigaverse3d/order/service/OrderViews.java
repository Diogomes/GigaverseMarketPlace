package br.com.gigaverse3d.order.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.catalog.repository.ProductRepository;
import br.com.gigaverse3d.order.api.OrderDtos.AdminOrderListItem;
import br.com.gigaverse3d.order.api.OrderDtos.OrderDetailView;
import br.com.gigaverse3d.order.api.OrderDtos.OrderItemView;
import br.com.gigaverse3d.order.api.OrderDtos.OrderSummaryView;
import br.com.gigaverse3d.order.api.OrderDtos.ShippingAddressView;
import br.com.gigaverse3d.order.api.OrderDtos.StatusChangeView;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderItem;
import br.com.gigaverse3d.order.domain.OrderStatus;
import br.com.gigaverse3d.order.domain.ShippingAddress;
import br.com.gigaverse3d.payment.domain.Payment;
import br.com.gigaverse3d.payment.repository.PaymentRepository;
import br.com.gigaverse3d.payment.service.PaymentProviders;

/** Monta as visões de pedido (deve rodar dentro de transação). */
@Component
public class OrderViews {

	private final ProductRepository products;

	private final PaymentRepository payments;

	private final PaymentProviders providers;

	public OrderViews(ProductRepository products, PaymentRepository payments, PaymentProviders providers) {
		this.products = products;
		this.payments = payments;
		this.providers = providers;
	}

	public OrderDetailView detail(Order order) {
		Map<Long, Product> byId = products
			.findAllById(order.getItems().stream().map(OrderItem::getProductId).filter(Objects::nonNull).toList())
			.stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));
		List<OrderItemView> items = order.getItems().stream().map(i -> {
			Product p = i.getProductId() == null ? null : byId.get(i.getProductId());
			return new OrderItemView(p == null ? null : p.getSlug(), i.getProductName(), i.getSku(), i.getOptionsSummary(),
					i.getUnitPrice(), i.getQuantity(), i.getLineTotal());
		}).toList();
		Payment payment = payments.findByOrderIdOrderByIdDesc(order.getId()).stream().findFirst().orElse(null);
		String instructions = order.getStatus() == OrderStatus.PENDING_PAYMENT && payment != null
				? providers.byCode(payment.getProvider()).map(p -> p.instructions(order)).orElse(null) : null;
		ShippingAddress a = order.getShippingAddress();
		ShippingAddressView address = a == null || a.postalCode() == null ? null
				: new ShippingAddressView(a.recipientName(), a.postalCode(), a.street(), a.number(), a.complement(),
						a.district(), a.city(), a.state());
		Integer productionDays = byId.values().stream().map(Product::getProductionDays).filter(Objects::nonNull)
			.max(Integer::compare).orElse(null);
		return new OrderDetailView(order.getNumber(), order.getCreatedAt(), order.getStatus(), items,
				order.getSubtotal(), order.getShippingTotal(), order.getDiscountTotal(), order.getTotal(),
				order.getCurrency(), order.getPaymentMethod(), payment == null ? null : payment.getStatus(),
				instructions, order.getShippingMethod(), address, order.getCustomerNote(),
				order.getHistory().stream().map(h -> new StatusChangeView(h.getToStatus(), h.getCreatedAt(), h.getReason()))
					.toList(),
				order.getStatus().customerMayCancel(), productionDays);
	}

	public OrderSummaryView summary(Order order) {
		OrderItem first = order.getItems().getFirst();
		String slug = first.getProductId() == null ? null
				: products.findById(first.getProductId()).map(Product::getSlug).orElse(null);
		return new OrderSummaryView(order.getNumber(), order.getCreatedAt(), order.getStatus(), order.getTotal(),
				order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(), first.getProductName(), slug);
	}

	public AdminOrderListItem adminListItem(Order order) {
		return new AdminOrderListItem(order.getNumber(), order.getCreatedAt(), order.getStatus(), order.getTotal(),
				order.getCustomerName(), order.getCustomerEmail(), order.getPaymentMethod(), order.getShippingMethod(),
				order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
	}

}
