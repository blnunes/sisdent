package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.service.PatientService;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.filter.PatientFilter;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.DocumentType;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public PageResponse<PatientResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDate birthDate,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String taxId,
            @RequestParam(required = false) DocumentType identificationType,
            @RequestParam(required = false) String identificationNumber,
            @RequestParam(required = false) String nationalityCode,
            @RequestParam(required = false) Long addressId,
            @RequestParam(required = false) Long specialityId) {
        return patientService.findPage(new PageQuery(page, size, sort, direction), new PatientFilter(
                id, name, birthDate, active, gender, taxId, identificationType, identificationNumber, nationalityCode, addressId, specialityId));
    }

    @GetMapping("/filter-options")
    public List<FilterOptionResponse> findFilterOptions(
            @RequestParam String field,
            @RequestParam(required = false) String query) {
        return patientService.findFilterOptions(field, query);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> findById(@PathVariable Long id) {
        return patientService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.create(request));
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) { return patientService.update(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { patientService.delete(id); return ResponseEntity.noContent().build(); }
}
