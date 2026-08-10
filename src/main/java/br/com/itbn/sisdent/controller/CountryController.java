package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.CountryRequest;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.service.CountryService;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.model.Continent;
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

import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    public PageResponse<CountryResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction,
            Locale locale) {
        return countryService.findPage(new PageQuery(page, size, sort, direction), locale);
    }

    /** Exposes the authoritative enum values for clients that create countries. */
    @GetMapping("/continents")
    public List<Continent> continents() {
        return List.of(Continent.values());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CountryResponse create(
            @Valid @RequestBody CountryRequest request,
            Locale locale) {
        return countryService.create(request, locale);
    }

    @PutMapping("/{id}")
    public CountryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CountryRequest request,
            Locale locale) {
        return countryService.update(id, request, locale);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        countryService.delete(id);
    }
}
