# Sisdent roles and access guide

## Who this guide is for

This guide explains what each Sisdent role can and cannot do. It is written for
system operators, clinic leaders, and support staff. It is not a technical API
manual.

## The two types of access

Sisdent separates access into two layers. Keeping them separate is important.

1. **Platform administration** manages the Sisdent environment and accounts
   across organizations. It does **not** automatically give access to patients,
   appointments, or clinical records.
2. **Organization or clinic access** controls the day-to-day work a person can
   do in a specific dental group or clinic unit.

An account can have more than one organization or clinic assignment. When this
happens, the organization selector in the header changes the working area. A
role never gives access to an organization or clinic unit that has not been
assigned to the account.

## Scope: where a role applies

A role can apply to either:

- **An entire organization** — the person can work across all of its clinic
  units, subject to the role's permissions.
- **One clinic unit** — the person can work only in that clinic unit.

Organization Administrator and Practitioner Manager are always
organization-wide. They cannot be assigned to only one clinic unit. The
practitioner catalogue is organization-owned; a clinic-unit membership never
grants practitioner management. All other operational roles may be assigned at
organization or clinic-unit scope.

## Platform Administrator

The Platform Administrator is the highest administrative role in Sisdent.

Can:

- View all accounts across all organizations.
- Create accounts, activate or deactivate accounts, and grant or remove the
  Platform Administrator flag.
- Manage account access in any organization or clinic unit.
- View all accounts at once from **Accounts and Access** by selecting **View
  all accounts**.
- Create organizations and perform platform-level catalog administration.

Cannot, unless also given an organization or clinic role:

- View patients.
- View, create, or manage appointments.
- View, write, finalize, or manage clinical records.

Important: at least one active Platform Administrator must always remain. The
system will not allow the last active Platform Administrator to be removed.

## Organization Administrator

This role manages an entire organization and all of its clinic units.

Can:

- Read and update patient information in the assigned organization.
- Manage practitioners and appointments in the assigned organization.
- Read, create, and manage clinical records in the assigned organization.
- Create clinic units for the assigned organization.
- Manage organization memberships and roles in the account's assigned
  **account-management organization**.

Cannot:

- View all accounts across Sisdent.
- Manage accounts in another organization through Accounts and Access, even if
  the person has an operational assignment there.
- Grant Platform Administrator access.

For account management, each non-platform administrator has one persisted
account-management organization. Switching the header to another operational
organization does not extend user-management access. This protects account
administration from being expanded accidentally.

## Manager

The Manager role is for operational leadership rather than identity
administration.

Can, within the assigned organization or clinic unit:

- Read and update patient information.
- Manage practitioners.
- Manage appointments.

Cannot:

- Manage accounts or assign user roles.
- Create clinic units.
- Access clinical records merely because of the Manager role.
- Grant Platform Administrator access.

## Practitioner Manager

The Practitioner Manager role is for people responsible for practitioner setup.

Can:

- Manage organization-owned practitioner records across the assigned
  organization.
- Read patient lists in the assigned scope.

Cannot:

- Manage user accounts or memberships.
- Manage appointments solely through this role.
- Access clinical records solely through this role.
- Update patient information solely through this role.

## Appointment Manager

The Appointment Manager role is for scheduling teams.

Can:

- View appointments in the assigned scope.
- Create, reschedule, cancel, complete, and mark appointments as no-show in
  the assigned scope.
- View patient lists needed for appointment work.

Cannot:

- Update patient information.
- Manage practitioners, user accounts, or memberships.
- Access clinical records solely through this role.

## Appointment Reader

The Appointment Reader role is for staff who need to see the schedule but must
not change it.

Can:

- View appointments and the relevant patient lists in the assigned scope.

Cannot:

- Create, change, cancel, complete, or mark appointments as no-show.
- Update patients, manage practitioners, manage accounts, or access clinical
  records solely through this role.

## Clinical Reader

The Clinical Reader role is for staff who need to review clinical information.

Can:

- Read clinical records and the current odontogram in the assigned scope.
- View patient lists in the assigned scope.

Cannot:

- Create, edit, finalize, correct, or void clinical records.
- Manage appointments, practitioners, accounts, or memberships.
- Update patient information solely through this role.

## Clinical Author

The Clinical Author role is for clinicians who document care.

Can:

- Read clinical records and odontograms in the assigned scope.
- Create clinical records and clinical findings.

Cannot:

- Perform clinical-management actions reserved for the Clinical Manager, such
  as managing the full clinical workspace.
- Manage accounts, memberships, practitioners, or appointments solely through
  this role.

## Clinical Manager

The Clinical Manager role is for clinical leads.

Can:

- Read, create, finalize, correct, and manage clinical records and odontogram
  findings in the assigned scope.

Cannot:

- Manage user accounts or memberships.
- Manage appointments or practitioners solely through this role.
- Update patient information solely through this role.

## Read Only

The Read Only role is for staff who need general visibility without making
changes.

Can:

- View patient lists and appointments in the assigned scope.

Cannot:

- Change patients, appointments, practitioners, clinical records, accounts,
  memberships, or platform settings.

## Quick comparison

| Role | Patients | Practitioners | Appointments | Clinical records | Accounts and memberships |
| --- | --- | --- | --- | --- | --- |
| Platform Administrator | No automatic access | No automatic access | No automatic access | No automatic access | All organizations |
| Organization Administrator | Read and update | Manage | Manage | Manage | Assigned account-management organization |
| Manager | Read and update | Manage | Manage | No | No |
| Practitioner Manager | Read lists | Manage (organization-wide only) | No | No | No |
| Appointment Manager | Read lists | No | Manage | No | No |
| Appointment Reader | Read lists | No | View | No | No |
| Clinical Reader | Read lists | No | No | View | No |
| Clinical Author | Read lists | No | No | Create and read | No |
| Clinical Manager | Read lists | No | No | Manage | No |
| Read Only | View | No | View | No | No |

## Good operating practice

- Give the smallest role that lets someone complete their work.
- Assign clinic-unit scope when a person does not need organization-wide access.
- Use Platform Administrator only for trusted system operators.
- Review memberships when a person changes clinic, role, or responsibilities.
- Remove or deactivate access promptly when a person no longer needs it.
- Do not share accounts. Each person should use their own account so activity
  remains traceable.
