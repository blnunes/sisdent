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

## Validation

```bash
npm test -- --watch=false
npm run build
npm run test:e2e
```

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
