package br.com.itbn.sisdent.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.itbn.sisdent.dto.AppointmentResponse;
import br.com.itbn.sisdent.dto.AppointmentAvailabilityResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.service.AppointmentService;
import br.com.itbn.sisdent.service.AppointmentAvailabilityService;
import br.com.itbn.sisdent.service.AppointmentBlockedPeriodService;
import br.com.itbn.sisdent.service.PerformedProcedureService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentGraphQlControllerTest {
    private final AppointmentService appointments = mock(AppointmentService.class);
    private final AppointmentAvailabilityService availability = mock(AppointmentAvailabilityService.class);
    private final PerformedProcedureService procedures = mock(PerformedProcedureService.class);
    private final AppointmentBlockedPeriodService blockedPeriods = mock(AppointmentBlockedPeriodService.class);
    private final AppointmentGraphQlController controller = new AppointmentGraphQlController(
            appointments, availability, procedures, blockedPeriods);

    @Test
    void delegatesScopedRangeQueryWithExplicitPagination() {
        UUID organizationId = UUID.randomUUID();
        UUID clinicUnitId = UUID.randomUUID();
        Instant from = Instant.parse("2030-01-01T00:00:00Z");
        Instant to = Instant.parse("2030-01-02T00:00:00Z");
        PageResponse<AppointmentResponse> expected = new PageResponse<>(List.of(), 1, 10, 0, 0);
        when(appointments.list(organizationId, clinicUnitId, from, to, List.of(), 1, 10)).thenReturn(expected);

        controller.appointments(organizationId, clinicUnitId, from.toString(), to.toString(), List.of(), 1, 10);

        verify(appointments).list(organizationId, clinicUnitId, from, to, List.of(), 1, 10);
    }

    @Test
    void rejectsMalformedRangeBeforeCallingTheService() {
        UUID organizationId = UUID.randomUUID();
        assertThatThrownBy(() -> controller.appointments(organizationId, null, "not-an-instant", null, null, null, null))
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

    @Test
    void delegatesScopedAvailabilityIntervalsQuery() {
        UUID organizationId = UUID.randomUUID();
        UUID clinicUnitId = UUID.randomUUID();
        Instant startAt = Instant.parse("2030-01-01T09:00:00Z");
        Instant endAt = Instant.parse("2030-01-01T10:00:00Z");
        List<UUID> practitionerIds = List.of(UUID.randomUUID());
        List<AppointmentAvailabilityResponse> expected = List.of(new AppointmentAvailabilityResponse(practitionerIds.getFirst(),
                startAt, endAt, AppointmentAvailabilityResponse.Availability.AVAILABLE,
                AppointmentAvailabilityResponse.Category.WORKING_HOURS));
        when(availability.list(organizationId, clinicUnitId, startAt, endAt, practitionerIds)).thenReturn(expected);

        assertThat(controller.appointmentAvailabilityIntervals(organizationId, clinicUnitId, startAt.toString(),
                endAt.toString(), practitionerIds)).isSameAs(expected);

        verify(availability).list(organizationId, clinicUnitId, startAt, endAt, practitionerIds);
    }

    @Test
    void delegatesAppointmentProcedureAndLifecycleOperations() {
        UUID organizationId = UUID.randomUUID();
        UUID clinicUnitId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID performedProcedureId = UUID.randomUUID();
        AppointmentMutationInput appointmentInput = new AppointmentMutationInput(clinicUnitId, UUID.randomUUID(),
                UUID.randomUUID(), "2030-01-01T09:00:00Z", "2030-01-01T10:00:00Z", "Europe/Lisbon");
        PerformedProcedureMutationInput procedureInput = new PerformedProcedureMutationInput(1L,
                "2030-01-01T10:00:00Z", null);
        VoidPerformedProcedureMutationInput voidInput = new VoidPerformedProcedureMutationInput("duplicate entry");

        controller.appointment(organizationId, clinicUnitId, appointmentId);
        controller.performedProcedures(organizationId, clinicUnitId, appointmentId);
        controller.eligiblePerformedProcedureOptions(organizationId, clinicUnitId, appointmentId);
        controller.rescheduleAppointment(organizationId, appointmentId, appointmentInput);
        controller.transitionAppointment(organizationId, clinicUnitId, appointmentId, AppointmentStatus.COMPLETED);
        controller.createPerformedProcedure(organizationId, clinicUnitId, appointmentId, procedureInput);
        controller.voidPerformedProcedure(organizationId, clinicUnitId, performedProcedureId, voidInput);

        verify(appointments).get(organizationId, clinicUnitId, appointmentId);
        verify(procedures).list(organizationId, clinicUnitId, appointmentId);
        verify(procedures).eligibleOptions(organizationId, clinicUnitId, appointmentId);
        verify(appointments).reschedule(organizationId, appointmentId, appointmentInput.toRequest());
        verify(appointments).transition(organizationId, clinicUnitId, appointmentId, AppointmentStatus.COMPLETED);
        verify(procedures).create(organizationId, clinicUnitId, appointmentId, procedureInput.toRequest());
        verify(procedures).voidRecord(organizationId, clinicUnitId, performedProcedureId, voidInput.toRequest());
    }

    @Test
    void defaultsMissingPageAndEndRangeValues() {
        UUID organizationId = UUID.randomUUID();
        Instant from = Instant.parse("2030-01-01T00:00:00Z");

        controller.appointments(organizationId, null, from.toString(), null, null, null, null);

        verify(appointments).list(organizationId, null, from, null, null, 0, 25);
    }

    @Test
    void delegatesScopedBlockedPeriodOperations() {
        UUID organizationId = UUID.randomUUID();
        UUID clinicUnitId = UUID.randomUUID();
        UUID blockedPeriodId = UUID.randomUUID();
        AppointmentBlockedPeriodMutationInput input = new AppointmentBlockedPeriodMutationInput(
                clinicUnitId, null, "2030-01-01T09:00:00Z", "2030-01-01T10:00:00Z");

        controller.appointmentBlockedPeriods(organizationId, clinicUnitId, input.startAt(), input.endAt());
        controller.createAppointmentBlockedPeriod(organizationId, input);
        controller.updateAppointmentBlockedPeriod(organizationId, blockedPeriodId, 3L, input);
        controller.deleteAppointmentBlockedPeriod(organizationId, clinicUnitId, blockedPeriodId, 3L);

        verify(blockedPeriods).list(
                organizationId,
                clinicUnitId,
                Instant.parse(input.startAt()),
                Instant.parse(input.endAt()));
        verify(blockedPeriods).create(organizationId, input.toRequest());
        verify(blockedPeriods).update(organizationId, blockedPeriodId, 3L, input.toRequest());
        verify(blockedPeriods).delete(organizationId, clinicUnitId, blockedPeriodId, 3L);
    }
}
