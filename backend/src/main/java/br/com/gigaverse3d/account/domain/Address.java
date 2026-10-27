package br.com.gigaverse3d.account.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.gigaverse3d.common.domain.TimestampedEntity;

@Entity
@Table(name = "addresses")
public class Address extends TimestampedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 40)
	private String label;

	@Column(name = "recipient_name", nullable = false, length = 120)
	private String recipientName;

	/** CEP com 8 dígitos, sem máscara. */
	@Column(name = "postal_code", nullable = false, length = 8)
	private String postalCode;

	@Column(nullable = false, length = 160)
	private String street;

	@Column(nullable = false, length = 20)
	private String number;

	@Column(length = 80)
	private String complement;

	@Column(nullable = false, length = 80)
	private String district;

	@Column(nullable = false, length = 80)
	private String city;

	@Column(nullable = false, length = 2)
	private String state;

	@Column(name = "is_default", nullable = false)
	private boolean defaultAddress;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected Address() {
	}

	public Address(User user, String label, String recipientName, String postalCode, String street, String number,
			String complement, String district, String city, String state) {
		this.user = user;
		this.label = label;
		this.recipientName = recipientName;
		this.postalCode = postalCode;
		this.street = street;
		this.number = number;
		this.complement = complement;
		this.district = district;
		this.city = city;
		this.state = state;
	}

	public void markAsDefault(boolean value) {
		this.defaultAddress = value;
	}

	public void update(String label, String recipientName, String postalCode, String street, String number,
			String complement, String district, String city, String state) {
		this.label = label;
		this.recipientName = recipientName;
		this.postalCode = postalCode;
		this.street = street;
		this.number = number;
		this.complement = complement;
		this.district = district;
		this.city = city;
		this.state = state;
	}

	/** Exclusão lógica: pedidos antigos guardam snapshot próprio, então nada depende deste registro. */
	public void softDelete(Instant now) {
		this.deletedAt = now;
		this.defaultAddress = false;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getLabel() {
		return label;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getStreet() {
		return street;
	}

	public String getNumber() {
		return number;
	}

	public String getComplement() {
		return complement;
	}

	public String getDistrict() {
		return district;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public boolean isDefaultAddress() {
		return defaultAddress;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

}
