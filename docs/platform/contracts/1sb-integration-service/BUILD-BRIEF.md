# 1sb-integration-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Amit (engineering) and Deepali (security) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Adapter to 1SB](../../api-reference/interactions/onesb-adapter-to-1sb.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Implemented** (134 main / 75 test classes), phase-1 coverage floor 90 % line / 70 % branch |
| Contract operations | 13 / 13 coded (quote, criteria, proposal schema + submit, payments, status, masters, product UI, ULIP funds) |
| Gaps (LLD §8) | Idempotency store per pod (memory); filter error content type not problem+json; Spring 400 without envelope; persistence failures unmapped `500`; no inbound caller auth; age 18–75 / term 1–80 hard-coded; audit only logged; 1SB read timeout 30 s (Hub needs ≤ 20 s) |

Maturity ≈ 80 %.

## 2. Scope for this build

1. Read timeout ≤ 20 s for submit paths (Hub LLD §8; HUB-4) — property with env override.
2. Shared idempotency store (bank-persistence job `idempotencyKey` or own table — decide with the
   owner, ONE-2) and problem+json from the filter.
3. Map persistence failures and missing params to the envelope.
4. Audit: replace `LoggingAuditEventPublisher` with `AuditRecorded` events (ADR-026 accepted, E2);
   the adapter has no datasource, so the transport is ONE-3.
5. Move age / term limits to configuration (ADR-024) — needs a product rule set id (ONE-4); until
   then keep the constants and list them.
6. `GET /v1/jobs?idempotencyKey=` **only after** it is added to this contract (HUB-2).
7. The payment-link operation stays but is **not routed** in R0 (D8: premium via AU Bank PG).

## 3. Operations

[openapi.yaml](./openapi.yaml): `createQuote`, `getQuote`, `getEligibilityCriteria`,
`submitEligibilityCriteria`, `getProposalSchema`, `submitProposal`, `getProposal`,
`createPaymentSession`, `getApplicationStatus`, `lookupMasterData`, `getProductUiData`,
`listUlipFunds`, `getUlipFundPerformance`.

## 4. Data owned

None. Jobs, offers, poll attempts, raw payloads, payment sessions are written to
bank-persistence-service over HTTP.

## 5. Rule sets

None today. Validation limits listed in LLD §4 are constants (ONE-4).

## 6. Events

None published. Audit actions (`QUOTE_CREATED`, `PROPOSAL_SUBMITTED`, `CONSENT_REF_MISSING`,
`PAYMENT_URL_RETRIEVED`, `APPLICATION_STATUS_CHECKED`) must become durable (ONE-3).

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| 1SB via Apigee | connect 3 s / read ≤ 20 s (target), breaker 5 failures / 30 s, no retry on submit | 1SB UAT; WireMock fixtures in tests |
| bank-persistence `/internal/v1/*` (`X-Internal-Service-Key`) | caller budget | Built |

Inbound: integration-hub only (target), 25 s submit budget.

## 8. Invariants

`INV-DIS-01` `distributorId` never caller-supplied · no offers / application number leaked while in
progress · consent reference before proposal submit (C2) · evidence stored before regulated calls ·
HTTPS payment URLs only · submits never auto-retried · `INV-ACL-01` 1SB types only in `adapter.onesb.*`.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-1SB-01 | 1SB slower than the configured read timeout on submit → job stays `RUNNING`, poller recovers; no resend | Hub LLD §8 |
| AT-1SB-02 | Same key across two pods (shared store) → replay, not a second 1SB call | LLD §8 #3 |
| AT-1SB-03 | Filter refusals are `application/problem+json` with the envelope | LLD §8 #3 |
| AT-1SB-04 | Persistence unavailable during proposal evidence write → `503`, nothing sent to 1SB | LLD §3.3 |
| AT-1SB-05 | Proposal without `consentRef` → `403 CONSENT_REQUIRED` + audit `CONSENT_REF_MISSING` (durable) | C2, LLD §3.3 |
| AT-1SB-06 | Quote validator table LLD §4 — one test per row | LLD §4 |
| AT-1SB-07 | `PARTIAL` poll result returns offers and partial errors | LLD §3.2 |
| AT-1SB-08 | Every audit action produces a durable `AuditRecorded` (once ONE-3 is decided) | ADR-026 |
| AT-1SB-09 | Existing suite and 90 / 70 coverage floor stay green | QA-001 |

## 10. Build order

**Tier 3** (provider edge). No platform upstream except bank-persistence (exists). Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| ONE-1 | Inbound caller authentication (mesh mTLS) | Deepali / Shivanshi | Go-live |
| ONE-2 | Shared idempotency store location | Amit | Multi-replica safety |
| ONE-3 | Audit transport for a datasource-less adapter (via bank-persistence or an outbox owned elsewhere) under ADR-026 | Mahesh | Durable adapter audit |
| ONE-4 | Product rule set for age / term limits (ADR-024) | Rajal | Removing constants |
| ONE-5 | `QUOTE_EXPIRED` answered `410` vs catalogue `409` | Engineering / QA | Contract alignment |
| ONE-6 | Contract additions requested by the Hub: `GET /v1/jobs?idempotencyKey=` (HUB-2), insurer code mapping (HUB-3) | 1SB adapter owner | Hub reconciliation |
