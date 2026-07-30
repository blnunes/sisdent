package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.SpecialityRequest;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.service.SpecialityService;
import br.com.itbn.sisdent.pagination.PageQuery;
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

    @GetMapping
    public PageResponse<SpecialityResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return specialityService.findPage(new PageQuery(page, size, sort, direction));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialityResponse create(@Valid @RequestBody SpecialityRequest request) {
        return specialityService.create(request);
    }

    @PutMapping("/{id}")
    public SpecialityResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SpecialityRequest request) {
        return specialityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { specialityService.delete(id); }
}
