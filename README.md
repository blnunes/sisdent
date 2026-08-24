# Sisdent

REST API built with Java 25, Spring Boot 4 and an in-memory H2 database.

The `frontend` directory contains an Angular 22 and Angular Material 22
interface for authentication and administrative user management.

## Run

```bash
mvn spring-boot:run
```

Static development data is loaded from
`src/main/resources/data/initial-data.json` whenever the application starts with
an empty database.

Flyway applies versioned migrations from `src/main/resources/db/migration`
before Hibernate validates the schema. Applied migrations are immutable;
subsequent schema changes must use a new version.

Start the web interface in a second terminal:

```bash
cd frontend
npm ci
npm start
```

Open `http://localhost:4200` and use `NATIONAL_ID / ADMIN / admin`. See
[`frontend/README.md`](frontend/README.md) for frontend details.

## API surface

GraphQL (`POST /graphql`) is the primary frontend API. Retained REST endpoints
serve explicitly documented operational and not-yet-migrated workflows. The
complete endpoint inventory, GraphQL replacements, and deprecation window are
in [`docs/REST_RETIREMENT_INVENTORY.md`](docs/REST_RETIREMENT_INVENTORY.md).

The following is an example of a retained REST path; scheduling is GraphQL-only:

```text
GET /api/organizations/{organizationId}/clinical/encounters
```

Addresses are GraphQL-only through `addresses`, `addressesByPostalCode`,
`addressPostalCodeSuggestions`, `createAddress`, `updateAddress`, and `deleteAddress`.
Catalogue translations are GraphQL-only through `catalogTranslations` and
`replaceCatalogTranslations`.

Country list, create, and update are GraphQL-only through `countries(page, locale)`,
`createCountry(input, locale)`, and `updateCountry(id, input, locale)`. The retained
country REST endpoints are continent lookup and delete. Country and speciality responses keep `name` as the canonical persisted value and
also expose a localized `displayName`. Send `Accept-Language: en`, `nl`, or
`pt-PT`; unsupported languages fall back to English and custom speciality names
fall back to their canonical value.

Countries use ISO 3166-1 alpha-2 codes. Patients have a stable global UUID and
carry a passport or national identity card with its issuing country. Addresses
reference their country of residence and an optional country-scoped
administrative division.

Example:

```bash
curl -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
  -d '{"query":"{ patients(organizationId: \\"{organizationId}\\") { content { globalId name } } }"}' \
  'http://localhost:8080/graphql'
```

Dental procedures are nested resources owned by a speciality. They are
returned, created, updated, and deactivated through the speciality endpoints;
there is no standalone `/api/procedures` endpoint.

## Clinical workspace

Clinical records are always addressed below the active organization and clinic
scope: `/api/organizations/{organizationId}/clinical`. A clinical reader can
read encounters and odontogram history; a clinical author can create and edit
only their draft encounters; a clinical manager (and an organization
administrator) can finalize encounters and create amendments. Final encounters
are never edited in place. Odontogram findings are likewise preserved: a
correction first voids the finding with a reason and version, then creates a
replacement that references the voided finding.

The workspace loads clinic units and patients only through the scoped
organization endpoints and surfaces forbidden scope, stale-version, finalized
record, and unavailable-patient errors to the user.

To replace a patient's speciality assignments, send their IDs in the GraphQL
`updatePatient` mutation input. Send an empty array
(`"specialityIds": []`) to remove every assignment from that patient; this
does not delete the speciality records. Catalog removal is logical so existing
history remains valid.

The H2 console is available at `http://localhost:8080/h2-console` with JDBC URL
`jdbc:h2:mem:sisdent`, username `sa`, and an empty password.

## Test

```bash
./mvnw test
cd frontend && npm ci
npm test -- --watch=false
npm run check:i18n
npm run build
npm run test:e2e
```

Backend code follows the existing Spring service/controller boundaries; keep
authorization checks in the server-side scope services. Angular components must
present translated, user-safe error messages rather than server error details.

## Deployment

The Render and local pre-production deployment flows are documented in
[`docs/PIPELINE.md`](docs/PIPELINE.md). The Ubuntu host bootstrap and operating
instructions are in [`docs/PREPROD.md`](docs/PREPROD.md).
