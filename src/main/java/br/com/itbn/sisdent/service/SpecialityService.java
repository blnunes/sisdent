package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.DentalProcedureRequest;
import br.com.itbn.sisdent.dto.SpecialityRequest;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.localization.CatalogNameLocalizer;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.dto.FilterOptionResponse;
import br.com.itbn.sisdent.filter.SpecialityFilter;
import br.com.itbn.sisdent.filter.SpecialitySpecifications;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.model.CatalogStatus;
import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.model.DentalProcedure;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpecialityService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("name", java.util.Set.of("id", "name"));

    private final SpecialityRepository specialityRepository;
    private final PageableFactory pageableFactory;
    private final CatalogNameLocalizer<Speciality> nameLocalizer;
    private final CatalogTranslationService translations;

    public SpecialityService(SpecialityRepository specialityRepository, PageableFactory pageableFactory,
                             CatalogNameLocalizer<Speciality> nameLocalizer,
                             CatalogTranslationService translations) {
        this.specialityRepository = specialityRepository;
        this.pageableFactory = pageableFactory;
        this.nameLocalizer = nameLocalizer;
        this.translations = translations;
    }

    @Transactional(readOnly = true)
    public List<SpecialityResponse> findAll() {
        return specialityRepository.findAll(Sort.by("name")).stream()
                .map(speciality -> toResponse(speciality, Locale.ENGLISH))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SpecialityResponse> findPage(PageQuery query, SpecialityFilter filter, Locale locale) {
        return PageResponse.from(specialityRepository.findAll(
                SpecialitySpecifications.matching(filter), pageableFactory.create(query, SORT_DEFINITION)),
                speciality -> toResponse(speciality, locale));
    }

    @Transactional(readOnly = true)
    public List<FilterOptionResponse> findFilterOptions(String field, String query) {
        Pageable limit = PageRequest.of(0, 10);
        String term = query == null ? "" : query.trim();
        return switch (field) {
            case "name" -> specialityRepository.findNameSuggestions(term, limit).stream()
                    .map(value -> new FilterOptionResponse(value, value)).toList();
            case "procedure" -> specialityRepository.findProcedureSuggestions(term, limit).stream()
                    .map(value -> new FilterOptionResponse(value, value)).toList();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported filter field");
        };
    }

    @Transactional
    public SpecialityResponse create(SpecialityRequest request, Locale locale) {
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
                        .map(DentalProcedureRequest::name)
                        .map(String::trim)
                        .toList());
        Speciality saved = specialityRepository.saveAndFlush(speciality);
        persistTranslations(saved, request);
        return toResponse(saved, locale);
    }

    @Transactional
    public SpecialityResponse update(Long specialityId, SpecialityRequest request, Locale locale) {
        validateProcedureNames(request.procedures());
        Speciality speciality = specialityRepository.findById(specialityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Speciality not found"));
        ensureNameAvailable(request.name(), specialityId);

        List<DentalProcedureRequest> existingProcedures = request.procedures().stream()
                .filter(procedure -> procedure.id() != null)
                .toList();
        Set<Long> retainedIds = existingProcedures.stream()
                .map(DentalProcedureRequest::id)
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
                .map(DentalProcedureRequest::name)
                .map(String::trim)
                .forEach(speciality::addProcedure);
        speciality.rename(request.name().trim());

        Speciality saved = specialityRepository.saveAndFlush(speciality);
        persistTranslations(saved, request);
        return toResponse(saved, locale);
    }

    @Transactional
    public void delete(Long id) {
        Speciality speciality = specialityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Speciality not found"));
        speciality.deactivate();
        specialityRepository.save(speciality);
    }

    List<Speciality> findAllByIds(Set<Long> ids) {
        List<Speciality> specialities = specialityRepository.findAllById(ids);
        if (specialities.size() != ids.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more specialities do not exist");
        }
        if (specialities.stream().anyMatch(speciality -> speciality.getStatus() != CatalogStatus.ACTIVE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inactive specialities cannot be assigned");
        }
        return specialities;
    }

    private void validateProcedureNames(List<DentalProcedureRequest> procedures) {
        Set<String> names = new HashSet<>();
        boolean hasDuplicate = procedures.stream()
                .map(DentalProcedureRequest::name)
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
                .filter(speciality -> currentSpecialityId == null
                        || !Objects.equals(speciality.getId(), currentSpecialityId))
                .ifPresent(speciality -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Speciality name already exists");
                });
    }

    private SpecialityResponse toResponse(Speciality speciality, Locale locale) {
        return ResponseMapper.toResponse(
                speciality,
                nameLocalizer.localize(speciality, locale),
                procedure -> translations.resolve(CatalogResourceType.PROCEDURE, procedure.getId(), procedure.getName(), locale),
                translations.effectiveTranslations(CatalogResourceType.SPECIALITY, speciality.getId(), speciality.getName()),
                procedure -> translations.effectiveTranslations(CatalogResourceType.PROCEDURE, procedure.getId(), procedure.getName()));
    }

    private void persistTranslations(Speciality speciality, SpecialityRequest request) {
        translations.merge(CatalogResourceType.SPECIALITY, speciality.getId(), request.translations());
        for (DentalProcedureRequest procedureRequest : request.procedures()) {
            DentalProcedure procedure = procedureRequest.id() == null
                    ? speciality.getProcedures().stream()
                        .filter(item -> item.getName().equalsIgnoreCase(procedureRequest.name().strip()))
                        .findFirst().orElseThrow()
                    : speciality.findProcedure(procedureRequest.id()).orElseThrow();
            translations.merge(CatalogResourceType.PROCEDURE, procedure.getId(), procedureRequest.translations());
        }
    }
}
