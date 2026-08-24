package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.*;
import br.com.itbn.sisdent.model.*;
import br.com.itbn.sisdent.repository.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.*;

import java.util.*;

@Service
public class PerformedProcedureService {
    private final AppointmentRepository appointments;
    private final DentalProcedureRepository catalog;
    private final PerformedProcedureRepository records;
    private final ScopeAuthorizationService authorization;
    private final CurrentAccountService current;

    public PerformedProcedureService(AppointmentRepository a, DentalProcedureRepository c, PerformedProcedureRepository r, ScopeAuthorizationService z, CurrentAccountService x) {
        appointments = a;
        catalog = c;
        records = r;
        authorization = z;
        current = x;
    }

    @Transactional(readOnly = true)
    public List<PerformedProcedureResponse> list(UUID org, UUID clinic, UUID appointment) {
        authorization.requireAppointmentRead(org, clinic);
        Appointment a = requireAppointment(org, appointment);
        check(a, clinic);
        return records.findAllByAppointment_GlobalIdAndAppointment_Organization_GlobalIdOrderByPerformedAt(appointment, org).stream().map(this::response).toList();
    }

    @Transactional
    public PerformedProcedureResponse create(UUID org, UUID clinic, UUID appointment, PerformedProcedureRequest r) {
        authorization.requireAppointmentManagement(org, clinic);
        Appointment a = requireAppointment(org, appointment);
        check(a, clinic);
        if (a.getStatus() != AppointmentStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Performed procedures require a completed appointment");
        DentalProcedure d = catalog.findById(r.dentalProcedureId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (d.getStatus() != CatalogStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dental procedure is inactive");
        return response(records.save(new PerformedProcedure(a, d, r.performedAt(), r.administrativeNote())));
    }

    @Transactional
    public PerformedProcedureResponse voidRecord(UUID org, UUID clinic, UUID id, VoidPerformedProcedureRequest r) {
        authorization.requireAppointmentManagement(org, clinic);
        PerformedProcedure p = records.findByGlobalIdAndAppointment_Organization_GlobalId(id, org).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        check(p.getAppointment(), clinic);
        try {
            p.voidRecord(r.reason(), current.require().getGlobalId().toString());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return response(p);
    }

    private Appointment requireAppointment(UUID org, UUID id) {
        return appointments.findByGlobalIdAndOrganization_GlobalId(id, org).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void check(Appointment a, UUID clinic) {
        if (clinic != null && !a.getClinicUnit().getGlobalId().equals(clinic))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private PerformedProcedureResponse response(PerformedProcedure p) {
        return new PerformedProcedureResponse(p.getGlobalId(), p.getDentalProcedure().getId(), p.getProcedureNameSnapshot(), p.getPerformedAt(), p.getAdministrativeNote(), p.getVoidedAt(), p.getVoidedBy(), p.getVoidReason());
    }
}
