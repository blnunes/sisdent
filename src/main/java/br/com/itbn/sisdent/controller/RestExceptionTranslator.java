package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.error.ApplicationException;
import br.com.itbn.sisdent.error.ErrorCategory;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.service.SchedulingConflictException;
import br.com.itbn.sisdent.observability.CorrelationIds;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.ObjectMapper;

/** The single REST adapter that turns failures into the public RFC 9457 contract. */
@Component
@RestControllerAdvice
public class RestExceptionTranslator {

    private static final String TYPE_PREFIX = "urn:sisdent:error:";
    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionTranslator.class);
    private final MessageSource messages;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RestExceptionTranslator(MessageSource messages, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.messages = messages;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(ApplicationException.class)
    ProblemDetail application(ApplicationException exception, Locale locale) {
        return problem(statusFor(exception.category()), exception.errorCode(), locale, exception.safeMetadata(), List.of());
    }

    @ExceptionHandler(SchedulingConflictException.class)
    ProblemDetail scheduling(SchedulingConflictException exception, Locale locale) {
        return application(exception, locale);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail beanValidation(MethodArgumentNotValidException exception, Locale locale) {
        List<Map<String, String>> violations = exception.getBindingResult().getFieldErrors().stream()
                .sorted(java.util.Comparator.comparing(FieldError::getField)
                        .thenComparing(FieldError::getDefaultMessage, java.util.Comparator.nullsFirst(String::compareTo)))
                .map(error -> violation(error, locale)).toList();
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, locale, Map.of(), violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintValidation(ConstraintViolationException exception, Locale locale) {
        List<Map<String, String>> violations = exception.getConstraintViolations().stream()
                .sorted(java.util.Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> Map.of("field", violation.getPropertyPath().toString(), "message", violation.getMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, locale, Map.of(), violations);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    ProblemDetail malformedRequest(Exception exception, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_MALFORMED, locale, Map.of(), List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class})
    ProblemDetail invalidParameter(Exception exception, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_PARAMETER_INVALID, locale, Map.of(), List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail uploadTooLarge(MaxUploadSizeExceededException exception, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.ACCOUNT_AVATAR_TOO_LARGE, locale, Map.of(), List.of());
    }

    @ExceptionHandler(MultipartException.class)
    ProblemDetail malformedMultipart(MultipartException exception, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_MALFORMED, locale, Map.of(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail authorization(Locale locale) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.AUTHORIZATION_DENIED, locale, Map.of(), List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authenticationFailure(Locale locale) {
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_FAILED, locale, Map.of(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail databaseConflict(Locale locale) {
        return problem(HttpStatus.CONFLICT, ErrorCode.CONFLICT, locale, Map.of(), List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail optimisticLock(Locale locale) {
        return problem(HttpStatus.CONFLICT, ErrorCode.CONFLICT, locale, Map.of(), List.of());
    }

    // Compatibility for REST endpoints not yet migrated away from HTTP exceptions. Their reason is never exposed.
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail responseStatus(ResponseStatusException exception, Locale locale) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return problem(status, codeFor(status), locale, Map.of(), List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail missingResource(Locale locale) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, locale, Map.of(), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail unmappedMethod(Locale locale) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, locale, Map.of(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, Locale locale) {
        // Do not log exception messages or stack traces here: they can contain request data or PII.
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("unexpected_error transport={} status={} correlationId={}", transport(), 500,
                    CorrelationIds.current());
        }
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, locale, Map.of(), List.of());
    }

    public void authentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(response, problem(HttpStatus.UNAUTHORIZED, ErrorCode.AUTHENTICATION_FAILED, request.getLocale(), Map.of(), List.of()));
    }

    public void authorization(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(response, problem(HttpStatus.FORBIDDEN, ErrorCode.AUTHORIZATION_DENIED, request.getLocale(), Map.of(), List.of()));
    }

    private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }

    private ProblemDetail problem(HttpStatus status, ErrorCode code, Locale locale, Map<String, String> metadata,
            List<Map<String, String>> violations) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message(code, metadata, locale));
        problem.setType(URI.create(TYPE_PREFIX + code.value().toLowerCase(Locale.ROOT)));
        problem.setTitle(message("title." + code.value(), status.getReasonPhrase(), locale));
        problem.setProperty("code", code.value());
        problem.setProperty("correlationId", CorrelationIds.current());
        if (!metadata.isEmpty()) {
            problem.setProperty("metadata", metadata);
        }
        if (!violations.isEmpty()) {
            problem.setProperty("violations", violations);
        }
        if (code != ErrorCode.INTERNAL_ERROR) recordKnownError(code, status);
        return problem;
    }

    private String message(ErrorCode code, Map<String, String> metadata, Locale locale) {
        Object[] arguments = metadata.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toArray();
        return messages.getMessage("error." + code.value(), arguments, code.value(), locale);
    }

    private String message(String key, String fallback, Locale locale) {
        return messages.getMessage(key, null, fallback, locale);
    }

    private Map<String, String> violation(FieldError error, Locale locale) {
        String detail = messages.getMessage(new DefaultMessageSourceResolvable(error), locale);
        return Map.of("field", error.getField(), "message", detail);
    }

    private void recordKnownError(ErrorCode code, HttpStatus status) {
        String category = categoryFor(code);
        String transport = transport();
        meterRegistry.counter("sisdent.error.count", "code", code.value(), "transport", transport).increment();
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("known_error code={} category={} transport={} status={} correlationId={}",
                    code.value(), category, transport, status.value(), CorrelationIds.current());
        }
    }

    private String transport() { return "graphql".equals(MDC.get("transport")) ? "graphql" : "rest"; }

    private String categoryFor(ErrorCode code) {
        return switch (code) {
            case RESOURCE_NOT_FOUND, CATALOG_UNKNOWN_COUNTRY, ACCOUNT_AVATAR_NOT_FOUND -> ErrorCategory.RESOURCE_NOT_FOUND.name();
            case VALIDATION_FAILED, PAGINATION_INVALID_VALUES, PAGINATION_UNSUPPORTED_SORT,
                    PAGINATION_UNSUPPORTED_DIRECTION, CATALOG_UNSUPPORTED_LOCALE,
                    REQUEST_MALFORMED, REQUEST_PARAMETER_INVALID -> ErrorCategory.VALIDATION.name();
            case BUSINESS_RULE_VIOLATION, SCHEDULING_PRACTITIONER_UNAVAILABLE -> ErrorCategory.BUSINESS_RULE_VIOLATION.name();
            case CONFLICT, ACCOUNT_PROFILE_VERSION_CONFLICT -> ErrorCategory.CONFLICT.name();
            case ACCOUNT_CURRENT_PASSWORD_INVALID, ACCOUNT_DISPLAY_NAME_INVALID, ACCOUNT_PREFERRED_LANGUAGE_INVALID,
                    ACCOUNT_AVATAR_EMPTY, ACCOUNT_AVATAR_TOO_LARGE, ACCOUNT_AVATAR_INVALID_TYPE,
                    ACCOUNT_AVATAR_INVALID_IMAGE -> ErrorCategory.VALIDATION.name();
            case AUTHENTICATION_FAILED -> ErrorCategory.AUTHENTICATION.name();
            case AUTHORIZATION_DENIED -> ErrorCategory.AUTHORIZATION.name();
            case INFRASTRUCTURE_FAILURE -> ErrorCategory.INFRASTRUCTURE.name();
            case INTERNAL_ERROR -> "UNEXPECTED";
        };
    }

    private HttpStatus statusFor(ErrorCategory category) {
        return switch (category) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case BUSINESS_RULE_VIOLATION, CONFLICT -> HttpStatus.CONFLICT;
            case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
            case AUTHORIZATION -> HttpStatus.FORBIDDEN;
            case INFRASTRUCTURE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }

    private ErrorCode codeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.REQUEST_PARAMETER_INVALID;
            case UNAUTHORIZED -> ErrorCode.AUTHENTICATION_FAILED;
            case FORBIDDEN -> ErrorCode.AUTHORIZATION_DENIED;
            case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND;
            case CONFLICT -> ErrorCode.CONFLICT;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
