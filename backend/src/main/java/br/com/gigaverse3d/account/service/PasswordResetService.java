package br.com.gigaverse3d.account.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.domain.PasswordResetToken;
import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.PasswordResetTokenRepository;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.AuthProperties;
import br.com.gigaverse3d.account.security.PasswordPolicy;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.domain.AfterTransaction;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.mail.EmailSender;
import br.com.gigaverse3d.mail.EmailSender.EmailMessage;

/**
 * Recuperação de senha: token aleatório de 256 bits, guardado só como SHA-256, uso único,
 * expiração curta. A resposta da solicitação é sempre a mesma (não revela se o e-mail existe).
 */
@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	public static final String GENERIC_FORGOT_MESSAGE = "Se houver uma conta com este e-mail, enviaremos um link para redefinir a senha.";

	public static final String INVALID_TOKEN_MESSAGE = "Link inválido ou expirado. Solicite um novo.";

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository users;

	private final PasswordResetTokenRepository tokens;

	private final PasswordEncoder passwordEncoder;

	private final PasswordPolicy passwordPolicy;

	private final EmailSender email;

	private final AuthProperties properties;

	private final AuditService audit;

	private final Clock clock;

	public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
			PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy, EmailSender email,
			AuthProperties properties, AuditService audit, Clock clock) {
		this.users = users;
		this.tokens = tokens;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.email = email;
		this.properties = properties;
		this.audit = audit;
		this.clock = clock;
	}

	@Transactional
	public void requestReset(String rawEmail) {
		Instant now = clock.instant();
		users.findByEmailAndDeletedAtIsNull(User.normalizeEmail(rawEmail))
			.filter(u -> u.getStatus() == User.Status.ACTIVE)
			.ifPresent(user -> {
				// Só o link mais recente vale.
				tokens.findUsableByUser(user.getId(), now).forEach(t -> t.consume(now));
				byte[] raw = new byte[32];
				RANDOM.nextBytes(raw);
				String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
				Duration ttl = Duration.ofMinutes(properties.resetTokenMinutes());
				tokens.save(new PasswordResetToken(user.getId(), sha256(token), now, now.plus(ttl)));
				audit.recordAs(user.getId(), "PASSWORD_RESET_REQUESTED", "user", user.getId(), Map.of());
				String link = properties.frontendBaseUrl() + "/redefinir-senha?token=" + token;
				EmailMessage message = new EmailMessage(user.getEmail(), "Gigaverse3D — redefinição de senha", """
						Olá, %s!

						Recebemos um pedido para redefinir a senha da sua conta Gigaverse3D.
						Use o link abaixo em até %d minutos:

						%s

						Se não foi você, ignore este e-mail: sua senha continua a mesma.
						""".formatted(user.getFullName(), ttl.toMinutes(), link));
				AfterTransaction.onCommit(() -> sendQuietly(message));
			});
	}

	@Transactional
	public void reset(String token, String newPassword) {
		Instant now = clock.instant();
		PasswordResetToken reset = tokens.findByTokenHash(sha256(token))
			.filter(t -> t.isUsable(now))
			.orElseThrow(() -> new FieldValidationException("token", INVALID_TOKEN_MESSAGE));
		User user = users.findById(reset.getUserId())
			.filter(u -> u.getStatus() == User.Status.ACTIVE && u.getDeletedAt() == null)
			.orElseThrow(() -> new FieldValidationException("token", INVALID_TOKEN_MESSAGE));
		passwordPolicy.validate(newPassword, user.getEmail(), "newPassword");
		reset.consume(now);
		tokens.findUsableByUser(user.getId(), now).forEach(t -> t.consume(now));
		// Troca a senha e o carimbo: todas as sessões abertas são encerradas.
		user.changePassword(passwordEncoder.encode(newPassword));
		audit.recordAs(user.getId(), "PASSWORD_RESET_COMPLETED", "user", user.getId(), Map.of());
		EmailMessage notice = new EmailMessage(user.getEmail(), "Gigaverse3D — sua senha foi alterada", """
				Olá, %s!

				A senha da sua conta Gigaverse3D foi redefinida agora há pouco.
				Se não foi você, entre em contato conosco imediatamente.
				""".formatted(user.getFullName()));
		AfterTransaction.onCommit(() -> sendQuietly(notice));
	}

	/** Falha de envio não altera a resposta (evita revelar existência da conta) — só registra. */
	private void sendQuietly(EmailMessage message) {
		try {
			email.send(message);
		}
		catch (RuntimeException e) {
			log.error("Falha ao enviar e-mail transacional: {}", e.getClass().getSimpleName());
		}
	}

	static String sha256(String value) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
