package br.com.gigaverse3d.admin;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.admin.StaffUserService.CreateStaffRequest;
import br.com.gigaverse3d.admin.StaffUserService.RoleRequest;
import br.com.gigaverse3d.admin.StaffUserService.StaffUserView;
import br.com.gigaverse3d.common.config.AppProperties;
import br.com.gigaverse3d.common.domain.BusinessTime;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.payment.service.PaymentProviders;
import br.com.gigaverse3d.shipping.ShippingService.ShippingProperties;
import br.com.gigaverse3d.storage.StorageProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Equipe, auditoria (somente ADMIN) e configurações operacionais (leitura, ADMIN/OPERATOR). */
public final class AdminController {

	private AdminController() {
	}

	@Tag(name = "Admin — equipe")
	@RestController
	@RequestMapping("/api/v1/admin/users")
	public static class StaffUsers {

		private final StaffUserService service;

		public StaffUsers(StaffUserService service) {
			this.service = service;
		}

		@Operation(summary = "Lista contas da equipe (OPERATOR e ADMIN)")
		@GetMapping
		public List<StaffUserView> list() {
			return service.list();
		}

		@Operation(summary = "Cria conta da equipe e envia convite para a pessoa definir a senha")
		@PostMapping
		@ResponseStatus(HttpStatus.CREATED)
		public StaffUserView create(@Valid @RequestBody CreateStaffRequest req) {
			return service.create(req);
		}

		@Operation(summary = "Reenvia o convite (novo link; o anterior deixa de valer)")
		@PostMapping("/{id}/invite")
		@ResponseStatus(HttpStatus.NO_CONTENT)
		public void invite(@PathVariable Long id) {
			service.resendInvitation(id);
		}

		@Operation(summary = "Altera o papel (encerra as sessões da pessoa)")
		@PutMapping("/{id}/role")
		public StaffUserView role(@PathVariable Long id, @Valid @RequestBody RoleRequest req) {
			return service.changeRole(id, req.role());
		}

		@Operation(summary = "Desativa a conta (encerra as sessões)")
		@PostMapping("/{id}/disable")
		public StaffUserView disable(@PathVariable Long id) {
			return service.disable(id);
		}

		@Operation(summary = "Reativa a conta")
		@PostMapping("/{id}/enable")
		public StaffUserView enable(@PathVariable Long id) {
			return service.enable(id);
		}

	}

	@Tag(name = "Admin — auditoria")
	@RestController
	@RequestMapping("/api/v1/admin/audit")
	public static class Audit {

		public record AuditEntry(Long id, Instant createdAt, Long actorId, String actorName, String actorEmail,
				String action, String entityType, String entityId, JsonNode details) {
		}

		private final NamedParameterJdbcTemplate jdbc;

		private final JsonMapper json;

		public Audit(NamedParameterJdbcTemplate jdbc, JsonMapper json) {
			this.jdbc = jdbc;
			this.json = json;
		}

