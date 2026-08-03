# Legacy compatibility boundaries

Phase 7 removed the legacy `/api/users` CRUD API, its Angular user-management
screen, the permission editor, and the `/users` and `/permissions` redirects.
They have no V2.0 runtime path; canonical accounts and organization memberships
replace them.

`app_users`, `user_permissions`, `Role`, and `Permission` remain only because
deployed databases and the verified-email enrollment flow still need the legacy
credential record. They are never emitted as JWT authorities and cannot grant
access to organization, clinical, scheduling, patient, or catalog APIs.

The legacy identification/password login boundary remains until every account
with `email_migration_required = true` completes verified-email enrollment.
Remove it only in a separately approved forward migration after production has
no such accounts. Existing Flyway migrations are intentionally retained.

`/api/states` remains a documented alias for administrative divisions until
external clients move to `/api/administrative-divisions`; no removal date is
set because this repository has no external-client inventory.
