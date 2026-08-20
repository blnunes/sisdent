package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.DentalProcedureRequest;
import br.com.itbn.sisdent.dto.SpecialityRequest;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SpecialityMutationInput(
        @NotBlank String name,
        @NotEmpty List<@Valid DentalProcedureMutationInput> procedures,
        List<@Valid CatalogTranslationMutationInput> translations) {

    SpecialityRequest toRequest() {
        return new SpecialityRequest(name, procedures.stream()
                .map(procedure -> new DentalProcedureRequest(procedure.id(), procedure.name(),
                        translations(procedure.translations())))
                .toList(), translations(translations));
    }

    private static Map<String, String> translations(List<CatalogTranslationMutationInput> entries) {
        if (entries == null) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (CatalogTranslationMutationInput entry : entries) {
            if (result.putIfAbsent(entry.locale(), entry.value()) != null) {
                throw new ValidationException(ErrorCode.VALIDATION_FAILED);
            }
        }
        return result;
    }
}
