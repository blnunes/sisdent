# Frontend feature boundaries refactor plan

## Objective

Remove domain behaviour from `shared` so that patient and catalogue modules own
their filters, API orchestration, permissions, field schemas, dialogs, and
record mapping. Keep `shared` limited to reusable UI primitives with explicit
inputs and outputs.

## Architectural rules

- A component in `shared` must not import a feature component or use a
  domain-specific type, endpoint, permission, translation key, or field name.
- Features own their route configuration and workflow decisions.
- Shared UI communicates only through typed inputs, outputs, and neutral view
  models; it never reads `ActivatedRoute` or calls a domain API directly.
- A new resource must be added by composing shared primitives in its feature,
  without modifying an existing unrelated feature or shared component.

## Target structure

```text
frontend/src/app/
  features/
    patients/
      patients.component.ts
      patient-list/
      patient-filters/
      patient-form-dialog/
      patient-details-dialog/
      patient-api.service.ts
      patient.models.ts
    specialities/
      specialities.component.ts
      speciality-list/
      speciality-form-dialog/
    addresses/
    countries/
    administrative-divisions/
  shared/
    layout/
    preferences/
    data-table/
      data-table.component.ts
      data-table.models.ts
    filters/
      filter-bar.component.ts
      filter.models.ts
    dialogs/
      form-dialog-shell.component.ts
```

## Delivery phases

### 1. Establish neutral shared contracts

Create typed, presentational primitives for the table, pagination, loading and
error states, and filter controls. They receive columns, rows, filter values,
and state from a feature and emit user actions. Do not inject `HttpClient`,
`ActivatedRoute`, `AuthService`, or feature-specific dialogs into these
components.

Tests:

- render rows, empty, loading, and error states;
- emit pagination, sorting, filter, and row-action events;
- reject or safely render missing optional configuration.

### 2. Extract the patient workflow

Move all patient-specific fields, filters, request/response mapping, tenant
endpoint construction, membership reload, autocomplete lookups, and patient
dialogs to `features/patients`. `PatientsComponent` becomes the container that
coordinates the patient API and passes view models to shared primitives.

Tests:

- endpoint contains the active organization and optional clinic unit;
- every patient filter produces the expected request parameter;
- create, update, deactivate, and details actions use `globalId`;
- no patient import or `patients` branch remains under `shared`.

### 3. Extract catalogue workflows

Move speciality, address, country, and administrative-division schemas,
columns, endpoints, and form preparation into their matching feature packages.
Each feature composes the neutral table and filter primitives independently.

Tests:

- each route loads its matching feature container;
- each catalogue sends requests only to its own endpoint;
- country continent lookup and speciality procedures retain their current
  validation and error handling.

### 4. Simplify routes and remove the legacy abstraction

Keep `app.routes.ts` responsible only for paths, guards, and lazy feature
loading. Remove route data used as an implicit domain configuration channel and
delete `RecordListComponent`, `RecordFormDialog`, and their configuration
models after every consumer has migrated.

Tests:

- route-to-feature mapping;
- unknown route fallback;
- import-boundary check that forbids `shared` from importing `features`.

### 5. Verification and acceptance

Run `npm run check:i18n`, unit tests, production build, and the relevant
Playwright suites (`patients` plus catalogue coverage). Confirm that no
behavioural API request or visible translation regressed.

Acceptance criteria:

- `shared` contains no `patient`, `speciality`, `address`, `country`, or
  `administrativeDivision` domain branch;
- patient files are discoverable only under `features/patients`;
- adding a patient filter changes only the patient feature and its tests;
- code coverage does not decrease.
