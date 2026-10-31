package br.com.gigaverse3d.payment.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Registro dos provedores disponíveis; o ativo para novos pedidos vem de app.payments.provider. */
@Component
public class PaymentProviders {

	private final Map<String, PaymentProvider> byCode;

	private final String active;

	public PaymentProviders(List<PaymentProvider> providers,
			@Value("${app.payments.provider:" + ManualPaymentProvider.CODE + "}") String active) {
		this.byCode = providers.stream().collect(Collectors.toMap(PaymentProvider::code, Function.identity()));
		if (!byCode.containsKey(active)) {
			throw new IllegalStateException("Provedor de pagamento desconhecido: " + active);
		}
		this.active = active;
	}

	public PaymentProvider active() {
		return byCode.get(active);
	}

	public Optional<PaymentProvider> byCode(String code) {
		return Optional.ofNullable(byCode.get(code));
	}

}
