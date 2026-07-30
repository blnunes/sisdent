package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Continent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CountryRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String code,
        @NotNull Continent continent) {
}
