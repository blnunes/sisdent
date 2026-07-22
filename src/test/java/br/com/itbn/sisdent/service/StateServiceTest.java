package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.StateRequest;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Test
    void reusesExistingState() {
        State existingState = new State("Goiás", "GO");
        StateRequest request = new StateRequest("Goiás", "GO");
        when(stateRepository.findByAbbreviation("GO")).thenReturn(Optional.of(existingState));

        State result = stateService.findOrCreate(request);

        assertThat(result).isSameAs(existingState);
        verify(stateRepository, never()).save(any(State.class));
    }

    @Test
    void createsMissingState() {
        StateRequest request = new StateRequest("Goiás", "GO");
        when(stateRepository.findByAbbreviation("GO")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        State result = stateService.findOrCreate(request);

        assertThat(result.getAbbreviation()).isEqualTo("GO");
        verify(stateRepository).save(any(State.class));
    }
}
