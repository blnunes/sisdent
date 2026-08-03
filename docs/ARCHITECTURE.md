# Sisdent architecture

Sisdent is a Spring Boot REST API with an Angular single-page application.
Authentication uses email/password and stateless JWT Bearer tokens. `Account`
is the global identity, `Person` provides its display name, and active
`Membership` records are the sole authority for organization and clinic work.

Patient identity is global for deduplication. Operational visibility and
lifecycle are scoped by active `PatientOrganizationLink` records; deactivation
removes only the current organization link. Appointments and clinical records
are organization and clinic scoped. Final clinical records are immutable and
corrections are modeled as amendments or replacement findings.

Foundational catalogues are platform-admin resources. Operational resources are
authorized in `ScopeAuthorizationService`; UI guards improve navigation but do
not provide security.

Flyway is the sole schema evolution mechanism. Applied migrations are
immutable. Migration V14 removes the retired identification-based
authentication, legacy user tables, and email-enrollment state.
