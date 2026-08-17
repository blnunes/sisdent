# Sisdent architecture

Sisdent is a Spring Boot REST API with an Angular single-page application.
Authentication uses email/password and stateless JWT Bearer tokens. `Account`
is the global identity, `Person` provides its display name, and active
`Membership` records are the sole authority for organization and clinic work.

Patient identity is global for deduplication. Operational visibility and
lifecycle are scoped by active `PatientOrganizationLink` records; deactivation
removes only the current organization link. Appointments and clinical records
are organization and clinic scoped. Final clinical records are immutable and
corrections are modeled as amendments or replacement findings.

Foundational catalogues are platform-admin resources. Operational resources are
authorized in `ScopeAuthorizationService`; UI guards improve navigation but do
not provide security.

Flyway is the sole schema evolution mechanism. Applied migrations are
immutable. Migration V14 removes the retired identification-based
authentication, legacy user tables, and email-enrollment state.

## Error handling

Expected application failures use the transport-neutral types in
`br.com.itbn.sisdent.error`. Each one has a semantic category and a stable
`ErrorCode`; services must not use an HTTP exception merely to express a
business or validation failure. Clients must branch on the error code, not on
the human-readable message.

REST translates failures in `controller/RestExceptionTranslator` to RFC 9457
responses. Its `code` property is stable, while `detail` and validation
violations are localized. Authentication and authorization that security
rejects before controller execution remain HTTP 401/403 problem responses.
Every HTTP request accepts a validated `X-Correlation-ID`, otherwise receives a generated
UUID. The final value is returned in that response header and REST problems expose it as
`correlationId`; GraphQL execution errors expose it as `errors[].extensions.correlationId`.
It is request-scoped in SLF4J MDC and cleared in a `finally` block.

GraphQL uses `graphql/ApplicationGraphQlExceptionResolver` as its single
public error translator. It covers both execution exceptions and parse or
validation errors that happen before data fetching. Execution failures retain
the GraphQL `errors` envelope; every translated error has a friendly localized
`message` and stable `errors[].extensions.code`. Explicitly safe application
metadata is exposed only as `errors[].extensions.metadata`; exception messages,
causes, SQL details, and request data are never exposed. Pagination uses the
`PAGINATION.*` codes, and unsupported locales use
`CATALOG.UNSUPPORTED_LOCALE`.

## Operational observability

Request completion logs contain only method, normalized route, status, duration, and
correlation ID. Known-error logs add stable code, category, transport and status;
unexpected errors add the server-side stack trace but retain the same safe public fields.
GraphQL logs and metrics use only recognised schema operations and operation types, never
the document variables. Bodies, authorization headers, credentials, tokens, patient data,
clinical notes, and business identifiers are not logged or used as metric labels.

Micrometer records HTTP request count/duration by transport, normalized route and status,
GraphQL execution count by recognised operation/type/outcome, and known-error count by
stable code and transport. Correlation and business identifiers are deliberately excluded
from metric labels. Actuator is dependency-backed but security permits only `/actuator/health`;
additional endpoints require explicit authorization and configuration. Distributed tracing is
deferred: an OpenTelemetry exporter may later bridge this correlation ID/MDC integration.

Some pre-existing service methods still throw Spring HTTP exceptions. They are
legacy REST-only paths and are safely translated by the REST adapter, but must
be converted to `ApplicationException` before being exposed through another
transport. The country lookup used by GraphQL has been migrated and returns
`CATALOG.UNKNOWN_COUNTRY` when absent.

## GraphQL learning example

REST remains the primary API. GraphQL is available alongside it at `POST /graphql`
as a small, read-only learning example: `countries`. It exposes the platform country
catalogue with the same pagination, sorting and localized display name used by REST.
Like `/api/countries`, it requires a JWT with `ROLE_PLATFORM_ADMIN`.

The GraphQL schema is in `src/main/resources/graphql/countries.graphqls`; the resolver
is `graphql/CountryQueryController`. The resolver is only a transport adapter: it builds
the transport-neutral `PageQuery` and calls `CountryService`. This preserves the project
rule that business logic, database access and authorization decisions do not move into
controllers or resolvers.

Example request (use a platform-admin bearer token):

```graphql
query {
  countries(page: 0, size: 10, sort: "name", direction: "asc", locale: "pt-PT") {
    content { id code name displayName continent }
    page size totalElements totalPages
  }
}
```

New GraphQL services should begin as a query and follow the same sequence: define a
small schema type, add a resolver that delegates to an existing service, protect the
endpoint in `SecurityConfiguration`, and add unit plus authenticated integration tests.
Mutations should only be introduced after their REST/service validations and transactional
behaviour are already understood.
