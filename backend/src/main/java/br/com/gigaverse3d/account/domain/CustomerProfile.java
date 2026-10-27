package br.com.gigaverse3d.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.gigaverse3d.common.domain.TimestampedEntity;

/** Dados complementares do cliente (mínimos necessários). */
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends TimestampedEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(length = 20)
	private String phone;

	protected CustomerProfile() {
	}

	public CustomerProfile(Long userId) {
		this.userId = userId;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Long getUserId() {
		return userId;
	}

	public String getPhone() {
		return phone;
	}

}
