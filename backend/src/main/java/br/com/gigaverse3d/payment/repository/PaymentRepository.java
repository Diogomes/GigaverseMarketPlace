package br.com.gigaverse3d.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	List<Payment> findByOrderIdOrderByIdDesc(Long orderId);

	Optional<Payment> findByProviderAndProviderReference(String provider, String providerReference);

	/** Idempotência de webhooks: insere o evento; 0 linhas = já processado antes. */
	@Modifying
	@Query(value = """
			INSERT INTO payment_webhook_events (provider, event_id, event_type, payload_sha256, processed_at)
			VALUES (:provider, :eventId, :eventType, :payloadSha256, now())
			ON CONFLICT (provider, event_id) DO NOTHING
			""", nativeQuery = true)
	int registerWebhookEvent(@Param("provider") String provider, @Param("eventId") String eventId,
			@Param("eventType") String eventType, @Param("payloadSha256") String payloadSha256);

}
