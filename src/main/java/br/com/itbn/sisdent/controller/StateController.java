package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.dto.StateRequest;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.service.StateService;
import br.com.itbn.sisdent.pagination.PageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/states")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    public PageResponse<StateResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return stateService.findPage(new PageQuery(page, size, sort, direction));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public StateResponse create(@Valid @RequestBody StateRequest request) { return stateService.create(request); }
    @PutMapping("/{id}")
    public StateResponse update(@PathVariable Long id, @Valid @RequestBody StateRequest request) { return stateService.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { stateService.delete(id); }
}
