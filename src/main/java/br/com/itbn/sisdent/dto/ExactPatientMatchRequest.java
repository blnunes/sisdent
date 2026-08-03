package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ExactPatientMatchRequest(
        @NotNull DocumentType documentType,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String issuerCountryCode,
        @NotBlank String documentNumber,
        @NotNull LocalDate birthDate) {
}
