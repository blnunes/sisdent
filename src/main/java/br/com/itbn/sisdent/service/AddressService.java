package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.repository.AddressRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findAll() {
        return addressRepository.findAll(Sort.by("street")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AddressResponse> findByPostalCode(String postalCode) {
        return addressRepository.findByPostalCode(postalCode)
                .map(ResponseMapper::toResponse);
    }
}
