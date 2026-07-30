# Sisdent project guide

## Purpose

Sisdent is an early dental-management platform. The implemented MVP covers
patients, international addresses, countries, administrative divisions,
specialities, dental procedures, global email accounts, organizations, clinic
units, scoped memberships, and JWT authentication.

Scheduling, practitioners, clinical records, odontograms, treatment plans,
billing, and patient portal access are future work.

## Current behavior

- Patient records have a stable global UUID.
- Patient documents are passports or national identity cards and include the
  issuing country.
- Tax ID is optional.
- Countries use ISO 3166-1 alpha-2 codes.
- Administrative divisions are country-scoped and may represent a state,
  province, district, region, or another local type.
- Address city and street are required; district, postal code, and
  administrative division are optional.
- Equal postal codes do not cause address reuse.
- Specialities own nested dental procedures.
- Removing catalog records deactivates them instead of deleting history.
- Core entities carry author/timestamp audit metadata and optimistic versions.
- Collection filtering, sorting, and pagination execute in the database.

## Main endpoints

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/api/administrative-divisions` | List administrative divisions |
| `POST/PUT/DELETE` | `/api/administrative-divisions/{id}` | Maintain administrative divisions |
| `GET` | `/api/countries` | List countries |
| `GET` | `/api/countries/continents` | List supported continent values |
| `GET` | `/api/addresses` | List addresses |
| `GET` | `/api/addresses/postal-code/{postalCode}?countryCode=PT` | Country-scoped postal lookup |
| `GET` | `/api/organizations/{organizationId}/patients` | Search linked patients in the selected scope |
| `POST` | `/api/organizations/{organizationId}/patient-intake/exact-match` | Return only whether an exact possible match exists |
| `POST` | `/api/organizations/{organizationId}/patient-links` | Create an explicit audited patient link |
| `POST` | `/api/platform/organizations` | Platform administrator creates an organization |
| `POST` | `/api/organizations/{organizationId}/clinic-units` | Organization administrator creates a unit |
| `POST/DELETE` | `/api/organizations/{organizationId}/memberships` | Add or revoke scoped membership |
| `GET` | `/api/session` | Current account and memberships |
| `GET/POST` | `/api/specialities` | List or create specialities |
| `PUT/DELETE` | `/api/specialities/{id}` | Replace or deactivate a speciality |
| `POST` | `/api/auth/login` | Authenticate by email/password; legacy identification is transitional |
| `PATCH` | `/api/users/me/password` | Change the current password |

All collection endpoints accept `page`, `size`, `sort`, and `direction`.
Resource-specific filters are documented in OpenAPI. `/api/states` is retained
as a compatibility alias only.

Dental procedures are nested under speciality requests and responses. There is
no standalone procedure endpoint.

## Patient request rules

A create or update request requires:

- name, birth date, active status, and gender;
- document type (`PASSPORT` or `NATIONAL_ID_CARD`);
- document number and document issuer country code;
- nationality country code;
- address street, city, and country code;
- `specialityIds`, which may be an empty array.

Document numbers are normalized before storage. Their uniqueness is scoped by
document type and issuer country. Postal code format is country-specific and is
therefore stored as text without an eight-digit rule.

An example address fragment:

```json
{
  "street": "Rua do Ouro 10",
  "district": "Lisboa",
  "city": "Lisboa",
  "postalCode": "1100-061",
  "administrativeDivision": {
    "name": "Lisboa",
    "code": "11",
    "type": "DISTRICT"
  },
  "countryCode": "PT"
}
```

The entire patient write runs in one transaction. Database uniqueness and
optimistic-lock conflicts return HTTP `409 Conflict`.

## Database and seed data

Flyway applies immutable migrations from
`src/main/resources/db/migration` before Hibernate validates the schema.
`InitialDataLoader` then synchronizes demonstration reference and patient data
from `src/main/resources/data/initial-data.json`.

Local development uses:

```text
jdbc:h2:mem:sisdent
```

This database disappears when the process ends. Pre-production uses file-backed
H2. Real production use requires durable managed persistence, backups, restore
tests, encryption, and an approved RGPD operating model.

## Local setup

Requirements:

- JDK 25;
- Node.js compatible with `frontend/package.json`;
- Git;
- Docker only for container tests.

Start the API:

```bash
./mvnw spring-boot:run
```

Start the web client:

```bash
cd frontend
npm ci
npm start
```

The local training administrator is `admin@sisdent.local` with password
`admin`. Transitional `NATIONAL_ID / ADMIN / admin` login remains available.
These credentials are deliberately weak and must never be used in a deployed
environment.

Login example:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sisdent.local","password":"admin"}'
```

Use the returned token as `Authorization: Bearer <accessToken>`.

## Verification

```bash
./mvnw test
./mvnw verify

cd frontend
npm test -- --watch=false
npm run build
```

The backend suite includes service, security, HTTP integration, seed-data, and
Flyway upgrade tests. Frontend browser journeys are in `frontend/e2e`.

## Engineering boundaries

- Source code, API names, migrations, and engineering documentation use English.
- Existing Flyway migrations are never edited after release; add a new version.
- API DTOs remain separate from JPA entities.
- New reference data must be country-aware.
- Do not use tax ID, postal code, or database sequence IDs as global person
  identity.
- Do not physically delete catalog or future clinical-history records.

See `docs/ARCHITECTURE.md` for the model, compatibility decisions, security
boundary, and planned evolution. See `docs/PHASE_2_IMPLEMENTATION.md` for the
authorization matrix and legacy migration strategy.
