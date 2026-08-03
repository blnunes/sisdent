# V2.0 release readiness

This checklist is the release handoff for the authorization and operational
workspace work completed in phases 1–8. The server is authoritative: hiding a
front-end action is never considered authorization evidence.

## End-to-end matrix

| Scope and role | Supported operational path | Denial / boundary evidence |
| --- | --- | --- |
| Platform Administrator | Creates canonical accounts and organization memberships across organizations. | Has no patient, appointment, or clinical access without an active membership. |
| Organization Administrator (organization-wide) | Creates clinic units; manages practitioners, patients, appointments, clinical records, and memberships in its persisted account-management organization. | Cross-organization clinic and account calls are denied; platform-account membership changes are denied. |
| Manager (organization-wide or clinic) | Updates patients and manages appointments; organization-wide scope also manages practitioners. | No clinical or membership administration. Clinic scope cannot manage practitioners. |
| Practitioner Manager (organization-wide) | Manages organization-owned practitioners and reads scoped patients. | Cannot be clinic-scoped and cannot manage appointments, clinical records, or memberships. |
| Appointment Manager / Reader / Read Only | Manager runs the appointment lifecycle; Reader and Read Only can read appointments and scoped patients. | Direct appointment mutation is denied to Reader and Read Only; clinical-only roles cannot read appointments. |
| Clinical Reader / Author / Manager | Reader views records; Author creates and edits own drafts; Manager finalizes, amends, and corrects odontogram history. | Appointment and membership calls are denied; finalized records cannot be edited in place. |
| Clinic-scoped membership | Reads or operates only its assigned clinic unit where the role allows it. | Cross-clinic resources resolve as not found; organization-wide-only roles are rejected at grant time. |

## Automated evidence

Browser and API coverage is intentionally split by workflow so tests reuse the
same production authorization boundary:

| Workflow | Automated evidence |
| --- | --- |
| Patient create, update, deactivate, cross-organization link isolation | `frontend/e2e/patients.spec.ts`, `frontend/e2e/patient-tenant-isolation.spec.ts` |
| Practitioner and clinic-unit scope | `frontend/e2e/clinic-unit-search.spec.ts`, backend integration tests |
| Appointment creation and scheduling conflict | `frontend/e2e/appointments.spec.ts`, `Phase4SchedulingIntegrationTests` |
| Clinical draft, finalization, amendment, odontogram correction | `frontend/e2e/clinical-workspace.spec.ts`, `Phase5ClinicalRecordsIntegrationTests` |
| Membership grant, role change, stale-version rejection, revoke, inactive-membership denial, and cross-organization denial | `frontend/e2e/authorization-roles.spec.ts`, `Phase2IdentityIntegrationTests` |
| Navigation visibility and direct API role enforcement | `frontend/e2e/authorization-roles.spec.ts` |

## Release commands

Run these from the repository root unless noted otherwise:

```bash
./mvnw test
cd frontend && npm run build
cd frontend && npm test -- --watch=false
cd frontend && npm run test:e2e
```

The Playwright runner starts an isolated backend on port `8081` when needed.
Install Chromium once with `cd frontend && npx playwright install chromium`.

## Migration and residual-risk statement

This phase adds no Flyway migration and no persistent-domain object. The schema
remains at migration `V13__add_patient_link_lifecycle.sql`.

The accepted residual compatibility risk is limited to the documented legacy
email-enrollment and `/api/states` boundaries in
[`LEGACY_COMPATIBILITY.md`](LEGACY_COMPATIBILITY.md). Legacy roles and
permissions cannot authorize V2.0 APIs. Production release approval still
requires the commands above to pass in the target CI/runtime environment.
