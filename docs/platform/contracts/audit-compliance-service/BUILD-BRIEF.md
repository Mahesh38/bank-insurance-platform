# audit-compliance-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture), Shailja (retention, SOLD sequence) and Deepali (tamper evidence) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[ADR-026](../../architecture-review/ADR-026-audit-store-and-transport.md) · [DDL `15-audit.sql`](../../data-architecture/schemas/15-audit.sql) ·
[Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes; no migration). 0 / 6 operations. The LLD says "do not build
before the owner's yes" — that yes was given in round 2 (**E2: ADR-026 accepted**, schema `audit`
accepted under E3). Maturity ≈ 5 %.

## 2. Scope for this build

`AuditRecorded` consumer (validate envelope, actor capacity, no PII; per-journey hash chain;
`ON CONFLICT (event_id) DO NOTHING`; offset after commit), evidence queries, `SOLD` completeness,
daily WORM export with manifest and verification, re-run export, legacy copy of
`bank_persistence.audit_event`.

## 3. Operations

[openapi.yaml](./openapi.yaml): `getJourneyCompleteness`, `listJourneyEvents`, `searchEvents`,
`getEvent`, `triggerExport`, `getExport`; plus the `AuditRecorded` consumer.

## 4. Data owned

Schema `audit` — design [`15-audit.sql`](../../data-architecture/schemas/15-audit.sql) (written as a
proposal before E2; check its banner) and LLD §7: `audit_event` (INSERT-only, `UNIQUE(event_id)`,
`UNIQUE(chain_id, sequence_no)`, `prev_hash`, `row_hash`, `retain_until`, BEFORE UPDATE/DELETE
trigger), `chain_head` (only updatable), `worm_export`. Note the design file names
`archive_export`; the LLD names `worm_export` + `chain_head` (AUD-6). S3 bucket with Object Lock
(COMPLIANCE mode).

## 5. Rule sets

`AUDIT.SOLD.COMPLETENESS` (parameters `requiredSteps`, classification `COMPLIANCE`) — **no seed and
not in ADR-024 §7**; the LLD's step list is derived, not a Compliance list (AUD-2). Until published,
completeness answers `CONFIGURATION_UNRESOLVABLE` and no journey reaches `SOLD` (fail closed).

## 6. Events

Consumed: `AuditRecorded` (every producer; topic choice AUD-5), `RuleSetPublished`, `PolicyClosed`
(not in catalogue). Produced: its own `EVIDENCE_VIEWED` / export records written directly into chain
`source:audit` (no outbox loop).

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| administration-config | 300 ms, cached | Sample `AUDIT.SOLD.COMPLETENESS` test resource |
| S3 Object Lock | — | Local / in-memory object store adapter; S09 for the real bucket |
| MSK | — | `InMemoryEventPublisher` + direct handler invocation in tests |

Inbound: journey completeness 2 s, 1 retry.

## 8. Invariants

`INV-AUD-01` immutable, ≥ 7 years (grants + trigger + Object Lock) · idempotent consume · hash chain
per `chain_id` verifiable from sequence 1 · `INV-JRN-05` completeness reads the store, never the
topic · no PII in evidence (allow-list + detector) · `INV-ACT-04` capacity on every human actor.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-AUD-01 | Same `eventId` delivered twice → one row; chain head not advanced by the duplicate | LLD §3 |
| AT-AUD-02 | Rows of one journey get `sequence_no` 1..n and `row_hash = sha256(prev ‖ canonical(row))` (RFC 8785) | ADR-026 §2 |
| AT-AUD-03 | On PostgreSQL UPDATE / DELETE on `audit_event` raises; `app_audit` has no UPDATE grant | `INV-AUD-01` |
| AT-AUD-04 | Event without `actingCapacity` for a human actor, or an IPR without `actorInsurerId` → DLQ, nothing stored | `INV-ACT-04` |
| AT-AUD-05 | `data` containing a PAN / mobile pattern → DLQ + security alert | ADR-025 no-PII rule |
| AT-AUD-06 | Completeness with all required steps and a valid chain → `complete=true`; one missing → listed in `missing` | LLD §5 |
| AT-AUD-07 | Tampered row (hash mismatch) → `complete=false`, `chainVerified=false`, security incident | LLD §8 |
| AT-AUD-08 | Rule set unresolvable → `CONFIGURATION_UNRESOLVABLE`; journey stays `ISSUED` | ADR-024 |
| AT-AUD-09 | Export for a date writes NDJSON.gz + manifest with chain heads; re-run is idempotent; verification mismatch → `FAILED` + alert | LLD §4 |
| AT-AUD-10 | Query range > 31 days → `422 VALIDATION_ERROR` | LLD §6 |
| AT-AUD-11 | Two consumers on PostgreSQL appending to one chain serialise on `chain_head FOR UPDATE` without gaps | LLD §3 step 4 |

## 10. Build order

**Tier 8** for real use (needs producers emitting `AuditRecorded`); startable now — producers are
simulated with envelopes built from [`AuditRecorded.v1.json`](../events/schemas/AuditRecorded.v1.json).

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| AUD-1 | ADR-026 accepted (E2) — LLD status line still says PROPOSED | Contract owner | LLD update only |
| AUD-2 | Approve the required `SOLD` steps and add `AUDIT.SOLD.COMPLETENESS` to ADR-024 §7 | Shailja; Mahesh | Any journey reaching `SOLD` |
| AUD-3 | Retention classes; lock extension for `RET-POLICY+7Y` on `PolicyClosed` | Shailja | Retention at go-live |
| AUD-4 | Hash-chain design review (canonicalisation, per-journey chains) | Deepali | Security sign-off |
| AUD-5 | Topic for `AuditRecorded`: catalogue says producer's aggregate topic; LLD says `{env}.{context}.audit.v1` | Mahesh / events owner | Consumer subscription (abstract it behind config) |
| AUD-6 | DDL `15-audit.sql` (`archive_export`) vs LLD (`worm_export`, `chain_head`) | Aarti | Migration parity |
| AUD-7 | S3 Object Lock bucket (S09) | Shivanshi | Go-live |
