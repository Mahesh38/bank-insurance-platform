# bank-persistence-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Amit (engineering), Aarti (DBA) and Shailja (audit evidence) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Adapter to persistence](../../api-reference/interactions/onesb-adapter-to-persistence.md) ·
[ADR-026](../../architecture-review/ADR-026-audit-store-and-transport.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Implemented** (39 main / 11 test classes), phase-1 floor 90 / 70 |
| Contract operations | 14 / 14 coded (jobs, offers, poll attempts, raw payloads, payment sessions, audit events) |
| Persistence | Flyway V1–V4 + vendor `V5__audit_event_sequence_and_immutability` |
| Round 1 fixes | Audit insert-only with duplicate = `200`, content conflict = `409`; `X-Internal-Service-Key` caller auth |
| Gaps | Duplicate job `idempotencyKey` / FK misses → unmapped `500`; validation answers `400` (kept, published behaviour); runtime schema `public` vs design `bank_persistence`; no ArchUnit test |
| Round 2 | **ADR-026 accepted (E2):** `bank_persistence.audit_event` is retired as the business record once audit-compliance-service runs; this service keeps adapter evidence only |

Maturity ≈ 80 %.

## 2. Scope for this build

1. Map constraint failures to the envelope (`409 IDEMPOTENCY_CONFLICT` / `CONFLICT` per contract);
   a duplicate job key answered as a replay **only if** the contract is changed to say so (PER-2).
2. ArchUnit test; PostgreSQL proof of V5 (sequence uniqueness, immutability trigger).
3. Schema name `bank_persistence` in runtime config (PER-3).
4. Audit retirement plan: keep `POST /audit-events` working until audit-compliance ingests; one-off
   copy to chain `legacy:bank_persistence` is audit-compliance's job (audit LLD §9). Do **not** delete
   the endpoint before the contract deprecates it.

## 3. Operations

[openapi.yaml](./openapi.yaml): `createJob`, `getJob`, `patchJobStatus`, `listOffers`, `addOffer`,
`listPollAttempts`, `addPollAttempt`, `storeRawPayload`, `listRawPayloads`, `getRawPayload`,
`createPaymentSession`, `getPaymentSession`, `appendAuditEvent`, `listAuditEventsByResource`.

## 4. Data owned

Schema `bank_persistence` — design [`02-bank_persistence.sql`](../../data-architecture/schemas/02-bank_persistence.sql)
and [`14-audit_event_delta.sql`](../../data-architecture/schemas/14-audit_event_delta.sql):
`integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload` (AES-256-GCM,
`retain_until` +7 y), `payment_session`, `audit_event` (retiring).

## 5. Rule sets

None. Raw-payload retention years is a property (`raw-payload.retention.years`, default 7, `RET-7Y`).

## 6. Events

None produced or consumed.

## 7. Synchronous dependencies

None outbound. Inbound: 1SB adapter (and future direct-insurer adapter) with `X-Internal-Service-Key`.

## 8. Invariants

`INV-AUD-01` audit rows never updated/deleted (grants + trigger) · duplicate `eventId` is success ·
`INV-ACT-04` acting capacity recorded · raw payloads encrypted, plaintext only on by-id read ·
7-year retention.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-PER-01 | Same audit `eventId` + same content → `200`, nothing written; different content → `409 IDEMPOTENCY_CONFLICT` | LLD §3.3 |
| AT-PER-02 | `(journeyId, sequenceNo)` taken → `409 CONFLICT` | LLD §4 |
| AT-PER-03 | On PostgreSQL, UPDATE / DELETE on `audit_event` raises (V5 vendor trigger) | `INV-AUD-01` |
| AT-PER-04 | Raw payload stored encrypted; list returns metadata only; by-id decrypts | LLD §3.2 |
| AT-PER-05 | Duplicate job `idempotencyKey` → envelope (not unmapped `500`) | LLD §8 #3 |
| AT-PER-06 | Missing / wrong `X-Internal-Service-Key` → `401` | LLD §2 |
| AT-PER-07 | ArchUnit rules active | playbook §3.1 |
| AT-PER-08 | Conformance test for all 14 operations | ADR-027 |

## 10. Build order

**Tier 3** (with the 1SB adapter). No upstream. Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| PER-1 | Retire `bank_persistence.audit_event` after audit-compliance ingests (ADR-026 §2) and deprecate `appendAuditEvent` in the contract | Mahesh, Shailja | Single business audit store |
| PER-2 | Duplicate job key: replay vs error (contract) | Amit | That mapping |
| PER-3 | Runtime schema `public` vs `bank_persistence` | Aarti | S09 migration |
