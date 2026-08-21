package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AppointmentRequest;
import br.com.itbn.sisdent.model.Appointment;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.repository.AppointmentRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    @Mock AppointmentRepository appointments;
    @Mock PractitionerRepository practitioners;
    @Mock PatientOrganizationLinkRepository links;
    @Mock OrganizationRepository organizations;
    @Mock ScopeAuthorizationService authorization;
    @InjectMocks AppointmentService service;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID clinicId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID practitionerId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-08-04T09:00:00Z");
    private final Instant end = Instant.parse("2026-08-04T10:00:00Z");
    private AppointmentRequest request;
    private Practitioner practitioner;
    private PatientOrganizationLink link;
    private ClinicUnit clinic;

    @BeforeEach
    void setUp() {
        request = new AppointmentRequest(clinicId, patientId, practitionerId, start, end, "Europe/Lisbon");
        practitioner = mock(Practitioner.class);
        link = mock(PatientOrganizationLink.class);
        clinic = mock(ClinicUnit.class);
        Patient patient = mock(Patient.class);
        lenient().when(practitioner.isActive()).thenReturn(true);
        lenient().when(practitioner.getId()).thenReturn(1L);
        lenient().when(practitioner.getGlobalId()).thenReturn(practitionerId);
        lenient().when(practitioner.getDisplayName()).thenReturn("Dr Ana");
        lenient().when(link.getPatient()).thenReturn(patient);
        lenient().when(patient.getGlobalId()).thenReturn(patientId);
        lenient().when(patient.getName()).thenReturn("Patient");
        lenient().when(clinic.getGlobalId()).thenReturn(clinicId);
    }

    @Test
    void createsAppointmentForAnAvailablePractitioner() {
        Organization organization = new Organization("Alpha");
        when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(organization));
        when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);
        when(practitioners.lockByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)).thenReturn(Optional.of(practitioner));
        when(links.findFirstByPatient_GlobalIdAndOrganization_GlobalIdAndClinicUnit_GlobalIdAndActiveTrue(patientId, organizationId, clinicId)).thenReturn(Optional.of(link));
        when(appointments.hasOverlap(1L, start, end, null)).thenReturn(false);
        when(appointments.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(organizationId, request).schedulingTimezone()).isEqualTo("Europe/Lisbon");
    }

    @Test
    void rejectsInvalidTimesTimezonesAndSchedulingConflicts() {
        AppointmentRequest invalidRange = new AppointmentRequest(clinicId, patientId, practitionerId, end, start, "Europe/Lisbon");
        AppointmentRequest invalidZone = new AppointmentRequest(clinicId, patientId, practitionerId, start, end, "invalid-zone");
        assertThatThrownBy(() -> service.create(organizationId, invalidRange)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create(organizationId, invalidZone)).isInstanceOf(ResponseStatusException.class);

        when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(new Organization("Alpha")));
        when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);
        when(practitioners.lockByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)).thenReturn(Optional.of(practitioner));
        when(links.findFirstByPatient_GlobalIdAndOrganization_GlobalIdAndClinicUnit_GlobalIdAndActiveTrue(patientId, organizationId, clinicId))
                .thenReturn(Optional.of(link));
        when(appointments.hasOverlap(1L, start, end, null)).thenReturn(true);
        assertThatThrownBy(() -> service.create(organizationId, request)).isInstanceOf(SchedulingConflictException.class);
    }

    @Test
    void getsAndTransitionsAppointmentsWithinTheRequestedClinic() {
        Appointment appointment = new Appointment(new Organization("Alpha"), clinic, link, practitioner, start, end, "Europe/Lisbon");
        var appointmentId = appointment.getGlobalId();
        when(appointments.findByGlobalIdAndOrganization_GlobalId(appointmentId, organizationId)).thenReturn(Optional.of(appointment));

        assertThat(service.get(organizationId, clinicId, appointmentId).status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(service.transition(organizationId, clinicId, appointmentId, AppointmentStatus.COMPLETED).status())
                .isEqualTo(AppointmentStatus.COMPLETED);
        assertThatThrownBy(() -> service.transition(organizationId, clinicId, appointmentId, AppointmentStatus.CANCELLED))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listsAndReschedulesAppointmentsWithinTheAuthorizedClinic() {
        Appointment appointment = new Appointment(new Organization("Alpha"), clinic, link, practitioner, start, end, "Europe/Lisbon");
        when(appointments.findScoped(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);
        when(appointments.findByGlobalIdAndOrganization_GlobalId(appointment.getGlobalId(), organizationId)).thenReturn(Optional.of(appointment));
        when(links.findFirstByPatient_GlobalIdAndOrganization_GlobalIdAndClinicUnit_GlobalIdAndActiveTrue(patientId, organizationId, clinicId))
                .thenReturn(Optional.of(link));
        when(practitioners.lockByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)).thenReturn(Optional.of(practitioner));
        when(appointments.hasOverlap(1L, start, end, null)).thenReturn(false);

        assertThat(service.list(organizationId, clinicId, start, end, 0, 200).content()).isEmpty();
        assertThat(service.reschedule(organizationId, appointment.getGlobalId(), request).status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void listsAppointmentsFromTheRequestedDayWithoutAnUpperDateLimit() {
        when(appointments.findFrom(any(), any(), any(), any())).thenReturn(Page.empty());
        when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);

        assertThat(service.list(organizationId, clinicId, start, null, 1, 10).content()).isEmpty();

        verify(appointments).findFrom(any(), any(), any(), any());
    }

    @Test
    void rejectsInactivePractitionersAndAppointmentsOutsideTheClinicScope() {
        when(practitioner.isActive()).thenReturn(false);
        when(practitioners.lockByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)).thenReturn(Optional.of(practitioner));
        when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(new Organization("Alpha")));
        when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);

        assertThatThrownBy(() -> service.create(organizationId, request)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inactive");

        Appointment appointment = new Appointment(new Organization("Alpha"), clinic, link, practitioner, start, end, "Europe/Lisbon");
        var appointmentId = appointment.getGlobalId();
        var unrelatedClinicId = UUID.randomUUID();
        when(appointments.findByGlobalIdAndOrganization_GlobalId(appointmentId, organizationId)).thenReturn(Optional.of(appointment));
        assertThatThrownBy(() -> service.get(organizationId, unrelatedClinicId, appointmentId))
                .isInstanceOf(ResponseStatusException.class);
    }
}
