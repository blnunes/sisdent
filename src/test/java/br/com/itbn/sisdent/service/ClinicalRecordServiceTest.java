package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AmendEncounterRequest;
import br.com.itbn.sisdent.dto.ClinicalEncounterCreateRequest;
import br.com.itbn.sisdent.dto.ClinicalEncounterRequest;
import br.com.itbn.sisdent.dto.ClinicalEncounterResponse;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Appointment;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.ClinicalEncounter;
import br.com.itbn.sisdent.model.EncounterStatus;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.repository.AppointmentRepository;
import br.com.itbn.sisdent.repository.ClinicalEncounterRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalRecordServiceTest {
    private static final Instant CARE_AT = Instant.parse("2026-09-02T09:30:00Z");

    @Mock
    private ClinicalEncounterRepository encounters;
    @Mock
    private PatientOrganizationLinkRepository links;
    @Mock
    private OrganizationRepository organizations;
    @Mock
    private AppointmentRepository appointments;
    @Mock
    private PractitionerRepository practitioners;
    @Mock
    private ScopeAuthorizationService authorization;
    @Mock
    private CurrentAccountService current;
    @Mock
    private Organization organization;
    @Mock
    private ClinicUnit clinic;
    @Mock
    private PatientOrganizationLink link;
    @Mock
    private Patient patient;
    @Mock
    private Account account;

    private ClinicalRecordService service;
    private UUID organizationId;
    private UUID clinicId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        service = new ClinicalRecordService(encounters, links, organizations, appointments, practitioners, authorization,
                current);
        organizationId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        patientId = UUID.randomUUID();

    }

    @Test
    void createsARecordWithNormalizedClinicalData() {
        configureValidParts();
        ClinicalEncounterResponse response = service.create(organizationId,
                createRequest(null, null, " Europe/Lisbon ", "  Treatment completed  ", "  Reviewed  "));

        ArgumentCaptor<ClinicalEncounter> saved = ArgumentCaptor.forClass(ClinicalEncounter.class);
        verify(encounters).save(saved.capture());
        assertThat(saved.getValue().getCareTimezone()).isEqualTo("Europe/Lisbon");
        assertThat(saved.getValue().getNarrative()).isEqualTo("Treatment completed");
        assertThat(saved.getValue().getAdministrativeNote()).isEqualTo("Reviewed");
        assertThat(response.patientId()).isEqualTo(patientId);
        verify(authorization).requireClinicalAuthor(organizationId, clinicId);
    }

    @Test
    void rejectsMissingCareTimeAndInvalidTimezone() {
        ClinicalEncounterCreateRequest missingTime = new ClinicalEncounterCreateRequest(clinicId, patientId, null, null,
                null, "Europe/Lisbon", "Note", null);

        assertBadRequest(() -> service.create(organizationId, missingTime), "careAt is required");
        configureValidParts();
        assertBadRequest(() -> service.create(organizationId,
                createRequest(null, null, "Not/AZone", "Note", null)), "A valid IANA timezone is required");
        verify(encounters, never()).save(any());
    }

    @Test
    void rejectsInactiveOrClinicScopedPatientLinksOutsideTheRequestedClinic() {
        configureValidParts();
        when(link.isActive()).thenReturn(false);
        assertNotFound(() -> service.create(organizationId, createRequest(null, null, "UTC", "Note", null)));

        when(link.isActive()).thenReturn(true);
        ClinicUnit anotherClinic = org.mockito.Mockito.mock(ClinicUnit.class);
        when(anotherClinic.getGlobalId()).thenReturn(UUID.randomUUID());
        when(link.getClinicUnit()).thenReturn(anotherClinic);
        assertNotFound(() -> service.create(organizationId, createRequest(null, null, "UTC", "Note", null)));
    }

    @Test
    void rejectsInactivePractitionersAndAppointmentsOutsideTheClinicalContext() {
        configureValidParts();
        UUID practitionerId = UUID.randomUUID();
        Practitioner practitioner = org.mockito.Mockito.mock(Practitioner.class);
        when(practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId))
                .thenReturn(Optional.of(practitioner));
        when(practitioner.isActive()).thenReturn(false);
        assertNotFound(() -> service.create(organizationId,
                createRequest(null, practitionerId, "UTC", "Note", null)));

        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        when(appointments.findByGlobalIdAndOrganization_GlobalId(appointmentId, organizationId))
                .thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        assertNotFound(() -> service.create(organizationId,
                createRequest(appointmentId, null, "UTC", "Note", null)));
    }

    @Test
    void validatesPagingBeforeQueryingRecords() {
        assertBadRequest(() -> service.list(organizationId, clinicId, patientId, -1, 20), "Invalid page");
        assertBadRequest(() -> service.list(organizationId, clinicId, patientId, 0, 0), "Invalid page");
        verify(encounters, never()).findAllByOrganization_GlobalIdAndPatientLink_Patient_GlobalIdAndClinicUnit_GlobalId(
                any(), any(), any(), any());
    }

    @Test
    void rejectsUpdatesForStaleVersionsOrAnotherAuthor() {
        UUID encounterId = UUID.randomUUID();
        ClinicalEncounter encounter = org.mockito.Mockito.mock(ClinicalEncounter.class);
        when(encounters.findByGlobalIdAndOrganization_GlobalId(encounterId, organizationId)).thenReturn(Optional.of(encounter));
        when(encounter.getClinicUnit()).thenReturn(clinic);
        when(clinic.getGlobalId()).thenReturn(clinicId);
        when(encounter.getPatientLink()).thenReturn(link);
        when(link.isActive()).thenReturn(true);
        when(encounter.getStatus()).thenReturn(EncounterStatus.DRAFT);
        when(encounter.getCreatedBy()).thenReturn("another-account");
        UUID accountId = UUID.randomUUID();
        when(current.require()).thenReturn(account);
        when(account.getGlobalId()).thenReturn(accountId);

        ClinicalEncounterRequest request = updateRequest(1L);
        assertConflict(() -> service.update(organizationId, encounterId, request));

        verify(encounter, never()).update(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsFinalizationAndAmendmentFromInvalidStates() {
        UUID encounterId = UUID.randomUUID();
        ClinicalEncounter encounter = org.mockito.Mockito.mock(ClinicalEncounter.class);
        when(encounters.findByGlobalIdAndOrganization_GlobalId(encounterId, organizationId)).thenReturn(Optional.of(encounter));
        when(encounter.getClinicUnit()).thenReturn(clinic);
        when(clinic.getGlobalId()).thenReturn(clinicId);
        when(encounter.getPatientLink()).thenReturn(link);
        when(link.isActive()).thenReturn(true);
        when(encounter.getStatus()).thenReturn(EncounterStatus.DRAFT);
        when(current.require()).thenReturn(account);
        when(account.getGlobalId()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.doThrow(new IllegalStateException()).when(encounter).finalizeRecord(any());

        assertConflict(() -> service.finalizeRecord(organizationId, clinicId, encounterId));
        assertConflict(() -> service.amend(organizationId, encounterId,
                new AmendEncounterRequest(clinicId, null, null, CARE_AT, "UTC", "Note", null, "Reason")));
    }

    @Test
    void rejectsRecordsOutsideTheRequestedClinicOrWithInactivePatients() {
        UUID encounterId = UUID.randomUUID();
        ClinicalEncounter encounter = org.mockito.Mockito.mock(ClinicalEncounter.class);
        ClinicUnit anotherClinic = org.mockito.Mockito.mock(ClinicUnit.class);
        when(anotherClinic.getGlobalId()).thenReturn(UUID.randomUUID());
        when(encounters.findByGlobalIdAndOrganization_GlobalId(encounterId, organizationId)).thenReturn(Optional.of(encounter));
        when(encounter.getClinicUnit()).thenReturn(anotherClinic);

        assertNotFound(() -> service.get(organizationId, clinicId, encounterId));
        verify(authorization).requireClinicalRead(organizationId, clinicId);
    }

    private ClinicalEncounterCreateRequest createRequest(UUID appointmentId, UUID practitionerId, String timezone,
            String narrative, String note) {
        return new ClinicalEncounterCreateRequest(clinicId, patientId, appointmentId, practitionerId, CARE_AT, timezone,
                narrative, note);
    }

    private void configureValidParts() {
        lenient().when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);
        lenient().when(links.findFirstByPatient_GlobalIdAndOrganization_GlobalId(patientId, organizationId))
                .thenReturn(Optional.of(link));
        lenient().when(link.isActive()).thenReturn(true);
        lenient().when(link.getClinicUnit()).thenReturn(null);
        lenient().when(link.getPatient()).thenReturn(patient);
        lenient().when(patient.getGlobalId()).thenReturn(patientId);
        lenient().when(clinic.getGlobalId()).thenReturn(clinicId);
        lenient().when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(organization));
        lenient().when(encounters.save(any(ClinicalEncounter.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ClinicalEncounterRequest updateRequest(Long version) {
        return new ClinicalEncounterRequest(clinicId, patientId, null, null, CARE_AT, "UTC", "Note", null, version);
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, String reason) {
        assertThatThrownBy(operation)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException response = (ResponseStatusException) exception;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getReason()).isEqualTo(reason);
                });
    }

    private void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
