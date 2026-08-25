# Appointments Calendar — Phase 0 decision record

**Audit date:** 2026-08-20  
**Scope:** post-implementation contract audit. No application code, migration, dependency, API, or test was changed.

## Decision

**PHASE 3 COMPLETE — 2026-08-24.** The clinic-unit IANA `timezone` is now exposed by the GraphQL
`clinicUnits` contract and is used as the authoritative display timezone. The read-only calendar acceptance
is complete: automated E2E coverage verifies Week/Day views, practitioner filtering, keyboard view switching,
UTC range requests, dark mode, narrow viewport behaviour, and the absence of write controls. Phase 3 additionally
verifies event keyboard activation, Escape/focus restoration, valid direct detail links, malformed and authorization-
denied links, selected-filter preservation, selected-clinic timezone rendering across DST, and the responsive dark
detail dialog. Detail reads use the existing scoped GraphQL query and display only the approved minimum data; they
never display raw GraphQL errors or identifiers. No write or lifecycle affordance is enabled. `npm audit fix` applied
five safe transitive updates; a final audit reports zero vulnerabilities.

## Phase 1.5 GraphQL contract approval — 2026-08-24

**Approved.** This section supersedes the REST-specific appointment and availability material below,
which is retained only as the original audit record. Calendar business reads use `POST /graphql` only:

- `clinicUnits(organizationId, clinicUnitId)` returns non-null `id`, `organizationId`, `name`, `active`, and
  authoritative IANA `timezone`.
- `appointments(organizationId, clinicUnitId, from, to, practitionerIds, page, size)` accepts the optional
  practitioner filter. Calendar calls always provide finite UTC `[from, to)` values and aggregate pages of at
  most 100 items.
- `appointmentAvailabilityIntervals(organizationId, clinicUnitId, from, to, practitionerIds)` returns
  `practitionerId`, UTC `startAt`/`endAt`, `availability`, and `category`. `availability` is `AVAILABLE` or
  `UNAVAILABLE`; categories are `WORKING_HOURS`, `BREAK`, `BLOCKED`, and `OCCUPIED`.

The former REST availability route and legacy boolean GraphQL overlap query are retired. GraphQL errors use
the standard envelope; calendar clients branch only on `errors[].extensions.code` and do not display raw
details or log business identifiers. The clinic unit's `timezone` is the calendar display timezone;
`Appointment.schedulingTimezone` remains historical metadata.

**Timezone adapter decision:** approved `@fullcalendar/luxon3` 6.1.21 with Luxon 3.7.2, matched to the
FullCalendar 6.1.21 Angular/Core/TimeGrid/Interaction package set. The adapter maps FullCalendar's named
IANA timezone dates through `toLuxonDateTime(...).toUTC().toISO()` for range requests, including DST
boundaries. No browser timezone, fixed offset, or manual date construction is used.

## 1. Current state verified

### Phase 1 actually implemented

Commit `58e6217` introduced Flyway migration `V20__add_appointment_availability.sql` and:

- a non-null `clinic_units.timezone`, defaulting to `Europe/Lisbon`;
- working-hours, break, and blocked-period persistence; new clinic units receive 00:00–24:00 working hours for every weekday;
- filtered, overlap-based appointment range queries;
- `GET` appointment availability intervals; and
- authoritative create/reschedule availability checks: working hours, breaks, blocked periods, and scheduled-appointment overlap.

It also uses `java.time` zone rules when materializing weekly intervals, including the `minute == 1440` next-day boundary. Unit tests cover the DST date 2026-03-29, break/out-of-hours, blocked/occupied conflicts, range validation, and the existing scheduling integration test asserts the 409 conflict code.

### Actual REST contract

All routes below are REST routes; `/appointments` remains the intended operational REST workflow.

