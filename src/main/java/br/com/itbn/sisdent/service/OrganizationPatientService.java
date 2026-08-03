package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ExactPatientMatchRequest;
import br.com.itbn.sisdent.dto.ExactPatientMatchResponse;
import br.com.itbn.sisdent.dto.PatientLinkRequest;
import br.com.itbn.sisdent.dto.PatientLinkResponse;
import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.filter.PatientFilter;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class OrganizationPatientService {
    private final PatientRepository patientRepository;
    private final PatientOrganizationLinkRepository linkRepository;
    private final OrganizationRepository organizationRepository;
    private final ScopeAuthorizationService authorization;
    private final PatientService patientService;

    public OrganizationPatientService(PatientRepository patientRepository,
            PatientOrganizationLinkRepository linkRepository, OrganizationRepository organizationRepository,
            ScopeAuthorizationService authorization, PatientService patientService) {
        this.patientRepository = patientRepository; this.linkRepository = linkRepository;
        this.organizationRepository = organizationRepository; this.authorization = authorization;
        this.patientService = patientService;
    }

    @Transactional(readOnly = true)
    public List<FilterOptionResponse> filterOptions(UUID organizationId, UUID clinicUnitId,
            String field, String query) {
        authorization.requireRead(organizationId, clinicUnitId);
        String term = query == null ? "" : query.strip().toLowerCase();
        return linkedPatients(organizationId, clinicUnitId).stream()
                .map(PatientOrganizationLink::getPatient)
                .distinct()
                .flatMap(patient -> filterOption(field, patient).stream())
                .filter(option -> option.label().toLowerCase().contains(term)
                        || option.value().toLowerCase().contains(term))
                .distinct()
                .sorted(java.util.Comparator.comparing(FilterOptionResponse::label))
                .limit(10)
                .toList();
    }

    @Transactional
    public PatientResponse create(UUID organizationId, UUID clinicUnitId, PatientRequest request) {
        authorization.requireWrite(organizationId, clinicUnitId);
        Organization organization = organizationRepository.findByGlobalId(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        ClinicUnit clinic = clinicUnitId == null ? null
                : authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        PatientResponse response = patientService.create(request);
        Patient patient = patientRepository.findByGlobalId(response.globalId()).orElseThrow();
        linkRepository.save(new PatientOrganizationLink(
                patient, organization, clinic, br.com.itbn.sisdent.model.PatientLinkBasis.INTAKE));
        return response;
    }

    @Transactional
    public PatientResponse update(UUID organizationId, UUID clinicUnitId, UUID patientId, PatientRequest request) {
        authorization.requireWrite(organizationId, clinicUnitId);
        Patient patient = requireLinkedPatient(organizationId, clinicUnitId, patientId);
        return patientService.update(patient.getId(), request);
    }

    @Transactional
    public void delete(UUID organizationId, UUID clinicUnitId, UUID patientId) {
        authorization.requireWrite(organizationId, clinicUnitId);
        Patient patient = requireLinkedPatient(organizationId, clinicUnitId, patientId);
        patientService.delete(patient.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> search(UUID organizationId, UUID clinicUnitId, PatientFilter filter) {
        authorization.requireRead(organizationId, clinicUnitId);
        List<PatientResponse> content = linkedPatients(organizationId, clinicUnitId).stream()
                .map(PatientOrganizationLink::getPatient).distinct()
                .filter(matches(filter))
                .sorted(java.util.Comparator.comparing(Patient::getName, String.CASE_INSENSITIVE_ORDER))
                .map(ResponseMapper::toResponse).toList();
        return new PageResponse<>(content, 0, content.size(), content.size(), content.isEmpty() ? 0 : 1);
    }

    private List<PatientOrganizationLink> linkedPatients(UUID organizationId, UUID clinicUnitId) {
        if (clinicUnitId != null) {
            authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        }
        return clinicUnitId == null
                ? linkRepository.findAllByOrganization_GlobalId(organizationId)
                : linkRepository.searchLinkedPatientsInClinic(organizationId, clinicUnitId, "");
    }

    private Predicate<Patient> matches(PatientFilter filter) {
        return patient -> matchesEquals(filter.id(), patient.getId())
                && contains(patient.getName(), filter.normalized(filter.name()))
                && matchesEquals(filter.birthDate(), patient.getBirthDate())
                && matchesEquals(filter.active(), patient.isActive())
                && matchesEquals(filter.gender(), patient.getGender())
                && contains(patient.getTaxId(), filter.normalized(filter.taxId()))
                && matchesEquals(filter.identificationType(), patient.getIdentificationType())
                && contains(patient.getIdentificationNumber(), filter.normalized(filter.identificationNumber()))
                && (filter.nationalityCode() == null || filter.nationalityCode().isBlank()
                        || patient.getNationality().getCode().equalsIgnoreCase(filter.nationalityCode().strip()))
                && matchesEquals(filter.addressId(), patient.getAddress().getId())
                && (filter.specialityId() == null || patient.getSpecialities().stream()
                        .anyMatch(speciality -> speciality.getId().equals(filter.specialityId())));
    }

    private <T> boolean matchesEquals(T expected, T actual) {
        return expected == null || expected.equals(actual);
    }

    private boolean contains(String value, String expected) {
        return expected == null || (value != null && value.toLowerCase().contains(expected));
    }

    private List<FilterOptionResponse> filterOption(String field, Patient patient) {
        return switch (field) {
            case "name" -> List.of(new FilterOptionResponse(patient.getName(), patient.getName()));
            case "taxId" -> patient.getTaxId() == null ? List.of()
                    : List.of(new FilterOptionResponse(patient.getTaxId(), patient.getTaxId()));
            case "nationalityCode" -> List.of(new FilterOptionResponse(patient.getNationality().getCode(),
                    patient.getNationality().getName() + " (" + patient.getNationality().getCode() + ")"));
            case "addressId" -> List.of(new FilterOptionResponse(String.valueOf(patient.getAddress().getId()),
                    patient.getAddress().getStreet() + " · " + patient.getAddress().getCity() + " · "
                            + patient.getAddress().getCountry().getCode()));
            case "specialityId" -> patient.getSpecialities().stream()
                    .map(speciality -> new FilterOptionResponse(String.valueOf(speciality.getId()), speciality.getName()))
                    .toList();
            default -> List.of();
        };
    }

    @Transactional(readOnly = true)
    public ExactPatientMatchResponse exactMatch(UUID organizationId, UUID clinicUnitId,
            ExactPatientMatchRequest request) {
        authorization.requireWrite(organizationId, clinicUnitId);
        if (clinicUnitId != null) {
            authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        }
        boolean exists = findExact(request.documentType(), request.issuerCountryCode(),
                request.documentNumber(), request.birthDate()) != null;
        return new ExactPatientMatchResponse(exists);
    }

    @Transactional
    public PatientLinkResponse link(UUID organizationId, PatientLinkRequest request) {
        authorization.requireWrite(organizationId, request.clinicUnitId());
        Organization organization = organizationRepository.findByGlobalId(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        ClinicUnit clinic = request.clinicUnitId() == null ? null
                : authorization.requireClinicInOrganization(organizationId, request.clinicUnitId());
        Patient patient = findExact(request.documentType(), request.issuerCountryCode(),
                request.documentNumber(), request.birthDate());
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No exact patient match");
        }
        boolean duplicate = clinic == null
                ? linkRepository.existsByPatient_IdAndOrganization_IdAndClinicUnitIsNull(
                        patient.getId(), organization.getId())
                : linkRepository.existsByPatient_IdAndOrganization_IdAndClinicUnit_Id(
                        patient.getId(), organization.getId(), clinic.getId());
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The patient is already linked to this organization scope");
        }
        PatientOrganizationLink link = linkRepository.saveAndFlush(
                new PatientOrganizationLink(patient, organization, clinic, request.operationalBasis()));
        return new PatientLinkResponse(link.getGlobalId(), patient.getGlobalId(), organizationId,
                clinic == null ? null : clinic.getGlobalId(), link.getOperationalBasis(),
                link.getCreatedBy(), link.getCreatedAt());
    }

    private Patient findExact(br.com.itbn.sisdent.model.DocumentType type, String issuerCountry,
            String documentNumber, java.time.LocalDate birthDate) {
        return patientRepository.findByIdentificationTypeAndDocumentIssuerCountry_CodeAndIdentificationNumber(
                        type, issuerCountry.strip().toUpperCase(),
                        IdentificationNumbers.normalize(documentNumber))
                .filter(patient -> patient.getBirthDate().equals(birthDate))
                .orElse(null);
    }

    private Patient requireLinkedPatient(UUID organizationId, UUID clinicUnitId, UUID patientId) {
        PatientOrganizationLink link = linkRepository
                .findFirstByPatient_GlobalIdAndOrganization_GlobalId(patientId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (clinicUnitId != null && (link.getClinicUnit() == null
                || !link.getClinicUnit().getGlobalId().equals(clinicUnitId))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Patient is outside the clinic-unit scope");
        }
        return link.getPatient();
    }
}
