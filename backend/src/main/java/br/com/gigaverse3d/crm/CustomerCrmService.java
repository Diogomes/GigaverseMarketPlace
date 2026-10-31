package br.com.gigaverse3d.crm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.common.web.CsvWriter;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.crm.CrmDtos.CustomerDetail;
import br.com.gigaverse3d.crm.CrmDtos.CustomerListItem;
import br.com.gigaverse3d.crm.CrmDtos.CustomerSort;
import br.com.gigaverse3d.crm.CrmDtos.NoteView;
import br.com.gigaverse3d.crm.CrmDtos.Segment;
import br.com.gigaverse3d.crm.CrmDtos.TagView;
import br.com.gigaverse3d.crm.CrmDtos.TagWithCount;

/**
 * CRM: clientes (só papel CUSTOMER), notas internas, etiquetas e exportação.
 * Notas e etiquetas nunca aparecem nas APIs do cliente. "Compra" = pedido pago e não cancelado.
 */
@Service
public class CustomerCrmService {

	public static final int MAX_EXPORT_ROWS = 5000;

	static final Duration LAPSED_AFTER = Duration.ofDays(90);

	private static final String VALID_PAID = "o.paid_at IS NOT NULL AND o.status <> 'CANCELLED'";

	private static final String CUSTOMER_FROM = """
			FROM users u
			LEFT JOIN LATERAL (
			    SELECT COUNT(*) FILTER (WHERE %1$s) AS paid_orders,
			           SUM(o.total) FILTER (WHERE %1$s) AS total_spent,
			           MAX(o.paid_at) FILTER (WHERE %1$s) AS last_purchase_at
			    FROM orders o WHERE o.user_id = u.id
			) s ON true
			WHERE u.role = 'CUSTOMER' AND u.deleted_at IS NULL
			  AND (CAST(:like AS text) IS NULL OR lower(u.full_name) LIKE CAST(:like AS text) ESCAPE '\\'
			       OR u.email LIKE CAST(:like AS text) ESCAPE '\\' OR u.id = CAST(:qid AS bigint))
			  AND (CAST(:status AS text) IS NULL OR u.status = CAST(:status AS text))
			  AND (CAST(:tagId AS bigint) IS NULL OR EXISTS (
			       SELECT 1 FROM customer_tag_assignments a WHERE a.user_id = u.id AND a.tag_id = CAST(:tagId AS bigint)))
			  AND (CAST(:segment AS text) IS NULL
			       OR (CAST(:segment AS text) = 'NO_PURCHASE' AND s.paid_orders = 0)
			       OR (CAST(:segment AS text) = 'REPEAT' AND s.paid_orders >= 2)
			       OR (CAST(:segment AS text) = 'LAPSED' AND s.last_purchase_at < CAST(:lapsedBefore AS timestamptz)))
			""".formatted(VALID_PAID);

	private static final String CUSTOMER_COLUMNS = """
			SELECT u.id, u.full_name, u.email, u.status, u.created_at,
			       s.paid_orders, COALESCE(s.total_spent, 0) AS total_spent, s.last_purchase_at
			""";

	private static final Map<CustomerSort, String> ORDER_BY = Map.of(
			CustomerSort.LAST_PURCHASE, "s.last_purchase_at DESC NULLS LAST, u.id DESC",
			CustomerSort.TOTAL_SPENT, "COALESCE(s.total_spent, 0) DESC, u.id DESC",
			CustomerSort.CREATED_AT, "u.created_at DESC, u.id DESC",
			CustomerSort.NAME, "lower(u.full_name), u.id");

	/** Filtros da lista/exportação de clientes. */
	public record CustomerFilter(String q, User.Status status, Long tagId, Segment segment) {
	}

	private final NamedParameterJdbcTemplate jdbc;

	private final AuditService audit;

	private final Clock clock;

