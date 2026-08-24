package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.*;
import br.com.itbn.sisdent.model.*;
import br.com.itbn.sisdent.repository.*;

import java.time.*;
import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClinicalRecordService {
    private final ClinicalEncounterRepository encounters;
    private final PatientOrganizationLinkRepository links;
    private final OrganizationRepository organizations;
    private final AppointmentRepository appointments;
    private final PractitionerRepository practitioners;
    private final ScopeAuthorizationService authorization;
    private final CurrentAccountService current;

    public ClinicalRecordService(ClinicalEncounterRepository e, PatientOrganizationLinkRepository l, OrganizationRepository o, AppointmentRepository a, PractitionerRepository p, ScopeAuthorizationService z, CurrentAccountService x) {
        encounters = e;
        links = l;
        organizations = o;
        appointments = a;
        practitioners = p;
        authorization = z;
        current = x;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClinicalEncounterResponse> list(UUID org, UUID clinic, UUID patient, int page, int size) {
        authorization.requireClinicalRead(org, clinic);
        return PageResponse.from(encounters.findAllByOrganization_GlobalIdAndPatientLink_Patient_GlobalIdAndClinicUnit_GlobalId(org, patient, clinic, page(page, size, "careAt")), this::response);
    }

    @Transactional(readOnly = true)
    public ClinicalEncounterResponse get(UUID org, UUID clinic, UUID id) {
        authorization.requireClinicalRead(org, clinic);
        ClinicalEncounter e = require(org, id);
        scope(e, clinic);
        return response(e);
    }

    @Transactional
    public ClinicalEncounterResponse create(UUID org, ClinicalEncounterCreateRequest r) {
        authorization.requireClinicalAuthor(org, r.clinicUnitId());
        return response(encounters.save(newEncounter(org, r, null, null)));
    }

    @Transactional
    public ClinicalEncounterResponse update(UUID org, UUID id, ClinicalEncounterRequest r) {
        authorization.requireClinicalAuthor(org, r.clinicUnitId());
        ClinicalEncounter e = require(org, id);
        scope(e, r.clinicUnitId());
        if (e.getStatus() != EncounterStatus.DRAFT
                || !java.util.Objects.equals(e.getCreatedBy(), current.require().getGlobalId().toString()))
            throw state();
        version(e.getVersion(), r.version());
        Parts p = parts(org, r.clinicUnitId(), r.patientId(), r.appointmentId(), r.practitionerId(), r.careAt(), r.careTimezone());
        if (!e.getPatientLink().getId().equals(p.link.getId())) throw notFound();
        e.update(r.careAt(), p.zone, r.narrative().strip(), blank(r.administrativeNote()), p.practitioner, p.appointment);
        return response(e);
    }

    @Transactional
    public ClinicalEncounterResponse finalizeRecord(UUID org, UUID clinic, UUID id) {
        authorization.requireClinicalManagement(org, clinic);
        ClinicalEncounter e = require(org, id);
        scope(e, clinic);
        try {
            e.finalizeRecord(current.require().getGlobalId().toString());
        } catch (IllegalStateException _) {
            throw state();
        }
        return response(e);
    }

    @Transactional
    public ClinicalEncounterResponse amend(UUID org, UUID id, AmendEncounterRequest r) {
        authorization.requireClinicalManagement(org, r.clinicUnitId());
        ClinicalEncounter original = require(org, id);
        scope(original, r.clinicUnitId());
        if (original.getStatus() != EncounterStatus.FINAL) throw state();
        ClinicalEncounterCreateRequest create = new ClinicalEncounterCreateRequest(r.clinicUnitId(), original.getPatientLink().getPatient().getGlobalId(), r.appointmentId(), r.practitionerId(), r.careAt(), r.careTimezone(), r.narrative(), r.administrativeNote());
        ClinicalEncounter amendment = newEncounter(org, create, original, r.reason().strip());
        amendment.finalizeRecord(current.require().getGlobalId().toString());
        return response(encounters.save(amendment));
    }

    @Transactional(readOnly = true)
    public List<ClinicalEncounterResponse> amendments(UUID org, UUID clinic, UUID id) {
        authorization.requireClinicalRead(org, clinic);
        ClinicalEncounter e = require(org, id);
        scope(e, clinic);
        return encounters.findAllByOriginalEncounter_GlobalIdAndOrganization_GlobalIdOrderByCareAtAsc(id, org).stream().map(this::response).toList();
    }

    private ClinicalEncounter newEncounter(UUID org, ClinicalEncounterCreateRequest r, ClinicalEncounter original, String reason) {
        Parts p = parts(org, r.clinicUnitId(), r.patientId(), r.appointmentId(), r.practitionerId(), r.careAt(), r.careTimezone());
        Organization o = organizations.findByGlobalId(org).orElseThrow(this::notFound);
        return new ClinicalEncounter(ClinicalEncounter.builder().organization(o).clinicUnit(p.clinic).patientLink(p.link).appointment(p.appointment).practitioner(p.practitioner).careAt(r.careAt()).careTimezone(p.zone).narrative(r.narrative().strip()).administrativeNote(blank(r.administrativeNote())).originalEncounter(original).amendmentReason(reason));
    }

    private Parts parts(UUID org, UUID clinicId, UUID patient, UUID appointment, UUID practitioner, Instant at, String zone) {
        if (at == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "careAt is required");
        ClinicUnit c = authorization.requireClinicInOrganization(org, clinicId);
        PatientOrganizationLink l = links.findFirstByPatient_GlobalIdAndOrganization_GlobalId(patient, org).filter(PatientOrganizationLink::isActive).orElseThrow(this::notFound);
        if (l.getClinicUnit() != null && !l.getClinicUnit().getGlobalId().equals(clinicId)) throw notFound();
        Practitioner p = null;
        if (practitioner != null) {
            p = practitioners.findByGlobalIdAndOrganization_GlobalId(practitioner, org).filter(Practitioner::isActive).orElseThrow(this::notFound);
        }
        Appointment a = null;
        if (appointment != null) {
            a = appointments.findByGlobalIdAndOrganization_GlobalId(appointment, org).orElseThrow(this::notFound);
            if (a.getStatus() != AppointmentStatus.COMPLETED || !a.getClinicUnit().getGlobalId().equals(clinicId) || !a.getPatientLink().getId().equals(l.getId()))
                throw notFound();
        }
        return new Parts(c, l, p, a, zone(zone));
    }

    private void scope(ClinicalEncounter e, UUID clinic) {
        if (!e.getClinicUnit().getGlobalId().equals(clinic) || !e.getPatientLink().isActive()) throw notFound();
    }

    private ClinicalEncounter require(UUID org, UUID id) {
        return encounters.findByGlobalIdAndOrganization_GlobalId(id, org).orElseThrow(this::notFound);
    }

    private String zone(String z) {
        try {
            return ZoneId.of(z.strip()).getId();
        } catch (Exception _) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid IANA timezone is required");
        }
    }

    private Pageable page(int p, int s, String sort) {
        if (p < 0 || s < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        return PageRequest.of(p, Math.min(s, 100), Sort.by(sort).descending().and(Sort.by("globalId").ascending()));
    }

    private void version(long actual, Long submitted) {
        if (submitted == null || actual != submitted)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stale clinical record version");
    }

    private ResponseStatusException state() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Invalid clinical encounter state transition");
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private String blank(String s) {
        return s == null ? null : s.strip();
    }

    private ClinicalEncounterResponse response(ClinicalEncounter e) {
        return new ClinicalEncounterResponse(e.getGlobalId(), e.getClinicUnit().getGlobalId(), e.getPatientLink().getPatient().getGlobalId(), e.getAppointment() == null ? null : e.getAppointment().getGlobalId(), e.getPractitioner() == null ? null : e.getPractitioner().getGlobalId(), e.getCareAt(), e.getCareTimezone(), e.getNarrative(), e.getAdministrativeNote(), e.getStatus(), e.getFinalizedAt(), e.getFinalizedBy(), e.getOriginalEncounter() == null ? null : e.getOriginalEncounter().getGlobalId(), e.getAmendmentReason(), e.getVersion());
    }

    private record Parts(ClinicUnit clinic, PatientOrganizationLink link, Practitioner practitioner,
                         Appointment appointment, String zone) {
    }
}
