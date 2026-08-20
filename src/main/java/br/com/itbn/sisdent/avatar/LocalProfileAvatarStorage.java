package br.com.itbn.sisdent.avatar;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.InfrastructureException;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalProfileAvatarStorage implements ProfileAvatarStorage {
    private final Path root;

    public LocalProfileAvatarStorage(@Value("${sisdent.avatar.storage-directory}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override public void save(String key, byte[] content) {
        try {
            Files.createDirectories(root);
            Files.write(pathFor(key), content);
        } catch (IOException exception) {
            throw new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override public StoredProfileAvatar get(String key) {
        Path path = pathFor(key);
        try {
            if (!Files.isRegularFile(path)) throw new ResourceNotFoundException(ErrorCode.ACCOUNT_AVATAR_NOT_FOUND);
            return new StoredProfileAvatar(Files.newInputStream(path), Files.size(path));
        } catch (IOException exception) {
            throw new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override public void delete(String key) {
        try { Files.deleteIfExists(pathFor(key)); }
        catch (IOException exception) { throw new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE); }
    }

    private Path pathFor(String key) {
        if (key == null || !key.matches("[a-zA-Z0-9_-]{1,160}\\.png")) throw new IllegalArgumentException("Invalid avatar key");
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid avatar key");
        return resolved;
    }
}
