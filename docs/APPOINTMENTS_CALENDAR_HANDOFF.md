# Appointments Calendar — Handoff memory

**Last updated:** 2026-08-24  
**Current state:** Phases 1.5, 2, 3, and 4 are complete. Phase 4 adds explicit, timezone-safe
creation and rescheduling dialogs without changing range loading or lifecycle behaviour.

## Goal

Replace the `/appointments` list/form experience with a dark, Angular Material-compatible,
Google Calendar-style schedule based on FullCalendar Standard `timeGridWeek` / `timeGridDay`.

## Architecture rule

GraphQL at `POST /graphql` is the sole business API. Authentication, session bootstrap, CSRF,
health, and development H2 remain HTTP by design; appointments, clinic units, practitioners,
and availability do not. The REST routes referenced by the former Phase 0 audit are historical
evidence only and must not be restored or extended.

## Phase status

| Phase | Status | Notes |
| --- | --- | --- |
| Phase 0 — architecture and contract audit | Superseded in part | Read its privacy, scope, and DST rules; replace its REST contract with this GraphQL contract. |
| Phase 1 — availability persistence/rules | Present but merge-incomplete | Originated in commit `58e6217`; current sources contain unresolved GraphQL merge defects. |
| Phase 1.5 — GraphQL realignment | Complete | GraphQL range/availability contract approved; REST availability removed. |
| Phase 2 — read-only frontend calendar foundation | Complete | E2E/visual acceptance passed; FullCalendar week/day, filters, paged range loading, availability backgrounds, and named-timezone adapter are in place. |
| Phase 3 — details/navigation | Complete | Appointment event click/keyboard activation opens a scoped, read-only Material dialog. `appointmentId` is deep-linkable; closing/Escape clears it and returns focus to the originating event. Invalid or unavailable details clear the selection and show only a localized generic message. |
| Phase 4 — creation/reschedule | Complete | Explicit Material dialogs use existing scoped GraphQL mutations only. Local inputs are parsed by Luxon in the selected clinic IANA timezone and submitted as UTC instants with that timezone. Successful and failed mutations refresh appointments and availability while retaining the calendar state. |
| Phase 5 — lifecycle/advanced behavior and regression | Complete | Slot shortcut, lifecycle transitions, blocked-period management, and completed-appointment procedures are shipped through scoped GraphQL. |

## Phase 1.5 — mandatory GraphQL realignment

The merge retained useful persistence and availability logic, but it must be reconciled with
the GraphQL architecture before it can be used.

### Verified defects

1. `AppointmentAvailabilityResponse` is the older boolean record (`available`), while
   `AppointmentAvailabilityService` constructs rich interval records. This produces compiler
   errors for missing `Availability`/`Category` types and constructors.
2. `AppointmentGraphQlController.appointments` does not pass the current
   `AppointmentService.list` `practitionerIds` argument, also preventing compilation.
3. `AppointmentAvailabilityController` newly exposes a REST business route and violates the
   GraphQL-only architecture. Remove it; do not repair it.
4. `clinic_units.timezone` is persisted but absent from `ClinicUnitResponse`, GraphQL type
   `ClinicUnit`, the Angular `ClinicUnit` model, and the `ClinicUnits` query.
5. The current GraphQL `appointmentAvailability` query is a legacy boolean slot-overlap check.
   It cannot return working hours, breaks, blocked periods, or occupied intervals for calendar
   rendering.

### Required correction

1. Restore a rich `AppointmentAvailabilityResponse` with `practitionerId`, `startAt`,
   `endAt`, `availability`, and `category`; categories are `WORKING_HOURS`, `BREAK`,
   `BLOCKED`, and `OCCUPIED`; availability values are `AVAILABLE` and `UNAVAILABLE`.
2. Remove `AppointmentAvailabilityController` and all new REST availability mappings.
3. Expose a non-null read-only `timezone` on clinic-unit DTOs and GraphQL. This IANA value is
   the authoritative display timezone for the selected clinic.
