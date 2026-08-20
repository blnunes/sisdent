package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.PatientResponse;
import java.util.UUID;

/** The frontend refreshes the patient search after save, so no persistence-shaped entity is exposed. */
public record PatientMutationResult(UUID globalId, String name, boolean active) {
    static PatientMutationResult from(PatientResponse patient) {
        return new PatientMutationResult(patient.globalId(), patient.name(), patient.active());
    }
}
