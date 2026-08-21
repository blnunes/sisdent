package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.service.AddressService;
import br.com.itbn.sisdent.service.OrganizationPatientService;
import br.com.itbn.sisdent.service.OrganizationService;
import br.com.itbn.sisdent.service.AccountManagementService;
import br.com.itbn.sisdent.service.PractitionerService;
import br.com.itbn.sisdent.service.SessionService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ControllerDelegationTest {
    @Test
    void delegatesReferenceDataEndpointsToTheirServices() {
        AddressService addresses = mock(AddressService.class);

        new AddressController(addresses).findAll(0, 10, "street", "asc");
        new AddressController(addresses).findByPostalCode("1250", "PT");
        new AddressController(addresses).suggestByPostalCode("PT", "12");
        new AddressController(addresses).create(null);
        new AddressController(addresses).update(1L, null);
        new AddressController(addresses).delete(1L);

        verify(addresses).findByPostalCode("PT", "1250");
        verify(addresses).suggestByPostalCode("PT", "12");
    }

    @Test
    void delegatesOrganizationPatientEndpointsAndCreatesExpectedHttpResponses() {
        OrganizationPatientService patients = mock(OrganizationPatientService.class);
        OrganizationPatientController controller = new OrganizationPatientController(patients);
        UUID organization = UUID.randomUUID();
        UUID clinic = UUID.randomUUID();
        UUID patient = UUID.randomUUID();

        controller.search(organization, clinic, 0, 10, "name", "asc", null, "Ana", null, true,
                null, null, null, null, null, null, null);
        controller.filterOptions(organization, clinic, "name", "Ana");
        assertThat(controller.create(organization, clinic, null).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.delete(organization, patient, clinic).getStatusCode().value()).isEqualTo(204);
        controller.exactMatch(organization, clinic, null);
        assertThat(controller.link(organization, null).getStatusCode().value()).isEqualTo(201);

        verify(patients).search(any(), any(), any(), any());
        verify(patients).filterOptions(organization, clinic, "name", "Ana");
        verify(patients).delete(organization, clinic, patient);
    }

    @Test
    void delegatesOrganizationAccountAndPractitionerOperations() {
        OrganizationService organizations = mock(OrganizationService.class);
        AccountManagementService accounts = mock(AccountManagementService.class);
        PractitionerService practitioners = mock(PractitionerService.class);
        OrganizationController organizationController = new OrganizationController(organizations);
        AccountManagementController accountController = new AccountManagementController(accounts);
        PractitionerController practitionerController = new PractitionerController(practitioners);
        UUID organization = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        UUID membership = UUID.randomUUID();
        UUID practitioner = UUID.randomUUID();

        organizationController.createOrganization(null);
        organizationController.listOrganizations();
        organizationController.createClinicUnit(organization, null);
        organizationController.listClinicUnits(organization, null);
        organizationController.addMembership(organization, null);
        organizationController.grantAccountMembership(organization, null);
        organizationController.revokeMembership(organization, membership);
        organizationController.revokeAccountMembership(organization, membership, null);
        organizationController.changeMembershipRole(organization, membership, null);
        accountController.current();
        accountController.platformPage(0, 10, "email", "asc", "admin");
        accountController.create(null);
        accountController.platformRead(account);
        accountController.lifecycle(account, null);
        accountController.platformAdministrator(account, null);
        accountController.organizationPage(organization, 0, 10, "email", "asc", "admin");
        accountController.organizationRead(organization, account);
        practitionerController.list(organization);
        practitionerController.create(organization, null);
        practitionerController.update(organization, practitioner, null);
        practitionerController.deactivate(organization, practitioner);

        verify(organizations).revokeMembership(organization, membership);
        verify(accounts).organizationRead(organization, account);
        verify(practitioners).deactivate(organization, practitioner);
    }

    @Test
    void delegatesTheCurrentSessionEndpoint() {
        SessionService sessions = mock(SessionService.class);
        new SessionController(sessions).current();
        verify(sessions).current();
    }
}
