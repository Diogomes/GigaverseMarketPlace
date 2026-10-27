package br.com.gigaverse3d.account.api;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.account.api.AccountDtos.AddressRequest;
import br.com.gigaverse3d.account.api.AccountDtos.AddressResponse;
import br.com.gigaverse3d.account.api.AccountDtos.ChangePasswordRequest;
import br.com.gigaverse3d.account.api.AccountDtos.MessageResponse;
import br.com.gigaverse3d.account.api.AccountDtos.ProfileRequest;
import br.com.gigaverse3d.account.api.AccountDtos.ProfileResponse;
import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.security.AccountUserDetails;
import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.account.service.AccountService;
import br.com.gigaverse3d.account.service.AddressService;

/** Conta do usuário logado. O dono é sempre o da sessão — não há id de usuário na rota. */
@Tag(name = "Minha conta")
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

	private final AccountService accounts;

	private final AddressService addresses;

	private final SessionManager sessions;

	public AccountController(AccountService accounts, AddressService addresses, SessionManager sessions) {
		this.accounts = accounts;
		this.addresses = addresses;
		this.sessions = sessions;
	}

	@Operation(summary = "Meus dados")
	@GetMapping("/profile")
	public ProfileResponse profile() {
		return accounts.profile(CurrentUser.requireId());
	}

	@Operation(summary = "Atualiza nome, telefone e preferência de comunicação (papel/e-mail não são editáveis aqui)")
	@PutMapping("/profile")
	public ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest req) {
		return accounts.updateProfile(CurrentUser.requireId(), req);
	}

	@Operation(summary = "Troca a senha (exige a atual; encerra as outras sessões)")
	@PutMapping("/password")
	public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest req, HttpServletRequest request,
			HttpServletResponse response) {
		User user = accounts.changePassword(CurrentUser.requireId(), req);
		// mantém ESTA sessão válida com o novo carimbo (e novo id de sessão)
		sessions.refresh(AccountUserDetails.of(user, Instant.now()), request, response);
		return new MessageResponse("Senha alterada.");
	}

	@Operation(summary = "Meus endereços")
	@GetMapping("/addresses")
	public List<AddressResponse> addresses() {
		return addresses.list(CurrentUser.requireId());
	}

	@Operation(summary = "Adiciona endereço (máx. 10; o primeiro vira principal)")
	@PostMapping("/addresses")
	@ResponseStatus(HttpStatus.CREATED)
	public AddressResponse addAddress(@Valid @RequestBody AddressRequest req) {
		return addresses.create(CurrentUser.requireId(), req);
	}

	@Operation(summary = "Atualiza endereço")
	@PutMapping("/addresses/{id}")
	public AddressResponse updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest req) {
		return addresses.update(CurrentUser.requireId(), id, req);
	}

	@Operation(summary = "Define endereço principal")
	@PostMapping("/addresses/{id}/default")
	public AddressResponse makeDefault(@PathVariable Long id) {
		return addresses.makeDefault(CurrentUser.requireId(), id);
	}

	@Operation(summary = "Remove endereço")
	@DeleteMapping("/addresses/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAddress(@PathVariable Long id) {
		addresses.delete(CurrentUser.requireId(), id);
	}

}
