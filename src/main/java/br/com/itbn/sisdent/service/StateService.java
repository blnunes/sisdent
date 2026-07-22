package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.StateRequest;
import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.repository.StateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StateService {

    private final StateRepository stateRepository;

    public StateService(StateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @Transactional(readOnly = true)
    public List<StateResponse> findAll() {
        return stateRepository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    State findOrCreate(StateRequest request) {
        return stateRepository.findByAbbreviation(request.abbreviation())
                .orElseGet(() -> stateRepository.save(
                        new State(request.name(), request.abbreviation())));
    }
}
