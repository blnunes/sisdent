package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AppointmentBlockedPeriodRequest;
import br.com.itbn.sisdent.dto.AppointmentBlockedPeriodResponse;
import br.com.itbn.sisdent.model.AppointmentBlockedPeriod;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.repository.AppointmentBlockedPeriodRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Server-authoritative management of generic scheduling unavailability. */
@Service
public class AppointmentBlockedPeriodService {
    private final AppointmentBlockedPeriodRepository blockedPeriods;
    private final PractitionerRepository practitioners;
    private final ScopeAuthorizationService authorization;

    public AppointmentBlockedPeriodService(
            AppointmentBlockedPeriodRepository blockedPeriods,
            PractitionerRepository practitioners,
            ScopeAuthorizationService authorization) {
        this.blockedPeriods = blockedPeriods;
        this.practitioners = practitioners;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<AppointmentBlockedPeriodResponse> list(
            UUID organizationId,
            UUID clinicUnitId,
            Instant from,
            Instant to) {
        authorization.requireAppointmentManagement(organizationId, clinicUnitId);
        authorization.requireClinicInOrganization(organizationId, clinicUnitId);
        validateRange(from, to);
        return blockedPeriods.findScopedOverlapping(organizationId, clinicUnitId, from, to).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public AppointmentBlockedPeriodResponse create(UUID organizationId, AppointmentBlockedPeriodRequest request) {
        ClinicUnit clinic = managedClinic(organizationId, request.clinicUnitId());
        validateRange(request.startAt(), request.endAt());
        Practitioner practitioner = activePractitioner(organizationId, request.practitionerId());
        AppointmentBlockedPeriod blockedPeriod = new AppointmentBlockedPeriod(
                clinic.getOrganization(), clinic, practitioner, request.startAt(), request.endAt());
        return response(blockedPeriods.save(blockedPeriod));
    }

    @Transactional
    public AppointmentBlockedPeriodResponse update(
            UUID organizationId,
            UUID blockedPeriodId,
            long version,
            AppointmentBlockedPeriodRequest request) {
        managedClinic(organizationId, request.clinicUnitId());
        validateRange(request.startAt(), request.endAt());
        AppointmentBlockedPeriod blockedPeriod = scopedBlockedPeriod(organizationId, blockedPeriodId);
        requireClinic(blockedPeriod, request.clinicUnitId());
        requireVersion(blockedPeriod, version);
        blockedPeriod.update(activePractitioner(organizationId, request.practitionerId()), request.startAt(), request.endAt());
        blockedPeriods.flush();
        return response(blockedPeriod);
    }

    @Transactional
    public boolean delete(UUID organizationId, UUID clinicUnitId, UUID blockedPeriodId, long version) {
        managedClinic(organizationId, clinicUnitId);
        AppointmentBlockedPeriod blockedPeriod = scopedBlockedPeriod(organizationId, blockedPeriodId);
        requireClinic(blockedPeriod, clinicUnitId);
        requireVersion(blockedPeriod, version);
        blockedPeriods.delete(blockedPeriod);
        return true;
    }

    private ClinicUnit managedClinic(UUID organizationId, UUID clinicUnitId) {
        authorization.requireAppointmentManagement(organizationId, clinicUnitId);
        return authorization.requireClinicInOrganization(organizationId, clinicUnitId);
    }

    private Practitioner activePractitioner(UUID organizationId, UUID practitionerId) {
        if (practitionerId == null) {
            return null;
        }
        Practitioner practitioner = practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!practitioner.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return practitioner;
    }

    private AppointmentBlockedPeriod scopedBlockedPeriod(UUID organizationId, UUID blockedPeriodId) {
        return blockedPeriods.findByGlobalIdAndOrganization_GlobalId(blockedPeriodId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validateRange(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void requireClinic(AppointmentBlockedPeriod blockedPeriod, UUID clinicUnitId) {
        if (!blockedPeriod.getClinicUnit().getGlobalId().equals(clinicUnitId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void requireVersion(AppointmentBlockedPeriod blockedPeriod, long version) {
        if (blockedPeriod.getVersion() != version) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    private AppointmentBlockedPeriodResponse response(AppointmentBlockedPeriod blockedPeriod) {
        Practitioner practitioner = blockedPeriod.getPractitioner();
        return new AppointmentBlockedPeriodResponse(
                blockedPeriod.getGlobalId(),
                blockedPeriod.getClinicUnit().getGlobalId(),
                practitioner == null ? null : practitioner.getGlobalId(),
                blockedPeriod.getStartAt(),
                blockedPeriod.getEndAt(),
                blockedPeriod.getVersion());
    }
}
