# Outbox physical design (CR-012 Aarti condition 1)

**Owner:** Aarti / Database (physical design) · Amit (publisher)  
**signature_status:** `AI-DRAFTED — design record; first platform-wide migration not applied. identity.outbox_event already exists.`

No system of record moves (`ADR-012`). The outbox is a table in Aurora, inside the existing restore path.

## Pattern (already in identity)

[`identity.outbox_event`](../../platform/data-architecture/schemas/01-identity.sql):

- PK `id uuid`
- `aggregate_type`, `aggregate_id`, `event_type`, `payload`, `created_at`
- `published_at` nullable (publisher drain frontier)
- `attempts`, `last_error`
- Index `ix_outbox_unpublished (published_at, created_at)` — publisher poll

## Binding design for every publishing business schema (DATA-002)

Before the first non-identity migration:

| Element | Decision |
|---|---|
| Primary key | `id uuid` |
| Publisher poll index | partial/covering equivalent of unpublished-first: `(published_at, created_at)` |
| Payload | TEXT/JSONB of the *event*, not the topic-as-record |
| Same transaction | INSERT outbox row in the same DB transaction as the business write |
| Archival | published rows eligible for delete/archive after `NFR-EVT-03` replay drill proves consumers are idempotent (`ASM-011`) |
| Autovacuum | treat as append-and-drain; do not inherit OLTP defaults blindly |
| IAM/grants | publisher UPDATE `published_at` only; consumers never write the table |

`bank_persistence` still has **no** `outbox_event`. Do not build `#16` Audit against a direct poll and move it later (`DEP-20260824-evd`).
