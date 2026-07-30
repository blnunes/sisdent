package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.MembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MembershipRequest(
        @Email @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank @Size(min = 8, max = 128) String password,
        UUID clinicUnitId,
        @NotNull MembershipRole role) {
}
