package br.com.gigaverse3d.account.api;

import java.time.Clock;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.account.api.AccountDtos.MessageResponse;
import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.account.service.PrivacyService;
import br.com.gigaverse3d.common.domain.BusinessTime;

/** Direitos do titular disponíveis na própria conta (LGPD art. 18, II e VI). */
@Tag(name = "Minha conta — privacidade")
@RestController
@RequestMapping("/api/v1/account/privacy")
public class PrivacyController {

	public record DeleteAccountRequest(
			@NotBlank(message = "Confirme com sua senha.") @Size(max = 128) String password) {
	}

	private final PrivacyService privacy;

	private final SessionManager sessions;

	private final Clock clock;

	public PrivacyController(PrivacyService privacy, SessionManager sessions, Clock clock) {
		this.privacy = privacy;
		this.sessions = sessions;
		this.clock = clock;
	}

	@Operation(summary = "Baixa uma cópia dos meus dados pessoais (JSON; até 5 por hora)")
	@GetMapping("/export")
	public ResponseEntity<Map<String, Object>> export() {
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_JSON)
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment()
						.filename("meus-dados-gigaverse3d-" + BusinessTime.today(clock) + ".json")
						.build()
						.toString())
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.body(privacy.export(CurrentUser.requireId()));
	}

	@Operation(summary = "Exclui (anonimiza) minha conta — exige a senha; encerra a sessão")
	@PostMapping("/delete-account")
	public MessageResponse deleteAccount(@Valid @RequestBody DeleteAccountRequest req, HttpServletRequest request,
			HttpServletResponse response) {
		privacy.anonymize(CurrentUser.requireId(), req.password());
		sessions.end(request, response);
		return new MessageResponse("Conta excluída. Os pedidos já realizados são mantidos pelo prazo legal, sem seus dados de contato.");
	}

}
