# Phase 4 implementation decisions

## Plan and compatibility risks

V10 adds organization-owned practitioners, appointments and append-only performed
procedures without changing V1--V9. Existing accounts, membership roles, patient
links and catalog rows are preserved. `MANAGER` retains its Phase 2 operational
write capability; the new `PRACTITIONER_MANAGER`, `APPOINTMENT_MANAGER` and
`APPOINTMENT_READER` roles make scheduling capabilities explicit for new grants.

| Membership role | Practitioner | Appointment read | Appointment manage | Performed procedures |
| --- | --- | --- | --- | --- |
| Organization administrator | manage | all units | all units | record/void |
| Manager (compatibility) | manage | assigned scope | assigned scope | record/void |
| Practitioner manager | manage | no | no | no |
| Appointment manager | read | assigned scope | assigned scope | record/void |
| Appointment reader / read-only | read | assigned scope | no | no |

Organization memberships apply to every unit; unit memberships are limited to
their unit. Platform administrators have none of these permissions.

## Lifecycle, scheduling and history

`SCHEDULED` may be rescheduled, cancelled, completed, or marked no-show.
`CANCELLED`, `COMPLETED`, and `NO_SHOW` are terminal. State changes update normal
audit metadata; rows are never deleted. A procedure can only be recorded for a
completed appointment. It snapshots the active catalog procedure name. It is
never edited: a correction sets `voided_at`, `voided_by`, and a required reason;
the original remains in history.

Create and reschedule lock the practitioner row with `PESSIMISTIC_WRITE`, then
query overlapping scheduled intervals (`start < requestedEnd && end >
requestedStart`) in the same transaction. This serializes competing scheduling
operations for a practitioner in H2 and PostgreSQL. It deliberately does not
use a PostgreSQL exclusion constraint because H2 cannot validate that migration;
PostgreSQL production should additionally consider a `tstzrange` exclusion
constraint after a database-specific migration strategy is adopted.

Requests carry `Instant` values and an IANA `ZoneId`; the server validates the
zone. Instants avoid ambiguous/nonexistent local-time conversion. Clients that
accept local wall time must resolve it explicitly before calling this API.
