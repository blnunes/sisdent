package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.VoidOdontogramFindingRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Typed GraphQL input for versioned odontogram voiding. */
public record VoidOdontogramFindingMutationInput(
        @NotBlank @Size(max = 500) String reason,
        @NotNull Long version) {

    VoidOdontogramFindingRequest toRequest() {
        return new VoidOdontogramFindingRequest(reason, version);
    }
}
