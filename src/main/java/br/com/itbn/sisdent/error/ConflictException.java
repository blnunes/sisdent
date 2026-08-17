package br.com.itbn.sisdent.error;

import java.util.Map;

public final class ConflictException extends ApplicationException {
    public ConflictException(ErrorCode errorCode) { this(errorCode, Map.of(), null); }
    public ConflictException(ErrorCode errorCode, Map<String, String> safeMetadata) { this(errorCode, safeMetadata, null); }
    public ConflictException(ErrorCode errorCode, Map<String, String> safeMetadata, Throwable cause) {
        super(errorCode, ErrorCategory.CONFLICT, safeMetadata, cause);
    }
}
