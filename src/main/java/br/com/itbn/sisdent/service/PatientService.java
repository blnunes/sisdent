package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.filter.PatientFilter;
import br.com.itbn.sisdent.filter.PatientSpecifications;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.repository.PatientRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private static final SortDefinition SORT_DEFINITION = new SortDefinition(
            "id", java.util.Set.of("id", "name", "identificationNumber", "birthDate", "gender", "active"));

    private final PatientRepository patientRepository;
    private final AddressService addressService;
    private final SpecialityService specialityService;
    private final CountryService countryService;
    private final PageableFactory pageableFactory;

    public PatientService(
            PatientRepository patientRepository,
            AddressService addressService,
            SpecialityService specialityService,
            CountryService countryService,
            PageableFactory pageableFactory) {
        this.patientRepository = patientRepository;
        this.addressService = addressService;
        this.specialityService = specialityService;
        this.countryService = countryService;
        this.pageableFactory = pageableFactory;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return patientRepository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> findPage(PageQuery query, PatientFilter filter) {
        return PageResponse.from(
                patientRepository.findAll(PatientSpecifications.matching(filter), pageableFactory.create(query, SORT_DEFINITION)),
                ResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<FilterOptionResponse> findFilterOptions(String field, String query) {
        Pageable limit = PageRequest.of(0, 10);
        String term = query == null ? "" : query.trim();
        return switch (field) {
            case "name" -> patientRepository.findNameSuggestions(term, limit).stream().map(value -> new FilterOptionResponse(value, value)).toList();
            case "taxId" -> patientRepository.findTaxIdSuggestions(term, limit).stream().map(value -> new FilterOptionResponse(value, value)).toList();
            case "identificationNumber" -> patientRepository.findIdentificationNumberSuggestions(term, limit).stream().map(value -> new FilterOptionResponse(value, value)).toList();
            case "nationalityCode" -> patientRepository.findNationalitySuggestions(term, limit).stream().map(row -> new FilterOptionResponse((String) row[0], row[1] + " (" + row[0] + ")")).toList();
            case "addressId" -> patientRepository.findAddressSuggestions(term, limit).stream().map(row -> new FilterOptionResponse(String.valueOf(row[0]), row[1] + " · " + row[2] + " · " + row[3])).toList();
            case "specialityId" -> patientRepository.findSpecialitySuggestions(term, limit).stream().map(row -> new FilterOptionResponse(String.valueOf(row[0]), (String) row[1])).toList();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported filter field");
        };
    }

    @Transactional(readOnly = true)
    public Optional<PatientResponse> findById(Long id) {
        return patientRepository.findById(id)
                .map(ResponseMapper::toResponse);
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        Patient patient = newPatient(request);
        return ResponseMapper.toResponse(patientRepository.saveAndFlush(patient));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Patient source = newPatient(request);
        patient.update(source.getName(), source.getBirthDate(), source.isActive(), source.getGender(), source.getTaxId(),
                source.getIdentificationType(), source.getIdentificationNumber(), source.getDocumentIssuerCountry(),
                source.getNationality(), source.getAddress(), source.getSpecialities());
        return ResponseMapper.toResponse(patientRepository.saveAndFlush(patient));
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!patient.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inactive patients cannot be deleted");
        }
        patient.deactivate();
        patientRepository.save(patient);
    }

    private Patient newPatient(PatientRequest request) {
        return new Patient(request.name(), request.birthDate(), request.active(), request.gender(),
                normalizeNullable(request.taxId()),
                request.identificationType(), IdentificationNumbers.normalize(request.identificationNumber()),
                countryService.requireByCode(request.documentIssuerCountryCode()),
                countryService.requireByCode(request.nationalityCode()),
                addressService.createPatientAddress(request.address()),
                specialityService.findAllByIds(request.specialityIds()));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : IdentificationNumbers.normalize(value);
    }
}
