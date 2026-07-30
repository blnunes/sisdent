package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.ClinicUnitRequest;
import br.com.itbn.sisdent.dto.ClinicUnitResponse;
import br.com.itbn.sisdent.dto.MembershipRequest;
import br.com.itbn.sisdent.dto.MembershipResponse;
import br.com.itbn.sisdent.dto.OrganizationRequest;
import br.com.itbn.sisdent.dto.OrganizationResponse;
import br.com.itbn.sisdent.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class OrganizationController {
    private final OrganizationService organizationService;
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/api/platform/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.createOrganization(request));
    }

    @PostMapping("/api/organizations/{organizationId}/clinic-units")
    public ResponseEntity<ClinicUnitResponse> createClinicUnit(@PathVariable UUID organizationId,
            @Valid @RequestBody ClinicUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createClinicUnit(organizationId, request));
    }

    @PostMapping("/api/organizations/{organizationId}/memberships")
    public ResponseEntity<MembershipResponse> addMembership(@PathVariable UUID organizationId,
            @Valid @RequestBody MembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.addMembership(organizationId, request));
    }

    @DeleteMapping("/api/organizations/{organizationId}/memberships/{membershipId}")
    public ResponseEntity<Void> revokeMembership(@PathVariable UUID organizationId,
            @PathVariable UUID membershipId) {
        organizationService.revokeMembership(organizationId, membershipId);
        return ResponseEntity.noContent().build();
    }
}
