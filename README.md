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
GET /api/states
GET /api/countries
GET /api/addresses
GET /api/addresses/postal-code/{postalCode}
GET /api/patients
GET /api/patients/{id}
POST /api/patients
PUT /api/patients/{id}
GET /api/specialities
POST /api/specialities
PUT /api/specialities/{id}
```

Countries use ISO 3166-1 alpha-2 codes. Patients carry nationality plus a
globally unique passport or national identity number, and addresses reference
their country of residence.

Example:

```bash
curl http://localhost:8080/api/patients
```

Procedures are nested resources owned by a speciality. They are returned,
created, updated, and removed through the speciality endpoints; there is no
standalone `/api/procedures` endpoint.

To replace a patient's speciality assignments, send their IDs in
`specialityIds` to `PUT /api/patients/{id}`. Send an empty array
(`"specialityIds": []`) to remove every assignment from that patient; this
does not delete the speciality records.

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
