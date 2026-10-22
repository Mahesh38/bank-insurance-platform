# policy-issuance-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Amit (engineering), Mahesh (architecture), Aarti (DBA) and Shailja (TAT) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Scenario 13](../../api-reference/scenarios/13-policy-issuance-and-sold.md) · [Scenario 14](../../api-reference/scenarios/14-provider-status-sync.md) ·
[Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes, placeholder `V1`). 0 / 7 operations (all `PLANNED`).
Maturity ≈ 5 %.

## 2. Scope for this build

Start tracking on `PaymentReconciled` (after `RECONCILED`, `INV-POL-01`), status poll through the Hub
(S-16) or callback, append-only `policy_status_history`, one-time issued facts, documents to S3,
`ACTIVE` only after documents + audit, refund closure, repair sweep, TAT warning / breach events.
Round-2 values (all configuration in `PROPOSAL.TAT`): poll interval **4 h**, warning at **day 5**,
decision **7 calendar days** (C4). The C4 start event is **proposal receipt**, which under E1 is
before payment — see POL-1 before building the clock.

## 3. Operations

[openapi.yaml](./openapi.yaml): `startIssuanceTracking`, `findPolicies`, `getPolicy`,
`getPolicyStatusHistory`, `receiveStatusCallback`, `getPolicyDocument`, `ingestOffPlatformPolicy`;
plus the `PaymentReconciled` consumer (same handler as `startIssuanceTracking`).

## 4. Data owned

Schema `policy` — design [`11-policy.sql`](../../data-architecture/schemas/11-policy.sql) with LLD
§11 deltas: `policy` (nullable `policy_number` until `ISSUED` + CHECK, TAT and poll columns,
`UNIQUE(proposal_id)`), `policy_status_history` (append-only), `policy_document_ref`,
`idempotency_record`, `processed_event`, `outbox_event`; grants for `app_policy` (missing in the
design until round 1). S3 prefix with Object Lock for PDFs.

## 5. Rule sets

`PROPOSAL.TAT` (parameters only): `decisionDays` 7, `warningAt` (day 5, round-2 default),
`startEvent`, `dayBasis` (calendar, C4), `statusPollInterval` (4 h, round-2 default). The version is
stored on the policy (`tat_rule_set_version`) so a later change never moves an existing clock. The
seed in `libs/bank-rule-engine` may still carry `warningAtDays` unset — round 2 is updating it.
Status map (1SB `bankStatus` → history status, LLD §3) is a **seed proposal** owned by the Hub
routing owner (POL-4).

## 6. Events

Produced: `PolicyStatusChanged`, `PolicyIssued`, `ProposalDeclined`, `PolicyIssuanceFailed`,
`IssuanceTatEscalated` (`level` `WARNING` | `BREACHED`) (catalogue); `IssuanceTrackingStarted`,
`PolicyClosed` (LLD / audit LLD — **not in catalogue**, POL-5); `AuditRecorded`.
Consumed: `PaymentReconciled`, `PaymentRefunded`.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| payment `getPayment`, `listPayments` | — | Stub per [payment openapi](../payment-service/openapi.yaml) (`RECONCILED` and not) |
| proposal `getProposal` | — | Stub per [proposal openapi](../proposal-service/openapi.yaml) |
| quotation `getQuote` (selected offer facts) | — | Stub per [quotation openapi](../quotation-service/openapi.yaml) |
| integration-hub `runProviderQuery` (`APPLICATION_STATUS`, document) | 3 s / 15 s, bounded retry (read) | Status sequence fixtures (`UNDER_REVIEW` → `ISSUED` / `DECLINED`) |
| S3 (documents) | — | Local file / in-memory adapter |

## 8. Invariants

`INV-POL-01` created only after `RECONCILED` (`409 PAYMENT_NOT_RECONCILED`) · `INV-POL-02` policy
number unique per insurer, set once (guarded UPDATE) · `INV-POL-03` `ACTIVE` only after documents and
audit · `INV-POL-04` append-only history (grants) · `INV-POL-05` off-platform ingest has null
`leadId` · one policy per proposal · tracking continues after a TAT breach · no cross-schema reads.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-POL-01 | `PaymentReconciled` for a non-`RECONCILED` payment → `409 PAYMENT_NOT_RECONCILED`, no row | `INV-POL-01` |
| AT-POL-02 | Same event twice → one policy (`UNIQUE proposal_id`), one `APPLICATION_RECEIVED` row | ADR-025 §4 |
| AT-POL-03 | Poll returns the same status → only `last_polled_at` changes; changed → one history row + `PolicyStatusChanged` | LLD §4.2 |
| AT-POL-04 | `ISSUED` → policy number, dates set once; a second `ISSUED` with another number → refused | `INV-POL-02` |
| AT-POL-05 | `ACTIVE` not reached before document ref + audit confirmation | `INV-POL-03` |
| AT-POL-06 | Clock with `decisionDays 7`, `warningAt 5`: day 5 → one `IssuanceTatEscalated WARNING`; day 7 undecided → one `BREACHED`; tracking continues | C4, round-2 defaults |
| AT-POL-07 | `PROPOSAL.TAT` republished mid-flight → existing clocks unchanged (`tat_rule_set_version`) | LLD §6 |
| AT-POL-08 | `PROPOSAL.TAT` unresolvable → `CONFIGURATION_UNRESOLVABLE`, ops paged, no guessed interval | LLD §4.2 |
| AT-POL-09 | `DECLINED` → `ISSUANCE_REJECTED`, `ProposalDeclined` | D8 |
| AT-POL-10 | Callback from a caller other than the Hub → `403 SERVICE_IDENTITY_REJECTED` | LLD §5 |
| AT-POL-11 | On PostgreSQL `app_policy` cannot UPDATE / DELETE history | `INV-POL-04` |
| AT-POL-12 | Off-platform ingest with a `leadId` → refused | `INV-POL-05` |
| AT-POL-13 | Two scheduler replicas poll each due policy once (lease / `SKIP LOCKED`) | LLD §4.2 |

## 10. Build order

**Tier 8.** Real dependencies: payment, proposal, quotation, Hub. Startable now with stubs.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| POL-1 | **TAT clock owner under E1 + C4**: the 7-day insurer decision runs from proposal receipt, i.e. before payment (proposal `UNDER_WRITING`); this LLD starts it at `PaymentReconciled`. Decide which service tracks pre-payment UW TAT and whether policy-issuance keeps a post-payment issuance clock | Mahesh, Shailja | The TAT clock only (build tracking, history and issuance meanwhile) |
| POL-2 | Does 1SB offer a status callback (else polling only) | Mahesh, 1SB | Callback path at go-live |
| POL-3 | Post-payment counter-offer handling | Rajal | That path |
| POL-4 | Hub status-map seed (LLD §3); `BankApplicationStatus.CLOSED` ambiguity | Mahesh | Correct history mapping |
| POL-5 | Catalogue entries for `IssuanceTrackingStarted`, `PolicyClosed` | Events owner | Publishing them |
| POL-6 | S3 bucket with Object Lock (S09) | Shivanshi | Document storage at go-live |
