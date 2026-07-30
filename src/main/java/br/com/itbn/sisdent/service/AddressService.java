package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.repository.AddressRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("street", java.util.Set.of("id", "street", "district", "postalCode"));

    private final AddressRepository addressRepository;
    private final StateService stateService;
    private final CountryService countryService;
    private final PageableFactory pageableFactory;

    public AddressService(
            AddressRepository addressRepository,
            StateService stateService,
            CountryService countryService,
            PageableFactory pageableFactory) {
        this.addressRepository = addressRepository;
        this.stateService = stateService;
        this.countryService = countryService;
        this.pageableFactory = pageableFactory;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findAll() {
        return addressRepository.findAll(Sort.by("street")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AddressResponse> findPage(PageQuery query) {
        return PageResponse.from(addressRepository.findAll(pageableFactory.create(query, SORT_DEFINITION)), ResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<AddressResponse> findByPostalCode(String postalCode) {
        return addressRepository.findByPostalCode(postalCode)
                .map(ResponseMapper::toResponse);
    }

    @Transactional
    public AddressResponse create(AddressRequest request) {
        return ResponseMapper.toResponse(addressRepository.saveAndFlush(newAddress(request)));
    }

    @Transactional
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = addressRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Address source = newAddress(request);
        address.update(source.getStreet(), source.getDistrict(), source.getAdditionalInfo(), source.getBlock(),
                source.getPostalCode(), source.getState(), source.getCountry());
        return ResponseMapper.toResponse(addressRepository.saveAndFlush(address));
    }

    @Transactional
    public void delete(Long id) {
        if (!addressRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        addressRepository.deleteById(id);
    }

    Address findOrCreate(AddressRequest request) {
        return addressRepository.findByPostalCode(request.postalCode())
                .orElseGet(() -> addressRepository.save(newAddress(request)));
    }

    private Address newAddress(AddressRequest request) {
        return new Address(request.street(), request.district(), request.additionalInfo(), request.block(), request.postalCode(),
                stateService.findOrCreate(request.state()), countryService.requireByCode(request.countryCode()));
    }
}
