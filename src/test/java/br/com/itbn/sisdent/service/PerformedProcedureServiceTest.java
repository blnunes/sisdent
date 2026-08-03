package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.PerformedProcedureRequest;
import br.com.itbn.sisdent.model.Appointment;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.model.CatalogStatus;
import br.com.itbn.sisdent.model.DentalProcedure;
import br.com.itbn.sisdent.model.PerformedProcedure;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        DentalProcedure procedure = org.mockito.Mockito.mock(DentalProcedure.class);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.COMPLETED);
        when(appointments.findByGlobalIdAndOrganization_GlobalId(appointmentId, organization)).thenReturn(Optional.of(appointment));
        when(procedure.getStatus()).thenReturn(CatalogStatus.ACTIVE);
        when(procedure.getName()).thenReturn("Cleaning");
        when(catalog.findById(1L)).thenReturn(Optional.of(procedure));
        when(records.save(any(PerformedProcedure.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(organization, null, appointmentId,
                new PerformedProcedureRequest(1L, Instant.now(), "done")).procedureNameSnapshot()).isEqualTo("Cleaning");

        when(procedure.getStatus()).thenReturn(CatalogStatus.INACTIVE);
        assertThatThrownBy(() -> service.create(organization, null, appointmentId,
                new PerformedProcedureRequest(1L, Instant.now(), null))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
