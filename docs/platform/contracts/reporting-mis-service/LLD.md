# reporting-mis-service — Low-level design (minimal R0)

**Status:** AI-DRAFTED 2026-10-21 — **NOT READY**, awaiting Mahesh (read-model store decision) and
Rajal (report definitions) review. This page fixes paths and inputs only; it does not pretend the
read model is designed.

[OpenAPI](./openapi.yaml) · [API reference page](../../api-reference/services/reporting-mis-service.md) ·
[Store segregation](../../data-architecture/04-store-segregation-and-roadmap.md) ·
[ADR-025 events](../../architecture-review/ADR-025-event-specification-and-publisher.md)

## 1. Purpose and ownership

Funnel, sold and attribution reports on an **isolated read path** built from events. It never queries
Lead, Payment or Policy schemas and is never granted a writer role — year-end MIS must not slow
origination (`CR-013` C-ISO-1).

| | |
|---|---|
| Module | `services/reporting-mis-service` · port 8103 · `service-id: reporting` · L5 INTERNAL |
| Callers | BFF admin / MIS routes (scope decided by the PDP and passed as filters) |
| Inputs | Events only (§3) |
| Owns | Its read model — **store undecided** (§4) |

## 2. API summary

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/internal/v1/reports/funnel?from&to&branch&lob` | Stage counts |
| `GET` | `/internal/v1/reports/sold?from&to&insurer` | Sold (journey `SOLD`), on- vs off-platform |
| `GET` | `/internal/v1/reports/attribution?spId&from&to` | Accountable SP attribution (`C-RET-1`) |
| `GET` | `/internal/v1/reports/freshness` | Last event applied per source — every report shows `asOf` |

Row schemas are open until §4 is decided.

## 3. Events consumed

Consumer group `reporting`, idempotent on `eventId`, at-least-once, DLQ per ADR-025. Names from
ADR-025 and [outbox-and-events §4](../../api-reference/interactions/outbox-and-events.md#4-event-catalogue);
`docs/platform/contracts/events/CATALOGUE.md` supersedes them once published.

| Event | Feeds |
|---|---|
| `OpportunityCreated`, `LeadQualified` | Funnel (lead stages), attribution |
| `SuitabilityCompleted` | Funnel |
| `QuoteCompleted`, `QuoteRejectedNoSuitability` | Funnel |
| `ProposalSubmitted` | Funnel |
| `PaymentReconciled` | Funnel (paid) |
| `PolicyIssued`, `ProposalDeclined` | Funnel (issued / declined) |
| `JourneySold` | Sold, attribution |
| Off-platform policy ingest | Sold (off-platform) — source and format **undecided** (R-3) |

Events carry ids and states only (ADR-025); branch, SP and insurer dimensions come from the event
`actor` and ids, never from customer PII.

## 4. Store decision — left open

| Option | For | Against |
|---|---|---|
| A. S3 (Parquet) + Athena, events landed by a consumer | Matches store segregation (`S3 + Athena / Redshift` for reporting); cheap; isolated | Query latency seconds; needs compaction |
| B. Redshift Serverless | Fast aggregates, SQL for MIS users | Cost and another engine to operate at R0 |
| C. Dedicated small Aurora reporting schema fed by the consumer | Simple, same tooling | Shares the OLTP cluster unless a separate cluster — conflicts with "never this OLTP cluster" |

No option is chosen. Mahesh decides (R-1); the volume estimate needed for the choice is not in the
design pack.

## 5. Validation

| Field | Rule | Error code |
|---|---|---|
| `from`, `to` | present, `from ≤ to`, span ≤ 366 days | `VALIDATION_ERROR` |
| `lob` | `LIFE` | `UNSUPPORTED_LOB` |

## 6. Open items

| # | Item | Owner |
|---|---|---|
| R-1 | Read-model store (§4) | Mahesh |
| R-2 | Report definitions (columns, stage definitions, on- vs off-platform) | Rajal |
| R-3 | Off-platform policy feed: source, format, frequency | Rajal / bank |
| R-4 | Report retention and export controls | Shailja |
