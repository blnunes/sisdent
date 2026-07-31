# Phase 5 implementation handoff

You are continuing the Sisdent dental-management platform after Phase 4. Read
this file, `docs/ARCHITECTURE.md`, `docs/PROJECT_GUIDE.md`,
`docs/PHASE_2_IMPLEMENTATION.md`, `docs/PHASE_3_IMPLEMENTATION.md`,
`docs/PHASE_4_IMPLEMENTATION.md`, and the current code before making changes.
The official engineering language is **English**: use English for source code,
API contracts, migrations, tests, commit messages, and engineering
documentation.

## Starting point

Phase 4 has delivered organization-owned practitioners, scoped appointment
scheduling, completed-appointment performed procedures, tenant-aware Angular
workflows, and isolated Playwright execution. The latest Phase 4 UX commit is:

```text
1ae4cf5 Use click-based date and time controls for appointments
```

The platform already has global accounts, organizations, clinic units, scoped
memberships, explicit patient-organization links, verified-email login,
platform catalogues, practitioners, appointments, and performed procedures.
Do not rewrite or alter released Flyway migrations V1--V10. Add forward-only
migrations for every schema change.

## Phase 5 objective

Implement the first protected clinical-record workflow: an authorized user can
create and review a patient's clinical encounters and maintain a structured
odontogram within that patient's organization and clinic scope. The system must
retain an intelligible, auditable history of clinical observations and their
corrections without making a patient globally discoverable.

This is a clinical-documentation phase. It is **not** treatment planning,
pricing, consent/acceptance, invoicing, payment, patient self-service,
prescribing, diagnostic imaging, attachment storage, digital signatures,
interoperability, reminders, or cross-organization clinical sharing.

Keep the scope deliberately small. Do not add a generic patient timeline,
medical-history questionnaire, allergies, medication lists, periodontal
charting, a free-form document store, AI diagnosis, procedure pricing, or a
patient portal unless an explicit requirement authorizes it.

## Product decisions already made

### Clinical-record ownership and encounter lifecycle

- A clinical encounter belongs to exactly one organization, clinic unit, and
  active patient-organization link. It may reference one appointment from the
  same organization, clinic unit, and patient link, but an appointment is not
  required for an encounter in this phase.
- If linked, the appointment must be `COMPLETED`. Do not turn a scheduled,
  cancelled, or no-show appointment into a clinical-record container.
- An encounter captures the care date/time, optional associated practitioner,
  concise clinical narrative, and an optional administrative internal note.
  The narrative is clinical data and must be treated as sensitive data in every
  response, log, test fixture, and UI state.
- A narrative has a bounded length. Do not add diagnosis, prescription, tooth,
  image, attachment, signature, or billing fields to the free-form encounter
  payload. Tooth-specific findings belong to the odontogram model below.
- An encounter starts as `DRAFT`. Its author may update the draft only while
  it remains a draft. Finalization changes it to `FINAL`; final records cannot
  be edited or deleted. A final record can only be corrected by a new,
  linked amendment with a required reason. The original remains visible in its
  history and is never overwritten or physically deleted.
- Finalization and amendments are application-level audit events, not digital
  signatures or a claim of legal non-repudiation. Do not label this feature as
  a legal signature.

### Structured odontogram

- The odontogram is an organization-scoped projection of observations for one
  patient link; it is not a global patient attribute and is never shared across
  organizations by implication.
- Use FDI tooth notation as stable string codes. Support permanent teeth
  matching `^[1-4][1-8]$` and deciduous teeth matching `^[5-8][1-5]$`; reject
  all other values. Never use database sequence identifiers as public tooth
  identifiers.
- A finding records the tooth, an optional surface, a controlled condition, the
  observation time, optional associated practitioner, and a short bounded
  clinical note. Supported surfaces are `WHOLE_TOOTH`, `MESIAL`, `DISTAL`,
  `OCCLUSAL_INCISAL`, `BUCCAL`, and `LINGUAL_PALATAL`.
