package br.com.gigaverse3d.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gigaverse3d.account.domain.CustomerProfile;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

}
