# Authorization contract

Organization-scoped APIs authorize exclusively through an active membership.
Legacy `app_users` roles and permissions remain compatibility data for legacy
or global resources and do not grant patient, practitioner, appointment, or
clinical access. Platform administration is likewise separate from operational
access.

An organization-wide membership applies to every clinic unit in its
organization; a clinic-unit membership applies only to that unit. A membership
in another organization never matches.

| Membership role | Patient list | Patient write | Practitioner management | Appointment read | Appointment manage | Clinical read | Clinical author | Clinical manage | Organization administration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Organization Administrator | Yes | Yes | Yes, organization-wide | Yes | Yes | Yes | Yes | Yes | Yes, organization-wide |
| Manager | Yes | Yes | Yes, organization-wide | Yes | Yes | No | No | No | No |
| Practitioner Manager | Yes | No | Yes, organization-wide | No | No | No | No | No | No |
| Appointment Manager | Yes | No | No | Yes | Yes | No | No | No | No |
| Appointment Reader | Yes | No | No | Yes | No | No | No | No | No |
| Clinical Reader | Yes | No | No | No | No | Yes | No | No | No |
| Clinical Author | Yes | No | No | No | No | Yes | Yes | No | No |
| Clinical Manager | Yes | No | No | No | No | Yes | Yes | Yes | No |
| Read Only | Yes | No | No | Yes | No | No | No | No | No |

Practitioners are organization-owned. Therefore practitioner list/create/update/
deactivate endpoints require an organization-wide Organization Administrator,
Manager, or Practitioner Manager membership; clinic-unit memberships cannot
manage practitioners.