- Start with a deliberately small controlled condition set:
  `SOUND`, `CARIES`, `RESTORATION`, `CROWN`, `MISSING`, `IMPLANT`, and
  `EXTRACTED`. Do not infer a finding from a performed procedure and do not
  silently mark an unobserved tooth as sound.
- Odontogram findings are append-only observations. A correction logically
  voids a finding with a mandatory reason and creates a replacement finding
  when needed; neither the original content nor audit author is overwritten.
  The current chart is derived from active observations using their observation
  time and a deterministic tie-breaker, while history remains available to
  authorized users.
- Recording an odontogram finding does not create a performed procedure,
  appointment, diagnosis, treatment plan, or billing item.

### Authorization and privacy

- Platform administration remains separate and grants no clinical encounter,
  odontogram, practitioner, appointment, or patient access merely by holding
  the platform role.
- Add explicit scoped clinical permissions/roles as needed, distinguishing
  clinical-record read, draft authoring, finalization/amendment, and odontogram
  recording/voiding. Do not infer clinical authority merely because an account
  has a practitioner profile. A practitioner must still hold an active scoped
  membership with the required permission.
- Organization-scoped clinical memberships apply to every clinic unit in that
  organization. Clinic-unit memberships act only within their unit. An
  organization administrator retains full organization clinical management.
- A patient can be found for clinical work only through the selected
  organization and its active patient link. Unknown, cross-organization, and
  out-of-scope public UUIDs must have indistinguishable controlled responses.
- Never expose clinical narrative, odontogram observations, or amendment
  reasons in appointment lists, generic patient lists, authentication/session
  responses, logs, notifications, or error details. Return the smallest data
  set required by the current scoped clinical workflow.

## Required design work before coding

1. Inspect the Phase 2 authorization matrix and organization-scoped patient
   services, Phase 4 appointment/practitioner authorization, audit entities,
   response mapping with `open-in-view=false`, Angular routes/navigation, and
   all Flyway migrations.
2. Write a short implementation plan and compatibility-risk section before
   changing code. Include an exact authorization matrix for every clinical
   encounter and odontogram action at organization and clinic-unit scope.
3. Define the encounter state-transition table, who can transition it, how
   draft editing works, and how a final-record amendment is linked and shown.
4. Define the odontogram projection rule, including same-time tie-breaking,
   logical voiding, replacement records, and optimistic-lock/concurrency
   handling. Do not rely on an in-memory chart as the source of truth.
5. Define payload limits and redaction/logging rules for clinical narrative,
   finding notes, and correction reasons. Explain why no field creates a
   disguised general medical-record or attachment store.
6. Define public identifier strategy. Continue using UUIDs for encounters,
   amendments, and odontogram findings. Do not expose database IDs in URLs or
   responses.
7. Present any proposal that changes finalized clinical content, gives a
   practitioner authority without membership, shares records between tenants,
   adds a diagnosis/medical-history model, exposes clinical records to a
   patient, or introduces legal-signature semantics before implementing it.

## Implementation expectations

- Add a forward-only V11 (or later) Flyway migration for clinical encounters,
  amendments, odontogram findings, permissions, indexes, constraints, and
  compatible backfills. Preserve V1--V10 data and auditability.
- Maintain the modular-monolith boundary: controllers adapt validated DTOs,
  services own transaction and authorization rules, repositories provide
  database-backed scoped filtering, and mappers initialize every response graph
  while `open-in-view=false`.
- New core entities extend the audit/optimistic-lock convention. State changes,
  finalization, amendments, and voiding must preserve author and timestamp
  metadata. Never use physical deletion for clinical content.
- Validate organization and clinic-unit consistency in both database and
  service layers. A linked appointment, patient link, practitioner, encounter,
  and odontogram finding must all be in the same authorized scope.
- Scope every public-UUID query by organization and, where appropriate, clinic
  unit and active patient link. Controllers must never load clinical data by an
  unscoped UUID.
