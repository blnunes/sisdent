# GraphQL mutation boundaries

GraphQL replaces REST for all business workflows under the Phase 10 total-migration decision. Each mutation below is a transport adapter over the same application service used by REST during its retirement.

| Frontend workflow | GraphQL mutation | Service boundary | REST status |
| --- | --- | --- | --- |
| Create/update country catalogue | `createCountry`, `updateCountry` | `CountryService` | REST removed; delete awaits mutation |
| Create/update speciality catalogue | `createSpeciality`, `updateSpeciality` | `SpecialityService` | REST removed; deactivate awaits mutation |
| Manage platform accounts and lifecycle | `createPlatformAccount`, `changeAccountLifecycle`, `changeAccountPlatformAdministrator` | `AccountManagementService` | REST removed; GraphQL-only |
| Create organization and clinic unit | `createOrganization`, `createClinicUnit` | `OrganizationService` | REST removed; GraphQL-only |
| Manage memberships | `createMembership`, `grantMembership`, `changeMembershipRole`, `revokeMembership` | `OrganizationService` | REST removed; GraphQL-only |
| Create/update/deactivate practitioner | `createPractitioner`, `updatePractitioner`, `deactivatePractitioner` | `PractitionerService` | REST removed; GraphQL-only |
| Update an organization-scoped patient | `updatePatient` | `OrganizationPatientService` | REST removed; create/deactivate await mutations |
| Schedule, reschedule, transition, procedure and availability workflows | `createAppointment`, `rescheduleAppointment`, `transitionAppointment`, `createPerformedProcedure`, `voidPerformedProcedure`, `appointmentAvailability` | `AppointmentService`, `PerformedProcedureService` | REST removed; GraphQL-only |

The Angular write services issue typed GraphQL inputs and refresh the existing GraphQL read state after success. The shared JWT interceptor still adds the Bearer token; the GraphQL endpoint requires authentication and services enforce platform, organization, clinic-unit and membership scope. Error messages and stable `extensions.code` values are surfaced through the safe GraphQL client error model, with the correlation ID available for support.

Resolvers do not access repositories, open transactions, or reimplement validation. Existing services remain responsible for authorization, tenant isolation, validation, transaction rollback and audit behavior. Catalogue translation input uses explicit `{ locale, value }` entries rather than a generic JSON map.

## Migration backlog

Catalogue deletion or deactivation and any future workflow are not permanent
REST exceptions. New business operations require a typed GraphQL contract and
the same service-layer validation, authorization and conflict guarantees.

High-risk mutations use explicit inputs and preserve service-layer version,
tenant-isolation, rollback and UX-recovery guarantees.
