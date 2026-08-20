package br.com.itbn.sisdent.error;

import java.util.Map;

public final class AuthenticationException extends ApplicationException {
    public AuthenticationException(ErrorCode errorCode) { this(errorCode, Map.of(), null); }
    public AuthenticationException(ErrorCode errorCode, Map<String, String> safeMetadata) { this(errorCode, safeMetadata, null); }
    public AuthenticationException(ErrorCode errorCode, Map<String, String> safeMetadata, Throwable cause) {
        super(errorCode, ErrorCategory.AUTHENTICATION, safeMetadata, cause);
    }
}
