# Sisdent product guide

The product manages dental organizations, clinic units, accounts, patients,
appointments, practitioners, clinical encounters, odontograms, and reference
catalogues.

Use the application with an email/password account. The local bootstrap account
is `admin@sisdent.local` / `admin`; production must supply
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`.

Patient operations use scoped GraphQL `patients` queries and patient mutations.
Appointments use the authenticated GraphQL scheduling operations and
clinical operations use `/api/organizations/{organizationId}/clinical`.
Organization scope always comes from an active membership.

Run checks with `./mvnw test`, `cd frontend && npm test -- --watch=false`,
`npm run build`, and `npm run test:e2e`.
