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

GraphQL translates `ApplicationException` instances through
`graphql/ApplicationGraphQlExceptionResolver`. Execution failures return a
friendly message from the application message bundle and the stable
`errors[].extensions.code`. The first migrated shared concern is pagination:
invalid values, an unsupported sort field, and an unsupported direction use
the `PAGINATION.*` codes. The country catalogue also rejects unsupported
locales (supported languages: `en`, `nl`, and `pt`) with
`CATALOG.UNSUPPORTED_LOCALE`, rather than silently applying a fallback.

REST's existing error handlers and the remaining service exceptions are legacy
paths to be migrated incrementally to this same model. Correlation IDs,
structured error logging, and a unified RFC 9457 REST response are the next
observability/error-handling slice; they are not yet part of the public
contract.

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
