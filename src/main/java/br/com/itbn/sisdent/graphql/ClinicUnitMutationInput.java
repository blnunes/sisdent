package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.ClinicUnitRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicUnitMutationInput(@NotBlank @Size(max = 255) String name) {
    ClinicUnitRequest toRequest() { return new ClinicUnitRequest(name); }
}
