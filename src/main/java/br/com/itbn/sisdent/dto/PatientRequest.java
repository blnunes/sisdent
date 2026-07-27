package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.IdentificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.Set;

public record PatientRequest(
        @NotBlank String name,
        @NotNull @Past LocalDate birthDate,
        @NotNull Boolean active,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "\\d{11}") String taxId,
        @NotNull IdentificationType identificationType,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9 -]{2,63}")
        String identificationNumber,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String nationalityCode,
        @NotNull @Valid AddressRequest address,
        @NotEmpty Set<@NotNull Long> specialityIds) {
}
