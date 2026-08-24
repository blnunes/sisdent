package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.VoidPerformedProcedureRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Typed GraphQL input for a performed-procedure void reason. */
public record VoidPerformedProcedureMutationInput(@NotBlank @Size(max = 500) String reason) {

    VoidPerformedProcedureRequest toRequest() {
        return new VoidPerformedProcedureRequest(reason);
    }
}
