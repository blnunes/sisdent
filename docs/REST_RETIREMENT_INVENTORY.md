# REST retirement inventory

Status date: 2026-08-18. This is the release gate for REST retirement, not a
claim that every HTTP endpoint can be removed. The inventory was produced from
Spring MVC mappings, Angular source and E2E tests, deployment scripts, and
repository documentation. There is no telemetry or API-consumer registry in
this repository, so every endpoint without a proven in-repository-only consumer
is treated as potentially externally consumed and is retained.

`Deprecated` means it is still supported during the deprecation window; it does
not mean a route was removed. No controller, DTO, service, authorization rule,
transaction, repository, audit rule, proxy rule, or compatibility handler is
removed in this phase.

## Decisions and deprecation window

The following individual REST operations are deprecated in generated OpenAPI on
2026-08-18 and remain available through **2027-02-18**. Removal requires a
release approval after consumer telemetry has shown no traffic throughout the
window and the listed GraphQL contract tests remain green.

| REST operation | GraphQL replacement | Angular consumer | Status |
| --- | --- | --- | --- |
| Country list | `countries(page, locale)` | none; country list uses GraphQL | Retired; GraphQL-only |
| Country create | `createCountry(input, locale)` | none; country write uses GraphQL | Retired; GraphQL-only |
| Country update | `updateCountry(id, input, locale)` | none; country write uses GraphQL | Retired; GraphQL-only |
| `GET /api/specialities` | `specialities(page, filter, locale)` | none; speciality list uses GraphQL | Deprecated |
| `POST /api/specialities` | `createSpeciality(input, locale)` | none; speciality write uses GraphQL | Deprecated |
| `PUT /api/specialities/{id}` | `updateSpeciality(id, input, locale)` | none; speciality write uses GraphQL | Deprecated |
| Patient update | `updatePatient(organizationId, clinicUnitId, patientId, input)` | none; patient edit uses GraphQL | Retired; GraphQL-only |

The OpenAPI `deprecated: true` flags are covered by
`OpenApiDeprecationIntegrationTests`. No deprecation response (for example 410)
is returned yet because these routes have not been approved for removal.

## Application REST mapping inventory

| Domain and mappings | Repository consumers, tests, and documentation | GraphQL status | Decision |
| --- | --- | --- | --- |
| Auth: `POST /api/auth/login` | `AuthService`, auth tests and all Playwright setup; security configuration | No replacement by design | Retained operational HTTP endpoint; it issues the JWT used by GraphQL. |
| Session: `GET /api/session` | `AuthService`, appointment/accounts/clinical E2E setup | No replacement by design | Retained authentication bootstrap endpoint. |
| Countries: `GET /api/countries/continents`; `DELETE /api/countries/{id}` | `CountriesComponent` still calls `continents`; catalogue tests; README | List/create/update are GraphQL-only; enum lookup and delete remain REST. | Retained. |
| Specialities: `GET`, `POST`, `PUT /api/specialities`; `GET /api/specialities/filter-options`; `DELETE /api/specialities/{id}` | Patient lookup/autocomplete, catalogue E2E/tests, README | Query/create/update replaced; filter autocomplete and delete are not | First three operations deprecated; remaining operations retained. |
| Addresses: `GET`, `POST /api/addresses`; `GET /api/addresses/postal-code/{postalCode}`; `GET /api/addresses/postal-code-suggestions`; `PUT`, `DELETE /api/addresses/{id}` | Address screen and patient form/service; catalogue tests; README | No replacement | Retained; external status unverified. |
| Administrative divisions: `GET`, `POST /api/administrative-divisions` and legacy alias `/api/states`; `PUT`, `DELETE /api/administrative-divisions/{id}` and `/api/states/{id}` | Division screen/tests; README | No replacement | Retained. `/api/states` is a compatibility alias with unknown consumers. |
| Catalogue translations: `GET /api/platform/catalog-translations`; `PUT /api/platform/catalog-translations/{type}/{id}` | `CatalogTranslationApiService` and its tests | No replacement | Retained. |
| Platform accounts: `GET`, `POST /api/platform/accounts`; `GET /api/platform/accounts/{accountId}`; `PATCH .../lifecycle`; `PATCH .../platform-administrator` | Account screen/service, authorization E2E, identity tests | No replacement | Retained. |
| Current account and organization accounts: `GET /api/account`; `GET /api/organizations/{organizationId}/accounts`; `GET .../accounts/{accountId}` | Account screen/service and integration tests | No replacement | Retained. |
| Platform organizations: `POST`, `GET /api/platform/organizations` | Account screen plus E2E setup/authorization tests | No replacement | Retained. |
| Clinic units: `POST`, `GET /api/organizations/{organizationId}/clinic-units` | Clinical and appointment screens, account dialog, E2E setup; organization workspace uses GraphQL | `clinicUnits` and `createClinicUnit` exist, but REST has active consumers | Retained until every caller is migrated and external use is known. |
| Memberships: `POST /api/organizations/{organizationId}/memberships`; `POST .../account-memberships`; `DELETE .../memberships/{membershipId}`; `POST .../memberships/{membershipId}/revoke`; `PATCH .../memberships/{membershipId}` | Account service/screen, authorization E2E, identity tests | No replacement | Retained. |
| Patients: `GET`, `POST /api/organizations/{organizationId}/patients`; `GET .../filter-options`; `PUT`, `DELETE .../{patientId}`; `POST .../patient-intake/exact-match`; `POST .../patient-links` | Patient, appointment, clinical screens and tenant-isolation E2E; README | Only update is replaced | `PUT` deprecated; every other operation retained. |
| Practitioners: `GET`, `POST /api/organizations/{organizationId}/practitioners`; `PUT`, `DELETE .../{id}` | Appointment screen, account service/tests, scheduling E2E; workspace uses GraphQL for list/create/update | Query/create/update exist, but REST has active consumers; delete is not replaced | Retained. |
| Appointments: `GET`, `POST /api/organizations/{organizationId}/appointments`; `GET .../{id}`; `PUT .../{id}/reschedule`; `POST .../{id}/cancel`, `/complete`, `/no-show`; `GET`, `POST .../{id}/performed-procedures`; `POST .../performed-procedures/{procedureId}/void` | Home and appointment screens, scheduling/role E2E, integration tests, PROJECT_GUIDE | No replacement | Retained. |
| Clinical: `GET`, `POST /api/organizations/{organizationId}/clinical/encounters`; `GET`, `PUT .../encounters/{id}`; `POST .../finalize`; `GET`, `POST .../amendments`; `GET .../odontogram/current`, `/history`; `POST .../odontogram/findings`; `POST .../odontogram/findings/{id}/void` | Clinical workspace, clinical E2E/integration tests, README/PROJECT_GUIDE | No replacement | Retained. Clinical data must not be mechanically migrated or removed. |

