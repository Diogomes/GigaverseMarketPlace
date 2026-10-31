package br.com.gigaverse3d.account.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.RateLimiter;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;

/**
 * Solicitações do titular feitas pelo próprio cliente (LGPD art. 18): cópia dos dados e exclusão
 * da conta. A exclusão é uma anonimização: a conta deixa de existir para login e contato, mas os
 * pedidos permanecem para obrigações fiscais/de consumo (base legal art. 16, I) — prazo e campos
 * retidos a validar com o jurídico (D-P07). Ver docs/PRIVACY_DATA_MAP.md.
 */
@Service
public class PrivacyService {

	private static final SecureRandom RANDOM = new SecureRandom();

	/** Status em que o pedido ainda depende de contato com o cliente. */
	private static final List<String> OPEN_ORDER_STATUSES = List.of("PENDING_PAYMENT", "PAID", "IN_PRODUCTION",
			"READY_FOR_PICKUP", "SHIPPED");

	private final JdbcTemplate jdbc;

	private final UserRepository users;

	private final PasswordEncoder encoder;

	private final RateLimiter limiter;

	private final AuditService audit;

	private final Clock clock;

	public PrivacyService(JdbcTemplate jdbc, UserRepository users, PasswordEncoder encoder, RateLimiter limiter,
			AuditService audit, Clock clock) {
		this.jdbc = jdbc;
		this.users = users;
		this.encoder = encoder;
		this.limiter = limiter;
		this.audit = audit;
		this.clock = clock;
	}

	/**
	 * Cópia legível dos dados pessoais do cliente. Não inclui hash de senha, tokens nem dados
	 * internos da equipe (notas/etiquetas do CRM) — estes só via solicitação formal ao encarregado.
	 */
	@Transactional
	public Map<String, Object> export(Long userId) {
		limiter.check("privacy-export:" + userId, 5, Duration.ofHours(1));
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("geradoEm", clock.instant().toString());
		data.put("aviso", "Cópia dos dados pessoais da sua conta na Gigaverse3D. Notas internas de atendimento "
				+ "podem ser solicitadas pelo canal de privacidade.");
		data.put("conta", jdbc.queryForMap("""
				SELECT email, full_name AS nome, created_at AS "criadaEm", last_login_at AS "ultimoAcesso"
				FROM users WHERE id = ? AND deleted_at IS NULL""", userId));
		data.put("telefone", jdbc.query("SELECT phone FROM customer_profiles WHERE user_id = ?",
				rs -> rs.next() ? rs.getString(1) : null, userId));
		data.put("enderecos", jdbc.queryForList("""
				SELECT label AS apelido, recipient_name AS destinatario, postal_code AS cep, street AS rua, number AS numero,
				       complement AS complemento, district AS bairro, city AS cidade, state AS uf, is_default AS principal
				FROM addresses WHERE user_id = ? AND deleted_at IS NULL ORDER BY id""", userId));
		data.put("consentimentos", jdbc.queryForList("""
				SELECT consent_type AS tipo, granted AS concedido, document_version AS "versaoDocumento",
				       recorded_at AS "registradoEm"
				FROM user_consents WHERE user_id = ? ORDER BY recorded_at, id""", userId));
		List<Map<String, Object>> orders = new ArrayList<>();
		for (Map<String, Object> o : jdbc.queryForList("""
				SELECT id, number AS numero, placed_at AS "criadoEm", status, total, payment_method AS pagamento,
				       shipping_method AS entrega, ship_recipient_name AS destinatario, ship_postal_code AS cep,
				       ship_street AS rua, ship_number AS numero_endereco, ship_city AS cidade, ship_state AS uf,
				       customer_note AS observacao
				FROM orders WHERE user_id = ? ORDER BY placed_at""", userId)) {
			Object id = o.remove("id");
			o.put("itens", jdbc.queryForList("""
					SELECT product_name AS produto, options_summary AS opcoes, quantity AS quantidade,
					       unit_price AS "precoUnitario", line_total AS total
					FROM order_items WHERE order_id = ? ORDER BY id""", id));
			orders.add(o);
		}
		data.put("pedidos", orders);
		audit.recordAs(userId, "PERSONAL_DATA_EXPORTED", "user", userId, Map.of());
		return data;
	}

	/**
	 * Anonimiza a conta. Exige a senha atual e nenhum pedido em andamento (ainda precisamos falar
	 * com o cliente para entregar ou estornar). Encerra todas as sessões.
	 */
	@Transactional
	public void anonymize(Long userId, String password) {
		limiter.check("privacy-delete:" + userId, 5, Duration.ofMinutes(15));
		User user = users.findById(userId)
			.filter(u -> u.getDeletedAt() == null && u.getRole() == User.Role.CUSTOMER)
			.orElseThrow(() -> new NotFoundException("Conta não encontrada."));
		if (password == null || !encoder.matches(password, user.getPasswordHash())) {
			throw new FieldValidationException("password", "Senha incorreta.");
		}
		Integer open = jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = ANY (?)",
				Integer.class, userId, OPEN_ORDER_STATUSES.toArray(String[]::new));
		if (open != null && open > 0) {
			throw new ConflictException("Você tem pedidos em andamento. Aguarde a conclusão ou o cancelamento "
					+ "deles, ou fale conosco, antes de excluir a conta.");
		}
		byte[] raw = new byte[32];
		RANDOM.nextBytes(raw);
		String placeholder = "removido-" + userId + "@anonimizado.invalid";
		user.anonymize(placeholder, encoder.encode(Base64.getEncoder().encodeToString(raw)), clock.instant());

		jdbc.update("UPDATE customer_profiles SET phone = NULL, updated_at = now() WHERE user_id = ?", userId);
		jdbc.update("DELETE FROM addresses WHERE user_id = ?", userId);
		jdbc.update("DELETE FROM password_reset_tokens WHERE user_id = ?", userId);
		jdbc.update("DELETE FROM carts WHERE user_id = ?", userId);
		jdbc.update("DELETE FROM customer_tag_assignments WHERE user_id = ?", userId);
		jdbc.update("""
				UPDATE customer_notes SET body = '[removida: conta anonimizada]', deleted_at = COALESCE(deleted_at, now()),
				       updated_at = now()
				WHERE customer_id = ?""", userId);
		// Pedidos ficam (obrigação legal), mas sem o e-mail de contato e sem a observação livre do cliente.
		jdbc.update("UPDATE orders SET customer_email = ?, customer_note = NULL, updated_at = now() WHERE user_id = ?",
				placeholder, userId);
		// Histórico de consentimentos é mantido como prova; registra a revogação de marketing.
		jdbc.update("""
				INSERT INTO user_consents (user_id, consent_type, granted, document_version)
				VALUES (?, 'MARKETING_EMAIL', false, 'anonimizacao')""", userId);
		audit.recordAs(userId, "ACCOUNT_ANONYMIZED", "user", userId, Map.of());
	}

}
