# GraphQL REST retirement plan

Status: active as of 2026-08-21. The target is GraphQL for all application
business workflows. REST remains only for authentication/bootstrap and
operational infrastructure that cannot be a GraphQL operation.

## Delivery dashboard

Legend: **Done** means the REST controller/routes and in-repository Angular
consumers are removed; **In progress** means implementation has started;
**Planned** means no production migration has started.

| Wave | Status | Delivered / next release gate |
| --- | --- | --- |
| 1 — catalogue | **In progress** | Countries are **Done**. Specialities, addresses, administrative divisions and catalogue translations are pending. |
| 2 — identity and organization | **Planned** | Account settings, account management, organizations, memberships, clinic units and practitioners. |
| 3 — patients | **Planned** | Patient reads, creation, deactivation, intake, linking and autocomplete. |
| 4 — scheduling | **Planned** | Appointment reads, availability, transitions and performed procedures. |
| 5 — clinical | **In progress** | GraphQL-only implementation is complete; the release gate remains blocked by unrelated full-suite failures. |

Update this table in the same change that moves an operation to GraphQL or
removes a REST mapping; it is the single progress view for this programme.

## Removal rule

For each REST operation: define the GraphQL contract and typed Angular service;
move every in-repository consumer; add GraphQL success, validation, authorization,
tenant-isolation (where applicable), conflict and boundary tests; observe external
traffic; then remove the MVC method, OpenAPI entry, proxy rule and REST tests in
one release. Do not remove services, authorization, transactions, audit logic or
repositories: GraphQL adapters delegate to those boundaries.

## Completed

| Area | GraphQL replacement | REST result |
| --- | --- | --- |
| Country catalogue | `countries`, `continents`, `createCountry`, `updateCountry`, `deleteCountry` | `CountryController` removed |
| Speciality list/create/update | `specialities`, `createSpeciality`, `updateSpeciality` | Removed 2026-08-21 |
| Patient update | `updatePatient` | Removed |
| Clinical workflow | clinical encounter and odontogram queries/mutations | `ClinicalRecordController` removed |

## Execution waves

| Wave | Controllers and workflows | Required GraphQL work | Exit condition |
| --- | --- | --- | --- |
| 1 — catalogue completion | Administrative divisions, addresses and catalogue translations | Catalogue queries, autocomplete queries and destructive mutations with explicit conflict contract | No Angular calls to these REST routes; platform-admin authorization and validation coverage |
| 2 — identity and organization | `AccountSettingsController`, `AccountManagementController`, `OrganizationController`, `PractitionerController` | Current-account/settings, account lifecycle, organizations, clinic units, memberships and practitioners | Membership/platform scope, self-service restriction, lifecycle and optimistic-lock coverage |
| 3 — patient workflow | `OrganizationPatientController` remaining list/create/deactivate/intake/link/filter operations | Scoped patient query and create/deactivate/intake/link mutations | Tenant isolation, duplicate identity, clinic scope and rollback coverage |
| 4 — scheduling | `AppointmentController` including performed procedures | Paginated appointment queries and state-transition mutations | Role matrix, time/conflict boundaries, idempotency and transaction rollback coverage |
| 5 — clinical | `ClinicalRecordController` including encounters, finalization, amendments and odontogram | Clinical queries and explicit versioned mutations | Complete: controller removed with GraphQL lifecycle, conflict and scope tests |

## Frontend migration worklist

| Priority | Angular consumers to change | GraphQL action | REST removal enabled |
| --- | --- | --- | --- |
| **Done** | `features/countries/countries.component.ts`, `features/catalogue-endpoints.spec.ts` | `continents` query and `deleteCountry` mutation use typed GraphQL services | `CountryController` removed |
| **Done** | `features/specialities/specialities.component.ts` | `specialityFilterOptions` query and `deactivateSpeciality` mutation; no REST fallback | `SpecialityController` removed |
| **Complete** | `features/addresses/addresses.component.ts`, `features/administrative-divisions/administrative-divisions.component.ts`, `features/patients/patient-api.service.ts` | Typed catalogue GraphQL services for address, division and autocomplete data | REST surface retired |
| **Complete** | `core/catalog-translation-api.service.ts`, `features/catalog-translations/catalog-translations.component.ts` | Typed GraphQL translation read/replace service | REST surface retired |
| **Planned** | `core/account-api.service.ts`, `core/account-settings-api.service.ts` | Replace account, settings, organization and membership HTTP methods with typed GraphQL services | Account and organization controllers |
| 3 | `features/patients/patient-api.service.ts`, `features/patients/patients.component.ts` | Complete scoped patient queries and create/deactivate/intake/link mutations | `OrganizationPatientController` |
| 4 | `features/home/home.component.ts`, `features/appointments/appointments.component.ts` | Add appointment query, availability and transition mutations; consume only typed GraphQL service | `AppointmentController` |
| **Done** | `features/clinical/clinical-workspace.component.ts` | Typed clinical GraphQL query/mutation service | `ClinicalRecordController` removed |

### Wave 2: account settings checkpoint

`account-settings.component.html` consumes the API through
`core/account-settings-api.service.ts`; the component itself does not contain
HTTP calls. Its migration requires GraphQL operations for current settings,
profile, language and password changes. Avatar upload/download/removal needs a
separate approved GraphQL multipart-upload contract before its REST route can
be removed; it is not silently converted to JSON.

Tests that currently assert `/api/**` calls must move with their production
consumer to assert the GraphQL operation name, variables, safe error mapping and
the same authorization/tenant boundary. `auth.service.ts`, `csrf.service.ts` and
their tests are excluded because their HTTP endpoints are permanent infrastructure.

## HTTP endpoints intentionally retained

`POST /api/auth/login`, `GET /api/session`, `GET /api/csrf`,
`GET /actuator/health` and development-only H2 support remain HTTP. They are
authentication/bootstrap or infrastructure, not business REST resources.
OpenAPI remains only until the final business REST route is removed.

## Governance and verification

Each wave needs a named owner, GraphQL schema review, consumer inventory
(Angular, E2E, gateway and external clients), production traffic evidence and
release approval. Before merging backend changes, run focused tests, the complete
Maven test suite, and `quality-gate.verify_quality` (with Sonar when
`SONAR_TOKEN` is available). Coverage thresholds remain at least 80% instruction
and 60% branch coverage.
