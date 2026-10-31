package br.com.gigaverse3d.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.gigaverse3d.order.domain.OrderStatus;

/** Respostas do dashboard. As definições de cada métrica estão em docs/METRICS.md. */
public final class DashboardDtos {

	private DashboardDtos() {
	}

	public record Kpis(
			@Schema(description = "Σ total dos pedidos com pagamento confirmado no período (inclui frete e pedidos cancelados depois)")
			BigDecimal grossSales,
			@Schema(description = "Σ (total − frete) dos pedidos pagos no período que não foram cancelados")
			BigDecimal netSales,
			@Schema(description = "Σ total dos pedidos pagos no período e cancelados depois (estorno manual)")
			BigDecimal cancelledAfterPayment,
			@Schema(description = "Σ frete dos pedidos pagos no período que não foram cancelados") BigDecimal shipping,
			@Schema(description = "Pedidos pagos no período que não foram cancelados") long paidOrders,
			@Schema(description = "Σ total dos pedidos pagos válidos ÷ pedidos pagos válidos") BigDecimal averageTicket,
			@Schema(description = "Pedidos criados no período, em qualquer status") long ordersPlaced) {
	}

	public record DailySales(LocalDate day, BigDecimal grossSales, BigDecimal netSales) {
	}

	public record StatusCount(OrderStatus status, long count) {
	}

	public record TopProduct(Long productId, String name, String sku, long quantity, BigDecimal revenue) {
	}

	public record Customers(
			@Schema(description = "Clientes distintos com ao menos um pedido pago válido no período") long buyers,
			@Schema(description = "Compradores cujo primeiro pedido pago válido foi no período") long newBuyers,
			@Schema(description = "Compradores que já tinham pedido pago antes do período") long returningBuyers,
			@Schema(description = "Contas de cliente criadas no período") long signups) {
	}

	public record LowStockItem(Long productId, String name, String sku, int onHand, int reserved, int available,
			int threshold) {
	}

	public record PendingOrder(Long number, Instant placedAt, OrderStatus status, String customerName,
			BigDecimal total) {
	}

	public record DashboardResponse(LocalDate from, LocalDate to, String timezone, Kpis kpis, LocalDate previousFrom,
			LocalDate previousTo, Kpis previous, List<DailySales> sales, List<StatusCount> ordersByStatus,
			List<TopProduct> topProducts, Customers customers, List<LowStockItem> lowStock, long lowStockTotal,
			@Schema(description = "Situação atual (não depende do período): pedidos que exigem ação da equipe")
			List<StatusCount> awaitingAction, List<PendingOrder> oldestAwaiting) {
	}

}
