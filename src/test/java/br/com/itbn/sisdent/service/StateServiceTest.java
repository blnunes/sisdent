package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.repository.StateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateServiceTest {

    @Mock
    private StateRepository stateRepository;

    @InjectMocks
    private StateService stateService;

    @Test
    void returnsStatesSortedByName() {
        when(stateRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(new State("Goiás", "GO")));

        List<StateResponse> responses = stateService.findAll();

        assertThat(responses).singleElement()
                .extracting(StateResponse::abbreviation)
                .isEqualTo("GO");
        verify(stateRepository).findAll(Sort.by("name"));
    }
}
