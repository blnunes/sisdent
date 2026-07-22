package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.repository.AddressRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final StateService stateService;

    public AddressService(AddressRepository addressRepository, StateService stateService) {
        this.addressRepository = addressRepository;
        this.stateService = stateService;
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

    Address findOrCreate(AddressRequest request) {
        return addressRepository.findByPostalCode(request.postalCode())
                .orElseGet(() -> addressRepository.save(new Address(
                        request.street(),
                        request.district(),
                        request.additionalInfo(),
                        request.block(),
                        request.postalCode(),
                        stateService.findOrCreate(request.state()))));
    }
}
