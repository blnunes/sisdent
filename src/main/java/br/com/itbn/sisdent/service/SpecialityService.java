package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class SpecialityService {

    private final SpecialityRepository specialityRepository;

    public SpecialityService(SpecialityRepository specialityRepository) {
        this.specialityRepository = specialityRepository;
    }

    @Transactional(readOnly = true)
    public List<SpecialityResponse> findAll() {
        return specialityRepository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    List<Speciality> findAllByIds(Set<Long> ids) {
        List<Speciality> specialities = specialityRepository.findAllById(ids);
        if (specialities.size() != ids.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more specialities do not exist");
        }
        return specialities;
    }
}
