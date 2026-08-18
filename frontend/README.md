# Sisdent frontend

Angular 22 and Angular Material 22 administrative interface for Sisdent.

## Requirements

- Node.js `^22.22.3`, `^24.15.0`, or `>=26`;
- npm 11;
- Sisdent API running at `http://localhost:8080`.

## Run locally

Start the API from the repository root:

```bash
./mvnw spring-boot:run
```

In another terminal:

```bash
cd frontend
npm ci
npm start
```

Open `http://localhost:4200`. The development server proxies `/api` to port 8080. The local training credentials are:

```text
Identification type: NATIONAL_ID
Identification: ADMIN
Password: admin
```

## Features

- public login with JWT session handling;
- functional authentication and administrator guards;
- friendly restricted/not-found page for non-administrators;
- paginated and searchable user table;
- create and update users;
- logical deletion;
- add and remove permissions;
- responsive Angular Material interface.
- organization administration routes: `/clinic-units` for organization-wide
  Organization Administrators and `/practitioners` for organization-wide
  Organization Administrators, Managers, and Practitioner Managers.

## GraphQL BFF boundary (Phase 10)

GraphQL is the primary frontend API for migrated flows. Components call typed
domain services, never the GraphQL client directly. `GraphQlClientService` owns
the common request/error envelope and maps `errors[].extensions.code` and
`correlationId` into the safe frontend error model; the regular authentication
interceptor attaches the JWT Bearer token.

Country and speciality reads/writes, clinic-unit reads/creation, practitioner
workspace reads/writes, and patient edits have GraphQL adapters. Authentication,
session bootstrap, health checks, and every workflow without a verified GraphQL
replacement remain HTTP. The full route decision and deprecation window are in
[`docs/REST_RETIREMENT_INVENTORY.md`](../docs/REST_RETIREMENT_INVENTORY.md).

## Validation

```bash
npm test -- --watch=false
npm run check:i18n
npm run build
npm run test:e2e
```

Run `npm run check:i18n` whenever user-facing copy changes. It verifies that
each configured locale has the same translation keys and that every statically
referenced key in Angular templates and TypeScript exists.

`npm run test:e2e` checks `/actuator/health` first. It reuses a healthy API or
starts a temporary Spring Boot process and waits for it before running the
Playwright test; the Angular development server is started automatically too.
Install the Chromium runtime once before running it locally:

```bash
npx playwright install chromium
```

## Visual direction

The palette uses white/off-white as a clean base, deep blue for confidence, and
teal/mint accents for calm and health. The direction follows guidance from the
American Dental Association and common dental branding practice, while keeping
adequate contrast and avoiding overly intense colors that can increase anxiety.

- Angular release schedule: <https://angular.dev/reference/releases>
- Angular Material theming: <https://material.angular.dev/guide/theming>
- ADA color guidance: <https://www.ada.org/resources/practice/practice-management/office-design/choosing-a-color-scheme>
