package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotNull;

public record MembershipRevokeRequest(@NotNull Long version) {
}
