# payment-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Amit (engineering), Mahesh (architecture) and Finance review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[PG interaction](../../api-reference/interactions/payment-to-payment-gateway.md) ·
[Scenario 12](../../api-reference/scenarios/12-payment-and-reconciliation.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes, placeholder `V1`). 0 / 8 operations (all `PLANNED`).
Maturity ≈ 5 %.

## 2. Scope for this build

Payment session with holds on proposal and quote, customer-device link (C4), PG callback ingestion
(signature, replay window, dedupe), reconciliation (`pgTxnId` AND amount), `UNCERTAIN` / break
handling, hold release on the four no-money outcomes, maker-checker refunds, list API for repair
sweeps, outbox incl. `PaymentReconciled` with the D8 ids.
**E1:** a payment can only start for a proposal in `AWAITING_PAYMENT`, which proposal reaches only
from `UW_APPROVED`; the link is generated after UW approval.
The PG wire (signature scheme, callback body, settlement file) is **unknown until the S10 PG
contract** — build behind a `PaymentGatewayPort` with a stub; do not invent the PG format (PAY-1).

## 3. Operations

[openapi.yaml](./openapi.yaml): `createPaymentSession`, `listPayments`, `getPayment`,
`receivePgCallback`, `startReconciliation`, `getReconciliationRun`, `requestRefund`, `approveRefund`.

## 4. Data owned

Schema `payment` — design [`10-payment.sql`](../../data-architecture/schemas/10-payment.sql) with
LLD §10 deltas: `payment` (`quote_id`, `proposal_id`, `application_number`, `method`, `paid_at`,
partial unique `ux_payment_active_per_proposal`), `payment_attempt`, `pg_callback_receipt`
(INSERT-only, `UNIQUE(pg_txn_id, callback_type)`), `refund` (+ state), `outbox_event` (ADR-025
columns), `processed_event`, `idempotency_record`.

## 5. Rule sets and configuration

| Value | Status |
|---|---|
| Refund auto-approval threshold | **No value** — every refund needs a checker (fail closed) until Finance sets it (PAY-2) |
| Reconciliation SLA (`CAPTURED` → break) | **No value** — breaks raised only manually until set (PAY-2) |
| Authorisation window (`AUTHORISED` → `UNCERTAIN`) | **No value** (PAY-3) |
| Link expiry | From the PG session (S10) |

None of these has a rule set id yet; they must be configuration (playbook §6), not code.

## 6. Events

Produced (catalogue): `PaymentLinkIssued`, `PaymentAuthorised`, `PaymentCaptured`,
`PaymentDeclined`, `PaymentExpired`, `PaymentReconciled` (D8 payload: `paymentId`, `quoteId`,
`proposalId`, `journeyId`, `applicationNumber`, …), `PaymentReconciliationBreak`; LLD-only
(**not in catalogue**, PAY-4): `PaymentUncertain`, `PaymentRejected`, `RefundInitiated`,
`PaymentRefunded`; `AuditRecorded`. Consumed: `ProposalDeclined` (refund **task**, never auto-refund).

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| proposal `holdProposalForPayment` / `releaseProposalPaymentHold` | per proposal contract | Stub per [proposal openapi](../proposal-service/openapi.yaml): ids, `409 PROPOSAL_NOT_PAYABLE`, `409 PAYMENT_ALREADY_IN_PROGRESS` |
| quotation `holdForPayment` / `releasePaymentHold` | per quotation contract | Stub per [quotation openapi](../quotation-service/openapi.yaml): premium + tax, `409 QUOTE_EXPIRED` |
| customer `getRegisteredContact` | 1 s | Stub contact |
| AU Bank PG session (S-13) | 3 s / 15 s, **no retry** | `PaymentGatewayPort` stub (S10 unknown) |
| notification (link) | async via `PaymentLinkIssued` | none needed |

## 8. Invariants

`INV-PAY-01` customer-device only (`403 PAYMENT_DEVICE_ISOLATION` + security event) · `INV-PAY-02`
one non-terminal payment per proposal · `INV-PAY-03` amount = premium + tax from the quote hold ·
`INV-PAY-04` no new attempt while `AUTHORISED`/`UNCERTAIN` · `INV-PAY-05` reconcile on `pgTxnId` AND
amount · `INV-PAY-06` maker-checker refunds · no timeout ever resolves a money question · holds
released only on `DECLINED`, `EXPIRED`, `REJECTED`, `REFUNDED`-from-break · C4 gate class 100 % branch.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-PAY-01 | Body carrying amount or destination → `422 VALIDATION_ERROR` | LLD §5 |
| AT-PAY-02 | Proposal not `AWAITING_PAYMENT` (e.g. `UNDER_WRITING`) → `409 PROPOSAL_NOT_PAYABLE` | E1 |
| AT-PAY-03 | Amount = quote hold premium + tax to the paise; mismatch → `422 PREMIUM_MISMATCH` | `INV-PAY-03` |
| AT-PAY-04 | Destination resolves to an RM / employee contact → `403 PAYMENT_DEVICE_ISOLATION` + security event | `INV-PAY-01` |
| AT-PAY-05 | Second non-terminal payment for a proposal → `409 PAYMENT_ALREADY_IN_PROGRESS` (partial unique on PostgreSQL) | `INV-PAY-02` |
| AT-PAY-06 | New attempt while prior `UNCERTAIN` → `409 PAYMENT_STATE_UNCERTAIN` | `INV-PAY-04` |
| AT-PAY-07 | Callback with bad signature / outside replay window → `401`; duplicate callback → stored result, `duplicate=true` | LLD §4.2 |
| AT-PAY-08 | Settlement matching `pgTxnId` but not amount → not `RECONCILED` | `INV-PAY-05` |
| AT-PAY-09 | `RECONCILED` emits one `PaymentReconciled` with the five D8 ids in the same transaction | D8 |
| AT-PAY-10 | `DECLINED` releases both holds; `UNCERTAIN` / `CAPTURED` / `RECONCILIATION_BREAK` release nothing | LLD §3 |
| AT-PAY-11 | Refund approved by the maker → `403`; no threshold configured → every refund needs a checker | `INV-PAY-06`, PAY-2 |
| AT-PAY-12 | PG session refused → `REJECTED`, `PaymentRejected`, holds released | LLD §4.1 step 6 |
| AT-PAY-13 | `listPayments?state=RECONCILED&since=` pages by `(state, reconciled_at, payment_id)`, ids only | LLD §4.6 |
| AT-PAY-14 | `ProposalDeclined` opens a refund task, no automatic refund | LLD §4.5 |

## 10. Build order

**Tier 7.** Real dependencies: proposal, quotation, customer, PG (S10). Startable now with stubs.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| PAY-1 | PG signature scheme, callback body, settlement file format (S10 PG contract) | Bank PG team, Amit | Real PG adapter; go-live |
| PAY-2 | Refund auto-approval threshold; reconciliation SLA | Finance | Automation only (fail-closed defaults) |
| PAY-3 | Authorisation window `AUTHORISED` → `UNCERTAIN` | Finance, Amit | `UNCERTAIN` transition by timer |
| PAY-4 | Catalogue entries for `PaymentUncertain`, `PaymentRejected`, `RefundInitiated`, `PaymentRefunded` | Events owner | Publishing them |
| PAY-5 | Rule-set / configuration ids for the values in §5 | Mahesh | Configuring them |
