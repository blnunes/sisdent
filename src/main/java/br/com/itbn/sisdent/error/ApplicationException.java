package br.com.itbn.sisdent.error;

import java.util.Map;
import java.util.Objects;

/**
 * Base exception for expected application failures, independent of HTTP, GraphQL, or persistence APIs.
 *
 * <p>Metadata is limited to pre-sanitized values that are safe to expose to a client. It is copied so
 * callers cannot alter the error after it is raised.</p>
 */
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final ErrorCategory category;
    private final Map<String, String> safeMetadata;

    protected ApplicationException(
            ErrorCode errorCode,
            ErrorCategory category,
            Map<String, String> safeMetadata,
            Throwable cause) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").value(), cause);
        this.errorCode = errorCode;
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.safeMetadata = safeMetadata == null ? Map.of() : Map.copyOf(safeMetadata);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public ErrorCategory category() {
        return category;
    }

    public Map<String, String> safeMetadata() {
        return safeMetadata;
    }
}
