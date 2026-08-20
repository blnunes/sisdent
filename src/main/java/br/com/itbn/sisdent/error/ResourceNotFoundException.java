package br.com.itbn.sisdent.error;

import java.util.Map;

public final class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(ErrorCode errorCode) { this(errorCode, Map.of(), null); }
    public ResourceNotFoundException(ErrorCode errorCode, Map<String, String> safeMetadata) { this(errorCode, safeMetadata, null); }
    public ResourceNotFoundException(ErrorCode errorCode, Map<String, String> safeMetadata, Throwable cause) {
        super(errorCode, ErrorCategory.RESOURCE_NOT_FOUND, safeMetadata, cause);
    }
}
