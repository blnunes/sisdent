package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.service.SpecialityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialities")
public class SpecialityController {

    private final SpecialityService specialityService;

    public SpecialityController(SpecialityService specialityService) {
        this.specialityService = specialityService;
    }

    @GetMapping
    public List<SpecialityResponse> findAll() {
        return specialityService.findAll();
    }
}