4. Extend `appointments` with optional `practitionerIds`, then reconcile schema, resolver,
   typed Angular transport, service call, and tests. Calendar callers always use a finite
   half-open `[from, to)` range and aggregate pages of no more than 100 results.
5. Add a dedicated GraphQL range query, suggested as
   `appointmentAvailabilityIntervals(organizationId, clinicUnitId, from, to, practitionerIds)`.
   It delegates to `AppointmentAvailabilityService` and returns the rich interval records.
   Do not overload the legacy boolean query with a different meaning; remove it only after no
   retained workflow needs it.
6. Use the standard GraphQL error envelope. Clients branch only on
   `errors[].extensions.code` and never show raw error details. Do not add correlation headers
   or patient/business identifiers to frontend logs.
7. Add focused GraphQL integration and unit tests, then run the Java quality gate and Sonar when
   `SONAR_TOKEN` is available. Do not add exclusions or suppressions.

**BLOCKED FOR PHASE 2:** until Phase 1.5 compiles and the GraphQL contract is accepted.

## Target GraphQL contract after Phase 1.5

- `clinicUnits(organizationId, clinicUnitId)` returns `id`, `organizationId`, `name`,
  `active`, and `timezone`.
- `appointments(organizationId, clinicUnitId, from, to, practitionerIds, page, size)` returns
  a paged result. The calendar supplies finite `from` and `to` UTC instants.
- `appointmentAvailabilityIntervals(organizationId, clinicUnitId, from, to, practitionerIds)`
  returns authoritative interval records and scopes both requests to the selected clinic.
- Appointment writes are outside Phase 2. Future conflicts use stable code
  `SCHEDULING.PRACTITIONER_UNAVAILABLE`; a write refreshes both range datasets.

## Timezone, privacy, and observability rules

- GraphQL interval values are UTC instants. Calendar navigation, labels, day boundaries, and FullCalendar
  `timeZone` must use the selected clinic's authoritative IANA timezone.
- `Appointment.schedulingTimezone` is historical metadata; it does not override the selected
  clinic display timezone.
- Never use browser timezone fallback, fixed offsets, manual date/time concatenation, or
  `new Date("YYYY-MM-DDTHH:mm")` calculations.
- `BLOCKED` rendering must use a generic localized label such as “Unavailable”; never expose
  block IDs, reasons, sources, staff identity, or patient information.
- Do not log patient data, appointment IDs, block IDs, bodies, credentials, or tokens. Existing
  GraphQL correlation, logging, and metrics use safe operation/code labels. Do not alter
  observability or add frontend correlation handling for calendar work.

## Timezone-adapter decision — required before FullCalendar installation

The former package decision allowed only four FullCalendar packages, but a named IANA timezone
without an adapter may UTC-coerce `Date` values. A simple `toISOString()` can then be wrong
at a DST boundary. This conflicts with the mandatory timezone rule.

Before installing dependencies or beginning Phase 2, explicitly approve one option and record
it in the Phase 0 decision record:

1. a tested named-timezone adapter compatible with the selected FullCalendar version; or
2. a browser-supported native timezone API with an explicit browser-support baseline.

No manual-offset workaround is acceptable.

## Phase 2 approved scope once unblocked

- Add only approved FullCalendar packages and the explicitly approved timezone adapter/API.
- No Angular, Material, RxJS, or TypeScript upgrade.
- No FullCalendar Premium or Resource TimeGrid.
- Week/Day views, Material toolbar, clinic/practitioner filters, paged range loading,
  availability background rendering, dark theme, responsive Day fallback, i18n, accessibility,
  and read-only state.
- Use Signals/RxJS and cancel obsolete range/filter requests with latest-request-wins behavior.
- Explicitly exclude dialogs, slot selection, writes, details, drag/drop, resize, lifecycle
  actions, blocked-period management, and backend changes.

