package br.com.itbn.sisdent.dto;

import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID accountId,
        String email,
        String displayName,
        boolean platformAdministrator,
        UUID accountManagementOrganizationId,
        String preferredLanguage,
        String avatarUrl,
        List<MembershipResponse> memberships) {
}
