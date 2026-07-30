package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ClinicUnitRequest;
import br.com.itbn.sisdent.dto.ClinicUnitResponse;
import br.com.itbn.sisdent.dto.MembershipRequest;
import br.com.itbn.sisdent.dto.MembershipResponse;
import br.com.itbn.sisdent.dto.OrganizationRequest;
import br.com.itbn.sisdent.dto.OrganizationResponse;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final ClinicUnitRepository clinicUnitRepository;
    private final MembershipRepository membershipRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final ScopeAuthorizationService authorization;

    public OrganizationService(OrganizationRepository organizationRepository,
            ClinicUnitRepository clinicUnitRepository, MembershipRepository membershipRepository,
            AccountRepository accountRepository, PersonRepository personRepository,
            PasswordEncoder passwordEncoder, ScopeAuthorizationService authorization) {
        this.organizationRepository = organizationRepository; this.clinicUnitRepository = clinicUnitRepository;
        this.membershipRepository = membershipRepository; this.accountRepository = accountRepository;
        this.personRepository = personRepository; this.passwordEncoder = passwordEncoder; this.authorization = authorization;
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        authorization.requirePlatformAdministrator();
        Organization organization = organizationRepository.saveAndFlush(new Organization(request.name()));
        return new OrganizationResponse(organization.getGlobalId(), organization.getName(), organization.isActive());
    }

    @Transactional
    public ClinicUnitResponse createClinicUnit(UUID organizationId, ClinicUnitRequest request) {
        authorization.requireOrganizationAdministration(organizationId);
        Organization organization = requireOrganization(organizationId);
        ClinicUnit unit = clinicUnitRepository.saveAndFlush(new ClinicUnit(organization, request.name()));
        return new ClinicUnitResponse(unit.getGlobalId(), organizationId, unit.getName(), unit.isActive());
    }

    @Transactional
    public MembershipResponse addMembership(UUID organizationId, MembershipRequest request) {
        authorization.requireOrganizationAdministration(organizationId);
        Organization organization = requireOrganization(organizationId);
        ClinicUnit clinicUnit = request.clinicUnitId() == null ? null
                : authorization.requireClinicInOrganization(organizationId, request.clinicUnitId());
        if (request.role() == MembershipRole.ORGANIZATION_ADMIN && clinicUnit != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Organization administrators must have organization-wide scope");
        }
        String email = Account.normalizeEmail(request.email());
        Account account = accountRepository.findByEmail(email).orElseGet(() -> {
            Person person = personRepository.save(new Person(request.displayName()));
            return accountRepository.save(new Account(person, null, email,
                    passwordEncoder.encode(request.password()), false, false));
        });
        boolean duplicate = clinicUnit == null
                ? membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(
                        account.getId(), organization.getId())
                : membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnit_Id(
                        account.getId(), organization.getId(), clinicUnit.getId());
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A membership already exists for this scope");
        }
        return toResponse(membershipRepository.saveAndFlush(
                new Membership(account, organization, clinicUnit, request.role())));
    }

    @Transactional
    public void revokeMembership(UUID organizationId, UUID membershipId) {
        authorization.requireOrganizationAdministration(organizationId);
        Membership membership = membershipRepository.findByGlobalId(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!membership.getOrganization().getGlobalId().equals(organizationId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Membership is outside the organization scope");
        }
        membership.revoke();
        membershipRepository.save(membership);
    }

    private Organization requireOrganization(UUID id) {
        return organizationRepository.findByGlobalId(id)
                .filter(Organization::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    static MembershipResponse toResponse(Membership membership) {
        ClinicUnit clinic = membership.getClinicUnit();
        return new MembershipResponse(membership.getGlobalId(),
                membership.getOrganization().getGlobalId(), membership.getOrganization().getName(),
                clinic == null ? null : clinic.getGlobalId(), clinic == null ? null : clinic.getName(),
                membership.getRole());
    }
}