- Paginate and deterministically sort encounter and odontogram history. Bound
  time-range and page-size inputs; do not create a global clinical export or
  unbounded patient-history endpoint.
- Use `Instant` for persisted observation/finalization timestamps and validate
  IANA zones whenever a client supplies a local clinical time. Preserve the
  submitted scheduling/observation zone where needed for display and audit.
- Build an Angular clinical workspace only after backend contracts are stable:
  patient search within the active organization, encounter list/detail,
  draft/final/amendment actions, odontogram chart and history, controlled error
  states, keyboard access, and complete English/Portuguese/Dutch translations.
  Do not show clinical content in broad dashboard or schedule cards.
- Keep English as the default system language and for engineering-facing
  hard-coded strings.

## Minimum API behavior

The exact paths and DTO names may follow existing conventions, but implement at
least these capabilities:

- organization/clinic-unit-scoped patient search for the clinical workspace;
- scoped encounter list, create, read, draft update, finalization, and
  amendment history operations;
- optional linking of an encounter to a completed same-scope appointment;
- scoped odontogram current-chart and paginated history reads;
- creation and logical voiding of odontogram findings, with replacement linked
  to the corrected finding where applicable;
- stable machine-readable errors for invalid state transitions, invalid tooth
  or surface values, invalid clinic/appointment/practitioner association,
  missing clinical permission, stale optimistic version, and out-of-scope
  records;
- no public global endpoint to discover clinical encounters, clinical notes,
  odontogram observations, patients, or practitioners.

## Minimum test and verification bar

Add tests that demonstrate at least:

- V11 upgrade preserves Phase 1--4 accounts, verification data, organizations,
  memberships, patient links, catalogues, practitioners, appointments, and
  performed procedures;
- only appropriately authorized organization or clinic-unit memberships can
  read, draft, finalize, amend, record, or void clinical data in their scope;
- platform administrators and practitioner profiles without the relevant
  membership receive no clinical access;
- a clinical encounter cannot use a patient without an active organization
  link, a practitioner from another organization, a clinic unit from another
  organization, or a non-completed linked appointment;
- drafts can be edited by their permitted author, final records cannot be
  altered/deleted, and amendments preserve the original record and reason;
- tooth notation, surfaces, conditions, note-length limits, timestamps, and
  optimistic-lock conflicts are validated predictably;
- odontogram voiding preserves history, replacements are linked, and current
  chart projection is deterministic without treating unknown teeth as sound;
- conflict and out-of-scope responses disclose no other tenant's patient,
  encounter, practitioner, appointment, or clinical content;
- Phase 2 tenant-boundary, Phase 3 enrollment, and Phase 4 scheduling tests
  remain valid;
- frontend tests cover clinic scope, clinical permissions, draft/final/
  amendment behavior, odontogram validation/history, errors, and translations;
- a Playwright journey covers a permitted user selecting a linked patient,
  drafting and finalizing an encounter, recording an odontogram finding,
  correcting it through void-and-replace, and a rejected out-of-scope action.

Run and report:

```bash
./mvnw test
cd frontend && npm test -- --watch=false
cd frontend && npm run build
cd frontend && npm run test:e2e
```

The Playwright suite must start a fresh, dedicated backend using the existing
isolated E2E runner; never reuse a local backend on port 8080.

## Deliverables

At the end, provide:

1. a concise description of clinical ownership, encounter lifecycle, and
   final-record correction rules;
2. the clinical authorization matrix and tenant/privacy boundary notes;
3. the odontogram code set, current-chart projection, and void/replace rules;
4. migration compatibility and audit-history notes;
5. test/build/E2E results and any unavailable verification;
6. updated architecture/project documentation;
7. an English commit message on a dedicated feature branch.

If an ambiguity affects clinical-record legal validity, consent, data
retention, access by a patient or support staff, cross-organization disclosure,
diagnosis semantics, finalized-record correction, or a health-data regulatory
obligation, stop and request direction instead of making a permissive
assumption.
