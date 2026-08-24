package br.com.itbn.sisdent.graphql;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.web.multipart.MultipartFile;

/** Typed, in-memory GraphQL avatar input. It is intentionally never logged. */
public record AvatarUploadInput(String fileName, String contentType, String contentBase64) {
    MultipartFile toMultipartFile() {
        byte[] content = Base64.getDecoder().decode(contentBase64);
        return new AvatarMultipartFile(fileName, contentType, content);
    }

    private record AvatarMultipartFile(String fileName, String contentType, byte[] content) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return fileName; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File destination) throws IOException { java.nio.file.Files.write(destination.toPath(), content); }
        @Override public boolean equals(Object other) {
            return other instanceof AvatarMultipartFile(String otherFileName, String otherContentType, byte[] otherContent)
                    && java.util.Objects.equals(fileName, otherFileName)
                    && java.util.Objects.equals(contentType, otherContentType)
                    && Arrays.equals(content, otherContent);
        }
        @Override public int hashCode() {
            return 31 * (31 * java.util.Objects.hash(fileName, contentType) + Arrays.hashCode(content));
        }
        @Override public String toString() {
            return "AvatarMultipartFile[fileName=" + fileName + ", contentType=" + contentType
                    + ", contentLength=" + content.length + ']';
        }
    }
}
