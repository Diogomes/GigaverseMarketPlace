package br.com.gigaverse3d.account.security;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.gigaverse3d.account.domain.User;

/** Principal guardado na sessão. O hash da senha é apagado após a autenticação. */
public final class AccountUserDetails implements UserDetails, CredentialsContainer {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long id;

	private final String email;

	private String passwordHash;

	private final String fullName;

	private final User.Role role;

	private final boolean enabled;

	private final boolean nonLocked;

	private final String securityStamp;

	private AccountUserDetails(Long id, String email, String passwordHash, String fullName, User.Role role,
			boolean enabled, boolean nonLocked, String securityStamp) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.role = role;
		this.enabled = enabled;
		this.nonLocked = nonLocked;
		this.securityStamp = securityStamp;
	}

	public static AccountUserDetails of(User user, Instant now) {
		return new AccountUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(), user.getFullName(),
				user.getRole(), user.getStatus() == User.Status.ACTIVE && user.getDeletedAt() == null,
				!user.isLocked(now), user.getSecurityStamp());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isAccountNonLocked() {
		return nonLocked;
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public User.Role getRole() {
		return role;
	}

	public String getSecurityStamp() {
		return securityStamp;
	}

	public boolean isStaff() {
		return role == User.Role.ADMIN || role == User.Role.OPERATOR;
	}

}
