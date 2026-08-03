package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.NotNull;

public record AccountLifecycleRequest(@NotNull Boolean active, @NotNull Long version) {
}
