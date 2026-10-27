package br.com.gigaverse3d.common.audit;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.security.CurrentUser;
import tools.jackson.databind.json.JsonMapper;

/** Grava a trilha de auditoria na mesma transação da ação auditada. */
@Service
public class AuditService {

	private final AuditLogRepository repository;

	private final JsonMapper json;

	public AuditService(AuditLogRepository repository, JsonMapper json) {
		this.repository = repository;
		this.json = json;
	}

	/** Ação do usuário autenticado na requisição atual. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void record(String action, String entityType, Object entityId, Map<String, ?> details) {
		Map<String, Object> payload = new LinkedHashMap<>(details);
		Long actorId = currentUserId();
		if (actorId == null && currentUsername() != null) {
			// Usuário fora da tabela (ex.: testes com usuário simulado): guarda só o nome.
			payload.put("actor", currentUsername());
		}
		save(actorId, action, entityType, entityId, payload);
	}

	/** Ação com ator explícito (ex.: login, cadastro, recuperação de senha). */
	@Transactional(propagation = Propagation.MANDATORY)
	public void recordAs(Long actorUserId, String action, String entityType, Object entityId, Map<String, ?> details) {
		save(actorUserId, action, entityType, entityId, new LinkedHashMap<>(details));
	}

	private void save(Long actorId, String action, String entityType, Object entityId, Map<String, Object> payload) {
		repository.save(new AuditLog(actorId, action, entityType, entityId == null ? null : String.valueOf(entityId),
				json.writeValueAsString(payload)));
	}

	public static String currentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth == null || !auth.isAuthenticated() ? null : auth.getName();
	}

	public Long currentUserId() {
		return CurrentUser.get().map(p -> p.getId()).orElse(null);
	}

}
