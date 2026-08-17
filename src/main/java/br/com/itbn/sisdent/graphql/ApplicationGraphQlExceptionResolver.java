package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.error.ApplicationException;
import br.com.itbn.sisdent.error.ErrorCode;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import graphql.validation.ValidationError;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Translates expected application errors into the public GraphQL error contract. */
@Component
public class ApplicationGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter
        implements WebGraphQlInterceptor {

    private final MessageSource messageSource;

    public ApplicationGraphQlExceptionResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        return exception instanceof ApplicationException applicationException
                ? applicationError(applicationException, environment)
                : error(ErrorCode.INTERNAL_ERROR, Map.of(), environment.getLocale());
    }

    /**
     * Sanitizes parse and validation failures raised before a data fetcher is invoked.
     * Execution failures are handled above; keeping both protocol phases here gives GraphQL
     * one public error translator.
     */
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        return chain.next(request).map(response -> response.transform(builder -> builder.errors(
                response.getExecutionResult().getErrors().stream()
                        .map(error -> hasPublicCode(error)
                                ? error
                                : error(error instanceof ValidationError
                                        ? ErrorCode.VALIDATION_FAILED
                                        : ErrorCode.REQUEST_MALFORMED, Map.of(), request.getLocale()))
                        .toList())));
    }

    private GraphQLError applicationError(ApplicationException exception, DataFetchingEnvironment environment) {
        return error(exception.errorCode(), exception.safeMetadata(), environment.getLocale());
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
}
