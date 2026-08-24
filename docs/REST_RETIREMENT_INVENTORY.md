# REST retirement inventory

Status date: 2026-08-21. This is the release gate for REST retirement. The inventory was produced from
Spring MVC mappings, Angular source and E2E tests, deployment scripts, and
repository documentation. There is no telemetry or API-consumer registry in
this repository, so every endpoint without a proven in-repository-only consumer
is treated as potentially externally consumed and is retained.

`Deprecated` means it is still supported during the deprecation window; it does
not mean a route was removed. A route can be removed only after its consumers
are migrated, its GraphQL parity tests are green, and release approval is recorded.

## Decisions and deprecation window

The speciality list and write routes were removed on 2026-08-21 after the
Angular patient catalogue consumer moved to GraphQL and the replacement
contracts were tested. `GET /api/specialities/filter-options` and
`DELETE /api/specialities/{id}` remain until their GraphQL replacements exist.

| REST operation | GraphQL replacement | Angular consumer | Status |
| --- | --- | --- | --- |
| Country list | `countries(page, locale)` | none; country list uses GraphQL | Retired; GraphQL-only |
| Country create | `createCountry(input, locale)` | none; country write uses GraphQL | Retired; GraphQL-only |
| Country update | `updateCountry(id, input, locale)` | none; country write uses GraphQL | Retired; GraphQL-only |
| `GET /api/specialities` | `specialities(page, filter, locale)` | speciality list and patient picker | Removed 2026-08-21 |
| `POST /api/specialities` | `createSpeciality(input, locale)` | speciality write | Removed 2026-08-21 |
| `PUT /api/specialities/{id}` | `updateSpeciality(id, input, locale)` | speciality write | Removed 2026-08-21 |
| Patient workflow | `patients`, `patientFilterOptions`, `createPatient`, `updatePatient`, `deactivatePatient`, `exactPatientMatch`, `linkPatient` | Patient, appointment and clinical screens | Retired; GraphQL-only |

The OpenAPI `deprecated: true` flags are covered by
`OpenApiDeprecationIntegrationTests`. No deprecation response (for example 410)
is returned yet because these routes have not been approved for removal.

## Application REST mapping inventory

| Domain and mappings | Repository consumers, tests, and documentation | GraphQL status | Decision |
| --- | --- | --- | --- |
| Auth: `POST /api/auth/login` | `AuthService`, auth tests and all Playwright setup; security configuration | No replacement by design | Retained operational HTTP endpoint; it issues the JWT used by GraphQL. |
| Session: `GET /api/session` | `AuthService`, appointment/accounts/clinical E2E setup | No replacement by design | Retained authentication bootstrap endpoint. |
| Countries | `countries`, `continents`, `createCountry`, `updateCountry`, `deleteCountry` | `CountriesComponent` and catalogue tests | REST removed; GraphQL-only. |
| Specialities | `specialities`, `specialityFilterOptions`, `createSpeciality`, `updateSpeciality`, `deactivateSpeciality` | Catalogue E2E/tests | REST removed; GraphQL-only. |
| Addresses | `addresses`, `addressesByPostalCode`, `addressPostalCodeSuggestions`, `createAddress`, `updateAddress`, `deleteAddress` | Address screen and patient form/service; catalogue tests; README | Retired; GraphQL-only |
| Administrative divisions | `administrativeDivisions`, `createAdministrativeDivision`, `updateAdministrativeDivision`, `deleteAdministrativeDivision` | Division screen/tests | Retired; GraphQL-only |
| Catalogue translations | `catalogTranslations`, `replaceCatalogTranslations` | `CatalogTranslationApiService` and `CatalogTranslationsComponent` | Retired; GraphQL-only |
| Account settings: current/profile/preferred-language/password/avatar | `currentAccountSettings`, `updateOwnProfile`, `updateOwnPreferredLanguage`, `changeOwnPassword`, `uploadOwnAvatar`, `removeOwnAvatar`, `ownAvatar` | Account settings screen and language selector | Retired; GraphQL-only |
| Platform accounts: `GET`, `POST /api/platform/accounts`; `GET /api/platform/accounts/{accountId}`; `PATCH .../lifecycle`; `PATCH .../platform-administrator` | Account screen/service, authorization E2E, identity tests | No replacement | Retained. |
| Current account and organization accounts: `GET /api/account`; `GET /api/organizations/{organizationId}/accounts`; `GET .../accounts/{accountId}` | Account screen/service and integration tests | No replacement | Retained. |
| Platform organizations: `POST`, `GET /api/platform/organizations` | Account screen plus E2E setup/authorization tests | No replacement | Retained. |
| Clinic units: `POST`, `GET /api/organizations/{organizationId}/clinic-units` | Clinical and appointment screens, account dialog, E2E setup; organization workspace uses GraphQL | `clinicUnits` and `createClinicUnit` exist, but REST has active consumers | Retained until every caller is migrated and external use is known. |
| Memberships: `POST /api/organizations/{organizationId}/memberships`; `POST .../account-memberships`; `DELETE .../memberships/{membershipId}`; `POST .../memberships/{membershipId}/revoke`; `PATCH .../memberships/{membershipId}` | Account service/screen, authorization E2E, identity tests | No replacement | Retained. |
| Patients | Patient, appointment, clinical screens and tenant-isolation tests | `patients`, `patientFilterOptions`, `createPatient`, `updatePatient`, `deactivatePatient`, `exactPatientMatch`, `linkPatient` | Retired; GraphQL-only. |
| Practitioners: `GET`, `POST /api/organizations/{organizationId}/practitioners`; `PUT`, `DELETE .../{id}` | Appointment screen, account service/tests, scheduling E2E; workspace uses GraphQL for list/create/update | Query/create/update exist, but REST has active consumers; delete is not replaced | Retained. |
| Appointments and performed procedures | `appointments`, `appointment`, `performedProcedures`, `createAppointment`, `rescheduleAppointment`, `transitionAppointment`, `createPerformedProcedure`, `voidPerformedProcedure` | Home and appointment screens, scheduling/role E2E and integration tests | Retired; GraphQL-only. |
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

There are no REST controller mappings for file upload/download, refresh-token,
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
