# GraphQL mutation boundaries

GraphQL replaces REST for all business workflows under the Phase 10 total-migration decision. Each mutation below is a transport adapter over the same application service used by REST during its retirement.

| Frontend workflow | GraphQL mutation | Service boundary | REST status |
| --- | --- | --- | --- |
| Create/update country catalogue | `createCountry`, `updateCountry` | `CountryService` | REST removed; delete awaits mutation |
| Create/update speciality catalogue | `createSpeciality`, `updateSpeciality` | `SpecialityService` | REST removed; deactivate awaits mutation |
| Create clinic unit | `createClinicUnit` | `OrganizationService` | Migrate all frontend consumers, then remove REST |
| Create/update practitioner | `createPractitioner`, `updatePractitioner` | `PractitionerService` | Migrate all frontend consumers; deactivate awaits mutation |
| Update an organization-scoped patient | `updatePatient` | `OrganizationPatientService` | REST removed; create/deactivate await mutations |

The Angular write services issue typed GraphQL inputs and refresh the existing GraphQL read state after success. The shared JWT interceptor still adds the Bearer token; the GraphQL endpoint requires authentication and services enforce platform, organization, clinic-unit and membership scope. Error messages and stable `extensions.code` values are surfaced through the safe GraphQL client error model, with the correlation ID available for support.

Resolvers do not access repositories, open transactions, or reimplement validation. Existing services remain responsible for authorization, tenant isolation, validation, transaction rollback and audit behavior. Catalogue translation input uses explicit `{ locale, value }` entries rather than a generic JSON map.

## Migration backlog

Catalogue/practitioner deletion or deactivation, catalogue-translation replacement,
patient intake/deactivation, membership and account lifecycle, scheduling,
clinical records, performed procedures and odontogram updates are pending GraphQL
work. They are not permanent REST exceptions.

High-risk workflows are deliberately deferred: clinical finalization and amendments, odontogram replacement, account lifecycle, all destructive operations, and patient intake/deactivation. A later phase must first approve their version/conflict contracts, tenant-isolation tests, rollback coverage and UX recovery behavior before exposing mutations.
