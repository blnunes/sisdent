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

@Service
public class AddressService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("street", java.util.Set.of("id", "street", "district", "postalCode"));

    private final AddressRepository addressRepository;
    private final AdministrativeDivisionService administrativeDivisionService;
    private final CountryService countryService;
    private final PageableFactory pageableFactory;

    public AddressService(
            AddressRepository addressRepository,
            AdministrativeDivisionService administrativeDivisionService,
            CountryService countryService,
            PageableFactory pageableFactory) {
        this.addressRepository = addressRepository;
        this.administrativeDivisionService = administrativeDivisionService;
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
    public List<AddressResponse> findByPostalCode(String countryCode, String postalCode) {
        return addressRepository.findAllByCountry_CodeAndPostalCodeOrderByStreet(countryCode, postalCode)
                .stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> suggestByPostalCode(String countryCode, String query) {
        if (countryCode == null || !countryCode.matches("[A-Za-z]{2}") || query == null || query.trim().length() < 2) {
            return List.of();
        }
        return addressRepository.findTop10ByCountry_CodeAndPostalCodeStartingWithOrderByPostalCodeAscStreetAsc(
                        countryCode.trim().toUpperCase(java.util.Locale.ROOT), query.trim())
                .stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(AddressRequest request) {
        return ResponseMapper.toResponse(addressRepository.saveAndFlush(newAddress(request)));
    }

    @Transactional
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = addressRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Address source = newAddress(request);
        address.update(source.getStreet(), source.getDistrict(), source.getCity(), source.getAdditionalInfo(),
                source.getBlock(), source.getPostalCode(), source.getAdministrativeDivision(), source.getCountry());
        return ResponseMapper.toResponse(addressRepository.saveAndFlush(address));
    }

    @Transactional
    public void delete(Long id) {
        if (!addressRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        addressRepository.deleteById(id);
    }

    Address resolvePatientAddress(Long addressId, AddressRequest request) {
        if (addressId != null && request != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide either addressId or address, not both");
        }
        if (addressId != null) {
            return addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An address or addressId is required");
        }
        Address candidate = newAddress(request);
        return addressRepository.findAllByCountry_CodeAndPostalCodeOrderByStreet(
                        candidate.getCountry().getCode(), candidate.getPostalCode())
                .stream()
                .filter(address -> sameAddress(address, candidate))
                .findFirst()
                .orElseGet(() -> addressRepository.save(candidate));
    }

    private boolean sameAddress(Address left, Address right) {
        return java.util.Objects.equals(left.getStreet(), right.getStreet())
                && java.util.Objects.equals(left.getDistrict(), right.getDistrict())
                && java.util.Objects.equals(left.getCity(), right.getCity())
                && java.util.Objects.equals(left.getAdditionalInfo(), right.getAdditionalInfo())
                && java.util.Objects.equals(left.getBlock(), right.getBlock())
                && java.util.Objects.equals(left.getPostalCode(), right.getPostalCode())
                && java.util.Objects.equals(
                        left.getAdministrativeDivision() == null ? null : left.getAdministrativeDivision().getId(),
                        right.getAdministrativeDivision() == null ? null : right.getAdministrativeDivision().getId());
    }

    private Address newAddress(AddressRequest request) {
        br.com.itbn.sisdent.model.Country country = countryService.requireByCode(request.countryCode());
        return new Address(
                request.street().trim(),
                normalizeNullable(request.district()),
                request.city().trim(),
                normalizeNullable(request.additionalInfo()),
                normalizeNullable(request.block()),
                normalizeNullable(request.postalCode()),
                administrativeDivisionService.findOrCreate(request.administrativeDivision(), country),
                country);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
