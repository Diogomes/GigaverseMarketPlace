package br.com.gigaverse3d.order.service;

import java.text.NumberFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.gigaverse3d.account.security.AuthProperties;
import br.com.gigaverse3d.common.domain.AfterTransaction;
import br.com.gigaverse3d.mail.EmailSender;
import br.com.gigaverse3d.mail.EmailSender.EmailMessage;
import br.com.gigaverse3d.order.domain.Order;
import br.com.gigaverse3d.order.domain.OrderStatus;

/** E-mails de pedido, enviados só após o commit (nunca anunciam algo que foi desfeito). */
@Component
public class OrderNotifier {

	private static final Logger log = LoggerFactory.getLogger(OrderNotifier.class);

	private final EmailSender email;

	private final AuthProperties auth;

	public OrderNotifier(EmailSender email, AuthProperties auth) {
		this.email = email;
		this.auth = auth;
	}

	static String label(OrderStatus status) {
		return switch (status) {
			case PENDING_PAYMENT -> "Aguardando pagamento";
			case PAID -> "Pagamento confirmado";
			case IN_PRODUCTION -> "Em produção";
			case READY_FOR_PICKUP -> "Pronto para retirada";
			case SHIPPED -> "Enviado";
			case DELIVERED -> "Entregue";
			case CANCELLED -> "Cancelado";
		};
	}

	public void orderPlaced(Order order, String paymentInstructions) {
		String total = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(order.getTotal());
		send(new EmailMessage(order.getCustomerEmail(), "Gigaverse3D — pedido #" + order.getNumber() + " recebido", """
				Olá, %s!

				Recebemos o seu pedido #%d no valor de %s.
				Status: Aguardando pagamento.

				%s

				Acompanhe em: %s/conta/pedidos/%d
				""".formatted(order.getCustomerName(), order.getNumber(), total, paymentInstructions,
				auth.frontendBaseUrl(), order.getNumber())));
	}

	public void statusChanged(Order order, String note) {
		send(new EmailMessage(order.getCustomerEmail(),
				"Gigaverse3D — pedido #" + order.getNumber() + ": " + label(order.getStatus()), """
						Olá, %s!

						Seu pedido #%d agora está: %s.%s

						Detalhes: %s/conta/pedidos/%d
						""".formatted(order.getCustomerName(), order.getNumber(), label(order.getStatus()),
						note == null || note.isBlank() ? "" : "\n" + note, auth.frontendBaseUrl(),
						order.getNumber())));
	}

	private void send(EmailMessage message) {
		AfterTransaction.onCommit(() -> {
			try {
				email.send(message);
			}
			catch (RuntimeException e) {
				log.error("Falha ao enviar e-mail de pedido: {}", e.getClass().getSimpleName());
			}
		});
	}

}
