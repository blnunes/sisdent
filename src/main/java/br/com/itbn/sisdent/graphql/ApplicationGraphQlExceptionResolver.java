package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.error.ApplicationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

/** Translates expected application errors into the public GraphQL error contract. */
@Component
public class ApplicationGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private final MessageSource messageSource;

    public ApplicationGraphQlExceptionResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (!(exception instanceof ApplicationException applicationException)) {
            return null;
        }

        String code = applicationException.errorCode().value();
        String message = messageSource.getMessage(
                "error." + code,
                applicationException.safeMetadata().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(Map.Entry::getValue)
                        .toArray(),
                code,
                environment.getLocale());
        return GraphqlErrorBuilder.newError(environment)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
