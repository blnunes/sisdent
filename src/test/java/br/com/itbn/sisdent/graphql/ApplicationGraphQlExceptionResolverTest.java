package br.com.itbn.sisdent.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.itbn.sisdent.error.ApplicationException;
import br.com.itbn.sisdent.error.AuthenticationException;
import br.com.itbn.sisdent.error.AuthorizationException;
import br.com.itbn.sisdent.error.BusinessRuleViolationException;
import br.com.itbn.sisdent.error.ConflictException;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.InfrastructureException;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
import br.com.itbn.sisdent.error.ValidationException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.MDC;

class ApplicationGraphQlExceptionResolverTest {

    private final DataFetchingEnvironment environment = environment();
    private final ApplicationGraphQlExceptionResolver resolver =
            new ApplicationGraphQlExceptionResolver(messages(), new SimpleMeterRegistry());

    @AfterEach
    void clearMdc() { MDC.clear(); }

    @ParameterizedTest
    @MethodSource("applicationExceptions")
    void translatesEveryApplicationCategoryToAnExactPublicError(
            ApplicationException exception, String expectedCode, String expectedMessage) {
        GraphQLError error = resolver.resolveException(exception, environment).block().getFirst();

        assertThat(error.getMessage()).isEqualTo(expectedMessage);
        assertThat(error.getExtensions()).containsEntry("code", expectedCode);
    }

    static Stream<Arguments> applicationExceptions() {
        return Stream.of(
                Arguments.of(new ValidationException(ErrorCode.VALIDATION_FAILED),
                        "VALIDATION.FAILED", "One or more fields are invalid."),
                Arguments.of(new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND),
                        "RESOURCE.NOT_FOUND", "The requested resource was not found."),
                Arguments.of(new BusinessRuleViolationException(ErrorCode.BUSINESS_RULE_VIOLATION),
                        "BUSINESS_RULE.VIOLATION", "The request violates a business rule."),
                Arguments.of(new ConflictException(ErrorCode.CONFLICT),
                        "CONFLICT", "The request conflicts with the current state of the resource."),
                Arguments.of(new AuthenticationException(ErrorCode.AUTHENTICATION_FAILED),
                        "AUTHENTICATION.FAILED", "Authentication is required to access this resource."),
                Arguments.of(new AuthorizationException(ErrorCode.AUTHORIZATION_DENIED),
                        "AUTHORIZATION.DENIED", "You are not allowed to access this resource."),
                Arguments.of(new InfrastructureException(ErrorCode.INFRASTRUCTURE_FAILURE),
                        "INFRASTRUCTURE.FAILURE", "The service is temporarily unavailable. Please try again later."));
    }

    @ParameterizedTest
    @MethodSource("safeMetadata")
    void exposesOnlyExplicitSafeMetadata(Map<String, String> metadata) {
        GraphQLError error = resolver.resolveException(
                new ValidationException(ErrorCode.VALIDATION_FAILED, metadata), environment).block().getFirst();

        assertThat(error.getExtensions()).containsEntry("code", "VALIDATION.FAILED")
                .containsEntry("metadata", metadata);
    }

    static Stream<Arguments> safeMetadata() {
        return Stream.of(Arguments.of(Map.of("field", "name")));
    }

    @ParameterizedTest
    @MethodSource("unexpectedExceptions")
    void unexpectedExecutionFailuresNeverExposeInternalDetails(Throwable exception) {
        GraphQLError error = resolver.resolveException(exception, environment).block().getFirst();

        assertThat(error.getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(error.getExtensions()).containsEntry("code", "INTERNAL.ERROR");
        assertThat(error.getMessage()).doesNotContain("database password", "clinical note");
    }

    static Stream<Arguments> unexpectedExceptions() {
        return Stream.of(Arguments.of(new IllegalStateException("database password and clinical note")));
    }

    private static DataFetchingEnvironment environment() {
        DataFetchingEnvironment environment = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
        when(environment.getLocale()).thenReturn(Locale.ENGLISH);
        return environment;
    }

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        return source;
    }
}
