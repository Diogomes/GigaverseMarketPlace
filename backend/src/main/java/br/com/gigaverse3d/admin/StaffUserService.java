package br.com.gigaverse3d.admin;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.account.security.AccountUserDetails;
import br.com.gigaverse3d.account.security.CurrentUser;
import br.com.gigaverse3d.account.service.PasswordResetService;
import br.com.gigaverse3d.common.audit.AuditService;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;

/**
 * Contas da equipe (OPERATOR/ADMIN). Privilégio mínimo: OPERATOR opera loja e CRM; só ADMIN
 * confirma pagamentos, exclui, exporta, vê auditoria e gerencia a equipe.
 *
 * Proteções: ninguém altera o próprio papel/status; sempre resta ao menos um ADMIN ativo;
 * clientes não viram equipe por aqui (contas separadas); a senha é criada pela própria pessoa.
 */
@Service
public class StaffUserService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final List<User.Role> STAFF = List.of(User.Role.OPERATOR, User.Role.ADMIN);

	public record StaffUserView(Long id, String name, String email, User.Role role, User.Status status,
			Instant lastLoginAt, Instant createdAt) {

		static StaffUserView of(User u) {
			return new StaffUserView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getStatus(),
					u.getLastLoginAt(), u.getCreatedAt());
		}
	}

	public record CreateStaffRequest(
			@NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email,
			@NotBlank(message = "Informe o nome.") @Size(min = 2, max = 120, message = "Entre 2 e 120 caracteres.")
			String fullName,
			@NotNull(message = "Informe o papel.") User.Role role) {
	}

	public record RoleRequest(@NotNull(message = "Informe o papel.") User.Role role) {
	}

	private final UserRepository users;

	private final PasswordEncoder encoder;

	private final PasswordResetService invitations;

	private final AuditService audit;

	private final JdbcTemplate jdbc;

	public StaffUserService(UserRepository users, PasswordEncoder encoder, PasswordResetService invitations,
			AuditService audit, JdbcTemplate jdbc) {
		this.users = users;
		this.encoder = encoder;
		this.invitations = invitations;
		this.audit = audit;
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public List<StaffUserView> list() {
		return users.findByRoleInAndDeletedAtIsNullOrderByFullNameAsc(STAFF).stream().map(StaffUserView::of).toList();
	}

	@Transactional
	public StaffUserView create(CreateStaffRequest req) {
		requireStaffRole(req.role());
		String email = User.normalizeEmail(req.email());
		if (users.existsByEmailAndDeletedAtIsNull(email)) {
			throw new ConflictException("Já existe uma conta com este e-mail. Use outro e-mail para o acesso da equipe.");
		}
		// Senha aleatória descartada: a conta só é usável depois que a pessoa define a própria senha.
		byte[] raw = new byte[32];
		RANDOM.nextBytes(raw);
		User user = users.save(new User(email, encoder.encode(Base64.getEncoder().encodeToString(raw)),
				req.fullName(), req.role()));
		audit.record("STAFF_USER_CREATED", "user", user.getId(), Map.of("role", user.getRole().name()));
		invitations.sendInvitation(user, CurrentUser.require().getFullName());
		return StaffUserView.of(user);
	}

	@Transactional
	public void resendInvitation(Long id) {
		User user = staff(id);
		if (user.getStatus() != User.Status.ACTIVE) {
			throw new ConflictException("Reative a conta antes de reenviar o convite.");
		}
		audit.record("STAFF_INVITATION_SENT", "user", id, Map.of());
		invitations.sendInvitation(user, CurrentUser.require().getFullName());
	}

	@Transactional
	public StaffUserView changeRole(Long id, User.Role role) {
		requireStaffRole(role);
		User user = staff(id);
		notSelf(id, "Você não pode alterar o próprio papel.");
		if (user.getRole() == role) {
			return StaffUserView.of(user);
		}
		if (user.getRole() == User.Role.ADMIN && user.getStatus() == User.Status.ACTIVE) {
			requireAnotherActiveAdmin(id);
		}
		User.Role previous = user.getRole();
		user.changeRole(role);
		audit.record("STAFF_ROLE_CHANGED", "user", id, Map.of("from", previous.name(), "to", role.name()));
		return StaffUserView.of(user);
	}

	@Transactional
	public StaffUserView disable(Long id) {
		User user = staff(id);
		notSelf(id, "Você não pode desativar a própria conta.");
		if (user.getStatus() == User.Status.DISABLED) {
			return StaffUserView.of(user);
		}
		if (user.getRole() == User.Role.ADMIN) {
			requireAnotherActiveAdmin(id);
		}
		user.disable();
		audit.record("STAFF_USER_DISABLED", "user", id, Map.of("role", user.getRole().name()));
		return StaffUserView.of(user);
	}

	@Transactional
	public StaffUserView enable(Long id) {
		User user = staff(id);
		if (user.getStatus() == User.Status.DISABLED) {
			user.enable();
			audit.record("STAFF_USER_ENABLED", "user", id, Map.of("role", user.getRole().name()));
		}
		return StaffUserView.of(user);
	}

	private User staff(Long id) {
		return users.findById(id)
			.filter(u -> u.getDeletedAt() == null && STAFF.contains(u.getRole()))
			.orElseThrow(() -> new NotFoundException("Usuário da equipe não encontrado."));
	}

	private static void requireStaffRole(User.Role role) {
		if (!STAFF.contains(role)) {
			throw new FieldValidationException("role", "Papel deve ser OPERATOR ou ADMIN.");
		}
	}

	private static void notSelf(Long id, String message) {
		AccountUserDetails me = CurrentUser.require();
		if (me.getId().equals(id)) {
			throw new ConflictException(message);
		}
	}

	/** Trava os ADMIN ativos (FOR UPDATE) para que duas remoções simultâneas não zerem a lista. */
	private void requireAnotherActiveAdmin(Long exceptId) {
		List<Long> admins = jdbc.queryForList("""
				SELECT id FROM users WHERE role = 'ADMIN' AND status = 'ACTIVE' AND deleted_at IS NULL FOR UPDATE
				""", Long.class);
		if (admins.stream().noneMatch(a -> !a.equals(exceptId))) {
			throw new ConflictException("É preciso manter ao menos um administrador ativo.");
		}
	}

}
