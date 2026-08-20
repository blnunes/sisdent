package br.com.itbn.sisdent.error;

import java.util.Map;

public final class BusinessRuleViolationException extends ApplicationException {
    public BusinessRuleViolationException(ErrorCode errorCode) { this(errorCode, Map.of(), null); }
    public BusinessRuleViolationException(ErrorCode errorCode, Map<String, String> safeMetadata) { this(errorCode, safeMetadata, null); }
    public BusinessRuleViolationException(ErrorCode errorCode, Map<String, String> safeMetadata, Throwable cause) {
        super(errorCode, ErrorCategory.BUSINESS_RULE_VIOLATION, safeMetadata, cause);
    }
}
