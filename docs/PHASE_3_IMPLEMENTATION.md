# Phase 3 implementation decisions

## Implementation plan

1. Add a forward-only V9 migration for account email-verification state,
   database-backed pending-email uniqueness, and auditable single-use
   verification challenges.
2. Restrict identification/password login to active accounts whose
   `email_migration_required` flag is still true. Keep verified email/password
   login available throughout the rollout.
3. Add authenticated enrollment and resend services that operate only on the
   current account, plus an unauthenticated token-verification service with a
   generic public result.
4. Deliver opaque challenges through a replaceable server-side interface.
   Enable inspectable delivery only in explicit test/e2e profiles and fail
   closed when production delivery is not configured.
5. Reject stale legacy-state JWTs after verification and require a new
   email/password login so all claims reflect the verified state.
6. Add Angular legacy login, required-enrollment, and verification routes after
   the backend contracts are stable.
7. Verify V9 upgrades, authentication cutover, privacy and tenant boundaries,
   frontend behavior, and a fresh-process browser journey.

## Account state and exact legacy cutover

An account has one authoritative normalized login email, an explicit
`email_verified` state, the existing `email_migration_required` transition
flag, and at most one normalized `pending_email`.

- Existing non-migrating bootstrap accounts are backfilled as verified.
- Migrated accounts retain their synthetic authoritative email, are unverified,
  and may use identification/password while
  `email_migration_required=true`.
- A pending email cannot authenticate and is never returned by login or public
  verification responses.
- Successful verification atomically promotes the pending normalized email to
  the authoritative email, marks it verified, clears the pending claim, and
  sets `email_migration_required=false`.
- Identification login is allowed if and only if the matched account remains
  active, has legacy credentials, and has
  `email_migration_required=true`. Verification is therefore the permanent
  per-account cutover; no application operation in this phase can restore the
  flag.
- Requests authenticated with a JWT whose migration-state claim no longer
  matches the database account are rejected. The user must sign in again with
  the verified email and password after cutover.

Email normalization intentionally remains compatible with Phase 2:
Java `String.strip()` followed by locale-independent lowercase using
`Locale.ROOT`. Database unique constraints enforce authoritative and pending
claims after this canonicalization. This phase does not introduce a different
Unicode or internationalized-domain canonicalization that could reinterpret
existing account keys.

## Verification challenge lifecycle

1. **Creation:** an authenticated migrating account submits a candidate email.
   The service normalizes and validates it, rejects verified-email replacement,
   reserves it in `accounts.pending_email`, revokes active challenges for that
   account, generates a cryptographically random 256-bit opaque secret, and
   stores only its SHA-256 hash.
2. **Delivery:** after persistence, the raw secret is passed only to the
   configured delivery interface. It is never returned from the enrollment
   API, persisted, logged, or placed in audit metadata.
3. **Resend/supersession:** resend is current-account-only. A minimum cooldown
   and bounded recent-challenge count are enforced server-side. An accepted
   resend revokes all still-active challenges before creating one replacement.
4. **Expiry:** challenges have a short configured lifetime. Expired challenges
   remain audit records but can never be consumed.
5. **Verification:** the unauthenticated endpoint hashes the supplied token,
   locks the matching challenge/account, and succeeds only when it is
   unexpired, unconsumed, unrevoked, and still targets the account's pending
   email. Account promotion and challenge consumption occur in one transaction.
6. **Invalidation:** success consumes the used challenge and revokes any other
   active challenges for the account. A changed pending target, newer
   challenge, expiry, replay, malformed token, or unknown hash produces the
   same controlled public failure.
7. **Audit:** the entity records normal creation/update auditors and timestamps,
   expiry, consumption/revocation timestamps, and non-secret delivery metadata
   such as provider message ID where available.

## Delivery isolation

`EmailVerificationDelivery` is the replaceable server-side boundary.
Production/default configuration uses a fail-closed implementation until a
real provider is explicitly configured. An in-memory delivery mailbox is
available only in explicit `development`, `test`, and `e2e` profiles. Only the
`e2e` profile may expose a test-support HTTP seam for the current local
journey; production cannot instantiate that controller or inspect delivered
secrets.

Delivery failure does not expose a token. The transaction is rolled back where
possible; an unusable/revoked persisted challenge is retained if a provider
fails after accepting a message.

## Privacy-preserving error semantics

- Login always returns the existing generic invalid-credentials response for
  unknown accounts, wrong passwords, unverified authoritative emails, and
  retired identification login.
- Enrollment is authenticated and current-account-only. A verified account
  receives a controlled conflict because replacement is out of scope. Invalid
  email, unavailable candidate, cooldown, and delivery failure use stable
  problem types/messages without identifying another account.
- Cooldown responses may include a retry interval for the current account but
  never another account's timestamps or challenge state.
- Verification returns only `VERIFIED` or `INVALID_OR_EXPIRED`. Unknown,
  malformed, expired, superseded, revoked, consumed, cross-account, and replayed
  tokens share `INVALID_OR_EXPIRED`.
- Enrollment and verification responses contain no account IDs, legacy
  identifiers, memberships, organizations, patients, pending emails, or raw
  secrets.

## Compatibility risks and mitigations

- V9 only adds nullable/defaulted state and a new table; it does not rewrite V8
  or remove legacy users, memberships, patient links, or passwords.
- Phase 2 accounts with `email_migration_required=false` are treated as verified
  and continue email/password login without enrollment.
- Migrated accounts retain access through identification/password until their
  own successful mailbox challenge, then lose that path immediately and
  permanently.
- Database constraints and pessimistic/optimistic locking prevent concurrent
  accounts from reserving the same pending email or consuming a token twice.
- Enrollment does not read or mutate memberships, organization scope, patient
  links, or platform privileges. Existing Phase 2 tenant-boundary tests remain
  mandatory.
- Verified-email replacement, support impersonation, recovery, and real email
  delivery from development remain explicitly out of scope.
