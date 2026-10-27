package br.com.gigaverse3d.account.service;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.api.AccountDtos.ChangePasswordRequest;
import br.com.gigaverse3d.account.api.AccountDtos.ProfileRequest;
import br.com.gigaverse3d.account.api.AccountDtos.ProfileResponse;
import br.com.gigaverse3d.account.domain.CustomerProfile;
import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.domain.UserConsent;
import br.com.gigaverse3d.account.repository.CustomerProfileRepository;
import br.com.gigaverse3d.account.repository.UserConsentRepository;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.AuthProperties;
import br.com.gigaverse3d.account.security.PasswordPolicy;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.UnauthorizedException;

/** Operações do próprio usuário. O id vem SEMPRE da sessão, nunca do corpo da requisição. */
@Service
@Transactional
public class AccountService {

	private final UserRepository users;

	private final CustomerProfileRepository profiles;

	private final UserConsentRepository consents;

	private final PasswordEncoder passwordEncoder;

	private final PasswordPolicy passwordPolicy;

	private final AuthProperties properties;

	private final AuditService audit;

	public AccountService(UserRepository users, CustomerProfileRepository profiles, UserConsentRepository consents,
			PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy, AuthProperties properties,
			AuditService audit) {
		this.users = users;
		this.profiles = profiles;
		this.consents = consents;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.properties = properties;
		this.audit = audit;
	}

	public User user(Long userId) {
		return users.findById(userId).orElseThrow(() -> new UnauthorizedException("Faça login para continuar."));
	}

	@Transactional(readOnly = true)
	public ProfileResponse profile(Long userId) {
		User user = user(userId);
		String phone = profiles.findById(userId).map(CustomerProfile::getPhone).orElse(null);
		return new ProfileResponse(user.getEmail(), user.getFullName(), phone, marketingOptIn(userId));
	}

	public ProfileResponse updateProfile(Long userId, ProfileRequest req) {
		User user = user(userId);
		user.updateName(req.fullName());
		CustomerProfile profile = profiles.findById(userId).orElseGet(() -> profiles.save(new CustomerProfile(userId)));
		profile.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone());
		if (req.marketingOptIn() != null && req.marketingOptIn() != marketingOptIn(userId)) {
			consents.save(new UserConsent(userId, UserConsent.Type.MARKETING_EMAIL, req.marketingOptIn(),
					properties.legalDocumentsVersion()));
			audit.recordAs(userId, "CONSENT_CHANGED", "user", userId, Map.of("marketingEmail", req.marketingOptIn()));
		}
		return new ProfileResponse(user.getEmail(), user.getFullName(), profile.getPhone(), marketingOptIn(userId));
	}

	/** Exige a senha atual; renova o carimbo de segurança (as outras sessões caem). */
	public User changePassword(Long userId, ChangePasswordRequest req) {
		User user = user(userId);
		if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
			throw new FieldValidationException("currentPassword", "Senha atual incorreta.");
		}
		passwordPolicy.validate(req.newPassword(), user.getEmail(), "newPassword");
		if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
			throw new FieldValidationException("newPassword", "A nova senha deve ser diferente da atual.");
		}
		user.changePassword(passwordEncoder.encode(req.newPassword()));
		users.flush();
		audit.recordAs(userId, "PASSWORD_CHANGED", "user", userId, Map.of());
		return user;
	}

	private boolean marketingOptIn(Long userId) {
		return consents.findFirstByUserIdAndTypeOrderByIdDesc(userId, UserConsent.Type.MARKETING_EMAIL)
			.map(UserConsent::isGranted)
			.orElse(false);
	}

}