| Route | Query/body | Success payload | Access / failures |
|---|---|---|---|
| `GET /api/organizations/{organizationId}/appointments` | Required `from` ISO-8601 instant; optional `to` ISO-8601 instant, `clinicUnitId`, repeated/comma-bindable `practitionerIds`, `page` (default 0), `size` (default 25; capped at 100) | `PageResponse`: `content`, `page`, `size`, `totalElements`, `totalPages`; each item is `{globalId, clinicUnitId, patientId, patientName, practitionerId, practitionerName, startAt, endAt, schedulingTimezone, status}` | Appointment-read roles. Finite range uses half-open overlap (`startAt < to && endAt > from`); without `to`, `endAt >= from`. Invalid/missing `from`, non-increasing range, invalid ID/parameter: 400 RFC 9457; unknown practitioner/appointment or cross-clinic appointment: 404; unauthorized: 403; unauthenticated: 401. |
| `GET /api/organizations/{organizationId}/appointment-availability` | Required `clinicUnitId`, `from`, `to` as ISO-8601 instants; optional `practitionerIds` | Array of `{practitionerId, startAt, endAt, availability, category}`. `availability` is `AVAILABLE`/`UNAVAILABLE`; `category` is `WORKING_HOURS`, `BREAK`, `BLOCKED`, or `OCCUPIED`. | Appointment-read roles, with clinic scope. `to` is required and must be after `from`. Selected practitioners must be active and in the organization, otherwise 404. 400/401/403 use RFC 9457 as above. |
| `GET /api/organizations/{organizationId}/clinic-units` | Optional `clinicUnitId` | Array of `{id, organizationId, name, active}` | Platform administrator or appointment-read access in scope. **Does not expose `timezone`.** |
| `GET /api/organizations/{organizationId}/practitioners` | none | Practitioner records including `globalId`, `displayName`, and `active` | Existing frontend filter source; Phase 2 must use active practitioners only. |

Existing appointment write and lifecycle routes remain out of Phase 2: `POST /appointments`, `GET /appointments/{id}`, `PUT /appointments/{id}/reschedule`, `POST /appointments/{id}/cancel|complete|no-show`, plus performed-procedure routes. Creation/reschedule accept `AppointmentRequest` with non-null `clinicUnitId`, `patientId`, `practitionerId`, `startAt`, `endAt`, and `schedulingTimezone`; `startAt`/`endAt` are instants and timezone must be a valid IANA zone matching the clinic's stored timezone. Management roles are organization admin, manager, or appointment manager; readers cannot write.

The authoritative scheduling conflict is HTTP 409 with `application/problem+json`, `type: urn:sisdent:error:scheduling.practitioner_unavailable`, and stable `code: SCHEDULING.PRACTITIONER_UNAVAILABLE`. Its payload contains the normal RFC 9457 fields plus `correlationId` and no scheduling identifiers.

### Differences from the intended Phase 1 baseline

1. The required authoritative clinic timezone is persisted but absent from the frontend-facing clinic-unit contract. This is the Phase 2 blocker.
2. Availability is a separate `/appointment-availability` endpoint rather than being embedded in appointment ranges; this is acceptable and is the contract below.
3. Appointment range results are paged (maximum 100) and may omit `to`; a calendar must use a finite range and load every page. The API does not impose a maximum range.
4. No Phase 1 REST integration test exercises the new availability route, authorization, error payloads, or its real migration data. Existing coverage is service-unit focused.
5. Blocked periods have no creation/administration endpoint. They can be rendered when seeded/persisted but cannot be managed by the calendar in Phase 2.

## 2. Calendar integration contract (effective after the blocker is resolved)

