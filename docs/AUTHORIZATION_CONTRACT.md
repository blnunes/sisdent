# Authorization contract

## Patient tenant lifecycle

Patient identity is global so an explicitly authorized intake workflow can deduplicate an
existing person. Operational access is not global: only active `patient_organization_links`
are readable and a clinic-unit request must match that link's clinic unit. Deleting a patient
from an organization workspace deactivates that organization link; it never deactivates the
shared patient identity or a link in another organization. Inactive links are excluded from
all organization and clinic patient lists. The exact-match endpoint only reports a match that
is already active in the caller's scope, so it cannot be used to discover patients in another
organization.

An organization workspace cannot update a patient that is actively shared with another
organization. A clinic workspace also cannot update a patient shared with another clinic unit.
This preserves the existing global identity model without allowing a local edit to silently
alter a different tenant's operational record.

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

Appointments and their performed procedures share the same organization and
clinic-unit scope. Appointment Readers may read both; Organization
Administrators, Managers, and Appointment Managers may create, reschedule, and
transition appointments, and record or void procedures only for a completed
appointment in that scope. Cross-scope resources resolve as not found. A
scheduling conflict is deliberately generic and never exposes the conflicting
appointment or patient.
