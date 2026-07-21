package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.service.StateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/states")
public class StateController {

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    public List<StateResponse> findAll() {
        return stateService.findAll();
    }
}
