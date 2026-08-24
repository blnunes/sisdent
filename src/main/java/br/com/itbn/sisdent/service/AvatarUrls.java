package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.model.Account;

import java.time.Instant;

/** Exposes avatar presence/version only; avatar bytes are retrieved through GraphQL. */
public final class AvatarUrls {
    private AvatarUrls() { }
    public static String forAccount(Account account) {
        Instant updated = account.getAvatarUpdatedAt();
        return updated == null ? null : "graphql-avatar?v=" + updated.toEpochMilli();
    }
}
