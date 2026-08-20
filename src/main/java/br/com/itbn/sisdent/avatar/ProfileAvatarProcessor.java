package br.com.itbn.sisdent.avatar;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/** Validates image bytes and emits a metadata-free, bounded PNG avatar. */
public final class ProfileAvatarProcessor {
    public static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MAX_INPUT_DIMENSION = 4096;
    private static final int AVATAR_SIZE = 256;
    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    public ProcessedAvatar process(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_EMPTY);
        if (file.getSize() > MAX_UPLOAD_BYTES) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_TOO_LARGE);
        try {
            byte[] bytes = file.getBytes();
            String type = detect(bytes);
            validateDeclaredType(file.getContentType(), type);
            validateExtension(file.getOriginalFilename(), type);
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_IMAGE);
            if (source.getWidth() <= 0 || source.getHeight() <= 0
                    || source.getWidth() > MAX_INPUT_DIMENSION || source.getHeight() > MAX_INPUT_DIMENSION) {
                throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_IMAGE);
            }
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
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_TYPE);
    }

    private static void validateDeclaredType(String declared, String detected) {
        if (declared == null || !detected.equals(declared.toLowerCase(Locale.ROOT).split(";", 2)[0].trim())) {
            throw new ValidationException(ErrorCode.ACCOUNT_AVATAR_INVALID_TYPE);
        }
    }

    private static void validateExtension(String filename, String detected) {
        if (filename == null || filename.isBlank()) return;
        int dot = filename.lastIndexOf('.');
        if (dot < 1 || !EXTENSIONS.get(detected).equals(filename.substring(dot + 1).toLowerCase(Locale.ROOT))) {
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

    private static byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG writer unavailable");
        return output.toByteArray();
    }

    public record ProcessedAvatar(byte[] content, String contentType) { }
}
