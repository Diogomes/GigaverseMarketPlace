package br.com.gigaverse3d.shipping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import br.com.gigaverse3d.common.domain.Money;
import br.com.gigaverse3d.order.domain.Order;

/**
 * Frete por adaptador. Implementação atual: retirada + tabela por faixa de CEP configurável.
 * Sem tabela configurada, só a retirada é oferecida — valores reais de frete são decisão do
 * proprietário (D-P02). A integração com transportadora/Melhor Envio entra como outro provedor.
 */
@Service
public class ShippingService {

	/** Origem dos envios: CEP 53625-816 (app.store.shipping-origin-postal-code). */
	@ConfigurationProperties(prefix = "app.shipping")
	public record ShippingProperties(@DefaultValue Pickup pickup, List<Zone> zones,
			@DefaultValue("false") boolean demoValues) {

		public record Pickup(@DefaultValue("true") boolean enabled,
				@DefaultValue("Retirada (local e horário combinados após a confirmação)") String label) {
		}

		/** Faixa de CEP (8 dígitos, inclusiva) com preço e prazo em dias úteis após a postagem. */
		public record Zone(String name, String fromPostalCode, String toPostalCode, BigDecimal price, int minDays,
				int maxDays) {

			boolean covers(String postalCode) {
				return postalCode.compareTo(fromPostalCode) >= 0 && postalCode.compareTo(toPostalCode) <= 0;
			}
		}
	}

	@Configuration
	@EnableConfigurationProperties(ShippingProperties.class)
	static class Config {

	}

	public record ShippingQuote(Order.ShippingMethod method, String label, BigDecimal price, Integer minDays,
			Integer maxDays, boolean demoValues) {
	}

	private final ShippingProperties properties;

	public ShippingService(ShippingProperties properties) {
		this.properties = properties;
	}

	public List<ShippingQuote> quote(String postalCode) {
		List<ShippingQuote> options = new ArrayList<>();
		if (properties.pickup().enabled()) {
			options.add(new ShippingQuote(Order.ShippingMethod.PICKUP, properties.pickup().label(), Money.of("0"), null,
					null, false));
		}
		delivery(postalCode).ifPresent(options::add);
		return options;
	}

	public Optional<ShippingQuote> delivery(String postalCode) {
		if (postalCode == null || !postalCode.matches("[0-9]{8}") || properties.zones() == null) {
			return Optional.empty();
		}
		return properties.zones()
			.stream()
			.filter(z -> z.covers(postalCode))
			.findFirst()
			.map(z -> new ShippingQuote(Order.ShippingMethod.DELIVERY, "Entrega — " + z.name(),
					Money.normalize(z.price()), z.minDays(), z.maxDays(), properties.demoValues()));
	}

	public boolean pickupEnabled() {
		return properties.pickup().enabled();
	}

}
