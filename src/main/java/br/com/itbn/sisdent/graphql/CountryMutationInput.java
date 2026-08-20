package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.CountryRequest;
import br.com.itbn.sisdent.model.Continent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Public GraphQL write contract; conversion keeps the application DTO transport-neutral. */
public record CountryMutationInput(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String code,
        @NotNull Continent continent) {
    CountryRequest toRequest() {
        return new CountryRequest(name, code, continent);
    }
}
