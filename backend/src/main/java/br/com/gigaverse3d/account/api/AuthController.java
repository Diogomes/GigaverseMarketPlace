package br.com.gigaverse3d.account.api;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.account.api.AccountDtos.CsrfResponse;
import br.com.gigaverse3d.account.api.AccountDtos.ForgotPasswordRequest;
import br.com.gigaverse3d.account.api.AccountDtos.LoginRequest;
import br.com.gigaverse3d.account.api.AccountDtos.MeResponse;
import br.com.gigaverse3d.account.api.AccountDtos.MessageResponse;
import br.com.gigaverse3d.account.api.AccountDtos.RegisterRequest;
import br.com.gigaverse3d.account.api.AccountDtos.ResetPasswordRequest;
import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.security.AccountUserDetails;
import br.com.gigaverse3d.account.security.AuthProperties;
import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.account.security.RateLimiter;
import br.com.gigaverse3d.account.service.AuthService;
import br.com.gigaverse3d.account.service.PasswordResetService;

@Tag(name = "Autenticação", description = "Sessão por cookie HttpOnly + CSRF (header X-XSRF-TOKEN)")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService auth;

	private final PasswordResetService passwordReset;

	private final SessionManager sessions;

	private final RateLimiter rateLimiter;

	private final AuthProperties properties;

	public AuthController(AuthService auth, PasswordResetService passwordReset, SessionManager sessions,
			RateLimiter rateLimiter, AuthProperties properties) {
		this.auth = auth;
		this.passwordReset = passwordReset;
		this.sessions = sessions;
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	private Duration window() {
		return Duration.ofMinutes(properties.rateLimits().windowMinutes());
	}

	@Operation(summary = "Obtém o token CSRF (também gravado no cookie XSRF-TOKEN)")
	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getToken(), token.getHeaderName());
	}

	@Operation(summary = "Cria conta de cliente e inicia a sessão")
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public MeResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request,
			HttpServletResponse response) {
		rateLimiter.check("register:ip:" + request.getRemoteAddr(), properties.rateLimits().registerPerIp(), window());
		User user = auth.register(req);
		Authentication authentication = auth.authenticate(user.getEmail(), req.password());
		sessions.establish(authentication, request, response);
		return me((AccountUserDetails) authentication.getPrincipal());
	}

	@Operation(summary = "Login (mensagem genérica em caso de falha; bloqueio temporário após falhas seguidas)")
	@PostMapping("/login")
	public MeResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
			HttpServletResponse response) {
		String ip = request.getRemoteAddr();
		rateLimiter.check("login:ip:" + ip, properties.rateLimits().loginPerIp(), window());
		rateLimiter.check("login:account:" + User.normalizeEmail(req.email()), properties.rateLimits().loginPerAccount(),
				window());
		Authentication authentication = auth.authenticate(req.email(), req.password());
		sessions.establish(authentication, request, response);
		return me((AccountUserDetails) authentication.getPrincipal());
	}

	@Operation(summary = "Encerra a sessão")
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		sessions.end(request, response);
	}

	@Operation(summary = "Usuário da sessão atual (204 sem corpo se não houver sessão)")
	@GetMapping("/me")
	public ResponseEntity<MeResponse> me() {
		// 204 em vez de 401: visitantes anônimos são o caso normal da loja (sem erro no console).
		return CurrentUser.get().map(p -> ResponseEntity.ok(me(p))).orElseGet(() -> ResponseEntity.noContent().build());
	}

	@Operation(summary = "Solicita link de redefinição de senha (resposta sempre igual)")
	@PostMapping("/password/forgot")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest req, HttpServletRequest request) {
		rateLimiter.check("forgot:ip:" + request.getRemoteAddr(), properties.rateLimits().forgotPerIp(), window());
		rateLimiter.check("forgot:email:" + User.normalizeEmail(req.email()), properties.rateLimits().forgotPerEmail(),
				Duration.ofHours(1));
		passwordReset.requestReset(req.email());
		return new MessageResponse(PasswordResetService.GENERIC_FORGOT_MESSAGE);
	}

	@Operation(summary = "Redefine a senha com o token recebido por e-mail (uso único)")
	@PostMapping("/password/reset")
	public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest req, HttpServletRequest request) {
		rateLimiter.check("reset:ip:" + request.getRemoteAddr(), properties.rateLimits().forgotPerIp() * 2, window());
		passwordReset.reset(req.token(), req.newPassword());
		return new MessageResponse("Senha redefinida. Faça login com a nova senha.");
	}

	private static MeResponse me(AccountUserDetails p) {
		return new MeResponse(p.getId(), p.getUsername(), p.getFullName(), p.getRole());
	}

}
