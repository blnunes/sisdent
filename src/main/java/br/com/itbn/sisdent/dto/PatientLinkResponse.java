package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.PatientLinkBasis;

import java.time.Instant;
import java.util.UUID;

public record PatientLinkResponse(
        UUID id,
        UUID patientId,
        UUID organizationId,
        UUID clinicUnitId,
        PatientLinkBasis operationalBasis,
        String createdBy,
        Instant createdAt) {
}
