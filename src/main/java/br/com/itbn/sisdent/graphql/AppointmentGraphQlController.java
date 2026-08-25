package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AppointmentResponse;
import br.com.itbn.sisdent.dto.AppointmentAvailabilityResponse;
import br.com.itbn.sisdent.dto.AppointmentBlockedPeriodResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.PerformedProcedureResponse;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.service.AppointmentService;
import br.com.itbn.sisdent.service.AppointmentAvailabilityService;
import br.com.itbn.sisdent.service.AppointmentBlockedPeriodService;
import br.com.itbn.sisdent.service.PerformedProcedureService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;

/** GraphQL adapter for scheduling; services remain the authority for scope and lifecycle rules. */
@Controller
public class AppointmentGraphQlController {
    private final AppointmentService appointments;
    private final AppointmentAvailabilityService availability;
    private final PerformedProcedureService procedures;
    private final AppointmentBlockedPeriodService blockedPeriods;

    public AppointmentGraphQlController(AppointmentService appointments, AppointmentAvailabilityService availability,
            PerformedProcedureService procedures, AppointmentBlockedPeriodService blockedPeriods) {
        this.appointments = appointments;
        this.availability = availability;
        this.procedures = procedures;
        this.blockedPeriods = blockedPeriods;
    }

    @QueryMapping
    public PageResponse<AppointmentResponse> appointments(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument String from, @Argument String to, @Argument List<UUID> practitionerIds, @Argument Integer page,
            @Argument Integer size) {
        return appointments.list(organizationId, clinicUnitId, instant(from), optionalInstant(to), practitionerIds,
                pageOrDefault(page), sizeOrDefault(size));
    }

    @QueryMapping
    public AppointmentResponse appointment(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID appointmentId) {
        return appointments.get(organizationId, clinicUnitId, appointmentId);
    }

    @QueryMapping
    public List<AppointmentAvailabilityResponse> appointmentAvailabilityIntervals(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument String from, @Argument String to,
            @Argument List<UUID> practitionerIds) {
        return availability.list(organizationId, clinicUnitId, instant(from), instant(to), practitionerIds);
    }

    @QueryMapping
    public List<AppointmentBlockedPeriodResponse> appointmentBlockedPeriods(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument String from, @Argument String to) {
        return blockedPeriods.list(organizationId, clinicUnitId, instant(from), instant(to));
    }

    @QueryMapping
    public List<PerformedProcedureResponse> performedProcedures(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument UUID appointmentId) {
        return procedures.list(organizationId, clinicUnitId, appointmentId);
    }

    @QueryMapping
    public List<PerformedProcedureService.ProcedureOption> eligiblePerformedProcedureOptions(
            @Argument UUID organizationId, @Argument UUID clinicUnitId, @Argument UUID appointmentId) {
        return procedures.eligibleOptions(organizationId, clinicUnitId, appointmentId);
    }

    @MutationMapping
    public AppointmentResponse createAppointment(@Argument UUID organizationId,
            @Argument @Valid AppointmentMutationInput input) {
        return appointments.create(organizationId, input.toRequest());
    }

    @MutationMapping
    public AppointmentResponse rescheduleAppointment(@Argument UUID organizationId, @Argument UUID appointmentId,
            @Argument @Valid AppointmentMutationInput input) {
        return appointments.reschedule(organizationId, appointmentId, input.toRequest());
    }

    @MutationMapping
    public AppointmentResponse transitionAppointment(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID appointmentId, @Argument AppointmentStatus status) {
        return appointments.transition(organizationId, clinicUnitId, appointmentId, status);
    }

    @MutationMapping
    public AppointmentBlockedPeriodResponse createAppointmentBlockedPeriod(@Argument UUID organizationId,
            @Argument @Valid AppointmentBlockedPeriodMutationInput input) {
        return blockedPeriods.create(organizationId, input.toRequest());
    }

    @MutationMapping
    public AppointmentBlockedPeriodResponse updateAppointmentBlockedPeriod(@Argument UUID organizationId,
            @Argument UUID blockedPeriodId, @Argument long version,
            @Argument @Valid AppointmentBlockedPeriodMutationInput input) {
        return blockedPeriods.update(organizationId, blockedPeriodId, version, input.toRequest());
    }

    @MutationMapping
    public boolean deleteAppointmentBlockedPeriod(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID blockedPeriodId, @Argument long version) {
        return blockedPeriods.delete(organizationId, clinicUnitId, blockedPeriodId, version);
    }

    @MutationMapping
    public PerformedProcedureResponse createPerformedProcedure(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID appointmentId, @Argument @Valid PerformedProcedureMutationInput input) {
        return procedures.create(organizationId, clinicUnitId, appointmentId, input.toRequest());
    }

    @MutationMapping
    public PerformedProcedureResponse voidPerformedProcedure(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID performedProcedureId, @Argument @Valid VoidPerformedProcedureMutationInput input) {
        return procedures.voidRecord(organizationId, clinicUnitId, performedProcedureId, input.toRequest());
    }

    private static Instant instant(String value) {
        if (value == null) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
        return parse(value);
    }

    private static Instant optionalInstant(String value) {
        return value == null ? null : parse(value);
    }

    private static Instant parse(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException _) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private static int pageOrDefault(Integer page) {
        return page == null ? 0 : page;
    }

    private static int sizeOrDefault(Integer size) {
        return size == null ? 25 : size;
    }
}
