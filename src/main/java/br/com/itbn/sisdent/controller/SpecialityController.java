package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.SpecialityRequest;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.service.SpecialityService;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.filter.SpecialityFilter;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Locale;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/specialities")
public class SpecialityController {

    private final SpecialityService specialityService;

    public SpecialityController(SpecialityService specialityService) {
        this.specialityService = specialityService;
    }

    @Deprecated(since = "2026-08-18", forRemoval = false)
    @Operation(deprecated = true, description = "Use the GraphQL specialities query. Scheduled for review after 2027-02-18.")
    @GetMapping
    public PageResponse<SpecialityResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String procedure,
            Locale locale) {
        return specialityService.findPage(
                new PageQuery(page, size, sort, direction),
                new SpecialityFilter(name, procedure),
                locale);
    }

    @GetMapping("/filter-options")
    public List<FilterOptionResponse> findFilterOptions(
            @RequestParam String field,
            @RequestParam(required = false) String query) {
        return specialityService.findFilterOptions(field, query);
    }

    @Deprecated(since = "2026-08-18", forRemoval = false)
    @Operation(deprecated = true, description = "Use the GraphQL createSpeciality mutation. Scheduled for review after 2027-02-18.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialityResponse create(@Valid @RequestBody SpecialityRequest request, Locale locale) {
        return specialityService.create(request, locale);
    }

    @Deprecated(since = "2026-08-18", forRemoval = false)
    @Operation(deprecated = true, description = "Use the GraphQL updateSpeciality mutation. Scheduled for review after 2027-02-18.")
    @PutMapping("/{id}")
    public SpecialityResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SpecialityRequest request,
            Locale locale) {
        return specialityService.update(id, request, locale);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        specialityService.delete(id);
    }
}
