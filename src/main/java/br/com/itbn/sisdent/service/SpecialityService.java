package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ProcedureRequest;
import br.com.itbn.sisdent.dto.SpecialityRequest;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Transactional
    public SpecialityResponse create(SpecialityRequest request) {
        validateProcedureNames(request.procedures());
        if (request.procedures().stream().anyMatch(procedure -> procedure.id() != null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New procedures must not define an id");
        }
        ensureNameAvailable(request.name(), null);

        Speciality speciality = new Speciality(
                request.name().trim(),
                request.procedures().stream()
                        .map(ProcedureRequest::name)
                        .map(String::trim)
                        .toList());
        return ResponseMapper.toResponse(specialityRepository.save(speciality));
    }

    @Transactional
    public SpecialityResponse update(Long specialityId, SpecialityRequest request) {
        validateProcedureNames(request.procedures());
        Speciality speciality = specialityRepository.findById(specialityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Speciality not found"));
        ensureNameAvailable(request.name(), specialityId);

        List<ProcedureRequest> existingProcedures = request.procedures().stream()
                .filter(procedure -> procedure.id() != null)
                .toList();
        Set<Long> retainedIds = existingProcedures.stream()
                .map(ProcedureRequest::id)
                .collect(Collectors.toSet());
        if (retainedIds.size() != existingProcedures.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Procedure ids must be unique");
        }

        existingProcedures.forEach(procedureRequest ->
                speciality.findProcedure(procedureRequest.id())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Procedure does not belong to speciality"))
                        .rename(procedureRequest.name().trim()));
        speciality.retainProcedures(retainedIds);
        request.procedures().stream()
                .filter(procedure -> procedure.id() == null)
                .map(ProcedureRequest::name)
                .map(String::trim)
                .forEach(speciality::addProcedure);
        speciality.rename(request.name().trim());

        return ResponseMapper.toResponse(specialityRepository.saveAndFlush(speciality));
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

    private void validateProcedureNames(List<ProcedureRequest> procedures) {
        Set<String> names = new HashSet<>();
        boolean hasDuplicate = procedures.stream()
                .map(ProcedureRequest::name)
                .map(String::trim)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> !names.add(name));
        if (hasDuplicate) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Procedure names must be unique within a speciality");
        }
    }

    private void ensureNameAvailable(String name, Long currentSpecialityId) {
        specialityRepository.findByName(name.trim())
                .filter(speciality -> !speciality.getId().equals(currentSpecialityId))
                .ifPresent(speciality -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Speciality name already exists");
                });
    }
}
