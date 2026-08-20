package br.com.itbn.sisdent.avatar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import br.com.itbn.sisdent.error.ValidationException;

class ProfileAvatarProcessorTest {

    @Test
    void readsPhoneCameraExifOrientationAndRotatesPixelsBeforeCropping() {
        byte[] jpeg = jpegWithOrientation(6);
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, 0xffff0000);
        source.setRGB(2, 1, 0xff0000ff);

        BufferedImage oriented = ProfileAvatarProcessor.applyOrientation(source, ProfileAvatarProcessor.exifOrientation(jpeg));

        assertThat(ProfileAvatarProcessor.exifOrientation(jpeg)).isEqualTo(6);
        assertThat(oriented.getWidth()).isEqualTo(2);
        assertThat(oriented.getHeight()).isEqualTo(3);
        assertThat(oriented.getRGB(1, 0)).isEqualTo(0xffff0000);
        assertThat(oriented.getRGB(0, 2)).isEqualTo(0xff0000ff);
    }

    @Test
    void fallsBackToNormalOrientationForMalformedExif() {
        assertThat(ProfileAvatarProcessor.exifOrientation(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe1})).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8})
    void appliesEveryExifOrientationWithoutLosingPixels(int orientation) {
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, 0xffff0000);
        source.setRGB(1, 0, 0xff00ff00);
        source.setRGB(2, 1, 0xff0000ff);

        BufferedImage oriented = ProfileAvatarProcessor.applyOrientation(source, orientation);

        assertThat(oriented.getWidth()).isEqualTo(orientation >= 5 ? 2 : 3);
        assertThat(oriented.getHeight()).isEqualTo(orientation >= 5 ? 3 : 2);
        assertThat(pixels(oriented)).containsExactlyInAnyOrderElementsOf(pixels(source));
    }

    @Test
    void processedAvatarUsesArrayContentForValueEquality() {
        var first = new ProfileAvatarProcessor.ProcessedAvatar(new byte[] {1, 2}, "image/png");
        var second = new ProfileAvatarProcessor.ProcessedAvatar(new byte[] {1, 2}, "image/png");

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second)
                .hasToString("ProcessedAvatar[contentLength=2, contentType=image/png]");
        assertThat(first)
                .isNotEqualTo(new ProfileAvatarProcessor.ProcessedAvatar(new byte[] {2, 1}, "image/png"))
                .isNotEqualTo(new ProfileAvatarProcessor.ProcessedAvatar(new byte[] {1, 2}, "image/jpeg"))
                .isNotEqualTo("not an avatar");
    }

    @Test
    void rejectsEmptyAndInvalidlyDeclaredUploadsBeforeImageProcessing() {
        ProfileAvatarProcessor processor = new ProfileAvatarProcessor();

        assertThatThrownBy(() -> processor.process(new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> processor.process(new MockMultipartFile(
                "file", "avatar.png", "image/jpeg", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})))
                .isInstanceOf(ValidationException.class);
    }

    private static java.util.List<Integer> pixels(BufferedImage image) {
        return java.util.stream.IntStream.range(0, image.getWidth() * image.getHeight())
                .map(index -> image.getRGB(index % image.getWidth(), index / image.getWidth()))
                .boxed().toList();
    }

    private static byte[] jpegWithOrientation(int orientation) {
        return new byte[] {
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe1, 0, 32,
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0, 42, 0, 0, 0, 8,
                0, 1, 1, 18, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0, 0, 0, 0, 0
        };
    }
}
