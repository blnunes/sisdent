# GraphQL total migration phases

This document is the execution contract for retiring business REST APIs. There
is deliberately no Phase 0. Work always starts at Phase 1.

## Agent execution prompt

When the user says **`Execute phase N`**, do the following before changing
code:

1. Read this document, `ARCHITECTURE.md`, `GRAPHQL_MUTATION_BOUNDARIES.md`,
   `REST_RETIREMENT_INVENTORY.md`, and the relevant controller, GraphQL schema,
   Angular consumer and tests.
2. Implement every unchecked item in Phase N, including frontend migration.
   Do not merely edit the plan or report intended work.
3. Preserve the service layer as the authority for authorization, validation,
   transactions, audit and tenant isolation. GraphQL resolvers delegate to it.
4. For each removed REST operation, add or update focused tests for GraphQL
   success, validation/error, authorization, and relevant boundary or isolation
   paths; update Angular tests to assert GraphQL operation/variables rather than
   `/api/**` requests.
5. Remove the MVC mapping/controller only after every in-repository consumer has
   migrated. Update OpenAPI absence tests, README, architecture and retirement
   inventory in the same change.
6. Run the Angular suite, focused Maven tests and `quality-gate.verify_quality`.
   Run Sonar when `SONAR_TOKEN` is available. Do not declare a phase complete
   while the quality gate fails.
7. Update this document: change its phase status and mark only actually
   completed checklist items. Report what was delivered, test results, and the
   next phase number.

Permanent HTTP exceptions are only `POST /api/auth/login`, `GET /api/session`,
`GET /api/csrf`, `GET /actuator/health`, and development-only H2 support.

## Phase 1 — catalogue completion

Status: **Complete**

- [x] Remove country REST routes; provide `countries`, `continents`,
  `createCountry`, `updateCountry`, and `deleteCountry` through GraphQL.
- [x] Remove speciality REST routes; provide list/write/deactivate GraphQL
  operations.
- [x] Add administrative-division GraphQL query and CRUD mutations; migrate its
  Angular component and patient form lookup.
- [x] Add focused GraphQL integration tests for administrative-division CRUD and
  authorization; remove the remaining REST inventory/documentation references.
- [x] Add address page, postal-code lookup/suggestions and CRUD GraphQL
  operations; migrate `AddressesComponent` and `PatientApiService`; retire the
  REST address surface.
- [x] Add catalogue-translation query and replacement mutation; migrate
  `CatalogTranslationApiService` and `CatalogTranslationsComponent`; retire the
  REST translation surface.

Exit: no business catalogue route remains under `/api/**`.

## Phase 2 — account settings

Status: **Complete**

- [x] Add typed GraphQL query/mutations for current settings, profile, preferred
  language and password changes.
- [x] Migrate `core/account-settings-api.service.ts` and its tests; the
  `account-settings.component.html` remains a presentational consumer.
- [x] Define and approve a GraphQL multipart upload/download contract for avatar
  operations, then migrate avatar calls without exposing file bytes in logs.
- [x] Remove `AccountSettingsController` REST business mappings and tests.

Exit: account settings screen has no `/api/account/settings/**` business call.

## Phase 3 — accounts and organization administration

Status: **Planned**

- [ ] Migrate account management, platform organizations, clinic units,
  memberships and practitioners to typed GraphQL services.
- [ ] Cover platform/organization scope, lifecycle and optimistic-lock failures.
- [ ] Remove `AccountManagementController`, `OrganizationController` and
  `PractitionerController` mappings after consumers migrate.

## Phase 4 — patient workflow

Status: **Complete**

- [x] Add scoped patient query, create/deactivate/intake/link/filter GraphQL
  operations and migrate `PatientApiService`/patient components.
- [x] Cover tenant isolation, clinic scope, duplicate identity and rollback.
- [x] Remove `OrganizationPatientController`.

## Phase 5 — scheduling

Status: **Planned**

- [ ] Add appointment queries, availability, lifecycle and performed-procedure
  GraphQL operations; migrate home and appointments components.
- [ ] Cover role matrix, timing conflicts, idempotency and rollback.
- [ ] Remove `AppointmentController`.

## Phase 6 — clinical workflow

Status: **Planned**

- [ ] Add encounter, amendment, finalization and odontogram GraphQL contracts;
  migrate `clinical-workspace.component.ts` away from direct `HttpClient` calls.
- [ ] Cover clinical immutability, version conflicts, audit and tenant isolation.
- [ ] Remove `ClinicalRecordController`.

## Phase 7 — final REST retirement

Status: **Planned**

- [ ] Verify no business `/api/**` calls or controller mappings remain.
- [ ] Remove temporary OpenAPI/proxy compatibility material and update all
  retirement documentation.
- [ ] Preserve only the permanent HTTP exceptions listed above and run complete
  backend, frontend, E2E and quality-gate verification.
