package br.com.itbn.sisdent.dto;

import java.util.List;
import java.util.UUID;

/** Canonical administrative account representation; it deliberately has no legacy or patient fields. */
public record AccountResponse(UUID id, String displayName, String email, boolean active,
                              boolean platformAdministrator, long version,
                              List<MembershipResponse> memberships) {
}
