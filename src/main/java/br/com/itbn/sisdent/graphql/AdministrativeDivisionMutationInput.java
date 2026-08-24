package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AdministrativeDivisionRequest;
import jakarta.validation.constraints.NotBlank;

public record AdministrativeDivisionMutationInput(
        @NotBlank String name,
        @NotBlank String code,
        @NotBlank String type,
        @NotBlank String countryCode) {

    AdministrativeDivisionRequest toRequest() {
        return new AdministrativeDivisionRequest(name, code, type, countryCode);
    }
}
