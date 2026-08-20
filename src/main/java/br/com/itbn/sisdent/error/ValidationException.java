package br.com.itbn.sisdent.error;

import java.util.Map;

public final class ValidationException extends ApplicationException {
    public ValidationException(ErrorCode errorCode) { this(errorCode, Map.of(), null); }
    public ValidationException(ErrorCode errorCode, Map<String, String> safeMetadata) { this(errorCode, safeMetadata, null); }
    public ValidationException(ErrorCode errorCode, Map<String, String> safeMetadata, Throwable cause) {
        super(errorCode, ErrorCategory.VALIDATION, safeMetadata, cause);
    }
}
