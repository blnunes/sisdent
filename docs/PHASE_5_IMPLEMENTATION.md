# Phase 5 implementation decisions

## Plan and compatibility risks

1. Add a forward-only V11 migration for clinical encounters and append-only
   odontogram observations, preserving every V1--V10 table and row.
2. Add explicit scoped clinical roles. A practitioner profile alone has no
   authority; an active membership remains mandatory.
3. Provide organization- and clinic-unit-scoped clinical services and APIs.
   Every public UUID lookup is additionally filtered by organization and checked
   against the requested clinic scope and active patient link.
4. Keep encounter drafts mutable only by their author. Finalization and
   amendments are preserved as immutable clinical history.
5. Derive the odontogram from persisted, non-voided observations. Do not store
   an in-memory or mutable tooth chart.

V11 is additive. It does not alter released migrations or backfill clinical
content. Existing organization administrators retain full clinical management;
the former operational roles do not gain clinical access implicitly.

## Clinical authorization matrix

| Membership role | Encounter read | Draft authoring/edit-own | Finalize / amend | Odontogram read | Record / void finding |
| --- | --- | --- | --- | --- | --- |
| Platform administrator | No | No | No | No | No |
| Organization administrator | All organization units | Yes | Yes | Yes | Yes |
| Clinical reader | Assigned organization or unit | No | No | Yes | No |
| Clinical author | Assigned organization or unit | Yes, own drafts | No | Yes | No |
| Clinical manager | Assigned organization or unit | Yes, own drafts | Yes | Yes | Yes |
| Practitioner profile without qualifying membership | No | No | No | No | No |
| Phase 2--4 manager/read-only/scheduling roles | No | No | No | No | No |

Organization-scoped memberships apply to all units in that organization;
clinic-unit memberships apply only to the named unit. An encounter or finding
is visible only where its organization, clinic unit, and active patient link
are all in scope. Cross-tenant and out-of-scope UUID lookups return the same
controlled not-found result and contain no clinical fields.

## Encounter lifecycle

| Current state | Action | Permitted actor | Result |
| --- | --- | --- | --- |
| `DRAFT` | Update | Its author with clinical-author authority | Replaces draft fields using optimistic versioning |
| `DRAFT` | Finalize | Clinical manager | `FINAL`, with finalization timestamp and actor |
| `FINAL` | Amend | Clinical manager | Creates a new `FINAL` encounter linked to the original, with required correction reason |

There is no delete operation. Finalization never edits the narrative. An
amendment carries its own complete clinical narrative and clinical fields; the
original final encounter remains visible in the amendment history. This is an
application audit event, not a legal or digital signature.

## Odontogram projection and corrections

Finding teeth use only FDI permanent `^[1-4][1-8]$` and deciduous
`^[5-8][1-5]$` codes. Conditions are `SOUND`, `CARIES`, `RESTORATION`,
`CROWN`, `MISSING`, `IMPLANT`, and `EXTRACTED`. Surfaces are `WHOLE_TOOTH`,
`MESIAL`, `DISTAL`, `OCCLUSAL_INCISAL`, `BUCCAL`, and `LINGUAL_PALATAL`.

The current chart chooses the latest active observation for each
`(toothCode, surface)` pair, ordered by `observedAt DESC, createdAt DESC,
globalId DESC`; this makes equal-time outcomes deterministic. Unknown teeth
are absent, never inferred as sound. Voiding records the void time, actor and
required reason. A replacement, when appropriate, is a new finding linked to
the voided observation. Both rows remain in paginated history. Each mutation
checks the submitted optimistic version, so concurrent corrections receive a
stable conflict rather than overwriting history.

## Sensitive-data controls

Clinical narrative is limited to 4,000 characters; administrative encounter
notes and finding notes to 500; amendment and void reasons to 500. All are
accepted only in clinical endpoints, omitted from session, patient-search and
appointment responses, and never included in exception messages. No payload
has diagnosis, prescription, attachment, image, medical-history, billing, or
free-form document fields, so these APIs cannot act as a disguised general
medical-record or attachment store.

Client-supplied observation times are `Instant` values plus a validated IANA
timezone retained with the observation. List ranges and pages are bounded and
deterministically ordered. UUIDs are the only exposed encounter, amendment and
finding identifiers; numeric database IDs remain internal.
