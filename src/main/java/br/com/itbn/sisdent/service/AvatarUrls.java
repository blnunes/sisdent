package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.model.Account;

import java.time.Instant;

/** Deliberately exposes only the authenticated self-service route, never a storage key. */
public final class AvatarUrls {
    private AvatarUrls() { }
    public static String forAccount(Account account) {
        Instant updated = account.getAvatarUpdatedAt();
        return updated == null ? null : "/api/account/settings/avatar?v=" + updated.toEpochMilli();
    }
}
