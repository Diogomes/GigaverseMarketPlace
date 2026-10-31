package br.com.gigaverse3d.account.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retenção mínima (docs/PRIVACY_DATA_MAP.md): apaga diariamente o que não tem mais finalidade —
 * tokens de redefinição/convite vencidos há mais de 30 dias e carrinhos de visitantes expirados.
 */
@Component
public class DataRetentionJob {

	private static final Logger log = LoggerFactory.getLogger(DataRetentionJob.class);

	static final Duration RESET_TOKEN_RETENTION = Duration.ofDays(30);

	public record PurgeResult(int resetTokens, int anonymousCarts) {
	}

	private final JdbcTemplate jdbc;

	private final Clock clock;

	public DataRetentionJob(JdbcTemplate jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Scheduled(cron = "${app.privacy.retention-cron:0 30 3 * * *}", zone = "America/Recife")
	public void run() {
		PurgeResult r = purge();
		if (r.resetTokens() > 0 || r.anonymousCarts() > 0) {
			log.info("Retenção: {} token(s) de redefinição e {} carrinho(s) de visitante apagados", r.resetTokens(),
					r.anonymousCarts());
		}
	}

	@Transactional
	public PurgeResult purge() {
		Timestamp now = Timestamp.from(clock.instant());
		int tokens = jdbc.update("DELETE FROM password_reset_tokens WHERE expires_at < ?",
				Timestamp.from(clock.instant().minus(RESET_TOKEN_RETENTION)));
		int carts = jdbc.update("DELETE FROM carts WHERE user_id IS NULL AND expires_at < ?", now);
		return new PurgeResult(tokens, carts);
	}

}
