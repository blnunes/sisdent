package br.com.itbn.sisdent.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationExceptionTest {

    @ParameterizedTest
    @MethodSource("exceptionCategories")
    void retainsStableCodeMessageAndSemanticCategory(
            ApplicationException exception,
            ErrorCode errorCode,
            ErrorCategory category,
            String expectedMessage) {
        assertThat(exception.errorCode()).isEqualTo(errorCode);
        assertThat(exception.category()).isEqualTo(category);
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("metadataConstructors")
    void retainsMetadataWhenCreatedWithoutACause(
            Function<Map<String, String>, ApplicationException> constructor,
            ErrorCategory category) {
        ApplicationException exception = constructor.apply(Map.of("field", "size"));

        assertThat(exception.category()).isEqualTo(category);
        assertThat(exception.safeMetadata()).containsExactly(Map.entry("field", "size"));
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @MethodSource("causeConstructors")
    void retainsMetadataAndCauseWhenProvided(
            ExceptionConstructor constructor,
            ErrorCategory category) {
        IllegalArgumentException cause = new IllegalArgumentException("invalid");
        ApplicationException exception = constructor.create(Map.of("field", "size"), cause);

        assertThat(exception.category()).isEqualTo(category);
        assertThat(exception.safeMetadata()).containsExactly(Map.entry("field", "size"));
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @ParameterizedTest
    @MethodSource("codes")
    void exposesTheStableWireValueAndUsesItAsTheExceptionMessage(ErrorCode errorCode, String expected) {
        assertThat(errorCode.value()).isEqualTo(expected);
        assertThat(new ValidationException(errorCode).getMessage()).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    void copiesMetadataAndRetainsAnOptionalCause() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("field", "size");
        IllegalArgumentException cause = new IllegalArgumentException("invalid");

        ApplicationException exception = new ValidationException(
                ErrorCode.PAGINATION_INVALID_VALUES, metadata, cause);
        metadata.put("field", "page");

        assertThat(exception.safeMetadata()).containsExactly(Map.entry("field", "size"));
        assertThat(exception.getCause()).isSameAs(cause);
        Map<String, String> safeMetadata = exception.safeMetadata();
        assertThatThrownBy(() -> safeMetadata.put("another", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Stream<Arguments> metadataConstructors() {
        return Stream.of(
                Arguments.of((Function<Map<String, String>, ApplicationException>) metadata ->
                        new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED, metadata), ErrorCategory.RESOURCE_NOT_FOUND),
                Arguments.of((Function<Map<String, String>, ApplicationException>) metadata ->
                        new ValidationException(ErrorCode.VALIDATION_FAILED, metadata), ErrorCategory.VALIDATION),
                Arguments.of((Function<Map<String, String>, ApplicationException>) metadata ->
                        new BusinessRuleViolationException(ErrorCode.VALIDATION_FAILED, metadata), ErrorCategory.BUSINESS_RULE_VIOLATION),
                Arguments.of((Function<Map<String, String>, ApplicationException>) metadata ->
                        new ConflictException(ErrorCode.VALIDATION_FAILED, metadata), ErrorCategory.CONFLICT),
                Arguments.of((Function<Map<String, String>, ApplicationException>) metadata ->
                        new AuthenticationException(ErrorCode.VALIDATION_FAILED, metadata), ErrorCategory.AUTHENTICATION),
                Arguments.of((Function<Map<String, String>, ApplicationException>) metadata ->
                        new AuthorizationException(ErrorCode.VALIDATION_FAILED, metadata), ErrorCategory.AUTHORIZATION),
                Arguments.of((Function<Map<String, String>, ApplicationException>) metadata ->
                        new InfrastructureException(ErrorCode.VALIDATION_FAILED, metadata), ErrorCategory.INFRASTRUCTURE));
    }

    private static Stream<Arguments> causeConstructors() {
        return Stream.of(
                Arguments.of((ExceptionConstructor) (metadata, cause) -> new ResourceNotFoundException(ErrorCode.VALIDATION_FAILED, metadata, cause), ErrorCategory.RESOURCE_NOT_FOUND),
                Arguments.of((ExceptionConstructor) (metadata, cause) -> new ValidationException(ErrorCode.VALIDATION_FAILED, metadata, cause), ErrorCategory.VALIDATION),
                Arguments.of((ExceptionConstructor) (metadata, cause) -> new BusinessRuleViolationException(ErrorCode.VALIDATION_FAILED, metadata, cause), ErrorCategory.BUSINESS_RULE_VIOLATION),
                Arguments.of((ExceptionConstructor) (metadata, cause) -> new ConflictException(ErrorCode.VALIDATION_FAILED, metadata, cause), ErrorCategory.CONFLICT),
                Arguments.of((ExceptionConstructor) (metadata, cause) -> new AuthenticationException(ErrorCode.VALIDATION_FAILED, metadata, cause), ErrorCategory.AUTHENTICATION),
                Arguments.of((ExceptionConstructor) (metadata, cause) -> new AuthorizationException(ErrorCode.VALIDATION_FAILED, metadata, cause), ErrorCategory.AUTHORIZATION),
                Arguments.of((ExceptionConstructor) (metadata, cause) -> new InfrastructureException(ErrorCode.VALIDATION_FAILED, metadata, cause), ErrorCategory.INFRASTRUCTURE));
    }

    private static Stream<Arguments> exceptionCategories() {
        return Stream.of(
                Arguments.of(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND), ErrorCode.RESOURCE_NOT_FOUND,
                        ErrorCategory.RESOURCE_NOT_FOUND, "RESOURCE.NOT_FOUND"),
                Arguments.of(new ValidationException(ErrorCode.PAGINATION_INVALID_VALUES), ErrorCode.PAGINATION_INVALID_VALUES,
                        ErrorCategory.VALIDATION, "PAGINATION.INVALID_VALUES"),
                Arguments.of(new BusinessRuleViolationException(ErrorCode.BUSINESS_RULE_VIOLATION), ErrorCode.BUSINESS_RULE_VIOLATION,
                        ErrorCategory.BUSINESS_RULE_VIOLATION, "BUSINESS_RULE.VIOLATION"),
                Arguments.of(new ConflictException(ErrorCode.SCHEDULING_PRACTITIONER_UNAVAILABLE), ErrorCode.SCHEDULING_PRACTITIONER_UNAVAILABLE,
                        ErrorCategory.CONFLICT, "SCHEDULING.PRACTITIONER_UNAVAILABLE"),
                Arguments.of(new AuthenticationException(ErrorCode.AUTHENTICATION_FAILED), ErrorCode.AUTHENTICATION_FAILED,
                        ErrorCategory.AUTHENTICATION, "AUTHENTICATION.FAILED"),
                Arguments.of(new AuthorizationException(ErrorCode.AUTHORIZATION_DENIED), ErrorCode.AUTHORIZATION_DENIED,
                        ErrorCategory.AUTHORIZATION, "AUTHORIZATION.DENIED"),
                Arguments.of(new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE), ErrorCode.INFRASTRUCTURE_FAILURE,
                        ErrorCategory.INFRASTRUCTURE, "INFRASTRUCTURE.FAILURE"));
    }

    private static Stream<Arguments> codes() {
        return Stream.of(
                Arguments.of(ErrorCode.RESOURCE_NOT_FOUND, "RESOURCE.NOT_FOUND"),
                Arguments.of(ErrorCode.VALIDATION_FAILED, "VALIDATION.FAILED"),
                Arguments.of(ErrorCode.PAGINATION_INVALID_VALUES, "PAGINATION.INVALID_VALUES"),
                Arguments.of(ErrorCode.PAGINATION_UNSUPPORTED_SORT, "PAGINATION.UNSUPPORTED_SORT"),
                Arguments.of(ErrorCode.PAGINATION_UNSUPPORTED_DIRECTION, "PAGINATION.UNSUPPORTED_DIRECTION"),
                Arguments.of(ErrorCode.SCHEDULING_PRACTITIONER_UNAVAILABLE, "SCHEDULING.PRACTITIONER_UNAVAILABLE"),
                Arguments.of(ErrorCode.CATALOG_UNKNOWN_COUNTRY, "CATALOG.UNKNOWN_COUNTRY"),
                Arguments.of(ErrorCode.CATALOG_UNSUPPORTED_LOCALE, "CATALOG.UNSUPPORTED_LOCALE"),
                Arguments.of(ErrorCode.BUSINESS_RULE_VIOLATION, "BUSINESS_RULE.VIOLATION"),
                Arguments.of(ErrorCode.CONFLICT, "CONFLICT"),
                Arguments.of(ErrorCode.AUTHENTICATION_FAILED, "AUTHENTICATION.FAILED"),
                Arguments.of(ErrorCode.AUTHORIZATION_DENIED, "AUTHORIZATION.DENIED"),
                Arguments.of(ErrorCode.INFRASTRUCTURE_FAILURE, "INFRASTRUCTURE.FAILURE"),
                Arguments.of(ErrorCode.REQUEST_MALFORMED, "REQUEST.MALFORMED"),
                Arguments.of(ErrorCode.REQUEST_PARAMETER_INVALID, "REQUEST.PARAMETER_INVALID"),
                Arguments.of(ErrorCode.INTERNAL_ERROR, "INTERNAL.ERROR"));
    }

    @FunctionalInterface
    private interface ExceptionConstructor {
        ApplicationException create(Map<String, String> safeMetadata, Throwable cause);
    }
}