	public CustomerCrmService(NamedParameterJdbcTemplate jdbc, AuditService audit, Clock clock) {
		this.jdbc = jdbc;
		this.audit = audit;
		this.clock = clock;
	}

	// ------------------------------------------------------------------ clientes

	@Transactional(readOnly = true)
	public PageResponse<CustomerListItem> list(CustomerFilter filter, CustomerSort sort, int page, int size) {
		MapSqlParameterSource params = params(filter);
		Long total = jdbc.queryForObject("SELECT COUNT(*) " + CUSTOMER_FROM, params, Long.class);
		params.addValue("limit", size).addValue("offset", (long) page * size);
		List<CustomerListItem> rows = jdbc.query(CUSTOMER_COLUMNS + CUSTOMER_FROM + " ORDER BY "
				+ ORDER_BY.get(sort == null ? CustomerSort.LAST_PURCHASE : sort) + " LIMIT :limit OFFSET :offset",
				params, (rs, i) -> listItem(rs, List.of()));
		Map<Long, List<TagView>> tags = tagsOf(rows.stream().map(CustomerListItem::id).toList());
		List<CustomerListItem> content = rows.stream()
			.map(r -> new CustomerListItem(r.id(), r.name(), r.email(), r.status(), r.createdAt(), r.paidOrders(),
					r.totalSpent(), r.lastPurchaseAt(), tags.getOrDefault(r.id(), List.of())))
			.toList();
		long totalElements = total == null ? 0 : total;
		return new PageResponse<>(content, page, size, totalElements, (int) ((totalElements + size - 1) / size));
	}

	@Transactional(readOnly = true)
	public CustomerDetail detail(Long id) {
		requireCustomer(id);
		MapSqlParameterSource p = new MapSqlParameterSource("id", id);
		CustomerDetail base = jdbc.queryForObject("""
				SELECT u.id, u.full_name, u.email, u.status, u.created_at, u.last_login_at, cp.phone,
				       COALESCE((SELECT c.granted FROM user_consents c
				                 WHERE c.user_id = u.id AND c.consent_type = 'MARKETING_EMAIL'
				                 ORDER BY c.id DESC LIMIT 1), false) AS marketing,
				       (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id) AS orders,
				       s.paid_orders, COALESCE(s.total_spent, 0) AS total_spent, s.first_purchase_at, s.last_purchase_at
				FROM users u
				LEFT JOIN customer_profiles cp ON cp.user_id = u.id
				LEFT JOIN LATERAL (
				    SELECT COUNT(*) AS paid_orders, SUM(o.total) AS total_spent,
				           MIN(o.paid_at) AS first_purchase_at, MAX(o.paid_at) AS last_purchase_at
				    FROM orders o WHERE o.user_id = u.id AND %s
				) s ON true
				WHERE u.id = :id
				""".formatted(VALID_PAID), p, (rs, i) -> {
			long paid = rs.getLong("paid_orders");
			BigDecimal spent = Money.normalize(rs.getBigDecimal("total_spent"));
			BigDecimal avg = paid == 0 ? Money.normalize(BigDecimal.ZERO)
					: spent.divide(BigDecimal.valueOf(paid), Money.SCALE, RoundingMode.HALF_EVEN);
			return new CustomerDetail(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"),
					rs.getString("phone"), User.Status.valueOf(rs.getString("status")), instant(rs, "created_at"),
					instant(rs, "last_login_at"), rs.getBoolean("marketing"), rs.getLong("orders"), paid, spent, avg,
					instant(rs, "first_purchase_at"), instant(rs, "last_purchase_at"), List.of());
		});
		return new CustomerDetail(base.id(), base.name(), base.email(), base.phone(), base.status(), base.createdAt(),
				base.lastLoginAt(), base.marketingOptIn(), base.orders(), base.paidOrders(), base.totalSpent(),
				base.averageTicket(), base.firstPurchaseAt(), base.lastPurchaseAt(),
				tagsOf(List.of(id)).getOrDefault(id, List.of()));
	}

