package br.com.gigaverse3d.common.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findByEntityTypeAndEntityIdOrderByIdDesc(String entityType, String entityId);

}
