package br.com.gigaverse3d.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Expira pedidos não pagos no prazo (app.orders.pending-expiration) e devolve o estoque. */
@Component
@ConditionalOnProperty(name = "app.orders.expiration-job-enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationJob {

	private static final Logger log = LoggerFactory.getLogger(OrderExpirationJob.class);

	@Configuration
	@EnableScheduling
	static class SchedulingConfig {

	}

	private final OrderService orders;

	public OrderExpirationJob(OrderService orders) {
		this.orders = orders;
	}

	@Scheduled(initialDelayString = "${app.orders.expiration-check-interval:PT15M}",
			fixedDelayString = "${app.orders.expiration-check-interval:PT15M}")
	public void run() {
		int expired = orders.expireUnpaid();
		if (expired > 0) {
			log.info("{} pedido(s) não pago(s) cancelado(s) por expiração", expired);
		}
	}

}
