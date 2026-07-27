package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.IdentificationType;
import br.com.itbn.sisdent.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotNull IdentificationType identificationType,
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[\\p{L}\\p{N} -]+$")
        String identificationNumber,
        @Size(min = 8, max = 72)
        String password,
        @NotNull Role role) {
}
