package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.*;
import br.com.itbn.sisdent.model.*;
import br.com.itbn.sisdent.service.*;
import jakarta.validation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/organizations/{organizationId}/appointments")
public class AppointmentController {
    private final AppointmentService service;
    private final PerformedProcedureService procedures;

    public AppointmentController(AppointmentService s, PerformedProcedureService p) {
        service = s;
        procedures = p;
    }

    @GetMapping
    public PageResponse<AppointmentResponse> list(@PathVariable UUID organizationId, @RequestParam(required = false) UUID clinicUnitId, @RequestParam Instant from, @RequestParam(required = false) Instant to, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return service.list(organizationId, clinicUnitId, from, to, page, size);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@PathVariable UUID organizationId, @Valid @RequestBody AppointmentRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(organizationId, r));
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable UUID organizationId, @PathVariable UUID id, @RequestParam(required = false) UUID clinicUnitId) {
        return service.get(organizationId, clinicUnitId, id);
    }

    @PutMapping("/{id}/reschedule")
    public AppointmentResponse reschedule(@PathVariable UUID organizationId, @PathVariable UUID id, @Valid @RequestBody AppointmentRequest r) {
        return service.reschedule(organizationId, id, r);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID id, @RequestParam UUID clinicUnitId) {
        return service.transition(organizationId, clinicUnitId, id, AppointmentStatus.CANCELLED);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable UUID organizationId, @PathVariable UUID id, @RequestParam UUID clinicUnitId) {
        return service.transition(organizationId, clinicUnitId, id, AppointmentStatus.COMPLETED);
    }

    @PostMapping("/{id}/no-show")
    public AppointmentResponse noShow(@PathVariable UUID organizationId, @PathVariable UUID id, @RequestParam UUID clinicUnitId) {
        return service.transition(organizationId, clinicUnitId, id, AppointmentStatus.NO_SHOW);
    }

    @GetMapping("/{id}/performed-procedures")
    public List<PerformedProcedureResponse> procedures(@PathVariable UUID organizationId, @PathVariable UUID id, @RequestParam(required = false) UUID clinicUnitId) {
        return procedures.list(organizationId, clinicUnitId, id);
    }

    @PostMapping("/{id}/performed-procedures")
    public ResponseEntity<PerformedProcedureResponse> addProcedure(@PathVariable UUID organizationId, @PathVariable UUID id, @RequestParam UUID clinicUnitId, @Valid @RequestBody PerformedProcedureRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procedures.create(organizationId, clinicUnitId, id, r));
    }

    @PostMapping("/performed-procedures/{procedureId}/void")
    public PerformedProcedureResponse voidProcedure(@PathVariable UUID organizationId, @PathVariable UUID procedureId, @RequestParam UUID clinicUnitId, @Valid @RequestBody VoidPerformedProcedureRequest r) {
        return procedures.voidRecord(organizationId, clinicUnitId, procedureId, r);
    }
}
