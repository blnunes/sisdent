package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ExactPatientMatchRequest;
import br.com.itbn.sisdent.dto.PatientLinkRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.PatientLinkBasis;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrganizationPatientServiceTest {
    @Mock PatientRepository patients;
    @Mock PatientOrganizationLinkRepository links;
    @Mock OrganizationRepository organizations;
    @Mock ScopeAuthorizationService authorization;
    @Mock PatientService patientService;
    @Mock PageableFactory pageableFactory;
    @InjectMocks OrganizationPatientService service;

    private final Organization organization = new Organization("Alpha");
    private final ClinicUnit clinic = new ClinicUnit(organization, "Central");
    private final UUID patientId = UUID.randomUUID();
    private final LocalDate birthDate = LocalDate.of(1990, 1, 2);
    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = org.mockito.Mockito.mock(Patient.class);
        lenient().when(patient.getGlobalId()).thenReturn(patientId);
        lenient().when(patient.getBirthDate()).thenReturn(birthDate);
        lenient().when(patient.getId()).thenReturn(10L);
        lenient().when(organizations.findByGlobalId(organization.getGlobalId())).thenReturn(Optional.of(organization));
    }

    @Test
    void returnsNoFilterOptionsForUnsupportedFields() {
        assertThat(service.filterOptions(organization.getGlobalId(), null, "taxId", "123")).isEmpty();
    }

    @Test
    void createsPatientAndOrganizationLinkInTheRequestedClinic() {
        PatientResponse response = response();
        when(patientService.create(any())).thenReturn(response);
        when(patients.findByGlobalId(patientId)).thenReturn(Optional.of(patient));
        when(authorization.requireClinicInOrganization(organization.getGlobalId(), clinic.getGlobalId())).thenReturn(clinic);

        assertThat(service.create(organization.getGlobalId(), clinic.getGlobalId(), null)).isSameAs(response);

        ArgumentCaptor<PatientOrganizationLink> link = ArgumentCaptor.forClass(PatientOrganizationLink.class);
        verify(links).save(link.capture());
        assertThat(link.getValue().getOrganization()).isSameAs(organization);
        assertThat(link.getValue().getClinicUnit()).isSameAs(clinic);
        assertThat(link.getValue().getOperationalBasis()).isEqualTo(PatientLinkBasis.INTAKE);
    }

    @Test
    void refusesUpdatesForPatientsSharedWithAnotherOrganization() {
        when(links.findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(patientId, organization.getGlobalId()))
                .thenReturn(List.of(new PatientOrganizationLink(patient, organization, null, PatientLinkBasis.INTAKE)));
        when(links.existsByPatient_IdAndOrganization_GlobalIdNotAndActiveTrue(10L, organization.getGlobalId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(organization.getGlobalId(), null, patientId, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("globally shared");
    }

    @Test
    void updatesPatientWhenItIsExclusiveToTheOrganizationScope() {
        PatientOrganizationLink link = new PatientOrganizationLink(patient, organization, null, PatientLinkBasis.INTAKE);
        PatientResponse response = response();
        when(links.findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(patientId, organization.getGlobalId()))
                .thenReturn(List.of(link));
        when(links.existsByPatient_IdAndOrganization_GlobalIdNotAndActiveTrue(10L, organization.getGlobalId())).thenReturn(false);
        when(patientService.update(eq(10L), any())).thenReturn(response);

        assertThat(service.update(organization.getGlobalId(), null, patientId, null)).isSameAs(response);
    }

    @Test
    void deactivatesOnlyAnExistingLinkInTheRequestedScope() {
        PatientOrganizationLink link = new PatientOrganizationLink(patient, organization, null, PatientLinkBasis.INTAKE);
        when(links.findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(patientId, organization.getGlobalId()))
                .thenReturn(List.of(link));

        service.delete(organization.getGlobalId(), null, patientId);

        assertThat(link.isActive()).isFalse();
        verify(links).save(link);
    }

    @Test
    void determinesExactMatchForOrganizationAndClinicScopes() {
        ExactPatientMatchRequest request = exactRequest();
        PatientOrganizationLink organizationLink = new PatientOrganizationLink(patient, organization, null, PatientLinkBasis.INTAKE);
        when(patients.findByIdentificationTypeAndDocumentIssuerCountry_CodeAndIdentificationNumber(
                DocumentType.PASSPORT, "PT", "AB123")).thenReturn(Optional.of(patient));
        when(links.findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(patientId, organization.getGlobalId()))
                .thenReturn(List.of(organizationLink));

        assertThat(service.exactMatch(organization.getGlobalId(), null, request).possibleMatchExists()).isTrue();
        when(authorization.requireClinicInOrganization(organization.getGlobalId(), clinic.getGlobalId())).thenReturn(clinic);
        assertThat(service.exactMatch(organization.getGlobalId(), clinic.getGlobalId(), request).possibleMatchExists()).isFalse();
    }

    @Test
    void linksAnExistingUnlinkedPatientAndRejectsMissingOrDuplicatePatients() {
        PatientLinkRequest request = linkRequest(null);
        when(patients.findByIdentificationTypeAndDocumentIssuerCountry_CodeAndIdentificationNumber(
                DocumentType.PASSPORT, "PT", "AB123")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.link(organization.getGlobalId(), request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("No exact patient match");

        when(patients.findByIdentificationTypeAndDocumentIssuerCountry_CodeAndIdentificationNumber(
                DocumentType.PASSPORT, "PT", "AB123")).thenReturn(Optional.of(patient));
        when(links.existsByPatient_IdAndOrganization_IdAndActiveTrue(10L, null)).thenReturn(true);
        assertThatThrownBy(() -> service.link(organization.getGlobalId(), request))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("already linked");

        when(links.existsByPatient_IdAndOrganization_IdAndActiveTrue(10L, null)).thenReturn(false);
        when(links.saveAndFlush(any(PatientOrganizationLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.link(organization.getGlobalId(), request).patientId()).isEqualTo(patientId);
    }

    private ExactPatientMatchRequest exactRequest() {
        return new ExactPatientMatchRequest(DocumentType.PASSPORT, "pt", " ab 123 ", birthDate);
    }

    private PatientLinkRequest linkRequest(UUID clinicUnitId) {
        return new PatientLinkRequest(DocumentType.PASSPORT, "pt", " ab 123 ", birthDate, clinicUnitId,
                PatientLinkBasis.BOOKING_REQUEST);
    }

    private PatientResponse response() {
        return new PatientResponse(10L, patientId, "Patient", birthDate, true, null, null,
                DocumentType.PASSPORT, "AB 123", null, null, null, List.of());
    }
}
