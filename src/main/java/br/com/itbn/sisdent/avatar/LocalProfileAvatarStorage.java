package br.com.itbn.sisdent.avatar;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.InfrastructureException;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;

@Component
public class LocalProfileAvatarStorage implements ProfileAvatarStorage {
    private final Path root;

    public LocalProfileAvatarStorage(@Value("${sisdent.avatar.storage-directory}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        initialize();
    }

    @Override public void save(String key, byte[] content) {
        Path target = pathFor(key);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(root, ".avatar-", ".tmp");
            moveFile(temporary, target, content);
        } catch (IOException _) {
            throw new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException _) { /* cleaned up on startup */ }
            }
        }
    }

    private static void moveFile(Path temporary, Path target, byte[] content) throws IOException {
        try {
            Files.write(temporary, content);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException _) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override public StoredProfileAvatar get(String key) {
        Path path = pathFor(key);
        try {
            if (!Files.isRegularFile(path)) throw new ResourceNotFoundException(ErrorCode.ACCOUNT_AVATAR_NOT_FOUND);
            return new StoredProfileAvatar(Files.newInputStream(path), Files.size(path));
        } catch (IOException _) {
            throw new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE);
        }
    }

    @Override public void delete(String key) {
        try { Files.deleteIfExists(pathFor(key)); }
        catch (IOException _) { throw new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE); }
    }

    private void initialize() {
        try {
            Files.createDirectories(root);
            if (!Files.isDirectory(root) || !Files.isWritable(root)) throw new IOException("Avatar directory is not writable");
            try (var files = Files.list(root)) {
                files.filter(path -> path.getFileName().toString().startsWith(".avatar-")
                                && path.getFileName().toString().endsWith(".tmp"))
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException _) { /* retried next startup */ }
                        });
            }
        } catch (IOException | SecurityException _) {
            throw new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE);
        }
    }

    private Path pathFor(String key) {
        if (key == null || !key.matches("[a-zA-Z0-9_-]{1,160}\\.png")) throw new IllegalArgumentException("Invalid avatar key");
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid avatar key");
        return resolved;
    }
}
