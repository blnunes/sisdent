# Authorization contract

Organization-scoped APIs require an active membership in the requested
organization. A clinic-scoped membership is valid only for its assigned clinic.
Platform administration grants account and catalogue administration; it does
not grant patient, appointment, or clinical access.

| Membership role | Patients | Practitioners | Appointments | Clinical | Memberships |
| --- | --- | --- | --- | --- | --- |
| Organization Administrator | Read/write | Manage | Manage | Manage | Manage assigned account-management organization |
| Manager | Read/write | Organization-wide only | Manage | No | No |
| Practitioner Manager | Read | Organization-wide only | No | No | No |
| Appointment Manager | Read | No | Manage | No | No |
| Appointment Reader / Read Only | Read | No | Read | No | No |
| Clinical Reader | Read | No | No | Read | No |
| Clinical Author | Read | No | No | Create/edit own drafts | No |
| Clinical Manager | Read | No | No | Read/manage | No |
