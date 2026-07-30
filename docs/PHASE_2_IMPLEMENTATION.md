# Phase 2 implementation decisions

## Implementation plan

1. Add forward-only global identity, organization, clinic-unit, membership, and
   patient-link tables while preserving Phase 1 rows.
2. Migrate every legacy user to a one-to-one global account and retain legacy
   identification login during the email transition.
3. Issue account-based JWTs, expose the current account session and
   memberships, and enforce organization/clinic scope in application services.
4. Close the unscoped patient API. Provide scoped patient search, exact intake
   matching, and explicit audited linking without cross-organization disclosure.
5. Move the Angular login to email and add active-membership session state.
6. Verify migration compatibility, authorization boundaries, backend tests,
   frontend tests/build, and fresh-process browser journeys.

## Compatibility risks and mitigations

- Legacy accounts do not have verified email addresses. V8 assigns a unique,
  non-deliverable `@legacy.sisdent.invalid` address and sets
  `email_migration_required=true`; identification/password login remains
  available until a later verified-email enrollment flow retires it.
- The bootstrap administrator keeps `NATIONAL_ID / ADMIN / admin` compatibility
  and also receives `admin@sisdent.local`. These training credentials are not
  suitable for deployment.
- V8 creates a legacy organization, clinic unit, memberships, and explicit
  `LEGACY_MIGRATION` patient links when upgrading populated databases.
- Legacy users inserted after migration are converted transactionally on their
  first successful legacy login.
- The old `/api/patients` route is closed because it has no tenant scope.
  Clients use `/api/organizations/{organizationId}/patients`.
- Membership revocation is logical. It does not deactivate or delete the
  account, person, other memberships, or patient data.

## Authorization matrix

| Actor | Scope | Organization and unit administration | Patient read | Patient write/intake/link |
| --- | --- | --- | --- | --- |
| Platform administrator | Platform only | Create organizations | None | None |
| Organization administrator | One organization, all its units | Units and memberships in that organization | Linked patients in that organization | Yes |
| Manager | Organization or one clinic unit | None | Linked patients in the assigned scope | Yes |
| Read-only staff | Organization or one clinic unit | None | Linked patients in the assigned scope | No |

An organization-wide membership applies to all clinic units in that
organization. A clinic-unit membership applies only to that unit. An
`ORGANIZATION_ADMIN` membership must be organization-wide. Platform
administration is represented separately and is never treated as a clinical
permission.

## Patient privacy boundary

Name search executes only over patients already linked to the selected
organization and optional clinic unit. Exact intake matching uses document
type, issuer country, normalized document number, and birth date, and returns
only `possibleMatchExists`. It never returns a patient identifier, another
organization, clinic, history, or clinical content.

A patient becomes visible to a new organization only through an explicit
`PatientOrganizationLink`. The link records the organization, optional clinic
unit, creator, timestamp, and one operational basis: booking request,
attendance, intake, documentation acceptance, or legacy migration. Consent is
not represented as the only legal basis. Cross-organization clinical sharing
and temporary support access remain out of scope.