	/** CSV com dados mínimos; recusa (em vez de cortar em silêncio) acima do limite. Auditado. */
	@Transactional
	public CsvWriter export(CustomerFilter filter) {
		MapSqlParameterSource params = params(filter).addValue("limit", MAX_EXPORT_ROWS + 1);
		List<CustomerListItem> rows = jdbc.query(CUSTOMER_COLUMNS + CUSTOMER_FROM + " ORDER BY u.id LIMIT :limit",
				params, (rs, i) -> listItem(rs, List.of()));
		if (rows.size() > MAX_EXPORT_ROWS) {
			throw new ConflictException(
					"A exportação está limitada a " + MAX_EXPORT_ROWS + " clientes. Refine os filtros e tente novamente.");
		}
		Map<Long, List<TagView>> tags = tagsOf(rows.stream().map(CustomerListItem::id).toList());
		CsvWriter csv = new CsvWriter().row(List.of("id", "nome", "email", "status", "cadastro", "pedidos_pagos",
				"total_gasto", "ultima_compra", "etiquetas"));
		rows.forEach(r -> csv.row(Arrays.asList(r.id(), r.name(), r.email(), r.status(), r.createdAt(),
				r.paidOrders(), r.totalSpent(), r.lastPurchaseAt(),
				String.join(", ", tags.getOrDefault(r.id(), List.of()).stream().map(TagView::name).toList()))));
		audit.record("CUSTOMERS_EXPORTED", "customer", null, filterDetails(filter, rows.size()));
		return csv;
	}

	// ------------------------------------------------------------------ notas internas

	@Transactional(readOnly = true)
	public PageResponse<NoteView> notes(Long customerId, int page, int size) {
		requireCustomer(customerId);
		MapSqlParameterSource p = new MapSqlParameterSource("id", customerId).addValue("limit", size)
			.addValue("offset", (long) page * size);
		Long total = jdbc.queryForObject(
				"SELECT COUNT(*) FROM customer_notes WHERE customer_id = :id AND deleted_at IS NULL", p, Long.class);
		List<NoteView> content = jdbc.query("""
				SELECT n.id, n.body, n.author_user_id, a.full_name AS author_name, n.created_at
				FROM customer_notes n LEFT JOIN users a ON a.id = n.author_user_id
				WHERE n.customer_id = :id AND n.deleted_at IS NULL
				ORDER BY n.created_at DESC, n.id DESC LIMIT :limit OFFSET :offset
				""", p, (rs, i) -> new NoteView(rs.getLong("id"), rs.getString("body"),
				rs.getObject("author_user_id", Long.class), rs.getString("author_name"), instant(rs, "created_at")));
		long totalElements = total == null ? 0 : total;
		return new PageResponse<>(content, page, size, totalElements, (int) ((totalElements + size - 1) / size));
	}

	/** O conteúdo da nota não vai para a auditoria (pode conter dados pessoais): só o tamanho. */
	@Transactional
	public NoteView addNote(Long customerId, String body) {
		requireCustomer(customerId);
		Long authorId = CurrentUser.requireId();
		String text = body.strip();
		Long id = jdbc.queryForObject("""
				INSERT INTO customer_notes (customer_id, author_user_id, body) VALUES (:customer, :author, :body)
				RETURNING id
				""", new MapSqlParameterSource("customer", customerId).addValue("author", authorId).addValue("body", text),
				Long.class);
		audit.record("CUSTOMER_NOTE_ADDED", "customer", customerId, Map.of("noteId", id, "length", text.length()));
		return jdbc.queryForObject("""
				SELECT n.id, n.body, n.author_user_id, a.full_name AS author_name, n.created_at
				FROM customer_notes n LEFT JOIN users a ON a.id = n.author_user_id WHERE n.id = :id
				""", new MapSqlParameterSource("id", id), (rs, i) -> new NoteView(rs.getLong("id"), rs.getString("body"),
				rs.getObject("author_user_id", Long.class), rs.getString("author_name"), instant(rs, "created_at")));
	}