All application mappings above are generated by the controller classes in
`src/main/java/br/com/itbn/sisdent/controller`. Controller integration tests
under `src/test/java/br/com/itbn/sisdent` and frontend tests under
`frontend/src` are consumers too; they are intentionally kept for retained
contracts. The repository documentation references are `README.md`,
`docs/PROJECT_GUIDE.md`, `docs/ARCHITECTURE.md`, and `frontend/README.md`.

## Retained HTTP and operational surface

| Endpoint/surface | Reason it remains |
| --- | --- |
| `POST /graphql` | Primary authenticated frontend BFF. It continues to use the JWT issued by REST login and service-layer authorization. |
| `POST /api/auth/login`, `GET /api/session` | Authentication/bootstrap protocol; no GraphQL login mutation is approved. |
| `GET /actuator/health` | Deployment and container health check (`render.yaml`, `run-dev.sh`, `compose.preprod.yml`, pre-production scripts). |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Supported REST contract discovery during the retirement window. |
| `/h2-console/**` | Local-development support only; it is controlled by the existing application/security configuration. |
| Static SPA and `/i18n/**` | Browser application delivery, not data APIs. |

There are no controller mappings for file upload/download, refresh-token,
webhook, or third-party callback routes. Adding one requires an explicit entry
in this inventory before it can be considered for GraphQL migration.

## GraphQL parity evidence

GraphQL is an adapter over existing services. The deprecated operations call the
same `CountryService`, `SpecialityService`, and `OrganizationPatientService` as
REST. Consequently authorization, tenant isolation, validation, transactions,
conflict rules, audit behaviour, pagination/filtering, and localization remain
in the service layer. `ApplicationGraphQlExceptionResolver` maps expected
errors to localized safe messages with stable codes and correlation IDs; it does
not expose request data, credentials, tokens, stack traces, or causes.

| Replacement | Parity exercised by |
| --- | --- |
| Catalogue queries/mutations | `GraphQlIntegrationTests`, resolver/service tests, Angular catalogue GraphQL service tests |
| Patient update mutation | `GraphQlIntegrationTests`, patient mutation GraphQL service test, tenant-isolation integration/E2E coverage of the underlying service contract |
| Error, locale, pagination and safe correlation handling | `GraphQlIntegrationTests`, `ApplicationGraphQlExceptionResolverTest`, catalogue GraphQL tests |

Before removing a deprecated route, add a removal test that asserts the route's
documented 404/410 outcome and an equivalent GraphQL success, validation,
authorization, isolation, conflict, pagination/filter, and boundary test. Also
remove its Angular service/test references, gateway/proxy rule if any, and this
inventory entry only after production consumer telemetry and release approval.

## Observability and security checklist

`RequestCorrelationFilter` normalizes all `/api/**` metrics/logs as REST and
`/graphql` as GraphQL; no business identifiers, documents, tokens, credentials,
or bodies are labels. Existing CORS/proxy configuration continues to proxy both
`/api` and `/graphql`; do not remove the `/api` proxy while any retained route
exists. Health monitoring remains on `/actuator/health`. No dashboard, alert,
gateway, or third-party integration configuration is stored in this repository,
so its owners must confirm those consumers before any future removal approval.
