package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ExactPatientMatchRequest;
import br.com.itbn.sisdent.dto.ExactPatientMatchResponse;
import br.com.itbn.sisdent.dto.PatientLinkRequest;
import br.com.itbn.sisdent.dto.PatientLinkResponse;
import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.filter.PatientFilter;
import br.com.itbn.sisdent.filter.PatientLinkSpecifications;
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

@Service
public class OrganizationPatientService {
    private final PatientRepository patientRepository;
    private final PatientOrganizationLinkRepository linkRepository;
    private final OrganizationRepository organizationRepository;
    private final ScopeAuthorizationService authorization;
    private final PatientService patientService;
    private final PageableFactory pageableFactory;
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("patient.name",
            java.util.Set.of("patient.name", "patient.birthDate", "patient.active", "patient.gender"));

    public OrganizationPatientService(PatientRepository patientRepository,
            PatientOrganizationLinkRepository linkRepository, OrganizationRepository organizationRepository,
            ScopeAuthorizationService authorization, PatientService patientService, PageableFactory pageableFactory) {
        this.patientRepository = patientRepository; this.linkRepository = linkRepository;
        this.organizationRepository = organizationRepository; this.authorization = authorization;
        this.patientService = patientService;
        this.pageableFactory = pageableFactory;
    }

    @Transactional(readOnly = true)
    public List<FilterOptionResponse> filterOptions(UUID organizationId, UUID clinicUnitId,
            String field, String query) {
        if (!"name".equals(field)) {
            return List.of();
        }
        return search(organizationId, clinicUnitId, new PageQuery(0, 10, "patient.name", "asc"),
                new PatientFilter(null, query, null, null, null, null, null, null, null, null, null)).content().stream()
                .limit(10).map(patient -> new FilterOptionResponse(patient.name(), patient.name())).toList();
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
        if (linkRepository.existsByPatient_IdAndOrganization_GlobalIdNotAndActiveTrue(patient.getId(), organizationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A globally shared patient cannot be changed from an organization workspace");
        }
        if (clinicUnitId != null && linkRepository
                .findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(patientId, organizationId).stream()
                .anyMatch(link -> link.getClinicUnit() == null
                        || !clinicUnitId.equals(link.getClinicUnit().getGlobalId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A patient shared with another clinic unit cannot be changed from a clinic workspace");
        }
        return patientService.update(patient.getId(), request);
    }

    @Transactional
    public void delete(UUID organizationId, UUID clinicUnitId, UUID patientId) {
        authorization.requireWrite(organizationId, clinicUnitId);
        PatientOrganizationLink link = requireLinkedPatientLink(organizationId, clinicUnitId, patientId);
        link.deactivate();
        linkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> search(UUID organizationId, UUID clinicUnitId, PageQuery query,
            PatientFilter filter) {
        authorization.requireRead(organizationId, clinicUnitId);
        if (clinicUnitId != null) {
            authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        }
        return PageResponse.from(linkRepository.findAll(PatientLinkSpecifications.matching(organizationId, clinicUnitId, filter),
                pageableFactory.create(scopedQuery(query), SORT_DEFINITION)), link -> ResponseMapper.toResponse(link.getPatient()));
    }

    @Transactional(readOnly = true)
    public ExactPatientMatchResponse exactMatch(UUID organizationId, UUID clinicUnitId,
            ExactPatientMatchRequest request) {
        authorization.requireWrite(organizationId, clinicUnitId);
        if (clinicUnitId != null) {
            authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        }
        Patient patient = findExact(request.documentType(), request.issuerCountryCode(),
                request.documentNumber(), request.birthDate());
        boolean exists = patient != null && linkRepository
                .findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(patient.getGlobalId(), organizationId).stream()
                .anyMatch(link -> clinicUnitId == null || (link.getClinicUnit() != null
                        && clinicUnitId.equals(link.getClinicUnit().getGlobalId())));
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
        boolean duplicate = linkRepository.existsByPatient_IdAndOrganization_IdAndActiveTrue(patient.getId(), organization.getId());
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
        return requireLinkedPatientLink(organizationId, clinicUnitId, patientId).getPatient();
    }

    private PatientOrganizationLink requireLinkedPatientLink(UUID organizationId, UUID clinicUnitId, UUID patientId) {
        return linkRepository.findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(patientId, organizationId).stream()
                .filter(link -> clinicUnitId == null || (link.getClinicUnit() != null
                        && link.getClinicUnit().getGlobalId().equals(clinicUnitId)))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private PageQuery scopedQuery(PageQuery query) {
        String sort = switch (query.sort() == null ? "id" : query.sort()) {
            case "id", "name" -> "patient.name";
            case "birthDate" -> "patient.birthDate";
            case "active" -> "patient.active";
            case "gender" -> "patient.gender";
            default -> query.sort();
        };
        return new PageQuery(query.page(), query.size(), sort, query.direction());
    }
}
