package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AppointmentAvailabilityResponse;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.ClinicUnitBreak;
import br.com.itbn.sisdent.model.ClinicUnitWorkingHours;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.repository.AppointmentBlockedPeriodRepository;
import br.com.itbn.sisdent.repository.AppointmentRepository;
import br.com.itbn.sisdent.repository.ClinicUnitBreakRepository;
import br.com.itbn.sisdent.repository.ClinicUnitWorkingHoursRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentAvailabilityService {
    private final ClinicUnitWorkingHoursRepository hours;
    private final ClinicUnitBreakRepository breaks;
    private final AppointmentBlockedPeriodRepository blocked;
    private final AppointmentRepository appointments;
    private final PractitionerRepository practitioners;
    private final ScopeAuthorizationService authorization;

    public AppointmentAvailabilityService(ClinicUnitWorkingHoursRepository hours, ClinicUnitBreakRepository breaks,
            AppointmentBlockedPeriodRepository blocked, AppointmentRepository appointments,
            PractitionerRepository practitioners, ScopeAuthorizationService authorization) {
        this.hours = hours;
        this.breaks = breaks;
        this.blocked = blocked;
        this.appointments = appointments;
        this.practitioners = practitioners;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<AppointmentAvailabilityResponse> list(UUID organizationId, UUID clinicId, Instant from, Instant to,
            List<UUID> practitionerIds) {
        authorization.requireAppointmentRead(organizationId, clinicId);
        ClinicUnit clinic = authorization.requireClinicInOrganization(organizationId, clinicId);
        validRange(from, to);
        return selected(organizationId, practitionerIds).stream()
                .flatMap(practitioner -> intervals(organizationId, clinic, practitioner, from, to).stream())
                .toList();
    }

    public void requireAvailable(UUID organizationId, ClinicUnit clinic, Practitioner practitioner, Instant from, Instant to,
            Long ignoredAppointmentId) {
        validRange(from, to);
        if (!ZoneId.of(clinic.getTimezone()).getId().equals(clinic.getTimezone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid IANA timezone is required");
        }
        if (!withinWorkingHours(clinic, from, to)
                || overlapsBreak(clinic, from, to)
                || !blocked.findOverlapping(organizationId, clinic.getGlobalId(), practitioner.getGlobalId(), from, to).isEmpty()
                || appointments.hasOverlap(practitioner.getId(), from, to, ignoredAppointmentId)) {
            throw new SchedulingConflictException();
        }
    }

    private List<Practitioner> selected(UUID organizationId, List<UUID> practitionerIds) {
        if (practitionerIds == null || practitionerIds.isEmpty()) {
            return practitioners.findAllByOrganization_GlobalIdOrderByDisplayName(organizationId).stream()
                    .filter(Practitioner::isActive)
                    .toList();
        }
        return practitionerIds.stream()
                .distinct()
                .map(practitionerId -> practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)
                        .filter(Practitioner::isActive)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .toList();
    }

    private List<AppointmentAvailabilityResponse> intervals(UUID organizationId, ClinicUnit clinic,
            Practitioner practitioner, Instant from, Instant to) {
        List<AppointmentAvailabilityResponse> result = new ArrayList<>();
        ZoneId zone = ZoneId.of(clinic.getTimezone());
        LocalDate firstDay = from.atZone(zone).toLocalDate();
        LocalDate lastDay = to.minusNanos(1).atZone(zone).toLocalDate();
        List<ClinicUnitWorkingHours> workingHours = hours.findAllByClinicUnit_Id(clinic.getId());
        List<ClinicUnitBreak> clinicBreaks = breaks.findAllByClinicUnit_Id(clinic.getId());
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            addWorkingHourIntervals(result, practitioner, workingHours, day, zone, from, to);
            addBreakIntervals(result, practitioner, clinicBreaks, day, zone, from, to);
        }
        blocked.findOverlapping(organizationId, clinic.getGlobalId(), practitioner.getGlobalId(), from, to)
                .forEach(period -> add(result, new AvailabilityInterval(practitioner, period.getStartAt(), period.getEndAt(), from, to,
                        AppointmentAvailabilityResponse.Availability.UNAVAILABLE,
                        AppointmentAvailabilityResponse.Category.BLOCKED)));
        appointments.findScheduledOverlapping(organizationId, clinic.getGlobalId(), practitioner.getGlobalId(), from, to)
                .forEach(appointment -> add(result, new AvailabilityInterval(practitioner, appointment.getStartAt(), appointment.getEndAt(), from, to,
                        AppointmentAvailabilityResponse.Availability.UNAVAILABLE,
                        AppointmentAvailabilityResponse.Category.OCCUPIED)));
        return result;
    }

    private void addWorkingHourIntervals(List<AppointmentAvailabilityResponse> result, Practitioner practitioner,
            List<ClinicUnitWorkingHours> workingHours, LocalDate day, ZoneId zone, Instant from, Instant to) {
        for (ClinicUnitWorkingHours workingHour : workingHours) {
            if (workingHour.getDayOfWeek() == day.getDayOfWeek().getValue()) {
                add(result, new AvailabilityInterval(practitioner, bound(day, workingHour.getStartMinute(), zone),
                        bound(day, workingHour.getEndMinute(), zone), from, to,
                        AppointmentAvailabilityResponse.Availability.AVAILABLE,
                        AppointmentAvailabilityResponse.Category.WORKING_HOURS));
            }
        }
    }

    private void addBreakIntervals(List<AppointmentAvailabilityResponse> result, Practitioner practitioner,
            List<ClinicUnitBreak> clinicBreaks, LocalDate day, ZoneId zone, Instant from, Instant to) {
        for (ClinicUnitBreak clinicBreak : clinicBreaks) {
            if (clinicBreak.getDayOfWeek() == day.getDayOfWeek().getValue()) {
                add(result, new AvailabilityInterval(practitioner, bound(day, clinicBreak.getStartMinute(), zone),
                        bound(day, clinicBreak.getEndMinute(), zone), from, to,
                        AppointmentAvailabilityResponse.Availability.UNAVAILABLE,
                        AppointmentAvailabilityResponse.Category.BREAK));
            }
        }
    }

    private boolean withinWorkingHours(ClinicUnit clinic, Instant start, Instant end) {
        ZoneId zone = ZoneId.of(clinic.getTimezone());
        LocalDate day = start.atZone(zone).toLocalDate();
        LocalDate lastDay = end.minusNanos(1).atZone(zone).toLocalDate();
        List<ClinicUnitWorkingHours> configured = hours.findAllByClinicUnit_Id(clinic.getId());
        if (configured.isEmpty()) {
            return true;
        }
        if (!day.equals(lastDay)) {
            return false;
        }
        return configured.stream()
                .filter(workingHour -> workingHour.getDayOfWeek() == day.getDayOfWeek().getValue())
                .anyMatch(workingHour -> !start.isBefore(bound(day, workingHour.getStartMinute(), zone))
                        && !end.isAfter(bound(day, workingHour.getEndMinute(), zone)));
    }

    private boolean overlapsBreak(ClinicUnit clinic, Instant start, Instant end) {
        ZoneId zone = ZoneId.of(clinic.getTimezone());
        LocalDate lastDay = end.minusNanos(1).atZone(zone).toLocalDate();
        List<ClinicUnitBreak> clinicBreaks = breaks.findAllByClinicUnit_Id(clinic.getId());
        for (LocalDate day = start.atZone(zone).toLocalDate(); !day.isAfter(lastDay); day = day.plusDays(1)) {
            for (ClinicUnitBreak clinicBreak : clinicBreaks) {
                if (clinicBreak.getDayOfWeek() == day.getDayOfWeek().getValue()
                        && start.isBefore(bound(day, clinicBreak.getEndMinute(), zone))
                        && end.isAfter(bound(day, clinicBreak.getStartMinute(), zone))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Instant bound(LocalDate day, int minute, ZoneId zone) {
        if (minute == 1440) {
            return day.plusDays(1).atStartOfDay(zone).toInstant();
        }
        return day.atStartOfDay(zone).plusMinutes(minute).toInstant();
    }

    private void add(List<AppointmentAvailabilityResponse> result, AvailabilityInterval interval) {
        Instant start = interval.start().isBefore(interval.from()) ? interval.from() : interval.start();
        Instant end = interval.end().isAfter(interval.to()) ? interval.to() : interval.end();
        if (end.isAfter(start)) {
            result.add(new AppointmentAvailabilityResponse(interval.practitioner().getGlobalId(), start, end,
                    interval.availability(), interval.category()));
        }
    }

    private void validRange(Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment end must be after start");
        }
    }

    private record AvailabilityInterval(Practitioner practitioner, Instant start, Instant end, Instant from, Instant to,
            AppointmentAvailabilityResponse.Availability availability, AppointmentAvailabilityResponse.Category category) {
    }
}
