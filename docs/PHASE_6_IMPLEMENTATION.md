# Phase 6 implementation decisions

## Canonical model and compatibility boundary

`Account` is the global administrative and authentication identity and is
addressed exclusively by `accounts.global_id`. Its `Person` supplies the
display name. An active `Membership` is the only source of organization or
clinic-unit authority. A patient is neither an account nor included in account
management responses.

`app_users` and `Account.legacyUser` remain read-only migration evidence. They
are never created, edited, deactivated, or used for new grants by this phase.
The normal account list never contains an identification number. A platform
administrator may see only `legacyCompatibilityPresent` on an account detail,
and only while migration is required; no raw identifier or credential is
returned. Organization administrators never receive that field.

## Authorization matrix

| Actor | Global account list/read/create/lifecycle | Organization account list/read | Grant/revoke membership | Tenant/clinical access from this phase |
| --- | --- | --- | --- | --- |
| Platform administrator | Yes | May read all accounts and their membership summaries | May manage memberships in any active organization/unit | None |
| Organization administrator | No | Only accounts with a current or historical membership in that organization; summaries limited to that organization | Only its organization, including clinic-unit scope | Existing membership only |
| Clinic-scoped member | No | No | No | Existing scoped duties only |
| Ordinary/unauthorized account | No | No | No | None |

Out-of-scope account and membership UUIDs use controlled `404` results for
organization-scoped calls. Missing management authority is `403`. Platform
account lifecycle actions use optimistic versions and return `409` for stale
versions, email conflicts, and invalid transitions.

## Lifecycle

| Subject | Action | Result |
| --- | --- | --- |
| New account | Platform administrator creates | New `Person` and verified email/password `Account`; it has no tenant access |
| Account | Activate/deactivate | Logical active flag changes; memberships and history remain intact |
| Account | Grant/revoke platform administration | Platform administrators may transfer platform-wide authority; at least one active platform administrator is retained |
| Migration-pending account | Verify its reserved email through the Phase 3 flow | Authoritative email is promoted; migration flag is permanently cleared; legacy login retires |
| Membership | Authorized access administrator grants | A new active membership in the selected organization/unit and controlled Phase 2--5 role |
| Membership | Organization administrator revokes | Logical revocation; account, other memberships and authorship remain |

Phase 6 deliberately does not delete/merge accounts, replace a verified email,
reset passwords, restore legacy login, modify legacy credentials, impersonate
users, add MFA/SSO, share access between organizations, bulk import, or expose
patient/clinical/appointment data.

## API shape, filtering and response redaction

`/api/platform/accounts` provides platform-only paged account lists, reads,
creation, and activation changes. `/api/account` provides the current canonical
summary. `/api/organizations/{organizationId}/accounts` provides an
organization-scoped page and read; it includes only that organization’s
membership summaries. Membership creation/revocation remains scoped beneath
the organization. The administrative UI grants a membership from an exact
email entered in the selected account’s access dialog; it does not expose a
global account directory to tenant administrators. The dialog offers every
organization that the caller can administer, then its clinic units, so access
can be managed without changing the active operational context. Platform
administrators may select any active organization; organization administrators
only see their own organization-wide administration scopes.

Lists are bounded (`page` >= 0, `size` 1--100), sort by display name or email
with account UUID as a deterministic tie-breaker, and permit a bounded
case-insensitive name/email filter. Platform results may include all membership
summaries; organization results contain no evidence of memberships elsewhere.
Responses omit password hashes, pending email, verification challenges/tokens,
raw legacy fields, patients, appointments, and clinical content.

## Migration and audit compatibility

V12 is additive and does not alter V1--V11. It adds no duplicate identity or
membership profile table: existing account/person/membership rows and their
audit fields remain authoritative. Lifecycle and revocation mutations update
the existing optimistic version and normal audit metadata. Existing email
claims, enrollment challenges, legacy links, tenant records, scheduling, and
clinical records are untouched.
