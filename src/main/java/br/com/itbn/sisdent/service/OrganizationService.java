package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ClinicUnitRequest;
import br.com.itbn.sisdent.dto.ClinicUnitResponse;
import br.com.itbn.sisdent.dto.MembershipRequest;
import br.com.itbn.sisdent.dto.MembershipResponse;
import br.com.itbn.sisdent.dto.OrganizationRequest;
import br.com.itbn.sisdent.dto.OrganizationResponse;
import br.com.itbn.sisdent.dto.AccountMembershipRequest;
import br.com.itbn.sisdent.dto.MembershipRevokeRequest;
import br.com.itbn.sisdent.dto.MembershipRoleUpdateRequest;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.ClinicUnitWorkingHours;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.ClinicUnitWorkingHoursRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final ClinicUnitRepository clinicUnitRepository;
    private final MembershipRepository membershipRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final ScopeAuthorizationService authorization;
    private final ClinicUnitWorkingHoursRepository workingHours;

    public OrganizationService(OrganizationRepository organizationRepository,
            ClinicUnitRepository clinicUnitRepository, MembershipRepository membershipRepository,
            AccountRepository accountRepository, PersonRepository personRepository,
            PasswordEncoder passwordEncoder, ScopeAuthorizationService authorization, ClinicUnitWorkingHoursRepository workingHours) {
        this.organizationRepository = organizationRepository; this.clinicUnitRepository = clinicUnitRepository;
        this.membershipRepository = membershipRepository; this.accountRepository = accountRepository;
        this.personRepository = personRepository; this.passwordEncoder = passwordEncoder; this.authorization = authorization; this.workingHours = workingHours;
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        authorization.requirePlatformAdministrator();
        Organization organization = organizationRepository.saveAndFlush(new Organization(request.name()));
        return new OrganizationResponse(organization.getGlobalId(), organization.getName(), organization.isActive());
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizationsForPlatform() {
        authorization.requirePlatformAdministrator();
        return organizationRepository.findAll().stream()
                .filter(Organization::isActive)
                .sorted(java.util.Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER))
                .map(organization -> new OrganizationResponse(organization.getGlobalId(), organization.getName(), true))
                .toList();
    }

    @Transactional
    public ClinicUnitResponse createClinicUnit(UUID organizationId, ClinicUnitRequest request) {
        authorization.requireOrganizationAdministration(organizationId);
        Organization organization = requireOrganization(organizationId);
        ClinicUnit unit = clinicUnitRepository.saveAndFlush(new ClinicUnit(organization, request.name()));
        for (int day = 1; day <= 7; day++) workingHours.save(new ClinicUnitWorkingHours(unit, day, 0, 1440));
        return new ClinicUnitResponse(unit.getGlobalId(), organizationId, unit.getName(), unit.isActive(), unit.getTimezone());
    }

    @Transactional(readOnly = true)
    public List<ClinicUnitResponse> listClinicUnits(UUID organizationId, UUID clinicUnitId) {
        if (!authorization.isPlatformAdministrator()) {
            authorization.requireAppointmentRead(organizationId, clinicUnitId);
        }
        List<ClinicUnit> units = clinicUnitRepository.findAllByOrganization_GlobalIdAndActiveTrueOrderByName(organizationId);
        if (clinicUnitId != null) {
            units = units.stream().filter(unit -> unit.getGlobalId().equals(clinicUnitId)).toList();
        }
        return units.stream()
                .map(unit -> new ClinicUnitResponse(unit.getGlobalId(), organizationId, unit.getName(), unit.isActive(), unit.getTimezone()))
                .toList();
    }

    @Transactional
    public MembershipResponse addMembership(UUID organizationId, MembershipRequest request) {
        authorization.requireAccountAdministration(organizationId);
        Organization organization = requireOrganization(organizationId);
        ClinicUnit clinicUnit = request.clinicUnitId() == null ? null
                : authorization.requireClinicInOrganization(organizationId, request.clinicUnitId());
        requireOrganizationWideRole(request.role(), clinicUnit);
        String email = Account.normalizeEmail(request.email());
        Account account = accountRepository.findByEmail(email).orElseGet(() -> {
            Person person = personRepository.save(new Person(request.displayName()));
            return accountRepository.save(new Account(person, email,
                    passwordEncoder.encode(request.password()), false));
        });
        requirePlatformAccountManagementAllowed(account);
        boolean duplicate = clinicUnit == null
                ? membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(
                        account.getId(), organization.getId())
                : membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnit_Id(
                        account.getId(), organization.getId(), clinicUnit.getId());
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A membership already exists for this scope");
        }
        Membership membership = membershipRepository.saveAndFlush(new Membership(account, organization, clinicUnit, request.role()));
        assignAccountManagementOrganization(account, organization, clinicUnit, request.role());
        return toResponse(membership);
    }

    /** Exact email lookup supports a deliberate grant without exposing a global account directory. */
    @Transactional
    public MembershipResponse grantMembership(UUID organizationId, AccountMembershipRequest request) {
        authorization.requireAccountAdministration(organizationId);
        Organization organization = requireOrganization(organizationId);
        ClinicUnit clinicUnit = request.clinicUnitId() == null ? null
                : authorization.requireClinicInOrganization(organizationId, request.clinicUnitId());
        requireOrganizationWideRole(request.role(), clinicUnit);
        Account account = accountRepository.findByEmail(Account.normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requirePlatformAccountManagementAllowed(account);
        boolean duplicate = clinicUnit == null
                ? membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(account.getId(), organization.getId())
                : membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnit_Id(account.getId(), organization.getId(), clinicUnit.getId());
        if (duplicate) throw new ResponseStatusException(HttpStatus.CONFLICT, "A membership already exists for this scope");
        Membership membership = membershipRepository.saveAndFlush(new Membership(account, organization, clinicUnit, request.role()));
        assignAccountManagementOrganization(account, organization, clinicUnit, request.role());
        return toResponse(membership);
    }

    @Transactional
    public void revokeMembership(UUID organizationId, UUID membershipId) {
        authorization.requireAccountAdministration(organizationId);
        Membership membership = membershipRepository.findByGlobalId(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!membership.getOrganization().getGlobalId().equals(organizationId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Membership is outside the organization scope");
        }
        requirePlatformAccountManagementAllowed(membership.getAccount());
        membership.revoke();
        membershipRepository.save(membership);
    }

    @Transactional
    public void revokeMembership(UUID organizationId, UUID membershipId, MembershipRevokeRequest request) {
        authorization.requireAccountAdministration(organizationId);
        Membership membership = membershipRepository.findByGlobalId(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!membership.getOrganization().getGlobalId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (membership.getVersion() != request.version() || !membership.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The membership was changed by another request");
        }
        requirePlatformAccountManagementAllowed(membership.getAccount());
        membership.revoke();
        membershipRepository.saveAndFlush(membership);
    }

    @Transactional
    public MembershipResponse changeMembershipRole(UUID organizationId, UUID membershipId,
            MembershipRoleUpdateRequest request) {
        authorization.requireAccountAdministration(organizationId);
        Membership membership = membershipRepository.findByGlobalId(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!membership.getOrganization().getGlobalId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!membership.isActive() || membership.getVersion() != request.version()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The membership was changed by another request");
        }
        requirePlatformAccountManagementAllowed(membership.getAccount());
        requireOrganizationWideRole(request.role(), membership.getClinicUnit());
        membership.changeRole(request.role());
        return toResponse(membershipRepository.saveAndFlush(membership));
    }

    private Organization requireOrganization(UUID id) {
        return organizationRepository.findByGlobalId(id)
                .filter(Organization::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private void requireOrganizationWideRole(MembershipRole role, ClinicUnit clinicUnit) {
        if (clinicUnit != null && (role == MembershipRole.ORGANIZATION_ADMIN
                || role == MembershipRole.PRACTITIONER_MANAGER)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This role requires organization-wide scope");
        }
    }

    /** Only platform administrators may change the organization memberships of a platform account. */
    private void requirePlatformAccountManagementAllowed(Account account) {
        if (account.isPlatformAdministrator() && !authorization.isPlatformAdministrator()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Organization administrators cannot change a platform administrator account");
        }
    }

    private void assignAccountManagementOrganization(Account account, Organization organization, ClinicUnit clinicUnit,
            MembershipRole role) {
        if (role == MembershipRole.ORGANIZATION_ADMIN && clinicUnit == null) {
            account.assignAccountManagementOrganizationIfAbsent(organization);
        }
    }

    static MembershipResponse toResponse(Membership membership) {
        ClinicUnit clinic = membership.getClinicUnit();
        return new MembershipResponse(membership.getGlobalId(),
                membership.getOrganization().getGlobalId(), membership.getOrganization().getName(),
                clinic == null ? null : clinic.getGlobalId(), clinic == null ? null : clinic.getName(),
                membership.getRole(), membership.getVersion());
    }
}
