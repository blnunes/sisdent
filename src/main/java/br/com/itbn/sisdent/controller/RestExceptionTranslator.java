package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.error.ApplicationException;
import br.com.itbn.sisdent.error.ErrorCategory;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.service.SchedulingConflictException;
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
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.ObjectMapper;

/** The single REST adapter that turns failures into the public RFC 9457 contract. */
@Component
@RestControllerAdvice
public class RestExceptionTranslator {

    private static final String TYPE_PREFIX = "urn:sisdent:error:";
    private final MessageSource messages;
    private final ObjectMapper objectMapper;

    public RestExceptionTranslator(MessageSource messages, ObjectMapper objectMapper) {
        this.messages = messages;
        this.objectMapper = objectMapper;
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

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ProblemDetail invalidParameter(Exception exception, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_PARAMETER_INVALID, locale, Map.of(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail authorization(Locale locale) {
        return problem(HttpStatus.FORBIDDEN, ErrorCode.AUTHORIZATION_DENIED, locale, Map.of(), List.of());
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

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, Locale locale) {
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
        if (!violations.isEmpty()) {
            problem.setProperty("violations", violations);
        }
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
