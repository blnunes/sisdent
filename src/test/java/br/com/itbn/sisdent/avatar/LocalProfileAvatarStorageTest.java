package br.com.itbn.sisdent.avatar;

import br.com.itbn.sisdent.error.InfrastructureException;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalProfileAvatarStorageTest {
    @TempDir Path directory;

    @Test
    void savesAtomicallyReadsAndDeletesIdempotently() throws Exception {
        LocalProfileAvatarStorage storage = new LocalProfileAvatarStorage(directory.toString());
        String key = "account_test.png";

        storage.save(key, "avatar".getBytes(StandardCharsets.UTF_8));

        try (var content = storage.get(key).content()) {
            assertThat(content.readAllBytes()).isEqualTo("avatar".getBytes(StandardCharsets.UTF_8));
        }
        try (var files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString())).containsExactly("account_test.png");
        }
        storage.delete(key);
        storage.delete(key);
        assertThatThrownBy(() -> storage.get(key)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsAFileWhereTheConfiguredDirectoryMustBeCreated() throws Exception {
        Path file = Files.createTempFile(directory, "avatar-root", ".tmp");
        String configuredPath = file.toString();

        assertThatThrownBy(() -> new LocalProfileAvatarStorage(configuredPath))
                .isInstanceOf(InfrastructureException.class);
    }
}
