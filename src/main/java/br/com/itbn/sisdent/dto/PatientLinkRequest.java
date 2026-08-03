package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.PatientLinkBasis;
import br.com.itbn.sisdent.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PatientLinkRequest(
        @NotNull DocumentType documentType,
        @NotBlank String issuerCountryCode,
        @NotBlank String documentNumber,
        @NotNull LocalDate birthDate,
        UUID clinicUnitId,
        @NotNull PatientLinkBasis operationalBasis) {
}
