package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.CatalogTranslationEntryResponse;
import br.com.itbn.sisdent.dto.CatalogTranslationRequest;
import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.service.CatalogTranslationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platform/catalog-translations")
public class CatalogTranslationController {
    private final CatalogTranslationService service;

    public CatalogTranslationController(CatalogTranslationService service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogTranslationEntryResponse> findAll(
            @RequestParam(required = false) CatalogResourceType type,
            @RequestParam(required = false) String query) {
        return service.findAll(type, query);
    }

    @PutMapping("/{type}/{id}")
    public CatalogTranslationEntryResponse replace(
            @PathVariable CatalogResourceType type,
            @PathVariable Long id,
            @Valid @RequestBody CatalogTranslationRequest request) {
        return service.replace(type, id, request.translations());
    }
}