	/** Exclusão lógica (a nota some da tela, mas o registro fica para auditoria). */
	@Transactional
	public void deleteNote(Long customerId, Long noteId) {
		int updated = jdbc.update("""
				UPDATE customer_notes SET deleted_at = now(), updated_at = now()
				WHERE id = :note AND customer_id = :customer AND deleted_at IS NULL
				""", new MapSqlParameterSource("note", noteId).addValue("customer", customerId));
		if (updated == 0) {
			throw new NotFoundException("Nota não encontrada.");
		}
		audit.record("CUSTOMER_NOTE_DELETED", "customer", customerId, Map.of("noteId", noteId));
	}

	// ------------------------------------------------------------------ etiquetas

	@Transactional(readOnly = true)
	public List<TagWithCount> tags() {
		return jdbc.query("""
				SELECT t.id, t.name, COUNT(a.user_id) AS customers
				FROM customer_tags t LEFT JOIN customer_tag_assignments a ON a.tag_id = t.id
				GROUP BY t.id, t.name ORDER BY lower(t.name)
				""", (rs, i) -> new TagWithCount(rs.getLong("id"), rs.getString("name"), rs.getLong("customers")));
	}

	@Transactional
	public TagView createTag(String rawName) {
		String name = rawName.strip().replaceAll("\\s+", " ");
		MapSqlParameterSource p = new MapSqlParameterSource("name", name);
		Long existing = jdbc.query("SELECT id FROM customer_tags WHERE lower(name) = lower(:name)", p,
				rs -> rs.next() ? rs.getLong(1) : null);
		if (existing != null) {
			throw new ConflictException("Já existe a etiqueta '" + name + "'.");
		}
		Long id = jdbc.queryForObject("INSERT INTO customer_tags (name) VALUES (:name) RETURNING id", p, Long.class);
		audit.record("CUSTOMER_TAG_CREATED", "customer_tag", id, Map.of("name", name));
		return new TagView(id, name);
	}

	@Transactional
	public void deleteTag(Long tagId) {
		MapSqlParameterSource p = new MapSqlParameterSource("id", tagId);
		List<String> names = jdbc.queryForList("SELECT name FROM customer_tags WHERE id = :id", p, String.class);
		if (names.isEmpty()) {
			throw new NotFoundException("Etiqueta não encontrada.");
		}
		Long assigned = jdbc.queryForObject("SELECT COUNT(*) FROM customer_tag_assignments WHERE tag_id = :id", p,
				Long.class);
		jdbc.update("DELETE FROM customer_tags WHERE id = :id", p);
		audit.record("CUSTOMER_TAG_DELETED", "customer_tag", tagId,
				Map.of("name", names.getFirst(), "removedFromCustomers", assigned == null ? 0 : assigned));
	}

	/** Define o conjunto completo de etiquetas do cliente (idempotente). */
	@Transactional
	public List<TagView> assignTags(Long customerId, List<Long> tagIds) {
		requireCustomer(customerId);
		Set<Long> wanted = new LinkedHashSet<>(tagIds);
		Map<Long, String> known = new HashMap<>();
		if (!wanted.isEmpty()) {
			jdbc.query("SELECT id, name FROM customer_tags WHERE id IN (:ids)", new MapSqlParameterSource("ids", wanted),
					rs -> {
						known.put(rs.getLong("id"), rs.getString("name"));
					});
		}
		if (known.size() != wanted.size()) {
			throw new FieldValidationException("tagIds", "Etiqueta inexistente.");
		}
		List<TagView> current = tagsOf(List.of(customerId)).getOrDefault(customerId, List.of());
		List<String> removed = new ArrayList<>();
		List<String> added = new ArrayList<>();
		for (TagView t : current) {
			if (!wanted.contains(t.id())) {
				jdbc.update("DELETE FROM customer_tag_assignments WHERE user_id = :u AND tag_id = :t",
						new MapSqlParameterSource("u", customerId).addValue("t", t.id()));
				removed.add(t.name());
			}
		}
		Set<Long> currentIds = new HashSet<>(current.stream().map(TagView::id).toList());
		for (Long id : wanted) {
			if (!currentIds.contains(id)) {
				jdbc.update("""
						INSERT INTO customer_tag_assignments (user_id, tag_id, assigned_by) VALUES (:u, :t, :by)
						ON CONFLICT DO NOTHING
						""", new MapSqlParameterSource("u", customerId).addValue("t", id)
					.addValue("by", CurrentUser.requireId()));
				added.add(known.get(id));
			}
		}
		if (!added.isEmpty() || !removed.isEmpty()) {
			audit.record("CUSTOMER_TAGS_CHANGED", "customer", customerId, Map.of("added", added, "removed", removed));
		}
		return tagsOf(List.of(customerId)).getOrDefault(customerId, List.of());
	}

