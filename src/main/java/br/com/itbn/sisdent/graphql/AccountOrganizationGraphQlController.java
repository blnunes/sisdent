package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AccountCreateRequest;
import br.com.itbn.sisdent.dto.AccountLifecycleRequest;
import br.com.itbn.sisdent.dto.AccountMembershipRequest;
import br.com.itbn.sisdent.dto.AccountPlatformAdministratorRequest;
import br.com.itbn.sisdent.dto.AccountResponse;
import br.com.itbn.sisdent.dto.MembershipRequest;
import br.com.itbn.sisdent.dto.MembershipResponse;
import br.com.itbn.sisdent.dto.MembershipRevokeRequest;
import br.com.itbn.sisdent.dto.MembershipRoleUpdateRequest;
import br.com.itbn.sisdent.dto.OrganizationRequest;
import br.com.itbn.sisdent.dto.OrganizationResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.service.AccountManagementService;
import br.com.itbn.sisdent.service.OrganizationService;
import br.com.itbn.sisdent.service.PractitionerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL adapter for platform accounts and organization administration. */
@Controller
public class AccountOrganizationGraphQlController {
    private final AccountManagementService accounts;
    private final OrganizationService organizations;
    private final PractitionerService practitioners;

    public AccountOrganizationGraphQlController(
            AccountManagementService accounts,
            OrganizationService organizations,
            PractitionerService practitioners) {
        this.accounts = accounts;
        this.organizations = organizations;
        this.practitioners = practitioners;
    }

    @QueryMapping
    public AccountResponse currentAccount() {
        return accounts.currentAccount();
    }

    @QueryMapping
    public PageResponse<AccountResponse> platformAccounts(@Argument AccountPageInput page, @Argument String filter) {
        return accounts.platformPage(toPageQuery(page), filter);
    }

    @QueryMapping
    public AccountResponse platformAccount(@Argument UUID accountId) {
        return accounts.platformRead(accountId);
    }

    @QueryMapping
    public PageResponse<AccountResponse> organizationAccounts(
            @Argument UUID organizationId,
            @Argument AccountPageInput page,
            @Argument String filter) {
        return accounts.organizationPage(organizationId, toPageQuery(page), filter);
    }

    @QueryMapping
    public AccountResponse organizationAccount(@Argument UUID organizationId, @Argument UUID accountId) {
        return accounts.organizationRead(organizationId, accountId);
    }

    @QueryMapping
    public List<OrganizationResponse> platformOrganizations() {
        return organizations.listOrganizationsForPlatform();
    }

    @MutationMapping
    public AccountResponse createPlatformAccount(@Argument @Valid AccountCreateRequest input) {
        return accounts.create(input);
    }

    @MutationMapping
    public AccountResponse changeAccountLifecycle(
            @Argument UUID accountId,
            @Argument @Valid AccountLifecycleRequest input) {
        return accounts.changeLifecycle(accountId, input);
    }

    @MutationMapping
    public AccountResponse changeAccountPlatformAdministrator(
            @Argument UUID accountId,
            @Argument @Valid AccountPlatformAdministratorRequest input) {
        return accounts.changePlatformAdministrator(accountId, input);
    }

    @MutationMapping
    public OrganizationResponse createOrganization(@Argument @Valid OrganizationRequest input) {
        return organizations.createOrganization(input);
    }

    @MutationMapping
    public MembershipResponse createMembership(@Argument UUID organizationId, @Argument @Valid MembershipRequest input) {
        return organizations.addMembership(organizationId, input);
    }

    @MutationMapping
    public MembershipResponse grantMembership(
            @Argument UUID organizationId,
            @Argument @Valid AccountMembershipRequest input) {
        return organizations.grantMembership(organizationId, input);
    }

    @MutationMapping
    public MembershipResponse changeMembershipRole(
            @Argument UUID organizationId,
            @Argument UUID membershipId,
            @Argument @Valid MembershipRoleUpdateRequest input) {
        return organizations.changeMembershipRole(organizationId, membershipId, input);
    }

    @MutationMapping
    public boolean revokeMembership(
            @Argument UUID organizationId,
            @Argument UUID membershipId,
            @Argument @Valid MembershipRevokeRequest input) {
        organizations.revokeMembership(organizationId, membershipId, input);
        return true;
    }

    @MutationMapping
    public boolean deactivatePractitioner(@Argument UUID organizationId, @Argument UUID practitionerId) {
        practitioners.deactivate(organizationId, practitionerId);
        return true;
    }

    private static PageQuery toPageQuery(AccountPageInput page) {
        return page == null ? new PageQuery(null, null, null, null) : page.toPageQuery();
    }

    public record AccountPageInput(Integer page, Integer size, String sort, String direction) {
        PageQuery toPageQuery() {
            return new PageQuery(page, size, sort, direction);
        }
    }
}