1. The selected clinic unit is mandatory before range or availability loading. Its `id` scopes both requests. An organization-wide membership may select any returned active unit; a clinic-scoped membership must use its assigned unit. Do not make a cross-unit availability request.
2. For every FullCalendar `datesSet`/visible range, derive a finite half-open interval `[from, to)` in the **clinic unit's authoritative IANA timezone**, then send `from` and `to` as ISO-8601 UTC instants (`Date.toISOString()`). Use the same instants for appointments and availability.
3. Call appointment range loading with `clinicUnitId`, `from`, `to`, and the selected practitioner IDs when filtered. Page through all `PageResponse` pages (size no more than 100) before treating the range as complete. A missing practitioner filter means all practitioners permitted by the selected scope.
4. Call availability with the same required `clinicUnitId`, `from`, `to`, and the same selected active practitioner IDs. A missing filter means the backend returns active practitioners only. Merge neither client-side availability rules nor inferred free slots: the server is authoritative.
5. Render appointment `startAt` and `endAt` as instants in the selected clinic timezone. `schedulingTimezone` is historical appointment metadata, not an override for the calendar display timezone.
6. Render availability intervals as background events: `WORKING_HOURS` is the positive/open background; `BREAK`, `OCCUPIED`, and `BLOCKED` are unavailable backgrounds. `BLOCKED` must display only a generic localized category such as “Unavailable”; never expose a blocked-period ID, reason, source, staff identity, or patient information. `OCCUPIED` must not imply that the viewer can infer a patient's identity from background styling.
7. A 409 on a future write is handled by `code`, not localized `title`/`detail`. For `SCHEDULING.PRACTITIONER_UNAVAILABLE`, retain the current view/filter state, show a generic localized conflict message, and refresh both range datasets. Phase 2 has no writes, so it only needs the shared error model/handoff rule.

### Time and DST rules

- API `startAt`, `endAt`, `from`, and `to` are UTC instants. FullCalendar's `timeZone` and all labels, day boundaries, and view navigation use the selected clinic IANA timezone.
- Working hours and breaks are recurring clinic-local weekday/minute definitions. The backend maps them through `ZoneId` rules, so the calendar renders the returned instants only.
- Use a timezone-aware API/library and IANA identifiers for local-calendar boundaries and conversion; rely on the native `Intl` APIs only where they can represent the required operation unambiguously. **Manual date/time string concatenation, fixed UTC offsets, and `new Date("YYYY-MM-DDTHH:mm")` calculations are forbidden.** DST gaps/overlaps must be resolved by the chosen timezone-aware library and tested at both transition directions.

## 3. Phase boundaries

### Phase 2 may implement

FullCalendar foundation using Standard/open-source packages; Week and Day views; clinic-unit and practitioner filters; finite visible-range loading; paged appointment aggregation; availability/background and appointment rendering; selected-clinic timezone display; current theme tokens including dark mode; responsive layout; localization; loading/empty/error states; and strictly read-only event interaction structure (keyboard/focus semantics included).

### Phase 2 must not implement

Creation or reschedule dialogs; appointment details; any appointment write/lifecycle call; slot selection; drag/drop; resize; blocked-period management; backend, migration, API, or authorization changes; Angular/Material upgrades; or any FullCalendar Premium feature.

### Phase 3 prerequisites and responsibilities

Phase 3 starts only after Phase 2 is accepted, including timezone and range-loading acceptance. It owns the explicitly approved read-only appointment-detail interaction and any approved navigation/accessibility refinement. If Phase 3 is to introduce writes, it must first obtain an approved timezone-safe appointment form contract, 409 refresh behavior, and role/error acceptance tests; those writes are not implied by Phase 2.

## 4. Security, privacy, and observability

`RequestCorrelationFilter` applies to every new REST request automatically. It accepts/creates `X-Correlation-ID`, echoes it, records normalized handler routes (or bounded fallbacks), sets safe `transport` MDC, and clears MDC in `finally`; no frontend correlation-header work is required.

Do not put patient names/data, appointment IDs, blocked-period IDs, request bodies, credentials, or bearer tokens in frontend/backend logs, exceptions, telemetry, analytics, traces, or metric labels. Metric labels must remain bounded and normalized (for example route, status, transport, stable error code). Existing responses contain appointment/patient fields for authorized UI rendering only; they must not be logged.

Handle RFC 9457 as a safe display contract: read stable `code` and `correlationId`; localize the UI independently; never parse or display raw `detail`, request echoes, or an exception message. Known 4xx mappings include `REQUEST.PARAMETER_INVALID`, `VALIDATION.FAILED`, `RESOURCE.NOT_FOUND`, `AUTHENTICATION.FAILED`, `AUTHORIZATION.DENIED`, `CONFLICT`, and `SCHEDULING.PRACTITIONER_UNAVAILABLE`.

## 5. Test and acceptance matrix

