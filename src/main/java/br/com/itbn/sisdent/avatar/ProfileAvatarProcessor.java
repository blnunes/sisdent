package br.com.itbn.sisdent.avatar;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Validates image bytes and emits a metadata-free, bounded PNG avatar. */
public final class ProfileAvatarProcessor {
    public static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MAX_INPUT_DIMENSION = 4096;
    private static final int AVATAR_SIZE = 256;
    private static final Map<String, Set<String>> EXTENSIONS = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"), "image/png", Set.of("png"));

    public ProcessedAvatar process(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_EMPTY);
        if (file.getSize() > MAX_UPLOAD_BYTES) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_TOO_LARGE);
        try {
            byte[] bytes = file.getBytes();
            String type = detect(bytes);
            validateDeclaredType(file.getContentType(), type);
            validateExtension(file.getOriginalFilename(), type);
            BufferedImage source = applyOrientation(readBounded(bytes), "image/jpeg".equals(type) ? exifOrientation(bytes) : 1);
            return new ProcessedAvatar(toPng(square(source)), "image/png");
        } catch (ValidationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_IMAGE);
        }
    }

    private static String detect(byte[] bytes) {
        if (bytes.length < 8) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_IMAGE);
        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_TYPE);
    }

    /** Checks dimensions through the reader before decoding pixels to bound image allocations. */
    private static BufferedImage readBounded(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_IMAGE);
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_INPUT_DIMENSION || height > MAX_INPUT_DIMENSION) {
                    throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_IMAGE);
                }
                BufferedImage source = reader.read(0);
                if (source == null) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_IMAGE);
                return source;
            } finally { reader.dispose(); }
        }
    }

    private static void validateDeclaredType(String declared, String detected) {
        if (declared == null || !detected.equals(declared.toLowerCase(Locale.ROOT).split(";", 2)[0].trim())) {
            throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_TYPE);
        }
    }

    private static void validateExtension(String filename, String detected) {
        if (filename == null || filename.isBlank()) return;
        int dot = filename.lastIndexOf('.');
        if (dot < 1 || !EXTENSIONS.get(detected).contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT))) {
            throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_TYPE);
        }
    }

    private static BufferedImage square(BufferedImage source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        BufferedImage target = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, AVATAR_SIZE, AVATAR_SIZE, x, y, x + side, y + side, null);
        } finally { graphics.dispose(); }
        return target;
    }

    /** Applies the display orientation embedded by phone cameras before the centre square crop. */
    static BufferedImage applyOrientation(BufferedImage source, int orientation) {
        if (orientation < 2 || orientation > 8) return source;
        int width = source.getWidth();
        int height = source.getHeight();
        boolean swapAxes = orientation >= 5;
        BufferedImage target = new BufferedImage(swapAxes ? height : width, swapAxes ? width : height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int targetX;
            int targetY;
            switch (orientation) {
                case 2 -> { targetX = width - 1 - x; targetY = y; }
                case 3 -> { targetX = width - 1 - x; targetY = height - 1 - y; }
                case 4 -> { targetX = x; targetY = height - 1 - y; }
                case 5 -> { targetX = y; targetY = x; }
                case 6 -> { targetX = height - 1 - y; targetY = x; }
                case 7 -> { targetX = height - 1 - y; targetY = width - 1 - x; }
                case 8 -> { targetX = y; targetY = width - 1 - x; }
                default -> { targetX = x; targetY = y; }
            }
            target.setRGB(targetX, targetY, source.getRGB(x, y));
        }
        return target;
    }

    /** Reads only the EXIF orientation tag (0x0112); malformed metadata safely falls back to normal orientation. */
    static int exifOrientation(byte[] jpeg) {
        int index = 2;
        while (index + 4 <= jpeg.length && (jpeg[index] & 0xff) == 0xff) {
            int marker = jpeg[index + 1] & 0xff;
            if (marker == 0xda || marker == 0xd9) return 1;
            int length = unsignedShort(jpeg, index + 2, false);
            if (length < 8 || index + 2 + length > jpeg.length) return 1;
            int payload = index + 4;
            if (marker == 0xe1 && hasExifHeader(jpeg, payload)) return tiffOrientation(jpeg, payload + 6, index + 2 + length);
            index += 2 + length;
        }
        return 1;
    }

    private static boolean hasExifHeader(byte[] bytes, int index) {
        return index + 6 <= bytes.length && bytes[index] == 'E' && bytes[index + 1] == 'x' && bytes[index + 2] == 'i'
                && bytes[index + 3] == 'f' && bytes[index + 4] == 0 && bytes[index + 5] == 0;
    }

    private static int tiffOrientation(byte[] bytes, int tiff, int end) {
        if (tiff + 8 > end) return 1;
        boolean littleEndian = bytes[tiff] == 'I' && bytes[tiff + 1] == 'I';
        if (!littleEndian && !(bytes[tiff] == 'M' && bytes[tiff + 1] == 'M') || unsignedShort(bytes, tiff + 2, littleEndian) != 42) return 1;
        int ifd = tiff + unsignedInt(bytes, tiff + 4, littleEndian);
        if (ifd < tiff || ifd + 2 > end) return 1;
        int entries = unsignedShort(bytes, ifd, littleEndian);
        for (int entry = ifd + 2; entry + 12 <= end && entry < ifd + 2 + entries * 12; entry += 12) {
            if (unsignedShort(bytes, entry, littleEndian) == 0x0112 && unsignedShort(bytes, entry + 2, littleEndian) == 3
                    && unsignedInt(bytes, entry + 4, littleEndian) == 1) {
                int orientation = unsignedShort(bytes, entry + 8, littleEndian);
                return orientation >= 1 && orientation <= 8 ? orientation : 1;
            }
        }
        return 1;
    }

    private static int unsignedShort(byte[] bytes, int offset, boolean littleEndian) {
        return littleEndian ? (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8)
                : ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);
    }

    private static int unsignedInt(byte[] bytes, int offset, boolean littleEndian) {
        return littleEndian ? (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8) | ((bytes[offset + 2] & 0xff) << 16) | ((bytes[offset + 3] & 0xff) << 24)
                : ((bytes[offset] & 0xff) << 24) | ((bytes[offset + 1] & 0xff) << 16) | ((bytes[offset + 2] & 0xff) << 8) | (bytes[offset + 3] & 0xff);
    }

    private static byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG writer unavailable");
        return output.toByteArray();
    }

    public record ProcessedAvatar(byte[] content, String contentType) { }
}
