package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.CatalogTranslationEntryResponse;
import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.model.CatalogTranslation;
import br.com.itbn.sisdent.model.DentalProcedure;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.CatalogTranslationRepository;
import br.com.itbn.sisdent.repository.DentalProcedureRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CatalogTranslationService {
    public static final List<String> SUPPORTED_LOCALES = List.of("en", "pt-PT", "nl");

    private final CatalogTranslationRepository translations;
    private final SpecialityRepository specialities;
    private final DentalProcedureRepository procedures;
    private final MessageSource messages;

    public CatalogTranslationService(CatalogTranslationRepository translations,
                                     SpecialityRepository specialities,
                                     DentalProcedureRepository procedures,
                                     MessageSource messages) {
        this.translations = translations;
        this.specialities = specialities;
        this.procedures = procedures;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public String resolve(
            CatalogResourceType type,
            Long resourceId,
            String canonicalName,
            Locale requestedLocale) {
        String locale = supportedTag(requestedLocale);
        return translations.findByResourceTypeAndResourceIdAndLocale(type, resourceId, locale)
                .map(CatalogTranslation::getTranslatedName)
                .or(() -> builtIn(type, canonicalName, locale))
                .orElse(canonicalName);
    }

    @Transactional(readOnly = true)
    public Map<String, String> effectiveTranslations(
            CatalogResourceType type,
            Long resourceId,
            String canonicalName) {
        return effectiveTranslations(
                type,
                canonicalName,
                translations.findByResourceTypeAndResourceId(type, resourceId));
    }

    private Map<String, String> effectiveTranslations(
            CatalogResourceType type,
            String canonicalName,
            List<CatalogTranslation> customTranslations) {
        Map<String, String> customValues = customTranslations.stream()
                .collect(java.util.stream.Collectors.toMap(
                        CatalogTranslation::getLocale,
                        CatalogTranslation::getTranslatedName));
        Map<String, String> result = new LinkedHashMap<>();
        for (String locale : SUPPORTED_LOCALES) {
            String customValue = customValues.get(locale);
            if (customValue != null) {
                result.put(locale, customValue);
            } else {
                builtIn(type, canonicalName, locale)
                        .ifPresent(value -> result.put(locale, value));
            }
        }
        return Map.copyOf(result);
    }

    @Transactional
    public CatalogTranslationEntryResponse replace(
            CatalogResourceType type,
            Long resourceId,
            Map<String, String> values) {
        rejectUnsupportedLocales(values.keySet());
        Resource resource = requireResource(type, resourceId);
        List<CatalogTranslation> existing = translations.findByResourceTypeAndResourceId(
                type,
                resourceId);
        for (String locale : SUPPORTED_LOCALES) {
            CatalogTranslation current = existing.stream()
                    .filter(item -> item.getLocale().equals(locale)).findFirst().orElse(null);
            String value = clean(values.get(locale));
            if (value == null && current != null) {
                translations.delete(current);
            }
            else if (value != null && current == null) {
                translations.save(new CatalogTranslation(type, resourceId, locale, value));
            } else if (value != null) current.rename(value);
        }
        translations.flush();
        return entry(type, resource.id(), resource.parentId(), resource.name());
    }

    @Transactional
    public void merge(CatalogResourceType type, Long resourceId, Map<String, String> values) {
        if (values == null || values.isEmpty() || resourceId == null) {
            return;
        }
        rejectUnsupportedLocales(values.keySet());
        requireResource(type, resourceId);
        values.forEach((locale, rawValue) -> {
            String value = clean(rawValue);
            CatalogTranslation current = translations
                    .findByResourceTypeAndResourceIdAndLocale(type, resourceId, locale).orElse(null);
            if (value == null && current != null) {
                translations.delete(current);
            } else if (value != null && current == null) {
                translations.save(new CatalogTranslation(type, resourceId, locale, value));
            } else if (value != null) {
                current.rename(value);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<CatalogTranslationEntryResponse> findAll(CatalogResourceType type, String query) {
        String term = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        List<CatalogTranslation> allTranslations = translations.findAll();
        List<CatalogTranslationEntryResponse> result = new ArrayList<>();
        if (type == null || type == CatalogResourceType.SPECIALITY) {
            specialities.findAll().stream()
                    .filter(item -> matches(item.getName(), term))
                    .map(item -> entry(
                            CatalogResourceType.SPECIALITY,
                            item.getId(),
                            null,
                            item.getName(),
                            allTranslations))
                    .forEach(result::add);
        }
        if (type == null || type == CatalogResourceType.PROCEDURE) {
            procedures.findAll().stream()
                    .filter(item -> matches(item.getName(), term))
                    .map(item -> entry(
                            CatalogResourceType.PROCEDURE,
                            item.getId(),
                            item.getSpeciality().getId(),
                            item.getName(),
                            allTranslations))
                    .forEach(result::add);
        }
        return result.stream()
                .sorted(java.util.Comparator.comparing(
                        CatalogTranslationEntryResponse::canonicalName))
                .toList();
    }

    private CatalogTranslationEntryResponse entry(
            CatalogResourceType type,
            Long id,
            Long parentId,
            String name) {
        return entry(
                type,
                id,
                parentId,
                name,
                translations.findByResourceTypeAndResourceId(type, id));
    }

    private CatalogTranslationEntryResponse entry(
            CatalogResourceType type,
            Long id,
            Long parentId,
            String name,
            List<CatalogTranslation> availableTranslations) {
        List<CatalogTranslation> custom = availableTranslations.stream()
                .filter(item -> item.getResourceType() == type && item.getResourceId().equals(id)).toList();
        Map<String, String> effective = effectiveTranslations(type, name, custom);
        Set<String> customized = custom.stream()
                .map(CatalogTranslation::getLocale)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> missing = SUPPORTED_LOCALES.stream()
                .filter(locale -> !effective.containsKey(locale))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new CatalogTranslationEntryResponse(
                type,
                id,
                parentId,
                name,
                effective,
                Set.copyOf(customized),
                Set.copyOf(missing));
    }

    private java.util.Optional<String> builtIn(CatalogResourceType type, String name, String locale) {
        String prefix = type == CatalogResourceType.SPECIALITY ? "catalog.speciality." : "catalog.procedure.";
        String value = messages.getMessage(prefix + slug(name), null, null, Locale.forLanguageTag(locale));
        return java.util.Optional.ofNullable(value);
    }

    private Resource requireResource(CatalogResourceType type, Long id) {
        if (type == CatalogResourceType.SPECIALITY) {
            Speciality item = specialities.findById(id).orElseThrow(this::notFound);
            return new Resource(item.getId(), null, item.getName());
        }
        DentalProcedure item = procedures.findById(id).orElseThrow(this::notFound);
        return new Resource(item.getId(), item.getSpeciality().getId(), item.getName());
    }

    private void rejectUnsupportedLocales(Set<String> locales) {
        if (!SUPPORTED_LOCALES.containsAll(locales)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported catalog translation locale");
        }
    }

    private String supportedTag(Locale locale) {
        if (locale != null && locale.getLanguage().equals("pt")) return "pt-PT";
        if (locale != null && locale.getLanguage().equals("nl")) return "nl";
        return "en";
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private boolean matches(String name, String term) {
        return term.isEmpty() || name.toLowerCase(Locale.ROOT).contains(term);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog resource not found");
    }

    private String slug(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-)|(-$)", "");
    }

    private record Resource(Long id, Long parentId, String name) {
    }
}