## Required entry check for Phase 2

Read `docs/APPOINTMENTS_CALENDAR_PHASE_0_DECISION.md` in full, then inspect the GraphQL schema,
resolvers, Angular typed services, approved timezone adapter, and a clean compilation. If any is
absent, stop with:

> BLOCKED FOR PHASE 2: the GraphQL calendar contract or approved timezone-safe range adapter is
> absent. Complete and approve Phase 1.5 before starting the calendar frontend.

## Relevant files

- `docs/APPOINTMENTS_CALENDAR_PHASE_0_DECISION.md` — privacy, scope, DST, and acceptance rules;
  update its REST-specific contract before Phase 2.
- `src/main/resources/graphql/organization-reads.graphqls`
- `src/main/java/br/com/itbn/sisdent/graphql/AppointmentGraphQlController.java`
- `src/main/java/br/com/itbn/sisdent/graphql/OrganizationReadQueryController.java`
- `src/main/java/br/com/itbn/sisdent/service/AppointmentService.java`
- `src/main/java/br/com/itbn/sisdent/service/AppointmentAvailabilityService.java`
- `src/main/resources/db/migration/V20__add_appointment_availability.sql`
- `frontend/src/app/features/appointments/`
- `frontend/src/app/core/appointment-graphql.service.ts`
- `frontend/src/app/core/organization-read-graphql.service.ts`
- `frontend/e2e/appointments.spec.ts`

## Phase 3 delivered

- Uses the existing `appointment(organizationId, clinicUnitId, appointmentId)` GraphQL query, scoped to
  the active organization and selected clinic. The detail query selects only patient/practitioner display
  names, start/end instants, and status; no identifiers or raw GraphQL errors are rendered or logged.
- Renders date/time with the selected clinic's IANA timezone through named `Intl` formatting. Historical
  `schedulingTimezone` does not affect detail display.
- Keeps the existing calendar view, visible range, clinic, and practitioner filters unchanged while the
  `appointmentId` URL query selection opens/closes. Material dialog focus trapping and Escape behavior are
  used; the event that opened the dialog regains focus on close.
- Phase 3 itself added no create, reschedule, lifecycle, drag/drop, resize, or slot-selection affordance.

## Phase 4 delivered

- The calendar provides one explicit “Schedule appointment” action and a “Reschedule appointment” action
  from loaded read-only details. There is no slot selection, inline editing, drag/drop, resize, blocked-period,
  cancellation, completion, no-show, or other lifecycle control.
- Forms load patients and practitioners through existing authorized, selected-clinic GraphQL read contracts.
  IDs remain internal form values and are never rendered or placed in URLs.
- Luxon parses `datetime-local` values in the selected clinic IANA timezone. Invalid and DST-gap local values,
  missing fields, and non-increasing ranges are safely localized; repeated local values are resolved by Luxon
  zone rules. Mutation inputs always include the active organization, selected clinic, UTC ISO instants, and the
  selected clinic as `schedulingTimezone`.
- A valid practitioner/time selection also reloads the authoritative finite availability interval for that
  practitioner and selected interval; the form reports only a generic localized refresh state.
- `SCHEDULING.PRACTITIONER_UNAVAILABLE` is handled by error code only with a generic localized message. Every
  mutation response refreshes both existing range datasets and keeps the view, range, filters, and timezone.
  Material dialogs preserve focus trapping, Escape close behaviour, and focus restoration. Phase 5 remains pending.

## Phase 5 planned decision

- An explicit click or keyboard calendar-slot selection may open the creation dialog with the selected clinic-local
  date/time already filled in. It is only a shortcut: patient/practitioner selection, validation, availability, and
  explicit save remain mandatory; the calendar selection itself never creates an appointment.

## Phase 5 delivered

