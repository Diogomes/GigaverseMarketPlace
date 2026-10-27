package br.com.gigaverse3d.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Snapshot do endereço de entrega no momento da compra (não acompanha edições do cadastro). */
@Embeddable
public record ShippingAddress(
		@Column(name = "ship_recipient_name", length = 120) String recipientName,
		@Column(name = "ship_postal_code", length = 8) String postalCode,
		@Column(name = "ship_street", length = 160) String street,
		@Column(name = "ship_number", length = 20) String number,
		@Column(name = "ship_complement", length = 80) String complement,
		@Column(name = "ship_district", length = 80) String district,
		@Column(name = "ship_city", length = 80) String city,
		@Column(name = "ship_state", length = 2) String state) {
}
