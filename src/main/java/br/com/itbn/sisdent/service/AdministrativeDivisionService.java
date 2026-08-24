package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.AdministrativeDivisionRequest;
import br.com.itbn.sisdent.dto.AdministrativeDivisionResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.AdministrativeDivision;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.repository.AdministrativeDivisionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ResourceNotFoundException;

import java.util.List;

@Service
public class AdministrativeDivisionService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition(
            "name", java.util.Set.of("id", "name", "code", "type"));

    private final AdministrativeDivisionRepository repository;
    private final CountryService countryService;
    private final PageableFactory pageableFactory;
    private final ScopeAuthorizationService authorization;

    public AdministrativeDivisionService(
            AdministrativeDivisionRepository repository,
            CountryService countryService,
            PageableFactory pageableFactory, ScopeAuthorizationService authorization) {
        this.repository = repository;
        this.countryService = countryService;
        this.pageableFactory = pageableFactory;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<AdministrativeDivisionResponse> findAll() {
        return repository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdministrativeDivisionResponse> findPage(PageQuery query) {
        authorization.requirePlatformAdministrator();
        return PageResponse.from(
                repository.findAll(pageableFactory.create(query, SORT_DEFINITION)),
                ResponseMapper::toResponse);
    }

    AdministrativeDivision findOrCreate(
            AddressRequest.AdministrativeDivisionReference request,
            Country country) {
        if (request == null) {
            return null;
        }
        String normalizedCode = request.code().trim().toUpperCase(java.util.Locale.ROOT);
        return repository.findByCountry_CodeAndCode(country.getCode(), normalizedCode)
                .orElseGet(() -> repository.save(new AdministrativeDivision(
                        request.name().trim(),
                        normalizedCode,
                        request.type().trim().toUpperCase(java.util.Locale.ROOT),
                        country)));
    }

    @Transactional
    public AdministrativeDivisionResponse create(AdministrativeDivisionRequest request) {
        authorization.requirePlatformAdministrator();
        Country country = countryService.requireByCode(request.countryCode());
        AdministrativeDivision division = new AdministrativeDivision(
                request.name().trim(),
                request.code().trim().toUpperCase(java.util.Locale.ROOT),
                request.type().trim().toUpperCase(java.util.Locale.ROOT),
                country);
        return ResponseMapper.toResponse(repository.saveAndFlush(division));
    }

    @Transactional
    public AdministrativeDivisionResponse update(Long id, AdministrativeDivisionRequest request) {
        AdministrativeDivision division = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        division.update(
                request.name().trim(),
                request.code().trim().toUpperCase(java.util.Locale.ROOT),
                request.type().trim().toUpperCase(java.util.Locale.ROOT),
                countryService.requireByCode(request.countryCode()));
        return ResponseMapper.toResponse(repository.saveAndFlush(division));
    }

    @Transactional
    public void delete(Long id) {
        authorization.requirePlatformAdministrator();
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
