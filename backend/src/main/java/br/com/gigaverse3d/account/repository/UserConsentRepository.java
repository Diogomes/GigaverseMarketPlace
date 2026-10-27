package br.com.gigaverse3d.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gigaverse3d.account.domain.UserConsent;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

	/** Estado atual = registro mais recente daquele tipo. */
	Optional<UserConsent> findFirstByUserIdAndTypeOrderByIdDesc(Long userId, UserConsent.Type type);

}
