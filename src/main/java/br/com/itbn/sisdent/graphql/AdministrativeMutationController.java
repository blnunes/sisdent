package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.ClinicUnitResponse;
import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.PractitionerResponse;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.service.CountryService;
import br.com.itbn.sisdent.service.OrganizationService;
import br.com.itbn.sisdent.service.PractitionerService;
import br.com.itbn.sisdent.service.SpecialityService;
import br.com.itbn.sisdent.service.OrganizationPatientService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/** GraphQL write adapter. Authorization, validation, audit and transactions remain in services. */
@Controller
public class AdministrativeMutationController {
    private final CountryService countries;
    private final SpecialityService specialities;
    private final OrganizationService organizations;
    private final PractitionerService practitioners;
    private final OrganizationPatientService patients;
    private final CatalogueLocaleArgument catalogueLocale;

    public AdministrativeMutationController(CountryService countries, SpecialityService specialities,
            OrganizationService organizations, PractitionerService practitioners, OrganizationPatientService patients,
            CatalogueLocaleArgument catalogueLocale) {
        this.countries = countries;
        this.specialities = specialities;
        this.organizations = organizations;
        this.practitioners = practitioners;
        this.patients = patients;
        this.catalogueLocale = catalogueLocale;
    }

    @MutationMapping
    public CountryResponse createCountry(@Argument @Valid CountryMutationInput input, @Argument String locale) {
        return countries.create(input.toRequest(), catalogueLocale.resolve(locale));
    }

    @MutationMapping
    public CountryResponse updateCountry(@Argument Long id, @Argument @Valid CountryMutationInput input,
            @Argument String locale) {
        return countries.update(id, input.toRequest(), catalogueLocale.resolve(locale));
    }

    @MutationMapping
    public SpecialityResponse createSpeciality(@Argument @Valid SpecialityMutationInput input,
            @Argument String locale) {
        return specialities.create(input.toRequest(), catalogueLocale.resolve(locale));
    }

    @MutationMapping
    public SpecialityResponse updateSpeciality(@Argument Long id, @Argument @Valid SpecialityMutationInput input,
            @Argument String locale) {
        return specialities.update(id, input.toRequest(), catalogueLocale.resolve(locale));
    }

    @MutationMapping
    public ClinicUnitResponse createClinicUnit(@Argument UUID organizationId,
            @Argument @Valid ClinicUnitMutationInput input) {
        return organizations.createClinicUnit(organizationId, input.toRequest());
    }

    @MutationMapping
    public PractitionerResponse createPractitioner(@Argument UUID organizationId,
            @Argument @Valid PractitionerMutationInput input) {
        return practitioners.create(organizationId, input.toRequest());
    }

    @MutationMapping
    public PractitionerResponse updatePractitioner(@Argument UUID organizationId, @Argument UUID practitionerId,
            @Argument @Valid PractitionerMutationInput input) {
        return practitioners.update(organizationId, practitionerId, input.toRequest());
    }

    @MutationMapping
    public PatientMutationResult updatePatient(@Argument UUID organizationId, @Argument UUID clinicUnitId,
            @Argument UUID patientId, @Argument @Valid PatientUpdateMutationInput input) {
        return PatientMutationResult.from(patients.update(organizationId, clinicUnitId, patientId, input.toRequest()));
    }
}
