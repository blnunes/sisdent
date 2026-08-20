package br.com.itbn.sisdent.avatar;

import java.io.InputStream;

/** Isolates storage so a production object-store adapter can replace the local implementation. */
public interface ProfileAvatarStorage {
    void save(String key, byte[] content);
    StoredProfileAvatar get(String key);
    void delete(String key);

    record StoredProfileAvatar(InputStream content, long length) { }
}
