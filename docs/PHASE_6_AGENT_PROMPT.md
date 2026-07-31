# Phase 6 implementation handoff

You are continuing the Sisdent dental-management platform after Phase 5. Read
this file, `docs/ARCHITECTURE.md`, `docs/PROJECT_GUIDE.md`,
`docs/PHASE_2_IMPLEMENTATION.md`, `docs/PHASE_3_IMPLEMENTATION.md`,
`docs/PHASE_4_IMPLEMENTATION.md`, `docs/PHASE_5_IMPLEMENTATION.md`, every
Flyway migration, and the current code before making changes. The official
engineering language is **English**: use English for source code, API
contracts, migrations, tests, commit messages, and engineering documentation.

## Starting point

The platform has two coexisting identity representations:

- `Account` is the global authentication identity. It owns a verified or
  migration-pending email, password, person, platform-administrator flag and
  scoped memberships.
- `User` / `app_users` is the Phase 1 legacy identity. A compatibility link
  remains only so migrated accounts may use identification/password while
  `email_migration_required=true`.

Phase 2 introduced accounts, persons, organizations, clinic units and
memberships. Phase 3 introduced verified-email enrollment and the per-account
legacy-login cutover. Phases 4 and 5 rely on memberships for operational and
clinical access. The current User UI still emphasizes `app_users`, which makes
an account's email, membership scope, and migration state difficult to
understand.

Do not rewrite or alter released Flyway migrations V1--V11. Add forward-only
migrations for every schema change.

## Phase 6 objective

Make `Account` the canonical, understandable administrative identity for all
new account and access management. Build a protected **Accounts and Access**
workflow that shows one person/account, its authentication state, and its
organization/clinic memberships together. Preserve legacy identities only as
an auditable compatibility mechanism until an explicitly approved retirement
phase.

This is an administrative identity and access-management phase. It is not a
patient portal, staff self-service, support impersonation, SSO, MFA, password
recovery, email replacement for verified accounts, role redesign, bulk import,
cross-organization access sharing, audit export, or final removal of legacy
credentials.

## Product decisions already made

### Canonical identity and legacy compatibility

- `Account` is the only canonical authentication and administrative identity
  presented by the new UI and APIs. Use its public UUID, never its database ID.
- `Person` remains the account's display identity. A patient is not implicitly
  an account and must not be made discoverable through account administration.
- `app_users` and `legacy_user_id` remain read-only compatibility data. Do not
  create a new legacy user, modify a legacy password, or delete a legacy row in
  this phase.
- A migration-pending account may retain identification/password login only
  under the existing Phase 3 rule. The new UI must make that state clear without
  displaying an identification number in ordinary account lists.
- A verified account continues to use email/password. Do not add verified-email
  replacement, password reset, MFA, or external identity providers.

### Accounts and access management

- Platform administrators may manage global account lifecycle and create
  accounts. They do not thereby gain patient, appointment, practitioner,
  clinical, or tenant operational access.
- Organization administrators may view and manage memberships only in their
  own organization. They cannot change an account's email, password,
  platform-administrator flag, or memberships in another organization.
- A clinic-unit administrator does not exist. Clinic-unit memberships may be
  granted or revoked only by the relevant organization administrator.
- Membership roles remain the Phase 2--5 controlled role set. Do not invent a
  generic permission editor or silently map legacy `Role`/`Permission` values
  to tenant access.
- Revocation is logical: it preserves the account, person, unrelated active
  memberships, historical clinical/operational authorship, and audit fields.
- An account with no memberships may still exist and authenticate, but must be
  shown as having no tenant access. The UI must not infer a default tenant.

### Privacy and response shaping

- Account lists may expose only account UUID, display name, email, active,
  verified/migration state, platform-administrator state and a bounded summary
  of memberships appropriate to the caller.
- Never expose password hashes, pending email, raw verification tokens, legacy
  credentials, patient associations, clinical data, appointment data, or
  membership data outside the caller's authorized organization.
- An organization administrator's response must not reveal whether an account
  has memberships in another organization. Out-of-scope account UUIDs and
  membership UUIDs require indistinguishable controlled responses.
- Do not log emails, identifiers, membership scopes or lifecycle transitions in
  an error detail beyond the minimum needed for the authorized actor.

## Required design work before coding

1. Inspect the `Account`, `Person`, `User`, `Membership`, session/JWT,
   enrollment, organization, authorization and audit implementations. Identify
   every endpoint and Angular feature that still treats `User` as the primary
   administrative identity.
2. Write `docs/PHASE_6_IMPLEMENTATION.md` before coding. Include an exact
   authorization matrix for platform administrators, organization
   administrators, clinic-scoped members and unauthorised accounts.
