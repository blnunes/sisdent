package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.ClinicUnitRequest;
import br.com.itbn.sisdent.dto.ClinicUnitResponse;
import br.com.itbn.sisdent.dto.MembershipRequest;
import br.com.itbn.sisdent.dto.MembershipResponse;
import br.com.itbn.sisdent.dto.OrganizationRequest;
import br.com.itbn.sisdent.dto.OrganizationResponse;
import br.com.itbn.sisdent.dto.AccountMembershipRequest;
import br.com.itbn.sisdent.dto.MembershipRevokeRequest;
import br.com.itbn.sisdent.dto.MembershipRoleUpdateRequest;
import br.com.itbn.sisdent.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/api/platform/organizations")
    public java.util.List<OrganizationResponse> listOrganizations() {
        return organizationService.listOrganizationsForPlatform();
    }

    @PostMapping("/api/organizations/{organizationId}/clinic-units")
    public ResponseEntity<ClinicUnitResponse> createClinicUnit(@PathVariable UUID organizationId,
            @Valid @RequestBody ClinicUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createClinicUnit(organizationId, request));
    }

    @GetMapping("/api/organizations/{organizationId}/clinic-units")
    public java.util.List<ClinicUnitResponse> listClinicUnits(@PathVariable UUID organizationId,
            @RequestParam(required = false) UUID clinicUnitId) {
        return organizationService.listClinicUnits(organizationId, clinicUnitId);
    }

    @PostMapping("/api/organizations/{organizationId}/memberships")
    public ResponseEntity<MembershipResponse> addMembership(@PathVariable UUID organizationId,
            @Valid @RequestBody MembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.addMembership(organizationId, request));
    }

    @PostMapping("/api/organizations/{organizationId}/account-memberships")
    public ResponseEntity<MembershipResponse> grantAccountMembership(@PathVariable UUID organizationId,
            @Valid @RequestBody AccountMembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.grantMembership(organizationId, request));
    }

    @DeleteMapping("/api/organizations/{organizationId}/memberships/{membershipId}")
    public ResponseEntity<Void> revokeMembership(@PathVariable UUID organizationId,
            @PathVariable UUID membershipId) {
        organizationService.revokeMembership(organizationId, membershipId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/organizations/{organizationId}/memberships/{membershipId}/revoke")
    public ResponseEntity<Void> revokeAccountMembership(@PathVariable UUID organizationId,
            @PathVariable UUID membershipId, @Valid @RequestBody MembershipRevokeRequest request) {
        organizationService.revokeMembership(organizationId, membershipId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/organizations/{organizationId}/memberships/{membershipId}")
    public MembershipResponse changeMembershipRole(@PathVariable UUID organizationId,
            @PathVariable UUID membershipId, @Valid @RequestBody MembershipRoleUpdateRequest request) {
        return organizationService.changeMembershipRole(organizationId, membershipId, request);
    }
}
