package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.ClinicalEncounterResponse;
import br.com.itbn.sisdent.dto.OdontogramFindingResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.service.ClinicalRecordService;
import br.com.itbn.sisdent.service.OdontogramService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL adapter for clinical workflows; services retain all scope and audit rules. */
@Controller
public class ClinicalRecordGraphQlController {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ClinicalRecordService encounters;
    private final OdontogramService odontogram;

    public ClinicalRecordGraphQlController(ClinicalRecordService encounters, OdontogramService odontogram) {
        this.encounters = encounters;
        this.odontogram = odontogram;
    }

    @QueryMapping
    public PageResponse<ClinicalEncounterResponse> clinicalEncounters(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument UUID patientId, @Argument Integer page, @Argument Integer size) {
        return encounters.list(organizationId, clinicUnitId, patientId, defaultPage(page), defaultSize(size));
    }

    @QueryMapping
    public ClinicalEncounterResponse clinicalEncounter(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID encounterId) {
        return encounters.get(organizationId, clinicUnitId, encounterId);
    }

    @QueryMapping
    public List<ClinicalEncounterResponse> encounterAmendments(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument UUID encounterId) {
        return encounters.amendments(organizationId, clinicUnitId, encounterId);
    }

    @QueryMapping
    public List<OdontogramFindingResponse> currentOdontogram(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument UUID patientId) {
        return odontogram.current(organizationId, clinicUnitId, patientId);
    }

    @QueryMapping
    public PageResponse<OdontogramFindingResponse> odontogramHistory(@Argument UUID organizationId,
            @Argument UUID clinicUnitId, @Argument UUID patientId, @Argument Integer page, @Argument Integer size) {
        return odontogram.history(organizationId, clinicUnitId, patientId, defaultPage(page), defaultSize(size));
    }

    @MutationMapping
    public ClinicalEncounterResponse createClinicalEncounter(@Argument UUID organizationId,
            @Argument @Valid ClinicalEncounterMutationInput input) {
        return encounters.create(organizationId, input.toCreateRequest());
    }

    @MutationMapping
    public ClinicalEncounterResponse updateClinicalEncounter(@Argument UUID organizationId, @Argument UUID encounterId,
            @Argument @Valid ClinicalEncounterMutationInput input) {
        return encounters.update(organizationId, encounterId, input.toUpdateRequest());
    }

    @MutationMapping
    public ClinicalEncounterResponse finalizeClinicalEncounter(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID encounterId) {
        return encounters.finalizeRecord(organizationId, clinicUnitId, encounterId);
    }

    @MutationMapping
    public ClinicalEncounterResponse amendClinicalEncounter(@Argument UUID organizationId, @Argument UUID encounterId,
            @Argument @Valid AmendEncounterMutationInput input) {
        return encounters.amend(organizationId, encounterId, input.toRequest());
    }

    @MutationMapping
    public OdontogramFindingResponse createOdontogramFinding(@Argument UUID organizationId,
            @Argument @Valid OdontogramFindingMutationInput input) {
        return odontogram.create(organizationId, input.toRequest());
    }

    @MutationMapping
    public OdontogramFindingResponse voidOdontogramFinding(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID findingId, @Argument @Valid VoidOdontogramFindingMutationInput input) {
        return odontogram.voidRecord(organizationId, clinicUnitId, findingId, input.toRequest());
    }

    private static int defaultPage(Integer page) {
        return page == null ? DEFAULT_PAGE : page;
    }

    private static int defaultSize(Integer size) {
        return size == null ? DEFAULT_PAGE_SIZE : size;
    }
}
