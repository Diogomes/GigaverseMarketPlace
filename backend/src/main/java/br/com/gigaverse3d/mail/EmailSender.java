package br.com.gigaverse3d.mail;

/**
 * Envio de e-mail transacional. Implementações: SMTP (Mailpit em dev; provedor real a definir — D-P05)
 * e um registro sem conteúdo quando nenhum provedor está configurado.
 */
public interface EmailSender {

	record EmailMessage(String to, String subject, String textBody) {
	}

	void send(EmailMessage message);

}
