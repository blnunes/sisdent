package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.MembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountMembershipRequest(@NotBlank @Email String email,
                                       java.util.UUID clinicUnitId,
                                       @NotNull MembershipRole role) {
}
