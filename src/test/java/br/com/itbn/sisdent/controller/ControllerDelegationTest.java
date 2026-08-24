package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.service.OrganizationPatientService;
import br.com.itbn.sisdent.service.SessionService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ControllerDelegationTest {
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
    void delegatesTheCurrentSessionEndpoint() {
        SessionService sessions = mock(SessionService.class);
        new SessionController(sessions).current();
        verify(sessions).current();
    }
}
