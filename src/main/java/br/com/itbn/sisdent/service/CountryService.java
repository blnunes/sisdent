package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.CountryRequest;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.repository.CountryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CountryService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("name", java.util.Set.of("id", "name", "code", "continent"));

    private final CountryRepository countryRepository;
    private final PageableFactory pageableFactory;

    public CountryService(CountryRepository countryRepository, PageableFactory pageableFactory) {
        this.countryRepository = countryRepository;
        this.pageableFactory = pageableFactory;
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> findAll() {
        return countryRepository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CountryResponse> findPage(PageQuery query) {
        return PageResponse.from(countryRepository.findAll(pageableFactory.create(query, SORT_DEFINITION)), ResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Country requireByCode(String code) {
        return countryRepository.findByCode(code)
                .orElseThrow(() -> new UnknownCountryException(code));
    }

    @Transactional
    public CountryResponse create(CountryRequest request) { return ResponseMapper.toResponse(countryRepository.saveAndFlush(new Country(request.name(), request.code(), request.continent()))); }

    @Transactional
    public CountryResponse update(Long id, CountryRequest request) {
        Country country = countryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        country.update(request.name(), request.code(), request.continent());
        return ResponseMapper.toResponse(countryRepository.saveAndFlush(country));
    }

    @Transactional
    public void delete(Long id) {
        if (!countryRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        countryRepository.deleteById(id);
    }
}
