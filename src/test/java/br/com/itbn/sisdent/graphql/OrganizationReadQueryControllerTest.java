package br.com.itbn.sisdent.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.itbn.sisdent.dto.ClinicUnitResponse;
import br.com.itbn.sisdent.dto.PractitionerResponse;
import br.com.itbn.sisdent.service.OrganizationService;
import br.com.itbn.sisdent.service.PractitionerService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizationReadQueryControllerTest {
    private final OrganizationService organizations = mock(OrganizationService.class);
    private final PractitionerService practitioners = mock(PractitionerService.class);
    private final OrganizationReadQueryController controller =
            new OrganizationReadQueryController(organizations, practitioners);

    @Test
    void delegatesClinicScopeWithoutOwningAuthorizationOrPersistence() {
        UUID organizationId = UUID.randomUUID();
        UUID clinicUnitId = UUID.randomUUID();
        List<ClinicUnitResponse> expected = List.of();
        when(organizations.listClinicUnits(organizationId, clinicUnitId)).thenReturn(expected);

        assertThat(controller.clinicUnits(organizationId, clinicUnitId)).isSameAs(expected);
        verify(organizations).listClinicUnits(organizationId, clinicUnitId);
    }

    @Test
    void delegatesPractitionerReadToExistingScopedService() {
        UUID organizationId = UUID.randomUUID();
        List<PractitionerResponse> expected = List.of();
        UUID clinicUnitId = UUID.randomUUID();
        when(practitioners.list(organizationId, clinicUnitId)).thenReturn(expected);

        assertThat(controller.practitioners(organizationId, clinicUnitId)).isSameAs(expected);
        verify(practitioners).list(organizationId, clinicUnitId);
    }
}
