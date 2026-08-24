package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AppointmentResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.PerformedProcedureResponse;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.service.AppointmentService;
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
    private final PerformedProcedureService procedures;

    public AppointmentGraphQlController(AppointmentService appointments, PerformedProcedureService procedures) {
        this.appointments = appointments;
        this.procedures = procedures;
    }

    @QueryMapping
    public PageResponse<AppointmentResponse> appointments(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument String from, @Argument String to, @Argument Integer page, @Argument Integer size) {
        return appointments.list(organizationId, clinicUnitId, instant(from), optionalInstant(to), pageOrDefault(page),
                sizeOrDefault(size));
    }

    @QueryMapping
    public AppointmentResponse appointment(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID appointmentId) {
        return appointments.get(organizationId, clinicUnitId, appointmentId);
    }

    @QueryMapping
    public List<PerformedProcedureResponse> performedProcedures(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument UUID appointmentId) {
        return procedures.list(organizationId, clinicUnitId, appointmentId);
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
        } catch (DateTimeParseException exception) {
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
