package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.ExactPatientMatchResponse;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.PatientLinkResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.filter.PatientFilter;
import br.com.itbn.sisdent.service.OrganizationPatientService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL adapter for scoped patient work; the service owns authorization and transactions. */
@Controller
public class PatientGraphQlController {
    private final OrganizationPatientService patients;

    public PatientGraphQlController(OrganizationPatientService patients) {
        this.patients = patients;
    }

    @QueryMapping
    public PageResponse<PatientResponse> patients(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument CataloguePageInput page, @Argument PatientFilterInput filter) {
        return patients.search(organizationId, clinicUnitId, page == null ? defaultPageQuery() : page.toPageQuery(),
                filter == null ? emptyFilter() : filter.toFilter());
    }

    @QueryMapping
    public List<FilterOptionResponse> patientFilterOptions(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument String field, @Argument String query) {
        return patients.filterOptions(organizationId, clinicUnitId, field, query);
    }

    @MutationMapping
    public PatientResponse createPatient(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument @Valid PatientMutationInput input) {
        return patients.create(organizationId, clinicUnitId, input.toRequest());
    }

    @MutationMapping
    public PatientMutationResult updatePatient(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID patientId, @Argument @Valid PatientMutationInput input) {
        return PatientMutationResult.from(patients.update(organizationId, clinicUnitId, patientId, input.toRequest()));
    }

    @MutationMapping
    public boolean deactivatePatient(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID patientId) {
        patients.delete(organizationId, clinicUnitId, patientId);
        return true;
    }

    @MutationMapping
    public ExactPatientMatchResponse exactPatientMatch(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument @Valid ExactPatientMatchInput input) {
        return patients.exactMatch(organizationId, clinicUnitId, input.toRequest());
    }

    @MutationMapping
    public PatientLinkResponse linkPatient(@Argument UUID organizationId, @Argument @Valid PatientLinkMutationInput input) {
        return patients.link(organizationId, input.toRequest());
    }

    private static br.com.itbn.sisdent.pagination.PageQuery defaultPageQuery() {
        return new CataloguePageInput(null, null, null, null).toPageQuery();
    }

    private static PatientFilter emptyFilter() {
        return new PatientFilter(null, null, null, null, null, null, null, null, null, null, null);
    }
}
