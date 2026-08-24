package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AppointmentRequest;
import br.com.itbn.sisdent.dto.AppointmentAvailabilityResponse;
import br.com.itbn.sisdent.dto.AppointmentResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.model.Appointment;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.repository.AppointmentRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final PractitionerRepository practitioners;
    private final PatientOrganizationLinkRepository links;
    private final OrganizationRepository organizations;
    private final ScopeAuthorizationService authorization;

    public AppointmentService(AppointmentRepository appointments, PractitionerRepository practitioners,
            PatientOrganizationLinkRepository links, OrganizationRepository organizations,
            ScopeAuthorizationService authorization) {
        this.appointments = appointments;
        this.practitioners = practitioners;
        this.links = links;
        this.organizations = organizations;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> list(UUID organizationId, UUID clinicUnitId, Instant from, Instant to,
            int page, int size) {
        authorization.requireAppointmentRead(organizationId, clinicUnitId);
        validListRange(from, to);
        if (clinicUnitId != null) {
            authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        }
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("startAt"));
        return PageResponse.from(to == null
                ? appointments.findFrom(organizationId, clinicUnitId, from, pageable)
                : appointments.findScoped(organizationId, clinicUnitId, from, to, pageable), this::response);
    }

    @Transactional
    public AppointmentResponse create(UUID organizationId, AppointmentRequest request) {
        authorization.requireAppointmentManagement(organizationId, request.clinicUnitId());
        valid(request);
        Organization organization = organizations.findByGlobalId(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ClinicUnit clinic = authorization.requireClinicInOrganization(organizationId, request.clinicUnitId());
        Practitioner practitioner = lockActive(organizationId, request.practitionerId());
        PatientOrganizationLink link = activeLink(organizationId, request.patientId(), request.clinicUnitId());
        conflict(practitioner, request.startAt(), request.endAt(), null);
        return response(appointments.save(new Appointment(organization, clinic, link, practitioner,
                request.startAt(), request.endAt(), zone(request.schedulingTimezone()))));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(UUID organizationId, UUID clinicUnitId, UUID appointmentId) {
        authorization.requireAppointmentRead(organizationId, clinicUnitId);
        Appointment appointment = require(organizationId, appointmentId);
        checkClinic(appointment, clinicUnitId);
        return response(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentAvailabilityResponse availability(UUID organizationId, UUID clinicUnitId, UUID practitionerId,
            Instant startAt, Instant endAt) {
        authorization.requireAppointmentRead(organizationId, clinicUnitId);
        authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        validRange(startAt, endAt);
        Practitioner practitioner = lockActive(organizationId, practitionerId);
        return new AppointmentAvailabilityResponse(!appointments.hasOverlap(practitioner.getId(), startAt, endAt, null));
    }

    @Transactional
    public AppointmentResponse reschedule(UUID organizationId, UUID appointmentId, AppointmentRequest request) {
        authorization.requireAppointmentManagement(organizationId, request.clinicUnitId());
        valid(request);
        Appointment appointment = require(organizationId, appointmentId);
        checkClinic(appointment, request.clinicUnitId());
        PatientOrganizationLink link = activeLink(organizationId, request.patientId(), request.clinicUnitId());
        if (!appointment.getPatientLink().getId().equals(link.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Practitioner practitioner = lockActive(organizationId, appointment.getPractitioner().getGlobalId());
        if (!practitioner.getId().equals(appointment.getPractitioner().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Practitioner cannot be changed when rescheduling");
        }
        conflict(practitioner, request.startAt(), request.endAt(), appointment.getId());
        appointment.reschedule(request.startAt(), request.endAt(), zone(request.schedulingTimezone()));
        return response(appointment);
    }

    @Transactional
    public AppointmentResponse transition(UUID organizationId, UUID clinicUnitId, UUID appointmentId,
            AppointmentStatus status) {
        authorization.requireAppointmentManagement(organizationId, clinicUnitId);
        Appointment appointment = require(organizationId, appointmentId);
        checkClinic(appointment, clinicUnitId);
        try {
            appointment.transition(status);
        } catch (IllegalStateException _) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid appointment state transition");
        }
        return response(appointment);
    }

    private Appointment require(UUID organizationId, UUID appointmentId) {
        return appointments.findByGlobalIdAndOrganization_GlobalId(appointmentId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private PatientOrganizationLink activeLink(UUID organizationId, UUID patientId, UUID clinicUnitId) {
        return links.findFirstByPatient_GlobalIdAndOrganization_GlobalIdAndClinicUnit_GlobalIdAndActiveTrue(
                patientId, organizationId, clinicUnitId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private Practitioner lockActive(UUID organizationId, UUID practitionerId) {
        Practitioner practitioner = practitioners.lockByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!practitioner.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Practitioner is inactive");
        }
        return practitioner;
    }
    private void conflict(Practitioner practitioner, Instant start, Instant end, Long excludedId) {
        if (appointments.hasOverlap(practitioner.getId(), start, end, excludedId)) {
            throw new SchedulingConflictException();
        }
    }
    private void valid(AppointmentRequest request) { validRange(request.startAt(), request.endAt()); zone(request.schedulingTimezone()); }
    private void validRange(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment end must be after start");
        }
    }
    private void validListRange(Instant from, Instant to) {
        if (from == null || (to != null && !to.isAfter(from))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment end must be after start");
        }
    }
    private String zone(String value) {
        try { return ZoneId.of(value.strip()).getId(); }
        catch (Exception _) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid IANA timezone is required"); }
    }
    private void checkClinic(Appointment appointment, UUID clinicUnitId) {
        if (clinicUnitId != null && !appointment.getClinicUnit().getGlobalId().equals(clinicUnitId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
    private AppointmentResponse response(Appointment appointment) {
        return new AppointmentResponse(appointment.getGlobalId(), appointment.getClinicUnit().getGlobalId(),
                appointment.getPatientLink().getPatient().getGlobalId(), appointment.getPatientLink().getPatient().getName(),
                appointment.getPractitioner().getGlobalId(), appointment.getPractitioner().getDisplayName(),
                appointment.getStartAt(), appointment.getEndAt(), appointment.getSchedulingTimezone(), appointment.getStatus());
    }
}
