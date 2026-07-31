# Sisdent project guide

## Purpose

Sisdent is an early dental-management platform. The implemented MVP covers
patients, international addresses, countries, administrative divisions,
specialities, dental procedures, global email accounts, organizations, clinic
units, scoped memberships, verified-email enrollment, and JWT authentication.

Scoped clinical encounters and odontograms are available to explicitly granted
clinical memberships. Treatment plans, billing, and patient portal access are
future work.

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
| `POST` | `/api/account/email-enrollment` | Reserve and send verification for the current migrating account |
| `POST` | `/api/account/email-enrollment/resend` | Supersede and resend the current account's challenge |
| `POST` | `/api/auth/email-verification` | Consume an opaque token with a generic verification outcome |
| `GET/POST` | `/api/specialities` | List or create specialities |
| `PUT/DELETE` | `/api/specialities/{id}` | Replace or deactivate a speciality |
| `POST` | `/api/auth/login` | Authenticate by email/password; legacy identification is transitional |
| `GET/POST/PUT/DELETE` | `/api/organizations/{organizationId}/practitioners` | Scoped practitioner management (DELETE deactivates) |
| `GET/POST` | `/api/organizations/{organizationId}/appointments` | Bounded schedule list and appointment creation |
| `POST` | `/api/organizations/{organizationId}/appointments/{id}/cancel|complete|no-show` | Terminal lifecycle transitions |
| `POST` | `/api/organizations/{organizationId}/appointments/{id}/performed-procedures` | Record catalog procedures after completion |
| `GET/POST/PUT` | `/api/organizations/{organizationId}/clinical/encounters` | Scoped encounter list, drafts, and updates |
| `GET/POST` | `/api/organizations/{organizationId}/clinical/odontogram/*` | Read chart/history and record findings |
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

The same file provides a self-contained operational scenario for the Phase 1–4
roles. All demo profile passwords are `odonto2026@O`.

| Profile | Tenant scope | Demonstration data |
| --- | --- | --- |
| `platform.operations@sisdent.demo` | Platform | Platform administration; it does not have tenant clinical access. |
| `group.admin@sisdent.demo` | Northstar Dental Group and Southstart Dental Group | Organization administrator memberships in both groups; use this account to test a single administrator switching between two organizations. |
| `northstar.admin@sisdent.demo` | Northstar Dental Group | Northstar Central Clinic and Northstar Lakeside Clinic, each with 6 linked patients, 2 practitioners, and appointment history. |
| `northstar.manager@sisdent.demo`, `northstar.scheduler@sisdent.demo`, `northstar.readonly@sisdent.demo` | Northstar Central Clinic | Clinic-scoped operational profiles for the Central scenario. |
| `northstar.practitioners@sisdent.demo`, `northstar.viewer@sisdent.demo` | Northstar Lakeside Clinic | Clinic-scoped practitioner-management and appointment-reader profiles. |
| `harbor.admin@sisdent.demo`, `harbor.scheduler@sisdent.demo` | Harbor Dental Clinic / Harbor Riverside Unit | Harbor Riverside Unit and Harbor Midtown Clinic, each with 6 linked patients, 2 practitioners, and appointment history. |
| `southstart.admin@sisdent.demo`, `southstart.scheduler@sisdent.demo` | Southstart Dental Group / Southstart Downtown Clinic | Southstart Downtown Clinic and Southstart Seaside Clinic, each with 6 linked patients, 2 practitioners, and appointment history. |

Organizations and clinic units are explicit seed sections. The Northstar,
Harbor, and Southstart patient, practitioner, appointment, and procedure
records are deliberately separate by clinic, so tenant and clinic-isolation
flows can be exercised.

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

The pre-filled local demonstration account is `group.admin@sisdent.demo` with
password `odonto2026@O`. It has organization-administrator memberships in both
Northstar and Southstart, so it is the preferred account for testing tenant
switching. The legacy local training administrator remains
`admin@sisdent.local` with password `admin` for compatibility only; its legacy
`NATIONAL_ID / ADMIN / admin` data remains available only to migrated accounts
that require enrollment. These credentials are deliberately weak and must
never be used in a deployed environment.

Login example:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"group.admin@sisdent.demo","password":"odonto2026@O"}'
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

## Operational scheduling

Appointment requests require `startAt` and `endAt` ISO-8601 instants and a
valid IANA `schedulingTimezone`; end must be later than start. The API returns
a generic scheduling conflict if an active practitioner already has a scheduled
overlap, without returning the other appointment or patient. Cancellation,
completion and no-show are terminal. Performed procedures snapshot the active
catalog name and can only be logically voided with a reason.

For the isolated enrollment delivery seam, start with an explicit profile:

```bash
SPRING_PROFILES_ACTIVE=development ./mvnw spring-boot:run
```

The development provider keeps messages in memory and does not send real email.
The `e2e` profile additionally enables an authenticated current-account-only
test-support endpoint used by Playwright. Neither implementation exposes a
production HTTP route. With no explicit development/test provider, enrollment
delivery fails closed until a production provider is configured.

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
authorization matrix and legacy migration strategy, and
`docs/PHASE_3_IMPLEMENTATION.md` for the account cutover, token lifecycle,
delivery isolation, and privacy-preserving errors.
