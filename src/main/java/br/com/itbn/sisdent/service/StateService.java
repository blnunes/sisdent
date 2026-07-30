package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.StateRequest;
import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.repository.StateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StateService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("name", java.util.Set.of("id", "name", "abbreviation"));

    private final StateRepository stateRepository;
    private final PageableFactory pageableFactory;

    public StateService(StateRepository stateRepository, PageableFactory pageableFactory) {
        this.stateRepository = stateRepository;
        this.pageableFactory = pageableFactory;
    }

    @Transactional(readOnly = true)
    public List<StateResponse> findAll() {
        return stateRepository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StateResponse> findPage(PageQuery query) {
        return PageResponse.from(stateRepository.findAll(pageableFactory.create(query, SORT_DEFINITION)), ResponseMapper::toResponse);
    }

    State findOrCreate(StateRequest request) {
        return stateRepository.findByAbbreviation(request.abbreviation())
                .orElseGet(() -> stateRepository.save(
                        new State(request.name(), request.abbreviation())));
    }

    @Transactional
    public StateResponse create(StateRequest request) { return ResponseMapper.toResponse(stateRepository.saveAndFlush(new State(request.name(), request.abbreviation()))); }

    @Transactional
    public StateResponse update(Long id, StateRequest request) {
        State state = stateRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        state.update(request.name(), request.abbreviation());
        return ResponseMapper.toResponse(stateRepository.saveAndFlush(state));
    }

    @Transactional
    public void delete(Long id) {
        if (!stateRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        stateRepository.deleteById(id);
    }
}
