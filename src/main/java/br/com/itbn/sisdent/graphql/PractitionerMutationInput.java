package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.PractitionerRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record PractitionerMutationInput(
        @NotBlank @Size(max = 255) String displayName,
        @Size(max = 128) String registrationNumber,
        UUID accountId,
        @NotNull Set<@NotNull Long> specialityIds) {
    PractitionerRequest toRequest() {
        return new PractitionerRequest(displayName, registrationNumber, accountId, specialityIds);
    }
}