	// ------------------------------------------------------------------ apoio

	private void requireCustomer(Long id) {
		Boolean exists = jdbc.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM users WHERE id = :id AND role = 'CUSTOMER' AND deleted_at IS NULL)",
				new MapSqlParameterSource("id", id), Boolean.class);
		if (!Boolean.TRUE.equals(exists)) {
			throw new NotFoundException("Cliente não encontrado.");
		}
	}

	private Map<Long, List<TagView>> tagsOf(List<Long> customerIds) {
		Map<Long, List<TagView>> result = new HashMap<>();
		if (customerIds.isEmpty()) {
			return result;
		}
		jdbc.query("""
				SELECT a.user_id, t.id, t.name FROM customer_tag_assignments a
				JOIN customer_tags t ON t.id = a.tag_id
				WHERE a.user_id IN (:ids) ORDER BY lower(t.name)
				""", new MapSqlParameterSource("ids", customerIds), rs -> {
			result.computeIfAbsent(rs.getLong("user_id"), k -> new ArrayList<>())
				.add(new TagView(rs.getLong("id"), rs.getString("name")));
		});
		return result;
	}

	private MapSqlParameterSource params(CustomerFilter f) {
		String like = null;
		Long qid = null;
		if (StringUtils.hasText(f.q())) {
			String term = f.q().strip().toLowerCase();
			like = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
			if (term.matches("[0-9]{1,18}")) {
				qid = Long.valueOf(term);
			}
		}
		return new MapSqlParameterSource("like", like).addValue("qid", qid)
			.addValue("status", f.status() == null ? null : f.status().name())
			.addValue("tagId", f.tagId())
			.addValue("segment", f.segment() == null ? null : f.segment().name())
			.addValue("lapsedBefore", Timestamp.from(clock.instant().minus(LAPSED_AFTER)));
	}

	/** Registra os filtros usados, sem o texto livre de busca (pode ser um e-mail). */
	private static Map<String, Object> filterDetails(CustomerFilter f, int rows) {
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("rows", rows);
		d.put("search", StringUtils.hasText(f.q()));
		if (f.status() != null) {
			d.put("status", f.status().name());
		}
		if (f.tagId() != null) {
			d.put("tagId", f.tagId());
		}
		if (f.segment() != null) {
			d.put("segment", f.segment().name());
		}
		return d;
	}

	private static CustomerListItem listItem(ResultSet rs, List<TagView> tags) throws SQLException {
		return new CustomerListItem(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"),
				User.Status.valueOf(rs.getString("status")), instant(rs, "created_at"), rs.getLong("paid_orders"),
				Money.normalize(rs.getBigDecimal("total_spent")), instant(rs, "last_purchase_at"), tags);
	}

	private static Instant instant(ResultSet rs, String column) throws SQLException {
		Timestamp t = rs.getTimestamp(column);
		return t == null ? null : t.toInstant();
	}

}
