package br.com.itbn.sisdent.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class AvatarUploadInputTest {
    @Test
    void createsValueSafeMultipartFilesWithoutExposingFileBytesInToString() {
        String content = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
        AvatarUploadInput input = new AvatarUploadInput("avatar.png", "image/png", content);

        Object first = input.toMultipartFile();
        Object sameContent = input.toMultipartFile();
        Object differentContent = new AvatarUploadInput("avatar.png", "image/png",
                Base64.getEncoder().encodeToString(new byte[] {3, 2, 1})).toMultipartFile();

        assertThat(first)
                .isEqualTo(sameContent)
                .hasSameHashCodeAs(sameContent)
                .isNotEqualTo(differentContent)
                .hasToString("AvatarMultipartFile[fileName=avatar.png, contentType=image/png, contentLength=3]");
    }
}