| Future phase | Required focused tests / acceptance |
|---|---|
| 2 backend contract (before or with blocker fix) | Integration tests for availability success, 400 missing/non-increasing range, 401/403, inactive/foreign practitioner 404, clinic-scope isolation, blocked response privacy, DST interval conversion, appointment pagination/filter overlap, clinic response timezone, and RFC 9457 correlation/error code. |
| 2 frontend unit | API query construction uses finite UTC instants, routes timezone from selected clinic, aggregates pages, cancels/stales old range loads, filters practitioners, maps every availability category, handles loading/empty/401/403/409 safely, light/dark tokens, i18n keys, and DST transition boundaries. |
| 2 E2E | Authorized Week/Day range rendering, clinic-scoped membership isolation, practitioner filter, background categories without secret block details, timezone label/boundaries across DST, responsive viewport, dark theme, keyboard navigation, and no create/detail/drag/resize affordance. |
| 3 | Detail interaction authorization, privacy/minimum-data display, focus return and escape behavior, deep-link/navigation state, and no write side effects unless separately approved. |
| 4 | Approved create/reschedule workflow, client/server timezone match, validation, 409 code-driven refresh, stale-data recovery, role denial, and DST slot tests. |
| 5 | Approved lifecycle/advanced scheduling behavior, concurrency/conflict tests, accessibility regression, visual/responsive regression, privacy/logging regression, and complete end-to-end role matrix. |

User acceptance mapping: calendar foundation, Week/Day, filters, range/availability rendering, dark/responsive/read-only behavior belong to Phase 2; details belong to Phase 3; creation/reschedule and 409 user workflow belong to Phase 4; lifecycle/advanced interactions and final cross-cutting regression belong to Phase 5. The required explicit approval before any frontend calendar work is the clinic timezone response contract described in the Decision section. No other repository inconsistency blocks the read-only foundation once it is resolved.

**Phase 2 acceptance result:** accepted.

**Phase 3 acceptance result — 2026-08-24:** accepted. Appointment events open a read-only Material dialog by
pointer or keyboard. The dialog queries `appointment(organizationId, clinicUnitId, appointmentId)` using the active
organization and selected clinic unit, shows date/time in the selected clinic IANA timezone, status, practitioner,
authorized patient display name, and clinic context only. `appointmentId` is a deep-link query parameter; close and
Escape remove it without resetting the calendar range, view, or filters, and restore originating-event focus. Invalid,
unavailable, cross-clinic, and unauthorized selections are handled through GraphQL error codes with a localized generic
message and no raw details. No scheduling write behaviour is implied by this acceptance.

**Phase 4 acceptance result — 2026-08-24:** accepted. Creation is initiated only by the explicit calendar
action, and rescheduling only from an authorized read-only detail. Both workflows use the existing
`createAppointment` and `rescheduleAppointment` GraphQL mutations scoped to the active organization and selected
clinic; no REST business route or lifecycle control was added. Patient and practitioner choices come from authorized,
selected-clinic GraphQL reads and their identifiers remain internal to form submissions. The selected clinic IANA
timezone controls local form interpretation, availability queries, and submitted UTC instants; mutation
`schedulingTimezone` is always that selected clinic timezone. Luxon rejects invalid and DST-gap local values and
deterministically resolves repeated local times through IANA zone rules. Required fields and non-increasing ranges
are localized client-side validations; server availability remains authoritative.

Every mutation response refreshes both finite range datasets without changing the view, visible range, clinic,
practitioner filter, or selected-clinic timezone. `SCHEDULING.PRACTITIONER_UNAVAILABLE` is handled exclusively by
its GraphQL extension code with a generic localized message and no raw detail. Material focus trapping, Escape, and
focus restoration remain available. Automated unit and E2E tests cover mutation scope/variables, UTC conversion in
both DST directions, validation, successful refreshes, conflict handling, keyboard operation, responsive dark mode,
and the absence of lifecycle, drag/drop, resize, slot-selection, and blocked-period controls. Phase 5 remains
explicitly out of scope.