- FullCalendar time-grid slots are explicitly selectable by pointer and keyboard. The selected clinic IANA timezone
  is applied through the Luxon adapter to prefill the existing Material scheduling dialog. Selection never sends a
  create mutation, does not add drag/drop, resizing, inline editing, or slot lifecycle controls, and returns focus
  to the originating slot when the dialog closes.
- Appointment details expose cancellation, completion, and no-show only for a scheduled appointment. Each opens a
  focus-trapped confirmation dialog and calls the existing scoped `transitionAppointment` GraphQL mutation only
  after confirmation. Success and safe failure refresh both appointments and availability while preserving view,
  finite range, clinic, practitioner filters, and timezone. Errors remain localized and generic.
- Escape closes a clean scheduling dialog and returns to the calendar; a dirty form requires an explicit discard
  confirmation. Lifecycle confirmation remains explicit. Blocked backgrounds remain generic localized
  unavailability with no identifiers, reasons, sources, staff, patient data, or raw payload rendering.
- No blocked-period write mutation exists in the approved GraphQL schema, so blocked-period management is deferred.
  Existing performed-procedure mutations require a dental-procedure selection, but the calendar has no approved
  selected-clinic catalogue read for that purpose; creation/voiding UI is intentionally deferred rather than
  broadening scope or exposing procedure metadata.

## Phase 5 blocked-period GraphQL contract — 2026-08-24

- The approved backend contract is now delivered without adding a REST business route. It provides
  `appointmentBlockedPeriods`, `createAppointmentBlockedPeriod`, `updateAppointmentBlockedPeriod`, and
  `deleteAppointmentBlockedPeriod` at `POST /graphql` only.
- Every operation requires active organization and clinic scope plus appointment-management authorization. Blocks
  are either clinic-wide or scoped to an active practitioner in the organization; their start/end fields are UTC
  instants with a strictly increasing range. Update/delete use optimistic-lock `version` values to reject stale
  requests. The management record's opaque identifier/version remain internal and are not rendered by the calendar.
- Flyway migration `V21__add_blocked_period_version.sql` adds optimistic locking to the existing persistence table.
  Calendar availability keeps exposing `BLOCKED` only as generic localized unavailability; no calendar UI for
  blocked-period management is included in this backend-first step.
- Focused resolver/service tests cover success, invalid ranges, inactive practitioners, stale updates, cross-clinic
  delete, and scoped listing. `quality-gate.verify_quality({ runSonar: true })` passed with 86.77% instruction and
  66.13% branch coverage; Sonar passed.

Validation: `npm --prefix frontend test -- --watch=false` (121 tests), `npm --prefix frontend run check:i18n`,
and `npm --prefix frontend run test:e2e -- e2e/appointments.spec.ts --workers=1` (10 Chromium tests) passed.
The E2E runner required permission to bind its local Spring Boot test server. `npm --prefix frontend audit
--audit-level=low` reported zero vulnerabilities and `git diff --check` passed.

## Phase 5 final completion

- Blocked-period management is a separate focus-trapped Material dialog available only to appointment managers.
  It uses finite scoped GraphQL reads, active practitioners, Luxon/IANA UTC conversion, internal optimistic-lock
  versions, and explicit delete confirmation. The calendar keeps `BLOCKED` as generic localized unavailability.
- `eligiblePerformedProcedureOptions(organizationId, clinicUnitId, appointmentId)` is the approved `POST /graphql`
  scoped procedure-selection contract: it authorizes the exact organization/clinic, verifies the appointment scope,
  and returns only active `{ id, displayName }` procedures from that practitioner's active specialities. Existing
  create/void mutations are used from completed appointment details; void needs a
  nonblank explicit reason. Outcomes refresh appointments and availability while retaining range/view/filter/zone.
- No REST business route, drag/drop, resize, inline editing, implicit mutation, browser-timezone conversion, or
  Phase 6 work was added. IDs, versions, reasons, staff identity, and raw GraphQL messages stay internal.
