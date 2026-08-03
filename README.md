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

## Endpoints

```text
GET /api/administrative-divisions
GET /api/countries
GET /api/addresses
GET /api/addresses/postal-code/{postalCode}?countryCode=PT
GET /api/patients
GET /api/patients/{id}
POST /api/patients
PUT /api/patients/{id}
GET /api/specialities
POST /api/specialities
PUT /api/specialities/{id}
```

Countries use ISO 3166-1 alpha-2 codes. Patients have a stable global UUID and
carry a passport or national identity card with its issuing country. Addresses
reference their country of residence and an optional country-scoped
administrative division.

Example:

```bash
curl http://localhost:8080/api/patients
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

To replace a patient's speciality assignments, send their IDs in
`specialityIds` to `PUT /api/patients/{id}`. Send an empty array
(`"specialityIds": []`) to remove every assignment from that patient; this
does not delete the speciality records. Catalog removal is logical so existing
history remains valid.

The H2 console is available at `http://localhost:8080/h2-console` with JDBC URL
`jdbc:h2:mem:sisdent`, username `sa`, and an empty password.

## Test

```bash
mvn test
```

## Deployment

The Render and local pre-production deployment flows are documented in
[`docs/PIPELINE.md`](docs/PIPELINE.md). The Ubuntu host bootstrap and operating
instructions are in [`docs/PREPROD.md`](docs/PREPROD.md).