**Phase 5 planned scheduling interaction — 2026-08-24:** an explicit calendar slot selection may open the
existing creation dialog with the selected clinic-local date/time prefilled (for example, selecting 24/08 at 10:00).
It is a convenience shortcut only: it must never create an appointment implicitly. The dialog remains responsible
for patient/practitioner selection, complete-range validation, authoritative availability, and explicit save through
the existing scoped GraphQL mutation. It must preserve calendar filters, view, visible range, selected-clinic IANA
timezone, focus handling, privacy, and generic error rules. Drag/drop, resize, inline edits, and lifecycle actions
from a calendar slot remain prohibited unless separately approved.

**Phase 5 acceptance decision — 2026-08-24:** approved calendar slot selection is enabled only as an
explicit shortcut to the existing scheduling dialog. Pointer selection and keyboard activation of a time-grid
slot pass clinic-local start/end values through the FullCalendar Luxon adapter; no mutation is issued until the
user explicitly saves the completed form. `transitionAppointment(organizationId, clinicUnitId, appointmentId,
status)` is the accepted existing GraphQL lifecycle contract for cancellation, completion, and no-show, and is
available only from scoped appointment details after an accessible confirmation. Every lifecycle outcome refreshes
both appointments and availability without changing the current calendar state. Blocked periods remain a generic
localized unavailable background: the approved schema has no blocked-period write contract, so blocked-period
management is explicitly deferred. Performed-procedure mutations exist, but no approved scoped dental-procedure
catalogue selection workflow is available to this calendar; their UI is also deferred rather than exposing an
unscoped catalogue or sensitive procedure metadata.

**Phase 5 blocked-period GraphQL decision — 2026-08-24:** approved. Blocked-period management is a
separate, appointment-manager-only GraphQL workflow scoped by `organizationId` and `clinicUnitId`. The
contract permits a clinic-wide block (`practitionerId: null`) or an active practitioner-specific block,
always with increasing UTC `startAt`/`endAt` instants. It provides scoped list, create, update, and delete
operations. Records carry an opaque identifier and optimistic-lock version for the management client only;
neither is rendered by the calendar. Cross-organization, cross-clinic, inactive-practitioner, malformed,
not-found, unauthorized, and stale operations are rejected by the server. Availability remains the only
calendar read and continues to render every `BLOCKED` interval as localized generic unavailability.

## 6. Dependency decision

Subject to confirming peer compatibility at installation time against the existing Angular `22.0.x` / Material `22.0.6` application, Phase 2 may add exactly:

```text
@fullcalendar/angular
@fullcalendar/core
@fullcalendar/timegrid
@fullcalendar/interaction
```

`@fullcalendar/interaction` is allowed only to provide the integration foundation and keyboard/event plumbing; Phase 2 must configure no selectable slots, editable events, drag/drop, or resize. Do not add `@fullcalendar/resource-timegrid`, any `@fullcalendar-premium/*` package, or a Premium license. Do not upgrade Angular, Angular Material, RxJS, or TypeScript as part of this work.

**BLOCKED FOR PHASE 2 — expose the stored clinic-unit IANA `timezone` in the read-only clinic-unit response (and approve it as the calendar’s authoritative display timezone).**

## Phase 5 final completion — 2026-08-24

Blocked periods are now administered in a separate appointment-management Material dialog. It lists the
finite visible range and supports clinic-wide or active-practitioner blocks, explicit edit, and confirmed delete.
Opaque IDs and versions are held only for GraphQL mutations, never rendered. Luxon converts local values in the
selected clinic IANA zone to UTC; every success or safe failure refreshes existing appointment and availability
datasets without changing calendar state. `BLOCKED` remains generic localized unavailability.

`eligiblePerformedProcedureOptions(organizationId, clinicUnitId, appointmentId)` is the minimal scoped GraphQL
selection read: it requires exact appointment-management scope, verifies the appointment belongs to that clinic,
and returns only active procedures in its practitioner's active specialities as identifier and display name. Existing
scoped `createPerformedProcedure` and `voidPerformedProcedure` are used from completed appointment
details only; voiding requires a localized, nonblank confirmed reason. IDs, versions, staff identity, reasons,
and raw GraphQL messages are not rendered. No REST business API or Phase 6 work is introduced.
