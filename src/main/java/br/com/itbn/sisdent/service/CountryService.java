package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.repository.CountryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CountryService {

    private final CountryRepository countryRepository;

    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> findAll() {
        return countryRepository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Country requireByCode(String code) {
        return countryRepository.findByCode(code)
                .orElseThrow(() -> new UnknownCountryException(code));
    }
}
