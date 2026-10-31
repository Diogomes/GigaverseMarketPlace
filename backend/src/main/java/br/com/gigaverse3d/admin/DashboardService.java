package br.com.gigaverse3d.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.admin.DashboardDtos.Customers;
import br.com.gigaverse3d.admin.DashboardDtos.DailySales;
import br.com.gigaverse3d.admin.DashboardDtos.DashboardResponse;
import br.com.gigaverse3d.admin.DashboardDtos.Kpis;
import br.com.gigaverse3d.admin.DashboardDtos.LowStockItem;
import br.com.gigaverse3d.admin.DashboardDtos.PendingOrder;
import br.com.gigaverse3d.admin.DashboardDtos.StatusCount;
import br.com.gigaverse3d.admin.DashboardDtos.TopProduct;
import br.com.gigaverse3d.common.domain.BusinessTime;
import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.order.domain.OrderStatus;

/**
 * Métricas do dashboard calculadas no banco (agregações SQL, nunca no navegador).
 *
 * Regras (D-P12, proposta registrada em docs/METRICS.md):
 * - venda conta na data do pagamento confirmado ({@code paid_at}), no fuso de Recife;
 * - pedido cancelado depois de pago entra nas vendas brutas e sai das líquidas (estorno manual);
 * - vendas líquidas excluem frete;
 * - "pedidos criados" e "pedidos por status" usam a data de criação ({@code placed_at}).
 */
@Service
public class DashboardService {

	public static final int MAX_RANGE_DAYS = 366;

