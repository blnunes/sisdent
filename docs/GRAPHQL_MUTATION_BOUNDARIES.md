# GraphQL mutation boundaries

REST remains active. GraphQL does not replace REST globally in Phase 9; each mutation below is a transport adapter over the same application service used by REST.

| Frontend workflow | GraphQL mutation | Service boundary | REST status |
| --- | --- | --- | --- |
| Create/update country catalogue | `createCountry`, `updateCountry` | `CountryService` | Retained for compatibility; delete stays REST-only |
| Create/update speciality catalogue | `createSpeciality`, `updateSpeciality` | `SpecialityService` | Retained for compatibility; deactivate stays REST-only |
| Create clinic unit | `createClinicUnit` | `OrganizationService` | Retained for compatibility |
| Create/update practitioner | `createPractitioner`, `updatePractitioner` | `PractitionerService` | Retained for compatibility; deactivate stays REST-only |
| Update an organization-scoped patient | `updatePatient` | `OrganizationPatientService` | Retained for compatibility; create/deactivate stay REST-only |

The Angular write services issue typed GraphQL inputs and refresh the existing GraphQL read state after success. The shared JWT interceptor still adds the Bearer token; the GraphQL endpoint requires authentication and services enforce platform, organization, clinic-unit and membership scope. Error messages and stable `extensions.code` values are surfaced through the safe GraphQL client error model, with the correlation ID available for support.

Resolvers do not access repositories, open transactions, or reimplement validation. Existing services remain responsible for authorization, tenant isolation, validation, transaction rollback and audit behavior. Catalogue translation input uses explicit `{ locale, value }` entries rather than a generic JSON map.

## Intentionally retained REST workflows

REST remains the only mutation transport for catalogue/practitioner deletion or deactivation, catalogue-translation replacement, patient intake/deactivation, membership and account lifecycle, scheduling, clinical records, performed procedures and odontogram updates.

High-risk workflows are deliberately deferred: clinical finalization and amendments, odontogram replacement, account lifecycle, all destructive operations, and patient intake/deactivation. A later phase must first approve their version/conflict contracts, tenant-isolation tests, rollback coverage and UX recovery behavior before exposing mutations.
