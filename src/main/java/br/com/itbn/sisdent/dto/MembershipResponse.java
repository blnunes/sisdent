package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.MembershipRole;

import java.util.UUID;

public record MembershipResponse(
        UUID id,
        UUID organizationId,
        String organizationName,
        UUID clinicUnitId,
        String clinicUnitName,
        MembershipRole role) {
}
