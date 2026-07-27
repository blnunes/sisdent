package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.service.CountryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    public List<CountryResponse> findAll() {
        return countryService.findAll();
    }
}
