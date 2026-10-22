# ADR-026 — Business audit: events into an append-only audit store, archived to WORM

**Status:** **Accepted** — owner yes on 2026-10-22 ([round 2 E2](./DECISIONS-2026-10-22.md#owner-decisions)).
Shailja (retention) and Deepali (tamper evidence) review the details.
**Date:** 2026-10-21
**Origin:** [`SUG-20261021-rul`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261021-rul--configuration-driven-rule-engine-and-the-nine-open-decisions) — review decision D7.

## Context

Three designs disagree: the HLD puts audit in DynamoDB + S3 WORM; the DDL/portal use
`bank_persistence.audit_event`; the audit consumer doc says transport is undecided. The 1SB adapter
only logs audit lines. Today the bank's practice is database-level audit logging.

Two different needs are mixed under "audit":

| Need | Question it answers | Example |
|---|---|---|
| **Business audit (evidence)** | *Who did what to which customer's sale, in what capacity, and what did the system decide?* | RM viewed CIF; suitability evaluated with rule set v1.0.0; consent captured; proposal submitted; policy issued |
| **Database activity audit** | *Who touched the database directly?* | A DBA ran an UPDATE; a role was granted |

Database-level audit (triggers, pgaudit) answers the second well and the first badly: it sees row
changes, not intent, actor capacity, reads (a customer search is a read), cross-service sequence,
or the rule version that decided.

## Decision

1. **Business audit is an event stream.** Every service writes an `AuditRecorded` event (envelope
   `ADR-025`, `data` = action, resource, outcome, `actingCapacity`, `priorState`/`newState`, rule-set
   version, no PII) **in the same transaction** as the business change (outbox). Reads that matter
   (customer search, document view) write one too.
2. **`audit-compliance-service` owns the store**: Aurora schema `audit`, table `audit_event`,
   INSERT-only grants, UPDATE/DELETE blocked by trigger, `UNIQUE (event_id)` for idempotent
   consume, `UNIQUE (journey_id, sequence_no)`, and a **hash chain** (`row_hash = sha256(prev_hash ‖
   canonical(row))`) so tampering is detectable. `bank_persistence.audit_event` is retired after
   migration; bank-persistence keeps only adapter evidence (jobs, raw payloads).
3. **Daily WORM archive:** audit rows older than 1 day are exported to S3 with Object Lock
   (compliance mode, retention per `RET-7Y` / `RET-POLICY+7Y`); the chain head is stored with the
   export so the archive is verifiable.
4. **`SOLD` completeness:** a journey is `SOLD` only when its mandatory audit sequence is present
   (`INV-JRN-05`) — the audit service answers `GET /internal/v1/audit/journeys/{id}/completeness`.
5. **Keep database activity audit as it is today** — pgaudit on Aurora + Database Activity Streams /
   CloudTrail for privileged access. It is not the business record.

## Why this over "store it in the database with triggers" (today's practice)

Triggers would still be kept for *DB activity*, but they cannot record reads, the actor's capacity,
the rule version, or a cross-service sequence, and they tie every service's schema to the audit
format. The event route gives one queryable store, tamper evidence, and WORM retention — and it
reuses the outbox every service needs anyway.

## Alternatives

| Option | Why not |
|---|---|
| DynamoDB + S3 (HLD) | A second store engine for evidence that needs SQL queries by journey / actor / time |
| Synchronous HTTP to audit on every action | Couples availability of every service to audit; outbox is durable |
| Logs (today in the 1SB adapter) | Logs are never evidence |