3. Define lifecycle tables for account creation, activation/deactivation,
   email-verification migration state and membership grant/revocation. State
   explicitly which actions are deliberately unavailable in Phase 6.
4. Define a legacy-data presentation policy: where a migration-pending marker
   is visible, when an authorized platform administrator may view the legacy
   reference, and why it is never the default list identifier.
5. Define public identifier strategy, pagination, deterministic sorting and
   bounded filtering. All new account and membership URLs/responses use UUIDs;
   numeric IDs are internal only.
6. Present any proposal that deletes/merges accounts, changes an authoritative
   email, restores legacy login after cutover, exposes patient data, grants
   cross-tenant account visibility, adds support impersonation, or retires
   `app_users` before implementing it.

## Implementation expectations

- Add a forward-only V12 (or later) migration only when schema changes are
  necessary. Preserve V1--V11 data, account-email claims, enrollment
  challenges, legacy links, memberships, and all audit history.
- Prefer reusing the existing `Account`, `Person`, and `Membership` entities.
  Do not duplicate account email or membership scope into a new profile table.
- Keep controllers as validated DTO adapters; services own transaction and
  authorization rules; repositories perform scoped queries; mappers initialize
  response graphs with `open-in-view=false`.
- Add explicit account-management service and DTOs rather than extending the
  legacy `UserService` until it becomes a mixed model.
- Keep all account lifecycle changes audited and optimistic-lock protected.
  Return stable `409` conflicts for stale versions, duplicate emails and
  invalid lifecycle transitions.
- Provide paged, deterministic account lists. Platform administration may list
  accounts globally; organization administration may only list accounts with an
  active or historical membership in the selected organization, as defined by
  the approved design. Never provide a global list to tenant administrators.
- The Angular replacement must be named and labelled **Accounts and Access**.
  It must show display name, email, account state, migration status, and the
  authorized membership summary. It must support accessible, controlled forms
  for allowed lifecycle and membership changes, clear empty/error states, and
  complete English/Portuguese/Dutch translations.
- Keep a temporary legacy-user route only if it is platform-admin-only, clearly
  marked as compatibility information, and cannot create or mutate legacy
  identities. Remove it from normal navigation.

## Minimum API behavior

The exact paths and DTO names may follow existing conventions, but implement at
least these capabilities:

- platform-scoped paginated account list/read/create/activate/deactivate;
- authenticated current-account read that returns the same canonical summary as
  the management UI, without sensitive migration secrets;
- organization-scoped member account list/read and membership create/revoke;
- clear scoped membership summaries for the authorized platform or organization
  administrator;
- stable errors for unavailable email, stale version, invalid role/scope,
  prohibited account state transition, missing account-management authority and
  out-of-scope account/membership references;
- no endpoint that exposes password hashes, verification tokens, pending email,
  patient data, clinical data, appointment data or global account discovery to
  a tenant administrator.

## Minimum test and verification bar

Add tests that demonstrate at least:

- V12 upgrade preserves all V1--V11 identities, email claims, challenges,
  legacy links, organizations, memberships, scheduling and clinical records;
- account lists show the canonical account/person identity and do not duplicate
  one account into multiple apparent users;
- platform administrators can perform only the permitted global management
  actions and receive no implicit tenant/clinical access;
- organization administrators can manage only their organization memberships
  and cannot discover accounts or memberships belonging solely to another
  organization;
- clinic-scoped and ordinary accounts cannot perform organization or platform
  account management;
- legacy migration indicators are visible only where authorized and never leak
  legacy identification or credentials in ordinary lists;
- email collisions, stale versions, invalid membership role/scope and logical
  revocation are predictable and audited;
- Phase 2 tenant boundaries, Phase 3 enrollment cutover, Phase 4 scheduling,
  and Phase 5 clinical authorization remain valid;
- frontend tests cover identity display, migration state, membership scope,
  role controls, errors and all translations;
- a Playwright journey covers platform account creation, organization-scoped
  membership grant/revocation, and rejection of an out-of-scope change using a
  fresh dedicated backend.

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

1. a concise explanation of the canonical Account/Person/Membership model and
   legacy compatibility boundary;
2. the authorization and lifecycle matrices;
3. tenant/privacy and response-redaction notes;
4. migration compatibility and audit-history notes;
5. test/build/E2E results and any unavailable verification;
6. updated architecture/project documentation; and
7. an English commit message on a dedicated feature branch.

If an ambiguity affects account deletion/merging, email ownership, password
recovery, access by support staff, cross-organization disclosure, legacy-login
retirement, or a legal/regulatory identity obligation, stop and request
direction instead of making a permissive assumption.
