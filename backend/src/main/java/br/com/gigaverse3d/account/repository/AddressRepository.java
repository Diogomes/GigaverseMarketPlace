package br.com.gigaverse3d.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gigaverse3d.account.domain.Address;

/** Toda consulta filtra pelo dono (user_id): evita acesso a endereços de outro cliente (IDOR). */
public interface AddressRepository extends JpaRepository<Address, Long> {

	List<Address> findByUserIdAndDeletedAtIsNullOrderByDefaultAddressDescIdAsc(Long userId);

	Optional<Address> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

	long countByUserIdAndDeletedAtIsNull(Long userId);

}
