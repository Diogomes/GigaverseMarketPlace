package br.com.gigaverse3d.account.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.gigaverse3d.account.domain.User;

/**
 * DTOs de autenticação e conta. Nenhum DTO de entrada aceita papel, status ou e-mail de
 * terceiros: campos extras enviados pelo cliente são ignorados (sem elevação de privilégio).
 */
public final class AccountDtos {

	private AccountDtos() {
	}

	public record RegisterRequest(
			@NotBlank(message = "Informe seu nome.") @Size(min = 3, max = 120, message = "Entre 3 e 120 caracteres.")
			String fullName,
			@NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email,
			@NotBlank(message = "Informe a senha.") @Size(max = 128) String password,
			@NotNull(message = "É preciso aceitar os termos.") @AssertTrue(message = "É preciso aceitar os termos e a política de privacidade.")
			Boolean acceptTerms,
			Boolean marketingOptIn) {
	}

	public record LoginRequest(
			@NotBlank(message = "Informe o e-mail.") @Size(max = 254) String email,
			@NotBlank(message = "Informe a senha.") @Size(max = 128) String password) {
	}

	public record MeResponse(Long id, String email, String fullName, User.Role role) {
	}

	public record CsrfResponse(@Schema(description = "Envie no header X-XSRF-TOKEN") String token, String headerName) {
	}

	public record ForgotPasswordRequest(
			@NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email) {
	}

	public record ResetPasswordRequest(
			@NotBlank(message = "Link inválido.") @Size(max = 100) String token,
			@NotBlank(message = "Informe a nova senha.") @Size(max = 128) String newPassword) {
	}

	public record MessageResponse(String message) {
	}

	public record ProfileResponse(String email, String fullName, String phone, boolean marketingOptIn) {
	}

	public record ProfileRequest(
			@NotBlank(message = "Informe seu nome.") @Size(min = 3, max = 120, message = "Entre 3 e 120 caracteres.")
			String fullName,
			@Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Telefone com DDD, só números (10 a 15 dígitos).")
			String phone,
			Boolean marketingOptIn) {
	}

	public record ChangePasswordRequest(
			@NotBlank(message = "Informe a senha atual.") @Size(max = 128) String currentPassword,
			@NotBlank(message = "Informe a nova senha.") @Size(max = 128) String newPassword) {
	}

	public record AddressRequest(
			@NotBlank(message = "Dê um nome ao endereço (ex.: Casa).") @Size(max = 40) String label,
			@NotBlank(message = "Informe o destinatário.") @Size(min = 3, max = 120) String recipientName,
			@NotBlank(message = "Informe o CEP.") @Pattern(regexp = "^[0-9]{5}-?[0-9]{3}$", message = "CEP inválido.")
			String postalCode,
			@NotBlank(message = "Informe a rua.") @Size(max = 160) String street,
			@NotBlank(message = "Informe o número (ou S/N).") @Size(max = 20) String number,
			@Size(max = 80) String complement,
			@NotBlank(message = "Informe o bairro.") @Size(max = 80) String district,
			@NotBlank(message = "Informe a cidade.") @Size(max = 80) String city,
			@NotBlank(message = "Informe a UF.")
			@Pattern(regexp = "^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$",
					message = "UF inválida.") String state,
			Boolean makeDefault) {
	}

	public record AddressResponse(Long id, String label, String recipientName, String postalCode, String street,
			String number, String complement, String district, String city, String state, boolean isDefault) {
	}

}
