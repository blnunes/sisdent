package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.PerformedProcedureRequest;
import br.com.itbn.sisdent.model.Appointment;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.model.CatalogStatus;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.DentalProcedure;
import br.com.itbn.sisdent.model.PerformedProcedure;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.AppointmentRepository;
import br.com.itbn.sisdent.repository.DentalProcedureRepository;
import br.com.itbn.sisdent.repository.PerformedProcedureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PerformedProcedureServiceTest {
    @Mock AppointmentRepository appointments;
    @Mock DentalProcedureRepository catalog;
    @Mock PerformedProcedureRepository records;
    @Mock ScopeAuthorizationService authorization;
    @Mock CurrentAccountService current;
    @InjectMocks PerformedProcedureService service;

    @Test
    void recordsCompletedAppointmentProceduresAndRejectsInactiveCatalogEntries() {
        UUID organization = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = mock(Appointment.class);
        DentalProcedure procedure = mock(DentalProcedure.class);
        Practitioner practitioner = mock(Practitioner.class);
        Speciality speciality = mock(Speciality.class);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.COMPLETED);
        when(appointments.findByGlobalIdAndOrganization_GlobalId(appointmentId, organization)).thenReturn(Optional.of(appointment));
        when(procedure.getStatus()).thenReturn(CatalogStatus.ACTIVE);
        when(procedure.getId()).thenReturn(1L);
        when(procedure.getName()).thenReturn("Cleaning");
        when(appointment.getPractitioner()).thenReturn(practitioner);
        when(practitioner.getSpecialities()).thenReturn(Set.of(speciality));
        when(speciality.getStatus()).thenReturn(CatalogStatus.ACTIVE);
        when(speciality.getProcedures()).thenReturn(Set.of(procedure));
        when(catalog.findById(1L)).thenReturn(Optional.of(procedure));
        when(records.save(any(PerformedProcedure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(organization, null, appointmentId,
                new PerformedProcedureRequest(1L, Instant.now(), "done")).procedureNameSnapshot()).isEqualTo("Cleaning");

        when(procedure.getStatus()).thenReturn(CatalogStatus.INACTIVE);
        assertThatThrownBy(() -> service.create(organization, null, appointmentId,
                new PerformedProcedureRequest(1L, Instant.now(), null))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void returnsOnlyActiveOptionsAfterAuthorizingTheSelectedClinic() {
        UUID organization = UUID.randomUUID();
        UUID clinic = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        DentalProcedure procedure = mock(DentalProcedure.class);
        Appointment appointment = mock(Appointment.class);
        Practitioner practitioner = mock(Practitioner.class);
        Speciality speciality = mock(Speciality.class);
        ClinicUnit scopedClinic = mock(ClinicUnit.class);
        when(procedure.getId()).thenReturn(7L);
        when(procedure.getName()).thenReturn("Cleaning");
        when(appointments.findByGlobalIdAndOrganization_GlobalId(appointmentId, organization)).thenReturn(Optional.of(appointment));
        when(appointment.getPractitioner()).thenReturn(practitioner);
        when(appointment.getClinicUnit()).thenReturn(scopedClinic);
        when(scopedClinic.getGlobalId()).thenReturn(clinic);
        when(practitioner.getSpecialities()).thenReturn(Set.of(speciality));
        when(speciality.getStatus()).thenReturn(CatalogStatus.ACTIVE);
        when(speciality.getProcedures()).thenReturn(Set.of(procedure));
        when(procedure.getStatus()).thenReturn(CatalogStatus.ACTIVE);

        assertThat(service.eligibleOptions(organization, clinic, appointmentId)).containsExactly(new PerformedProcedureService.ProcedureOption(7L, "Cleaning"));
        verify(authorization).requireAppointmentManagement(organization, clinic);
        verify(authorization).requireClinicInOrganization(organization, clinic);
    }
}
