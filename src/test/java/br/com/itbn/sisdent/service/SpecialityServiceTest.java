package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ProcedureRequest;
import br.com.itbn.sisdent.dto.SpecialityRequest;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SpecialityServiceTest {

    @Mock
    private SpecialityRepository specialityRepository;

    @InjectMocks
    private SpecialityService specialityService;

    @Test
    void returnsSpecialitiesSortedByName() {
        when(specialityRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(new Speciality(
                        "Endontia",
                        List.of("Root canal treatment", "Pulpotomy"))));

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
                        new ProcedureRequest(null, "Implant placement"),
                        new ProcedureRequest(null, "Bone graft")));
        when(specialityRepository.findByName(request.name())).thenReturn(java.util.Optional.empty());
        when(specialityRepository.save(any(Speciality.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SpecialityResponse response = specialityService.create(request);

        assertThat(response.name()).isEqualTo("Implant Dentistry");
        assertThat(response.procedures())
                .extracting(procedure -> procedure.name())
                .containsExactly("Bone graft", "Implant placement");
    }

    @Test
    void rejectsUnknownSpecialityIds() {
        Set<Long> unknownSpecialityIds = Set.of(999L);
        when(specialityRepository.findAllById(unknownSpecialityIds)).thenReturn(List.of());

        assertThatThrownBy(() -> specialityService.findAllByIds(unknownSpecialityIds))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("One or more specialities do not exist");
    }
}
