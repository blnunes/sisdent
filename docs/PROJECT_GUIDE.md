# Sisdent product guide

The product manages dental organizations, clinic units, accounts, patients,
appointments, practitioners, clinical encounters, odontograms, and reference
catalogues.

Use the application with an email/password account. The local bootstrap account
is `admin@sisdent.local` / `admin`; production must supply
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`.

Patient operations use scoped GraphQL `patients` queries and patient mutations.
Appointments and clinical operations use authenticated GraphQL operations.
Organization scope always comes from an active membership.

## Access model

Platform administration manages accounts and platform catalogues; it does not
by itself grant access to patients, appointments, or clinical records. Operational
access comes from an active membership in an organization or, when assigned,
one clinic unit. Organization Administrators and Practitioner Managers are
organization-wide; other operational roles can be organization- or clinic-scoped.

| Role | Patients | Practitioners | Appointments | Clinical records | Accounts/memberships |
| --- | --- | --- | --- | --- | --- |
| Platform Administrator | No automatic access | No automatic access | No automatic access | No automatic access | All organizations |
| Organization Administrator | Read/write | Manage | Manage | Manage | Assigned account-management organization |
| Manager | Read/write | Manage organization-wide | Manage | No | No |
| Practitioner Manager | Read | Manage organization-wide | No | No | No |
| Appointment Manager | Read | No | Manage | No | No |
| Appointment Reader | Read | No | Read | No | No |
| Clinical Reader | Read | No | No | Read | No |
| Clinical Author | Read | No | No | Create and edit own drafts | No |
| Clinical Manager | Read | No | No | Manage | No |
| Read Only | Read | No | Read | No | No |

Assign the smallest role and narrowest clinic scope that lets a person perform
their work. Do not share accounts; individual accounts keep activity traceable.

Run checks with `./mvnw test`, `cd frontend && npm test -- --watch=false`,
`npm run build`, and `npm run test:e2e`.
