package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String name,
        @NotNull @Past LocalDate birthDate,
        @NotNull Boolean active,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "\\d{11}") String taxId,
        @NotNull @Valid AddressRequest address) {
}
