package br.com.itbn.sisdent.error;

import java.util.Map;

public final class InfrastructureException extends ApplicationException {
    public InfrastructureException(ErrorCode errorCode) { this(errorCode, Map.of(), null); }
    public InfrastructureException(ErrorCode errorCode, Map<String, String> safeMetadata) { this(errorCode, safeMetadata, null); }
    public InfrastructureException(ErrorCode errorCode, Map<String, String> safeMetadata, Throwable cause) {
        super(errorCode, ErrorCategory.INFRASTRUCTURE, safeMetadata, cause);
    }
}
