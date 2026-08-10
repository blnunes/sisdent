package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.CountryRequest;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.localization.CatalogNameLocalizer;
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
import java.util.Locale;

@Service
public class CountryService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("name", java.util.Set.of("id", "name", "code", "continent"));

    private final CountryRepository countryRepository;
    private final PageableFactory pageableFactory;
    private final CatalogNameLocalizer<Country> nameLocalizer;

    public CountryService(CountryRepository countryRepository, PageableFactory pageableFactory,
                          CatalogNameLocalizer<Country> nameLocalizer) {
        this.countryRepository = countryRepository;
        this.pageableFactory = pageableFactory;
        this.nameLocalizer = nameLocalizer;
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> findAll() {
        return countryRepository.findAll(Sort.by("name")).stream()
                .map(country -> toResponse(country, Locale.ENGLISH))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CountryResponse> findPage(PageQuery query, Locale locale) {
        return PageResponse.from(countryRepository.findAll(pageableFactory.create(query, SORT_DEFINITION)),
                country -> toResponse(country, locale));
    }

    @Transactional(readOnly = true)
    public Country requireByCode(String code) {
        return countryRepository.findByCode(code)
                .orElseThrow(() -> new UnknownCountryException(code));
    }

    @Transactional
    public CountryResponse create(CountryRequest request, Locale locale) {
        return toResponse(countryRepository.saveAndFlush(
                new Country(request.name(), request.code(), request.continent())), locale);
    }

    @Transactional
    public CountryResponse update(Long id, CountryRequest request, Locale locale) {
        Country country = countryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        country.update(request.name(), request.code(), request.continent());
        return toResponse(countryRepository.saveAndFlush(country), locale);
    }

    @Transactional
    public void delete(Long id) {
        if (!countryRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        countryRepository.deleteById(id);
    }

    private CountryResponse toResponse(Country country, Locale locale) {
        return ResponseMapper.toResponse(country, nameLocalizer.localize(country, locale));
    }
}
