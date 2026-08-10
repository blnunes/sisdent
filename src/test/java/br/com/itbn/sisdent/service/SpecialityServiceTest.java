package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.DentalProcedureRequest;
import br.com.itbn.sisdent.dto.SpecialityRequest;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.model.CatalogStatus;
import br.com.itbn.sisdent.localization.CatalogNameLocalizer;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpecialityServiceTest {

    @Mock
    private SpecialityRepository specialityRepository;

    @Mock
    private PageableFactory pageableFactory;

    @Mock
    private CatalogNameLocalizer<Speciality> nameLocalizer;

    @Mock
    private CatalogTranslationService translations;

    @InjectMocks
    private SpecialityService specialityService;

    @Test
    void returnsSpecialitiesSortedByName() {
        when(specialityRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(new Speciality(
                        "Endontia",
                        List.of("Root canal treatment", "Pulpotomy"))));
        when(nameLocalizer.localize(any(Speciality.class), eq(Locale.ENGLISH))).thenReturn("Endontia");

        List<SpecialityResponse> responses = specialityService.findAll();

        assertThat(responses).singleElement()
                .extracting(SpecialityResponse::name)
                .isEqualTo("Endontia");
        assertThat(responses.getFirst().procedures())
                .extracting(procedure -> procedure.name())
                .containsExactly("Pulpotomy", "Root canal treatment");
    }

    @Test
    void createsSpecialityWithProcedures() {
        SpecialityRequest request = new SpecialityRequest(
                "Implant Dentistry",
                List.of(
                        new DentalProcedureRequest(null, "Implant placement"),
                        new DentalProcedureRequest(null, "Bone graft")));
        when(specialityRepository.findByName(request.name())).thenReturn(java.util.Optional.empty());
        when(specialityRepository.saveAndFlush(any(Speciality.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(nameLocalizer.localize(any(Speciality.class), eq(Locale.ENGLISH))).thenReturn("Implant Dentistry");
        SpecialityResponse response = specialityService.create(request, Locale.ENGLISH);

        assertThat(response.name()).isEqualTo("Implant Dentistry");
        assertThat(response.procedures())
                .extracting(procedure -> procedure.name())
                .containsExactly("Bone graft", "Implant placement");
    }

    @Test
    void returnsCanonicalAndLocalizedNamesOnPagedResponses() {
        Speciality speciality = new Speciality("Pediatric Dentistry");
        PageRequest pageable = PageRequest.of(0, 10);
        when(pageableFactory.create(any(), any())).thenReturn(pageable);
        when(specialityRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(speciality), pageable, 1));
        when(nameLocalizer.localize(speciality, Locale.forLanguageTag("pt-PT"))).thenReturn("Odontopediatria");

        SpecialityResponse response = specialityService.findPage(
                new br.com.itbn.sisdent.pagination.PageQuery(0, 10, "name", "asc"),
                new br.com.itbn.sisdent.filter.SpecialityFilter(null, null),
                Locale.forLanguageTag("pt-PT")).content().getFirst();

        assertThat(response.name()).isEqualTo("Pediatric Dentistry");
        assertThat(response.displayName()).isEqualTo("Odontopediatria");
    }

    @Test
    void rejectsUnknownSpecialityIds() {
        Set<Long> unknownSpecialityIds = Set.of(999L);
        when(specialityRepository.findAllById(unknownSpecialityIds)).thenReturn(List.of());

        assertThatThrownBy(() -> specialityService.findAllByIds(unknownSpecialityIds))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("One or more specialities do not exist");
    }

    @Test
    void supportsBothFilterOptionsAndRejectsUnsupportedFilters() {
        when(specialityRepository.findNameSuggestions(eq("implant"), any()))
                .thenReturn(List.of("Implant Dentistry"));
        when(specialityRepository.findProcedureSuggestions(eq("implant"), any()))
                .thenReturn(List.of("Implant placement"));

        assertThat(specialityService.findFilterOptions("name", " implant ")).singleElement()
                .extracting(response -> response.value()).isEqualTo("Implant Dentistry");
        assertThat(specialityService.findFilterOptions("procedure", " implant ")).singleElement()
                .extracting(response -> response.value()).isEqualTo("Implant placement");
        assertThatThrownBy(() -> specialityService.findFilterOptions("status", null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Unsupported");
    }

    @Test
    void updatesAndDeactivatesExistingSpecialities() {
        Speciality speciality = new Speciality("Old name", List.of("Cleaning"));
        SpecialityRequest request = new SpecialityRequest("New name",
                List.of(new DentalProcedureRequest(null, "Whitening")));
        when(specialityRepository.findById(1L)).thenReturn(java.util.Optional.of(speciality));
        when(specialityRepository.findByName("New name")).thenReturn(java.util.Optional.empty());
        when(specialityRepository.saveAndFlush(any(Speciality.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(nameLocalizer.localize(speciality, Locale.ENGLISH)).thenReturn("New name");
        assertThat(specialityService.update(1L, request, Locale.ENGLISH).name()).isEqualTo("New name");
        specialityService.delete(1L);

        assertThat(speciality.getStatus()).isEqualTo(CatalogStatus.INACTIVE);
        verify(specialityRepository).save(speciality);
    }

    @Test
    void validatesDuplicateNamesExistingNamesAndInactiveAssignments() {
        SpecialityRequest duplicateProcedures = new SpecialityRequest("Endodontics",
                List.of(new DentalProcedureRequest(null, "Root canal"), new DentalProcedureRequest(null, " root canal ")));
        assertThatThrownBy(() -> specialityService.create(duplicateProcedures, Locale.ENGLISH))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("unique within");

        Speciality existing = new Speciality("Existing");
        when(specialityRepository.findByName("Existing")).thenReturn(java.util.Optional.of(existing));
        assertThatThrownBy(() -> specialityService.create(new SpecialityRequest("Existing", List.of()), Locale.ENGLISH))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("already exists");

        Speciality inactive = new Speciality("Inactive");
        inactive.deactivate();
        when(specialityRepository.findAllById(Set.of(2L))).thenReturn(List.of(inactive));
        assertThatThrownBy(() -> specialityService.findAllByIds(Set.of(2L)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Inactive specialities");
    }
}
