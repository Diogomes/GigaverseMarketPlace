package br.com.gigaverse3d.account.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gigaverse3d.account.api.AccountDtos.AddressRequest;
import br.com.gigaverse3d.account.api.AccountDtos.AddressResponse;
import br.com.gigaverse3d.account.domain.Address;
import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.account.repository.AddressRepository;
import br.com.gigaverse3d.account.repository.UserRepository;
import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;

/** Endereços do usuário logado. Endereço de outro usuário é tratado como inexistente (404). */
@Service
@Transactional
public class AddressService {

	public static final int MAX_ADDRESSES = 10;

	private final AddressRepository addresses;

	private final UserRepository users;

	private final Clock clock;

	public AddressService(AddressRepository addresses, UserRepository users, Clock clock) {
		this.addresses = addresses;
		this.users = users;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<AddressResponse> list(Long userId) {
		return active(userId).stream().map(AddressService::toResponse).toList();
	}

	public AddressResponse create(Long userId, AddressRequest req) {
		if (addresses.countByUserIdAndDeletedAtIsNull(userId) >= MAX_ADDRESSES) {
			throw new ConflictException("Limite de " + MAX_ADDRESSES + " endereços atingido. Remova um para adicionar outro.");
		}
		User user = users.getReferenceById(userId);
		Address address = new Address(user, req.label().strip(), req.recipientName().strip(), digits(req.postalCode()),
				req.street().strip(), req.number().strip(), blankToNull(req.complement()), req.district().strip(),
				req.city().strip(), req.state());
		boolean first = addresses.countByUserIdAndDeletedAtIsNull(userId) == 0;
		addresses.save(address);
		if (first || Boolean.TRUE.equals(req.makeDefault())) {
			setDefault(userId, address);
		}
		return toResponse(address);
	}

	public AddressResponse update(Long userId, Long addressId, AddressRequest req) {
		Address address = find(userId, addressId);
		address.update(req.label().strip(), req.recipientName().strip(), digits(req.postalCode()), req.street().strip(),
				req.number().strip(), blankToNull(req.complement()), req.district().strip(), req.city().strip(),
				req.state());
		if (Boolean.TRUE.equals(req.makeDefault())) {
			setDefault(userId, address);
		}
		return toResponse(address);
	}

	public AddressResponse makeDefault(Long userId, Long addressId) {
		Address address = find(userId, addressId);
		setDefault(userId, address);
		return toResponse(address);
	}

	public void delete(Long userId, Long addressId) {
		Address address = find(userId, addressId);
		boolean wasDefault = address.isDefaultAddress();
		address.softDelete(clock.instant());
		addresses.flush();
		if (wasDefault) {
			active(userId).stream().findFirst().ifPresent(next -> setDefault(userId, next));
		}
	}

	private Address find(Long userId, Long addressId) {
		return addresses.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId)
			.orElseThrow(() -> new NotFoundException("Endereço não encontrado."));
	}

	private List<Address> active(Long userId) {
		return addresses.findByUserIdAndDeletedAtIsNullOrderByDefaultAddressDescIdAsc(userId);
	}

	/** Desmarca o atual antes de marcar o novo (índice único parcial no banco). */
	private void setDefault(Long userId, Address target) {
		for (Address a : active(userId)) {
			if (a.isDefaultAddress() && !a.getId().equals(target.getId())) {
				a.markAsDefault(false);
			}
		}
		addresses.flush();
		target.markAsDefault(true);
		addresses.flush();
	}

	private static String digits(String postalCode) {
		return postalCode.replace("-", "");
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	static AddressResponse toResponse(Address a) {
		return new AddressResponse(a.getId(), a.getLabel(), a.getRecipientName(), a.getPostalCode(), a.getStreet(),
				a.getNumber(), a.getComplement(), a.getDistrict(), a.getCity(), a.getState(), a.isDefaultAddress());
	}

}
