package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.ClinicUnitResponse;
import br.com.itbn.sisdent.dto.PractitionerResponse;
import br.com.itbn.sisdent.service.OrganizationService;
import br.com.itbn.sisdent.service.PractitionerService;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Read-only GraphQL adapter for organization-scoped operational reference data. */
@Controller
public class OrganizationReadQueryController {
    private final OrganizationService organizationService;
    private final PractitionerService practitionerService;

    public OrganizationReadQueryController(
            OrganizationService organizationService, PractitionerService practitionerService) {
        this.organizationService = organizationService;
        this.practitionerService = practitionerService;
    }

    @QueryMapping
    public List<ClinicUnitResponse> clinicUnits(
            @Argument UUID organizationId, @Argument UUID clinicUnitId) {
        return organizationService.listClinicUnits(organizationId, clinicUnitId);
    }

    @QueryMapping
    public List<PractitionerResponse> practitioners(@Argument UUID organizationId, @Argument UUID clinicUnitId) {
        return practitionerService.list(organizationId, clinicUnitId);
    }
}
