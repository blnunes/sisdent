package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.ExactPatientMatchRequest;
import br.com.itbn.sisdent.dto.ExactPatientMatchResponse;
import br.com.itbn.sisdent.dto.PatientLinkRequest;
import br.com.itbn.sisdent.dto.PatientLinkResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.service.OrganizationPatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class OrganizationPatientController {
    private final OrganizationPatientService patientService;
    public OrganizationPatientController(OrganizationPatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/api/organizations/{organizationId}/patients")
    public PageResponse<PatientResponse> search(@PathVariable UUID organizationId,
            @RequestParam(required = false) UUID clinicUnitId,
            @RequestParam(required = false) String name) {
        return patientService.search(organizationId, clinicUnitId, name);
    }

    @GetMapping("/api/organizations/{organizationId}/patients/filter-options")
    public List<FilterOptionResponse> filterOptions(@PathVariable UUID organizationId,
            @RequestParam(required = false) UUID clinicUnitId, @RequestParam String field,
            @RequestParam(required = false) String query) {
        return patientService.filterOptions(organizationId, clinicUnitId, field, query);
    }

    @PostMapping("/api/organizations/{organizationId}/patients")
    public ResponseEntity<PatientResponse> create(@PathVariable UUID organizationId,
            @RequestParam(required = false) UUID clinicUnitId,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.create(organizationId, clinicUnitId, request));
    }

    @PutMapping("/api/organizations/{organizationId}/patients/{patientId}")
    public PatientResponse update(@PathVariable UUID organizationId, @PathVariable UUID patientId,
            @RequestParam(required = false) UUID clinicUnitId,
            @Valid @RequestBody PatientRequest request) {
        return patientService.update(organizationId, clinicUnitId, patientId, request);
    }

    @DeleteMapping("/api/organizations/{organizationId}/patients/{patientId}")
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId, @PathVariable UUID patientId,
            @RequestParam(required = false) UUID clinicUnitId) {
        patientService.delete(organizationId, clinicUnitId, patientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/organizations/{organizationId}/patient-intake/exact-match")
    public ExactPatientMatchResponse exactMatch(@PathVariable UUID organizationId,
            @RequestParam(required = false) UUID clinicUnitId,
            @Valid @RequestBody ExactPatientMatchRequest request) {
        return patientService.exactMatch(organizationId, clinicUnitId, request);
    }

    @PostMapping("/api/organizations/{organizationId}/patient-links")
    public ResponseEntity<PatientLinkResponse> link(@PathVariable UUID organizationId,
            @Valid @RequestBody PatientLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.link(organizationId, request));
    }
}
