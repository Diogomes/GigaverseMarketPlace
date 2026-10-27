package br.com.gigaverse3d.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

	private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

	/** Com SMTP configurado (spring.mail.host): Mailpit em dev, provedor real em produção. */
	@Bean
	@ConditionalOnProperty(name = "spring.mail.host")
	EmailSender smtpEmailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
		return message -> {
			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setFrom(from);
			mail.setTo(message.to());
			mail.setSubject(message.subject());
			mail.setText(message.textBody());
			mailSender.send(mail);
		};
	}

	/**
	 * Sem provedor: não envia e registra apenas destinatário mascarado e assunto —
	 * nunca o corpo (que pode conter links/tokens).
	 */
	@Bean
	@ConditionalOnMissingBean(EmailSender.class)
	EmailSender loggingEmailSender() {
		return message -> log.warn("E-mail NÃO enviado (nenhum provedor configurado): para={} assunto=\"{}\"",
				mask(message.to()), message.subject());
	}

	static String mask(String email) {
		int at = email.indexOf('@');
		return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
	}

}
