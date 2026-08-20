package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.error.ApplicationException;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.observability.CorrelationIds;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.ExecutionResultImpl;
import graphql.schema.DataFetchingEnvironment;
import graphql.validation.ValidationError;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BindException;
import jakarta.validation.ConstraintViolationException;
import reactor.core.publisher.Mono;

/** Translates expected application errors into the public GraphQL error contract. */
@Component
public class ApplicationGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter
        implements WebGraphQlInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationGraphQlExceptionResolver.class);
    private final MessageSource messageSource;
    private final MeterRegistry meterRegistry;

    public ApplicationGraphQlExceptionResolver(MessageSource messageSource, MeterRegistry meterRegistry) {
        this.messageSource = messageSource;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof ApplicationException applicationException) {
            recordKnownError(applicationException.errorCode(), applicationException.category().name());
            return applicationError(applicationException, environment);
        }
        if (exception instanceof AccessDeniedException) {
            return error(ErrorCode.AUTHORIZATION_DENIED, Map.of(), environment.getLocale());
        }
        if (exception instanceof ResponseStatusException responseStatusException) {
            return error(errorCode(responseStatusException), Map.of(), environment.getLocale());
        }
        if (exception instanceof ConstraintViolationException || exception instanceof BindException) {
            return error(ErrorCode.VALIDATION_FAILED, Map.of(), environment.getLocale());
        }
        String field = Optional.ofNullable(environment.getField()).map(graphql.language.Field::getName).orElse("unknown");
        // Do not log exception messages or stack traces: resolver failures can originate in clinical services.
        LOG.error("unexpected_error transport=graphql status=200 correlationId={} field={}",
                CorrelationIds.current(), field);
        return error(ErrorCode.INTERNAL_ERROR, Map.of(), environment.getLocale());
    }

    /**
     * Sanitizes parse and validation failures raised before a data fetcher is invoked.
     * Execution failures are handled above; keeping both protocol phases here gives GraphQL
     * one public error translator.
     */
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        return chain.next(request).map(response -> {
            var errors = response.getExecutionResult().getErrors().stream()
                    .map(error -> hasPublicCode(error) ? withCorrelationId(error)
                            : error(error instanceof ValidationError
                                    ? ErrorCode.VALIDATION_FAILED
                                    : ErrorCode.REQUEST_MALFORMED, Map.of(), request.getLocale()))
                    .toList();
            var executionResult = ExecutionResultImpl.newExecutionResult().from(response.getExecutionResult())
                    .errors(errors).build();
            WebGraphQlResponse sanitized = new WebGraphQlResponse(
                    new DefaultExecutionGraphQlResponse(response.getExecutionInput(), executionResult));
            recordExecution(request, sanitized);
            return sanitized;
        });
    }

    private GraphQLError applicationError(ApplicationException exception, DataFetchingEnvironment environment) {
        return error(exception.errorCode(), exception.safeMetadata(), environment.getLocale());
    }

    private ErrorCode errorCode(ResponseStatusException exception) {
        return switch (exception.getStatusCode().value()) {
            case 400 -> ErrorCode.VALIDATION_FAILED;
            case 401 -> ErrorCode.AUTHENTICATION_FAILED;
            case 403 -> ErrorCode.AUTHORIZATION_DENIED;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    private GraphQLError error(ErrorCode errorCode, Map<String, String> safeMetadata, java.util.Locale locale) {
        String code = errorCode.value();
        String message = messageSource.getMessage(
                "error." + code,
                safeMetadata.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .toArray(),
                code,
                locale);
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("code", code);
        extensions.put("correlationId", CorrelationIds.current());
        if (!safeMetadata.isEmpty()) {
            extensions.put("metadata", safeMetadata);
        }
        return GraphqlErrorBuilder.newError()
                .message(message)
                .extensions(extensions)
                .build();
    }

    private boolean hasPublicCode(GraphQLError error) {
        return error.getExtensions() != null && error.getExtensions().containsKey("code");
    }

    private GraphQLError withCorrelationId(GraphQLError error) {
        Map<String, Object> extensions = new LinkedHashMap<>(error.getExtensions());
        extensions.put("correlationId", CorrelationIds.current());
        return GraphqlErrorBuilder.newError().message(error.getMessage()).path(error.getPath())
                .locations(error.getLocations()).extensions(extensions).build();
    }

    private void recordKnownError(ErrorCode code, String category) {
        meterRegistry.counter("sisdent.error.count", "code", code.value(), "transport", "graphql").increment();
        LOG.warn("known_error code={} category={} transport=graphql status=200 correlationId={}",
                code.value(), category, CorrelationIds.current());
    }

    private void recordExecution(WebGraphQlRequest request, WebGraphQlResponse response) {
        String document = request.getDocument();
        String operation = document.contains("countries") ? "countries"
                : document.contains("country") ? "country" : "unknown";
        String operationType = document.stripLeading().startsWith("mutation") ? "mutation"
                : document.stripLeading().startsWith("subscription") ? "subscription" : "query";
        String outcome = response.getExecutionResult().getErrors().isEmpty() ? "success" : "error";
        meterRegistry.counter("sisdent.graphql.execution.count", "operation", operation,
                "operationType", operationType, "outcome", outcome).increment();
        LOG.info("graphql_completed operation={} operationType={} outcome={} correlationId={}",
                operation, operationType, outcome, CorrelationIds.current());
    }
}
