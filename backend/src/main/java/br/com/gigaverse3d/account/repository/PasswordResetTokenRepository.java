package br.com.gigaverse3d.account.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.account.domain.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	@Query("SELECT t FROM PasswordResetToken t WHERE t.userId = :userId AND t.usedAt IS NULL AND t.expiresAt > :now")
	List<PasswordResetToken> findUsableByUser(@Param("userId") Long userId, @Param("now") Instant now);

}