		@Operation(summary = "Trilha de auditoria com filtros (mais recentes primeiro)")
		@GetMapping
		@Transactional(readOnly = true)
		public PageResponse<AuditEntry> list(
				@RequestParam(required = false) @Pattern(regexp = "^[A-Z_]{1,80}$") String action,
				@RequestParam(required = false) @Pattern(regexp = "^[a-z_]{1,60}$") String entityType,
				@RequestParam(required = false) @Pattern(regexp = "^[A-Za-z0-9-]{1,60}$") String entityId,
				@RequestParam(required = false) Long actorId,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
				@RequestParam(defaultValue = "0") @Min(0) int page,
				@RequestParam(defaultValue = "20") @Min(1) @Max(PageResponse.MAX_PAGE_SIZE) int size) {
			MapSqlParameterSource p = new MapSqlParameterSource("action", action).addValue("entityType", entityType)
				.addValue("entityId", entityId)
				.addValue("actorId", actorId)
				.addValue("start", from == null ? null : Timestamp.from(BusinessTime.startOf(from)))
				.addValue("end", to == null ? null : Timestamp.from(BusinessTime.endOf(to)))
				.addValue("limit", size)
				.addValue("offset", (long) page * size);
			String where = """
					FROM audit_log l LEFT JOIN users u ON u.id = l.actor_user_id
					WHERE (CAST(:action AS text) IS NULL OR l.action = CAST(:action AS text))
					  AND (CAST(:entityType AS text) IS NULL OR l.entity_type = CAST(:entityType AS text))
					  AND (CAST(:entityId AS text) IS NULL OR l.entity_id = CAST(:entityId AS text))
					  AND (CAST(:actorId AS bigint) IS NULL OR l.actor_user_id = CAST(:actorId AS bigint))
					  AND (CAST(:start AS timestamptz) IS NULL OR l.created_at >= CAST(:start AS timestamptz))
					  AND (CAST(:end AS timestamptz) IS NULL OR l.created_at < CAST(:end AS timestamptz))
					""";
			Long total = jdbc.queryForObject("SELECT COUNT(*) " + where, p, Long.class);
			List<AuditEntry> content = jdbc.query("""
					SELECT l.id, l.created_at, l.actor_user_id, u.full_name, u.email, l.action, l.entity_type,
					       l.entity_id, CAST(l.details AS text) AS details
					""" + where + " ORDER BY l.created_at DESC, l.id DESC LIMIT :limit OFFSET :offset", p,
					(rs, i) -> {
						String details = rs.getString("details");
						return new AuditEntry(rs.getLong("id"), rs.getTimestamp("created_at").toInstant(),
								rs.getObject("actor_user_id", Long.class), rs.getString("full_name"),
								rs.getString("email"), rs.getString("action"), rs.getString("entity_type"),
								rs.getString("entity_id"), details == null ? null : json.readTree(details));
					});
			long totalElements = total == null ? 0 : total;
			return new PageResponse<>(content, page, size, totalElements, (int) ((totalElements + size - 1) / size));
		}

	}

	@Tag(name = "Admin — configurações")
	@RestController
	@RequestMapping("/api/v1/admin/settings")
	public static class Settings {

		public record ShippingZoneView(String name, String fromPostalCode, String toPostalCode,
				BigDecimal price, int minDays, int maxDays) {
		}

		/** Somente leitura: mudanças passam por variáveis de ambiente e novo deploy (rastreável). */
		public record SettingsView(String paymentProvider, List<AppProperties.PaymentMethod> paymentMethods,
				long pendingPaymentExpirationHours, String shippingOriginPostalCode, boolean pickupEnabled,
				List<ShippingZoneView> shippingZones, boolean shippingDemoValues, String storageType,
				String timezone) {
		}

		private final PaymentProviders payments;

		private final AppProperties app;

		private final ShippingProperties shipping;

		private final StorageProperties storage;

		private final Duration pendingExpiration;

		public Settings(PaymentProviders payments, AppProperties app, ShippingProperties shipping,
				StorageProperties storage, @Value("${app.orders.pending-expiration:PT72H}") Duration pendingExpiration) {
			this.payments = payments;
			this.app = app;
			this.shipping = shipping;
			this.storage = storage;
			this.pendingExpiration = pendingExpiration;
		}

		@Operation(summary = "Configurações operacionais em vigor (sem segredos)")
		@GetMapping
		public SettingsView get() {
			List<ShippingZoneView> zones = shipping.zones() == null ? List.of()
					: shipping.zones().stream().map(z -> new ShippingZoneView(z.name(), z.fromPostalCode(),
							z.toPostalCode(), z.price(), z.minDays(), z.maxDays())).toList();
			return new SettingsView(payments.active().code(), app.store().paymentMethods(), pendingExpiration.toHours(),
					app.store().shippingOriginPostalCode(), shipping.pickup().enabled(), zones, shipping.demoValues(),
					storage.type(), BusinessTime.ZONE.getId());
		}

	}

}
