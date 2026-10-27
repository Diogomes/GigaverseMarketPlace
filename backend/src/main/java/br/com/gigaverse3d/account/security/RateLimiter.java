package br.com.gigaverse3d.account.security;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.gigaverse3d.common.web.ApiExceptions.TooManyRequestsException;

/**
 * Limitador em memória com janela deslizante (por chave: IP, conta, e-mail).
 * Adequado a uma única instância; com várias instâncias, trocar por armazenamento
 * compartilhado (Redis/banco) — registrado em DECISIONS.md.
 */
@Component
public class RateLimiter {

	private static final int MAX_KEYS = 50_000;

	private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

	private final Clock clock;

	public RateLimiter() {
		this(Clock.systemUTC());
	}

	RateLimiter(Clock clock) {
		this.clock = clock;
	}

	/** Registra uma tentativa; lança 429 se o limite da janela foi excedido. */
	public void check(String key, int max, Duration window) {
		long now = clock.millis();
		long cutoff = now - window.toMillis();
		if (hits.size() > MAX_KEYS) {
			hits.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast() < cutoff);
		}
		Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (deque) {
			while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
				deque.pollFirst();
			}
			if (deque.size() >= max) {
				long retryAfterMs = deque.peekFirst() + window.toMillis() - now;
				throw new TooManyRequestsException(Math.max(1, retryAfterMs / 1000));
			}
			deque.addLast(now);
		}
	}

	/** Somente para testes. */
	public void reset() {
		hits.clear();
	}

}
