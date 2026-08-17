package br.com.itbn.sisdent.error;

/** Semantic category used by transport adapters to select a safe response status. */
public enum ErrorCategory {
    RESOURCE_NOT_FOUND,
    VALIDATION,
    BUSINESS_RULE_VIOLATION,
    CONFLICT,
    AUTHENTICATION,
    AUTHORIZATION,
    INFRASTRUCTURE
}
