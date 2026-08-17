package br.com.itbn.sisdent.error;

import java.util.Map;

public final class AuthorizationException extends ApplicationException {
    public AuthorizationException(ErrorCode errorCode) { this(errorCode, Map.of(), null); }
    public AuthorizationException(ErrorCode errorCode, Map<String, String> safeMetadata) { this(errorCode, safeMetadata, null); }
    public AuthorizationException(ErrorCode errorCode, Map<String, String> safeMetadata, Throwable cause) {
        super(errorCode, ErrorCategory.AUTHORIZATION, safeMetadata, cause);
    }
}
