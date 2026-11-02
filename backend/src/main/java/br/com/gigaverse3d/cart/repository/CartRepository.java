package br.com.gigaverse3d.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.gigaverse3d.cart.domain.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

	Optional<Cart> findByUserId(Long userId);

	/**
	 * Cria o carrinho da conta de forma atômica. Requisições simultâneas (ex.: junção do carrinho do navegador e
	 * leitura logo após o login) não colidem na restrição única de {@code user_id}: a segunda espera a primeira e
	 * não faz nada.
	 */
	@Modifying
	@Query(value = "INSERT INTO carts (user_id) VALUES (:userId) ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
	int insertIfAbsent(@Param("userId") Long userId);

}
