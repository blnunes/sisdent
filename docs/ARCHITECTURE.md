# Sisdent architecture

## Phase 7

Phase 7 objective: establish GraphQL as a frontend-oriented BFF foundation while REST remains the primary supported API during the transition.

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
For example: `{"type":"urn:sisdent:error:catalog.unsupported_locale","status":400,
"code":"CATALOG.UNSUPPORTED_LOCALE","detail":"…","correlationId":"…",
"metadata":{"supportedLocales":"en, nl, pt"}}`.
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
`CATALOG.UNSUPPORTED_LOCALE`. For the equivalent GraphQL failure the envelope is
`{"errors":[{"message":"…","extensions":{"code":"CATALOG.UNSUPPORTED_LOCALE",
"correlationId":"…","metadata":{"supportedLocales":"en, nl, pt"}}}]}`.

Stable codes are additive public API: never rename, reuse, or derive them from
exception text. Error metadata is opt-in and must contain only documented safe
values. Catalogue language acceptance is centralized in `SupportedCatalogLocale`:
`en`, `pt`, and `nl` are accepted, including regional variants such as `pt-PT`.
Missing catalogue language defaults to English; blank, malformed, and unsupported
values are rejected as `CATALOG.UNSUPPORTED_LOCALE`. The rejected value is never
returned as metadata. Error localization follows the transport request locale;
the rejected catalogue locale never selects the error language.

## Operational observability

Request completion logs contain only method, normalized route, status, duration, and
correlation ID. Known-error logs add stable code, category, transport and status.
Unexpected errors deliberately omit exception messages and stack traces because those
can contain request or clinical data. MDC holds only correlation ID and transport for
the request lifetime and is cleared in `finally`.
GraphQL logs and metrics use only recognised schema operations and operation types, never
the document variables. Bodies, authorization headers, credentials, tokens, patient data,
clinical notes, and business identifiers are not logged or used as metric labels.

Micrometer records HTTP request count/duration by transport, normalized route and status,
GraphQL execution count by recognised operation/type/outcome, and known-error count by
stable code and transport. Correlation and business identifiers are deliberately excluded
from metric labels. Actuator is dependency-backed but security permits only `/actuator/health`;
additional endpoints require explicit authorization and configuration. Distributed tracing is
deferred: an OpenTelemetry exporter may later bridge this correlation ID/MDC integration.

### Exception rollout inventory

`CountryService` and `CatalogTranslationService` are migrated to
`ApplicationException`; their catalogue-locale and not-found paths now use the
shared stable-code contract. The remaining direct `ResponseStatusException`
uses are deferred legacy REST-only paths: `AccountManagementService`,
`AddressService`, `AdministrativeDivisionService`, `AppointmentService`,
`ClinicalRecordService`, `CurrentAccountService`, `OdontogramService`,
`OrganizationPatientService`, `OrganizationService`, `PatientService`,
`PerformedProcedureService`, `PractitionerService`, `ScopeAuthorizationService`,
and `SpecialityService`. They remain behind the compatibility handler in
`RestExceptionTranslator`; migrate each service transaction as soon as it gains a
GraphQL or other public transport, preserving the existing status before removing
that handler. `IllegalStateException` in domain entities is internal invariant
signalling and is caught/mapped by their services; configuration-time validation in
`SecurityConfiguration` is framework/internal only. No business exception uses
`@ResponseStatus`, and there are no bypassing controller-advice handlers.

## GraphQL BFF foundation

REST remains the primary supported API. GraphQL is available alongside it at `POST /graphql`
as a read-only frontend BFF boundary, not as a global REST replacement. It currently exposes
the platform `countries` and `specialities` catalogue listing flows. Like their REST endpoints,
it requires a JWT with `ROLE_PLATFORM_ADMIN`; the standard Angular authentication interceptor
sends that Bearer token to `/graphql` too. Security can reject malformed, absent, or unauthorized
Bearer credentials before GraphQL execution, in which case the normal safe 401/403 transport
response applies. Angular maps those responses into the same safe user-error model.

Schemas are organized by domain under `src/main/resources/graphql` (`countries.graphqls`,
`specialities.graphqls`). Queries use plural collection names and singular item lookups;
mutations are intentionally absent in Phase 7. Collection queries take the reusable
`CataloguePageInput` (page, size, sort, `SortDirection`) and domain-specific typed filters,
not REST query-string conventions. All catalogue queries accept an optional `locale` BCP 47
argument. `en`, `pt`, and `nl`, including valid regional variants, are supported; omitted locale
means English. The resolver parses only BFF input, then delegates to the existing application
service. It never owns business rules, authorization decisions, transactions, or repositories.

`ApplicationGraphQlExceptionResolver` is the single execution and pre-execution error
translator. Safe errors always use `errors[].extensions.code` and
`errors[].extensions.correlationId`; friendly messages are localized from the request locale.
Only opt-in safe metadata is exposed. Tokens, request bodies, stack traces, SQL, exception
messages, and authorization internals are never returned or logged. The service/error layer is
authoritative for pagination, sort, locale, authentication, and authorization codes.

Example request (use a platform-admin bearer token):

```graphql
query Countries($page: CataloguePageInput!, $locale: String) {
  countries(page: $page, locale: $locale) {
    content {
      id
      code
      name
      displayName
      continent
    }
    page
    size
    totalElements
    totalPages
  }
}
```

Variables: `{"page":{"page":0,"size":10,"sort":"name","direction":"ASC"},"locale":"pt-PT"}`.
The equivalent speciality read supports `filter: { name, procedure }`.

New GraphQL services should begin as a query and follow the same sequence: define a
small schema type, add a resolver that delegates to an existing service, protect the
endpoint in `SecurityConfiguration`, and add unit plus authenticated integration tests.
Mutations should only be introduced after their REST/service validations and transactional
behaviour are already understood.

Angular components never make GraphQL requests directly. `GraphQlClientService` owns transport
and safe error mapping; typed domain services own exact operations and variables. The migrated
read flows are country catalogue listing and speciality catalogue listing. Country creation,
editing, deletion, and continent lookup remain REST; speciality create/update/delete and filter
autocomplete remain REST; all other frontend flows intentionally remain REST in Phase 7. No
GraphQL mutations are introduced until their REST validation, authorization, and transactions are
explicitly approved and covered.
