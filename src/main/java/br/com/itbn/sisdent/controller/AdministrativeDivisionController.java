package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.AdministrativeDivisionRequest;
import br.com.itbn.sisdent.dto.AdministrativeDivisionResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.service.AdministrativeDivisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/administrative-divisions", "/api/states"})
public class AdministrativeDivisionController {

    private final AdministrativeDivisionService service;

    public AdministrativeDivisionController(AdministrativeDivisionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdministrativeDivisionResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return service.findPage(new PageQuery(page, size, sort, direction));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdministrativeDivisionResponse create(
            @Valid @RequestBody AdministrativeDivisionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AdministrativeDivisionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AdministrativeDivisionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
