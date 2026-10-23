# reporting-mis-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (read-model store) and Rajal (report definitions) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Store segregation](../../data-architecture/04-store-segregation-and-roadmap.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes). 0 / 4 operations. The LLD is marked **NOT READY**: the
read-model store is undecided and report row schemas are open. Maturity ≈ 2 %.

## 2. Scope for this build — limited, by design

This service **cannot reach 90–95 %** until MIS-1 and MIS-2 are decided. What can be built now
without inventing anything:

1. Event intake: consumer group `reporting`, idempotent on `eventId`, retry / DLQ (playbook §4),
   for the events in LLD §3 that exist in the [catalogue](../events/CATALOGUE.md) (`LeadCreated`
   replaces the LLD's `OpportunityCreated`).
2. A **store port** (`ReadModelPort`) with an in-memory adapter for tests — no production adapter
   until MIS-1.
3. `getReadModelFreshness` (last event applied per source) — fully specified.
4. Request validation for the three report endpoints (`from ≤ to`, span ≤ 366 days, `lob=LIFE`).
5. The three report endpoints stay `PLANNED` (row schemas are open).

## 3. Operations

[openapi.yaml](./openapi.yaml): `getReadModelFreshness` (buildable); `getFunnelReport`,
`getSoldReport`, `getAttributionReport` (validation only; bodies blocked by MIS-2).

## 4. Data owned

Read model — store undecided (S3 + Athena / Redshift Serverless / separate Aurora; LLD §4). Never
the OLTP cluster, never a writer role on another schema (`CR-013` C-ISO-1).

## 5. Rule sets

None.

## 6. Events

Consumed only: `LeadCreated`, `LeadQualified`, `SuitabilityCompleted`, `QuoteCompleted`,
`QuoteRejectedNoSuitability`, `ProposalSubmitted`, `PaymentReconciled`, `PolicyIssued`,
`ProposalDeclined`, `JourneySold`. Dimensions come from the envelope `actor` and ids, never PII.

## 7. Synchronous dependencies

None outbound. Inbound: BFF admin / MIS routes (not yet in the BFF contract).

## 8. Invariants

Isolated read path · events only, no OLTP reads · no PII in the read model · every report shows `asOf`.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-MIS-01 | Duplicate event delivery → applied once | ADR-025 §4 |
| AT-MIS-02 | Freshness reports the last applied `occurredAt` per source | LLD §2 |
| AT-MIS-03 | `from > to` or span > 366 days → `422 VALIDATION_ERROR`; `lob=HEALTH` → `422 UNSUPPORTED_LOB` | LLD §5 |
| AT-MIS-04 | ArchUnit: no datasource / client for another service's schema | C-ISO-1 |
| AT-MIS-05 | Envelope that fails `event-envelope/v1` → DLQ at once | catalogue §3.1 |

## 10. Build order

**Tier 9** (last). Startable now for the limited scope above.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| MIS-1 | Read-model store (LLD §4) and the volume estimate behind it | Mahesh | Production adapter; > 10 % maturity |
| MIS-2 | Report definitions (columns, stage definitions, on- vs off-platform) | Rajal | The three report endpoints |
| MIS-3 | Off-platform policy feed: source, format, frequency | Rajal / bank | Sold report (off-platform) |
| MIS-4 | Report retention and export controls | Shailja | Go-live |
| MIS-5 | BFF admin / MIS routes and PDP scope | Mahesh, Deepali | User access |