	/** Status que pedem ação da equipe: conferir pagamento, produzir, entregar/enviar. */
	static final List<OrderStatus> AWAITING_ACTION = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID,
			OrderStatus.IN_PRODUCTION, OrderStatus.READY_FOR_PICKUP);

	private static final String LOW_STOCK_WHERE = """
			FROM inventory_items i JOIN products p ON p.id = i.product_id
			WHERE p.status = 'ACTIVE' AND p.product_type = 'READY'
			  AND i.quantity_on_hand - i.quantity_reserved <= i.low_stock_threshold
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public DashboardService(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public DashboardResponse dashboard(LocalDate from, LocalDate to) {
		if (to.isBefore(from)) {
			throw new FieldValidationException("to", "A data final deve ser igual ou posterior à inicial.");
		}
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > MAX_RANGE_DAYS) {
			throw new FieldValidationException("from", "Período máximo de " + MAX_RANGE_DAYS + " dias.");
		}
		LocalDate previousTo = from.minusDays(1);
		LocalDate previousFrom = previousTo.minusDays(days - 1);
		MapSqlParameterSource range = range(from, to);
		return new DashboardResponse(from, to, BusinessTime.ZONE.getId(), kpis(range), previousFrom, previousTo,
				kpis(range(previousFrom, previousTo)), dailySales(range, from, to), ordersByStatus(range),
				topProducts(range), customers(range), lowStock(), lowStockTotal(), awaitingAction(), oldestAwaiting());
	}

	private static MapSqlParameterSource range(LocalDate from, LocalDate to) {
		return new MapSqlParameterSource().addValue("start", Timestamp.from(BusinessTime.startOf(from)))
			.addValue("end", Timestamp.from(BusinessTime.endOf(to)))
			.addValue("tz", BusinessTime.ZONE.getId());
	}

	private Kpis kpis(MapSqlParameterSource range) {
		Kpis sales = jdbc.queryForObject("""
				SELECT COALESCE(SUM(total), 0) AS gross,
				       COALESCE(SUM(total) FILTER (WHERE status = 'CANCELLED'), 0) AS cancelled,
				       COALESCE(SUM(total) FILTER (WHERE status <> 'CANCELLED'), 0) AS valid_total,
				       COALESCE(SUM(shipping_total) FILTER (WHERE status <> 'CANCELLED'), 0) AS shipping,
				       COUNT(*) FILTER (WHERE status <> 'CANCELLED') AS paid_orders
				FROM orders WHERE paid_at >= :start AND paid_at < :end
				""", range, (rs, i) -> {
			BigDecimal validTotal = rs.getBigDecimal("valid_total");
			BigDecimal shipping = rs.getBigDecimal("shipping");
			long paid = rs.getLong("paid_orders");
			BigDecimal avg = paid == 0 ? Money.normalize(BigDecimal.ZERO)
					: validTotal.divide(BigDecimal.valueOf(paid), Money.SCALE, RoundingMode.HALF_EVEN);
			return new Kpis(Money.normalize(rs.getBigDecimal("gross")), Money.normalize(validTotal.subtract(shipping)),
					Money.normalize(rs.getBigDecimal("cancelled")), Money.normalize(shipping), paid, avg, 0);
		});
		Long placed = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE placed_at >= :start AND placed_at < :end",
				range, Long.class);
		return new Kpis(sales.grossSales(), sales.netSales(), sales.cancelledAfterPayment(), sales.shipping(),
				sales.paidOrders(), sales.averageTicket(), placed == null ? 0 : placed);
	}

	/** Uma linha por dia do período (dias sem venda aparecem com zero). */
	private List<DailySales> dailySales(MapSqlParameterSource range, LocalDate from, LocalDate to) {
		Map<LocalDate, DailySales> byDay = new HashMap<>();
		jdbc.query("""
				SELECT CAST(paid_at AT TIME ZONE :tz AS date) AS day,
				       SUM(total) AS gross,
				       COALESCE(SUM(total - shipping_total) FILTER (WHERE status <> 'CANCELLED'), 0) AS net
				FROM orders WHERE paid_at >= :start AND paid_at < :end
				GROUP BY 1
				""", range, rs -> {
			LocalDate day = rs.getObject("day", LocalDate.class);
			byDay.put(day, new DailySales(day, Money.normalize(rs.getBigDecimal("gross")),
					Money.normalize(rs.getBigDecimal("net"))));
		});
		BigDecimal zero = Money.normalize(BigDecimal.ZERO);
		return from.datesUntil(to.plusDays(1)).map(d -> byDay.getOrDefault(d, new DailySales(d, zero, zero))).toList();
	}

	private List<StatusCount> ordersByStatus(MapSqlParameterSource range) {
		Map<String, Long> counts = new HashMap<>();
		jdbc.query("""
				SELECT status, COUNT(*) AS n FROM orders
				WHERE placed_at >= :start AND placed_at < :end GROUP BY status
				""", range, rs -> {
			counts.put(rs.getString("status"), rs.getLong("n"));
		});
		return Arrays.stream(OrderStatus.values())
			.map(s -> new StatusCount(s, counts.getOrDefault(s.name(), 0L)))
			.toList();
	}

	/** Mais vendidos por quantidade (pedidos pagos válidos). Nome atual do produto, ou o do pedido se excluído. */
	private List<TopProduct> topProducts(MapSqlParameterSource range) {
		return jdbc.query("""
				SELECT oi.product_id, COALESCE(MAX(p.name), MAX(oi.product_name)) AS name,
				       COALESCE(MAX(p.sku), MAX(oi.sku)) AS sku,
				       SUM(oi.quantity) AS quantity, SUM(oi.line_total) AS revenue
				FROM order_items oi
				JOIN orders o ON o.id = oi.order_id
				LEFT JOIN products p ON p.id = oi.product_id
				WHERE o.paid_at >= :start AND o.paid_at < :end AND o.status <> 'CANCELLED'
				GROUP BY oi.product_id, CASE WHEN oi.product_id IS NULL THEN oi.sku END
				ORDER BY quantity DESC, revenue DESC, name
				LIMIT 5
				""", range, (rs, i) -> new TopProduct(rs.getObject("product_id", Long.class), rs.getString("name"),
				rs.getString("sku"), rs.getLong("quantity"), Money.normalize(rs.getBigDecimal("revenue"))));
	}

	private Customers customers(MapSqlParameterSource range) {
		Customers buyers = jdbc.queryForObject("""
				WITH buyers AS (
				    SELECT DISTINCT user_id FROM orders
				    WHERE paid_at >= :start AND paid_at < :end AND status <> 'CANCELLED'
				), firsts AS (
				    SELECT o.user_id, MIN(o.paid_at) AS first_paid
				    FROM orders o JOIN buyers b ON b.user_id = o.user_id
				    WHERE o.paid_at IS NOT NULL AND o.status <> 'CANCELLED'
				    GROUP BY o.user_id
				)
				SELECT COUNT(*) AS buyers, COUNT(*) FILTER (WHERE first_paid >= :start) AS new_buyers FROM firsts
				""", range, (rs, i) -> new Customers(rs.getLong("buyers"), rs.getLong("new_buyers"),
				rs.getLong("buyers") - rs.getLong("new_buyers"), 0));
		Long signups = jdbc.queryForObject("""
				SELECT COUNT(*) FROM users
				WHERE role = 'CUSTOMER' AND deleted_at IS NULL AND created_at >= :start AND created_at < :end
				""", range, Long.class);
		return new Customers(buyers.buyers(), buyers.newBuyers(), buyers.returningBuyers(),
				signups == null ? 0 : signups);
	}

	/** Produtos ativos de pronta entrega com disponível (em estoque − reservado) ≤ mínimo. */
	private List<LowStockItem> lowStock() {
		return jdbc.query("SELECT p.id, p.name, p.sku, i.quantity_on_hand, i.quantity_reserved, i.low_stock_threshold "
				+ LOW_STOCK_WHERE + " ORDER BY i.quantity_on_hand - i.quantity_reserved, p.name LIMIT 10", (rs, i) -> {
					int onHand = rs.getInt("quantity_on_hand");
					int reserved = rs.getInt("quantity_reserved");
					return new LowStockItem(rs.getLong("id"), rs.getString("name"), rs.getString("sku"), onHand,
							reserved, onHand - reserved, rs.getInt("low_stock_threshold"));
				});
	}

	private long lowStockTotal() {
		Long n = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) " + LOW_STOCK_WHERE, Long.class);
		return n == null ? 0 : n;
	}

	private List<StatusCount> awaitingAction() {
		Map<String, Long> counts = new HashMap<>();
		jdbc.query("SELECT status, COUNT(*) AS n FROM orders WHERE status IN (:statuses) GROUP BY status",
				awaitingStatuses(), rs -> {
					counts.put(rs.getString("status"), rs.getLong("n"));
				});
		return AWAITING_ACTION.stream().map(s -> new StatusCount(s, counts.getOrDefault(s.name(), 0L))).toList();
	}

	private List<PendingOrder> oldestAwaiting() {
		return jdbc.query("""
				SELECT number, placed_at, status, customer_name, total FROM orders
				WHERE status IN (:statuses) ORDER BY placed_at, id LIMIT 5
				""", awaitingStatuses(),
				(rs, i) -> new PendingOrder(rs.getLong("number"), rs.getTimestamp("placed_at").toInstant(),
						OrderStatus.valueOf(rs.getString("status")), rs.getString("customer_name"),
						Money.normalize(rs.getBigDecimal("total"))));
	}

	private static MapSqlParameterSource awaitingStatuses() {
		return new MapSqlParameterSource("statuses", AWAITING_ACTION.stream().map(Enum::name).toList());
	}

}
