# REST retirement inventory

Status date: 2026-08-24. Final retirement verification completed from
Spring MVC mappings, Angular source and E2E tests, deployment scripts, and
repository documentation. No business MVC mapping or production Angular
`/api/**` call remains.

All former business routes are retired. GraphQL parity coverage remains as the
replacement contract; route-absence tests preserve the retirement boundary.

## Final cleanup and verification

The final obsolete-consumer cleanup removed the unused Angular REST list
orchestration and empty endpoint placeholder shims. The remaining list
components use typed GraphQL services, and
`catalogue-endpoints.spec.ts` prevents feature production code from importing
`HttpClient` or referencing `/api/**`.

The final verification on 2026-08-24 passed the Maven quality gate with 87.30%
instruction coverage and 66.73% branch coverage, the Angular suite (106 tests),
and the Playwright suite (13 tests). SonarCloud Quality Gate passed with 80.30%
new-code coverage; reliability, security, maintainability, duplication, and
security-hotspot conditions also passed.

## Retirement decisions

The final route inventory verifies that all speciality, country, patient,
administrative, account, organization, scheduling, and clinical routes are
retired. Their GraphQL replacements are listed below.

| REST operation | GraphQL replacement | Angular consumer | Status |
| --- | --- | --- | --- |
| Country list | `countries(page, locale)` | none; country list uses GraphQL | Retired; GraphQL-only |
| Country create | `createCountry(input, locale)` | none; country write uses GraphQL | Retired; GraphQL-only |
| Country update | `updateCountry(id, input, locale)` | none; country write uses GraphQL | Retired; GraphQL-only |
| `GET /api/specialities` | `specialities(page, filter, locale)` | speciality list and patient picker | Removed 2026-08-21 |
| `POST /api/specialities` | `createSpeciality(input, locale)` | speciality write | Removed 2026-08-21 |
| `PUT /api/specialities/{id}` | `updateSpeciality(id, input, locale)` | speciality write | Removed 2026-08-21 |
| Patient workflow | `patients`, `patientFilterOptions`, `createPatient`, `updatePatient`, `deactivatePatient`, `exactPatientMatch`, `linkPatient` | Patient, appointment and clinical screens | Retired; GraphQL-only |

`RestRetirementIntegrationTests` verifies that temporary OpenAPI and Swagger
endpoints are absent. Focused REST-removal tests preserve 404 coverage where a
retired route is intentionally exercised.

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
| Platform accounts: `GET`, `POST /api/platform/accounts`; `GET /api/platform/accounts/{accountId}`; `PATCH .../lifecycle`; `PATCH .../platform-administrator` | Account screen/service, authorization E2E and GraphQL integration tests | `platformAccounts`, `platformAccount`, `createPlatformAccount`, `changeAccountLifecycle`, `changeAccountPlatformAdministrator` | Retired; GraphQL-only. |
| Current account and organization accounts: `GET /api/account`; `GET /api/organizations/{organizationId}/accounts`; `GET .../accounts/{accountId}` | Account screen/service and GraphQL integration tests | `currentAccount`, `organizationAccounts`, `organizationAccount` | Retired; GraphQL-only. |
| Platform organizations: `POST`, `GET /api/platform/organizations` | Account screen and authorization E2E | `platformOrganizations`, `createOrganization` | Retired; GraphQL-only. |
| Clinic units: `POST`, `GET /api/organizations/{organizationId}/clinic-units` | Clinical and appointment screens, account dialog and E2E setup | `clinicUnits`, `createClinicUnit` | Retired; GraphQL-only. |
| Memberships: `POST /api/organizations/{organizationId}/memberships`; `POST .../account-memberships`; `DELETE .../memberships/{membershipId}`; `POST .../memberships/{membershipId}/revoke`; `PATCH .../memberships/{membershipId}` | Account service/screen, authorization E2E and GraphQL integration tests | `createMembership`, `grantMembership`, `changeMembershipRole`, `revokeMembership` | Retired; GraphQL-only. |
| Patients | Patient, appointment, clinical screens and tenant-isolation tests | `patients`, `patientFilterOptions`, `createPatient`, `updatePatient`, `deactivatePatient`, `exactPatientMatch`, `linkPatient` | Retired; GraphQL-only. |
| Practitioners: `GET`, `POST /api/organizations/{organizationId}/practitioners`; `PUT`, `DELETE .../{id}` | Appointment screen, account service/tests and scheduling E2E | `practitioners`, `createPractitioner`, `updatePractitioner`, `deactivatePractitioner` | Retired; GraphQL-only. |
| Appointments and performed procedures | `appointments`, `appointment`, `appointmentAvailability`, `performedProcedures`, `createAppointment`, `rescheduleAppointment`, `transitionAppointment`, `createPerformedProcedure`, `voidPerformedProcedure` | Home and appointment screens, scheduling/role E2E and integration tests | Retired; GraphQL-only. |
| Clinical encounters and odontogram | `clinicalEncounters`, `clinicalEncounter`, `encounterAmendments`, `currentOdontogram`, `odontogramHistory`, `createClinicalEncounter`, `updateClinicalEncounter`, `finalizeClinicalEncounter`, `amendClinicalEncounter`, `createOdontogramFinding`, `voidOdontogramFinding` | Clinical workspace and GraphQL integration tests | Retired; GraphQL-only. |

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
| `/h2-console/**` | Local-development support only; it is controlled by the existing application/security configuration. |
| Static SPA and `/i18n/**` | Browser application delivery, not data APIs. |

There are no REST controller mappings for file upload/download, refresh-token,
webhook, or third-party callback routes. Adding one requires an explicit entry
in this inventory before it can be considered for GraphQL migration.

## GraphQL parity evidence

GraphQL is an adapter over existing services. The retired operations call the
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

Before removing a business route, add a removal test that asserts the route's
documented 404/410 outcome and an equivalent GraphQL success, validation,
authorization, isolation, conflict, pagination/filter, and boundary test. Also
remove its Angular service/test references, gateway/proxy rule if any, and this
inventory entry only after production consumer telemetry and release approval.

## Observability and security checklist

`RequestCorrelationFilter` normalizes all `/api/**` metrics/logs as REST and
`/graphql` as GraphQL; no business identifiers, documents, tokens, credentials,
or bodies are labels. Development proxies expose `/graphql` and only the three
retained `/api` routes. Health monitoring remains on `/actuator/health`. No dashboard, alert,
gateway, or third-party integration configuration is stored in this repository,
so its owners must confirm those consumers before any future removal approval.
