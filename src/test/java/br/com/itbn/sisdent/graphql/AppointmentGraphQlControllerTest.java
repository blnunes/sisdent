package br.com.itbn.sisdent.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.itbn.sisdent.dto.AppointmentResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.service.AppointmentService;
import br.com.itbn.sisdent.service.PerformedProcedureService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentGraphQlControllerTest {
    private final AppointmentService appointments = mock(AppointmentService.class);
    private final PerformedProcedureService procedures = mock(PerformedProcedureService.class);
    private final AppointmentGraphQlController controller = new AppointmentGraphQlController(appointments, procedures);

    @Test
    void delegatesScopedRangeQueryWithExplicitPagination() {
        UUID organizationId = UUID.randomUUID();
        UUID clinicUnitId = UUID.randomUUID();
        Instant from = Instant.parse("2030-01-01T00:00:00Z");
        Instant to = Instant.parse("2030-01-02T00:00:00Z");
        PageResponse<AppointmentResponse> expected = new PageResponse<>(List.of(), 1, 10, 0, 0);
        when(appointments.list(organizationId, clinicUnitId, from, to, 1, 10)).thenReturn(expected);

        controller.appointments(organizationId, clinicUnitId, from.toString(), to.toString(), 1, 10);

        verify(appointments).list(organizationId, clinicUnitId, from, to, 1, 10);
    }

    @Test
    void rejectsMalformedRangeBeforeCallingTheService() {
        assertThatThrownBy(() -> controller.appointments(UUID.randomUUID(), null, "not-an-instant", null, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void mapsCreateInputAndDelegatesLifecycleMutation() {
        UUID organizationId = UUID.randomUUID();
        UUID clinicUnitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID practitionerId = UUID.randomUUID();
        AppointmentMutationInput input = new AppointmentMutationInput(
                clinicUnitId, patientId, practitionerId, "2030-01-01T09:00:00Z", "2030-01-01T10:00:00Z", "Europe/Lisbon");

        controller.createAppointment(organizationId, input);

        verify(appointments).create(organizationId, input.toRequest());
    }
}
