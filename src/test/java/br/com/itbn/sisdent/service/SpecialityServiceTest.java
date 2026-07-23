package br.com.itbn.sisdent.service;

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

@ExtendWith(MockitoExtension.class)
class SpecialityServiceTest {

    @Mock
    private SpecialityRepository specialityRepository;

    @InjectMocks
    private SpecialityService specialityService;

    @Test
    void returnsSpecialitiesSortedByName() {
        when(specialityRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(new Speciality("Endontia")));

        List<SpecialityResponse> responses = specialityService.findAll();

        assertThat(responses).singleElement()
                .extracting(SpecialityResponse::name)
                .isEqualTo("Endontia");
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
