package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.MembershipRole;
import jakarta.validation.constraints.NotNull;

public record MembershipRoleUpdateRequest(@NotNull MembershipRole role, @NotNull Long version) {
}
