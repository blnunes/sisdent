package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public List<PatientResponse> findAll() {
        return patientService.findAll();
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
}
