package br.com.gigaverse3d.account.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gigaverse3d.account.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailAndDeletedAtIsNull(String email);

	boolean existsByEmailAndDeletedAtIsNull(String email);

	boolean existsByRoleAndDeletedAtIsNull(User.Role role);

	List<User> findByRoleInAndDeletedAtIsNullOrderByFullNameAsc(Collection<User.Role> roles);

}
