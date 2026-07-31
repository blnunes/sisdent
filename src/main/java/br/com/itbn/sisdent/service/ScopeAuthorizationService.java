package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ScopeAuthorizationService {
    private final CurrentAccountService currentAccountService;
    private final MembershipRepository membershipRepository;
    private final ClinicUnitRepository clinicUnitRepository;

    public ScopeAuthorizationService(CurrentAccountService currentAccountService,
            MembershipRepository membershipRepository, ClinicUnitRepository clinicUnitRepository) {
        this.currentAccountService = currentAccountService;
        this.membershipRepository = membershipRepository;
        this.clinicUnitRepository = clinicUnitRepository;
    }

    public void requirePlatformAdministrator() {
        if (!currentAccountService.require().isPlatformAdministrator()) {
            throw new AccessDeniedException("Platform administrator access is required");
        }
    }

    public boolean isPlatformAdministrator() {
        return currentAccountService.require().isPlatformAdministrator();
    }

    public void requireRead(UUID organizationId, UUID clinicUnitId) {
        if (matchingMemberships(organizationId, clinicUnitId).isEmpty()) {
            throw new AccessDeniedException("No active membership grants access to this scope");
        }
    }

    public void requireWrite(UUID organizationId, UUID clinicUnitId) {
        boolean allowed = matchingMemberships(organizationId, clinicUnitId).stream()
                .anyMatch(membership -> membership.getRole() == MembershipRole.ORGANIZATION_ADMIN
                        || membership.getRole() == MembershipRole.MANAGER);
        if (!allowed) {
            throw new AccessDeniedException("No active membership grants write access to this scope");
        }
    }

    public void requirePractitionerManagement(UUID organizationId, UUID clinicUnitId) {
        requireRole(organizationId, clinicUnitId, MembershipRole.PRACTITIONER_MANAGER);
    }

    public void requireAppointmentRead(UUID organizationId, UUID clinicUnitId) {
        if (matchingMemberships(organizationId, clinicUnitId).stream().noneMatch(m -> m.getRole() != MembershipRole.PRACTITIONER_MANAGER)) {
            throw new AccessDeniedException("No active membership grants appointment read access");
        }
    }

    public void requireAppointmentManagement(UUID organizationId, UUID clinicUnitId) {
        requireRole(organizationId, clinicUnitId, MembershipRole.APPOINTMENT_MANAGER);
    }

    public void requireClinicalRead(UUID organizationId, UUID clinicUnitId) {
        boolean allowed = matchingMemberships(organizationId, clinicUnitId).stream().anyMatch(m ->
                m.getRole() == MembershipRole.ORGANIZATION_ADMIN || m.getRole() == MembershipRole.CLINICAL_READER
                        || m.getRole() == MembershipRole.CLINICAL_AUTHOR || m.getRole() == MembershipRole.CLINICAL_MANAGER);
        if (!allowed) throw new AccessDeniedException("No active membership grants clinical read access");
    }

    public void requireClinicalAuthor(UUID organizationId, UUID clinicUnitId) {
        requireClinicalRole(organizationId, clinicUnitId, MembershipRole.CLINICAL_AUTHOR);
    }

    public void requireClinicalManagement(UUID organizationId, UUID clinicUnitId) {
        requireClinicalRole(organizationId, clinicUnitId, MembershipRole.CLINICAL_MANAGER);
    }

    private void requireClinicalRole(UUID organizationId, UUID clinicUnitId, MembershipRole role) {
        boolean allowed = matchingMemberships(organizationId, clinicUnitId).stream().anyMatch(m ->
                m.getRole() == MembershipRole.ORGANIZATION_ADMIN || m.getRole() == role || (role == MembershipRole.CLINICAL_AUTHOR && m.getRole() == MembershipRole.CLINICAL_MANAGER));
        if (!allowed) throw new AccessDeniedException("No active membership grants this clinical access");
    }

    private void requireRole(UUID organizationId, UUID clinicUnitId, MembershipRole role) {
        boolean allowed = matchingMemberships(organizationId, clinicUnitId).stream().anyMatch(m ->
                m.getRole() == MembershipRole.ORGANIZATION_ADMIN || m.getRole() == MembershipRole.MANAGER || m.getRole() == role);
        if (!allowed) throw new AccessDeniedException("No active membership grants this operational access");
    }

    public void requireOrganizationAdministration(UUID organizationId) {
        if (currentAccountService.require().isPlatformAdministrator()) {
            return;
        }
        boolean allowed = membershipRepository.findAllByAccount_IdAndOrganization_GlobalIdAndActiveTrue(
                        currentAccountService.require().getId(), organizationId).stream()
                .anyMatch(membership -> membership.getClinicUnit() == null
                        && membership.getRole() == MembershipRole.ORGANIZATION_ADMIN);
        if (!allowed) {
            throw new AccessDeniedException("Organization administrator access is required");
        }
    }

    public ClinicUnit requireClinicInOrganization(UUID organizationId, UUID clinicUnitId) {
        ClinicUnit clinicUnit = clinicUnitRepository.findByGlobalId(clinicUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinic unit not found"));
        if (!clinicUnit.getOrganization().getGlobalId().equals(organizationId)) {
            throw new AccessDeniedException("Clinic unit is outside the organization scope");
        }
        return clinicUnit;
    }

    private List<Membership> matchingMemberships(UUID organizationId, UUID clinicUnitId) {
        return membershipRepository.findAllByAccount_IdAndOrganization_GlobalIdAndActiveTrue(
                        currentAccountService.require().getId(), organizationId).stream()
                .filter(membership -> membership.getClinicUnit() == null
                        || (clinicUnitId != null
                        && membership.getClinicUnit().getGlobalId().equals(clinicUnitId)))
                .toList();
    }
}
