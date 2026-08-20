package br.com.itbn.sisdent.avatar;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static byte[] jpegWithOrientation(int orientation) {
        return new byte[] {
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe1, 0, 32,
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0, 42, 0, 0, 0, 8,
                0, 1, 1, 18, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0, 0, 0, 0, 0
        };
    }
}
