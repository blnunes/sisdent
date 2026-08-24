package br.com.itbn.sisdent.graphql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.itbn.sisdent.dto.AccountCreateRequest;
import br.com.itbn.sisdent.dto.AccountLifecycleRequest;
import br.com.itbn.sisdent.dto.AccountMembershipRequest;
import br.com.itbn.sisdent.dto.AccountPlatformAdministratorRequest;
import br.com.itbn.sisdent.dto.MembershipRequest;
import br.com.itbn.sisdent.dto.MembershipRevokeRequest;
import br.com.itbn.sisdent.dto.MembershipRoleUpdateRequest;
import br.com.itbn.sisdent.dto.OrganizationRequest;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.service.AccountManagementService;
import br.com.itbn.sisdent.service.OrganizationService;
import br.com.itbn.sisdent.service.PractitionerService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountOrganizationGraphQlControllerTest {
    private final AccountManagementService accounts = mock(AccountManagementService.class);
    private final OrganizationService organizations = mock(OrganizationService.class);
    private final PractitionerService practitioners = mock(PractitionerService.class);
    private final AccountOrganizationGraphQlController controller = new AccountOrganizationGraphQlController(
            accounts, organizations, practitioners);

    @Test
    void delegatesAccountQueriesWithExplicitOrDefaultPageInputs() {
        UUID organizationId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountOrganizationGraphQlController.AccountPageInput page =
                new AccountOrganizationGraphQlController.AccountPageInput(1, 20, "displayName", "ASC");

        controller.currentAccount();
        controller.platformAccounts(page, "ana");
        controller.platformAccounts(null, null);
        controller.platformAccount(accountId);
        controller.organizationAccounts(organizationId, page, "ana");
        controller.organizationAccount(organizationId, accountId);
        controller.platformOrganizations();

        verify(accounts).currentAccount();
        verify(accounts).platformPage(new PageQuery(1, 20, "displayName", "ASC"), "ana");
        verify(accounts).platformPage(new PageQuery(null, null, null, null), null);
        verify(accounts).platformRead(accountId);
        verify(accounts).organizationPage(organizationId, new PageQuery(1, 20, "displayName", "ASC"), "ana");
        verify(accounts).organizationRead(organizationId, accountId);
        verify(organizations).listOrganizationsForPlatform();
    }

    @Test
    void delegatesLifecycleMembershipAndPractitionerMutations() {
        UUID organizationId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID practitionerId = UUID.randomUUID();
        AccountCreateRequest account = new AccountCreateRequest("Ana", "ana@example.test", "password1");
        AccountLifecycleRequest lifecycle = new AccountLifecycleRequest(false, 2L);
        AccountPlatformAdministratorRequest administrator = new AccountPlatformAdministratorRequest(true, 3L);
        OrganizationRequest organization = new OrganizationRequest("Clinic");
        MembershipRequest membership = new MembershipRequest("ana@example.test", "Ana", "password1", null,
                MembershipRole.CLINICAL_AUTHOR);
        AccountMembershipRequest grant = new AccountMembershipRequest("ana@example.test", null, MembershipRole.CLINICAL_AUTHOR);
        MembershipRoleUpdateRequest role = new MembershipRoleUpdateRequest(MembershipRole.APPOINTMENT_MANAGER, 4L);
        MembershipRevokeRequest revoke = new MembershipRevokeRequest(5L);

        controller.createPlatformAccount(account);
        controller.changeAccountLifecycle(accountId, lifecycle);
        controller.changeAccountPlatformAdministrator(accountId, administrator);
        controller.createOrganization(organization);
        controller.createMembership(organizationId, membership);
        controller.grantMembership(organizationId, grant);
        controller.changeMembershipRole(organizationId, membershipId, role);
        controller.revokeMembership(organizationId, membershipId, revoke);
        controller.deactivatePractitioner(organizationId, practitionerId);

        verify(accounts).create(account);
        verify(accounts).changeLifecycle(accountId, lifecycle);
        verify(accounts).changePlatformAdministrator(accountId, administrator);
        verify(organizations).createOrganization(organization);
        verify(organizations).addMembership(organizationId, membership);
        verify(organizations).grantMembership(organizationId, grant);
        verify(organizations).changeMembershipRole(organizationId, membershipId, role);
        verify(organizations).revokeMembership(organizationId, membershipId, revoke);
        verify(practitioners).deactivate(organizationId, practitionerId);
    }
}
