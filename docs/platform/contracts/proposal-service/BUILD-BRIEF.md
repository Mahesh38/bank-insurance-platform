# proposal-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Amit (engineering), Mahesh (architecture) and Aarti (DBA) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[DynamoDB design](../../data-architecture/schemas/20-dynamodb-working-payload.md) ·
[Scenario 11](../../api-reference/scenarios/11-proposal-and-underwriting.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Persistence built** (44 main / 9 test classes): `WorkingProposal` domain (create, answers, submit, acknowledge, underwriting, withdraw, payment in flight, materialise), DynamoDB store, Aurora materialiser |
| Contract operations | `onPaymentReconciled` BUILT; the other 10 PLANNED |
| Persistence | Flyway V1, `V2__proposal_structured`, vendor `V3__proposal_application_number` |
| Gaps (LLD §4) | Envelope stores `formValues` without `questionText` / `schemaVersion`; no counter-offer field; no `proposal_answer` / `proposal_status_history`; OUTBOX without `published` flag |

Maturity ≈ 25 %.

## 2. Scope for this build

The 10 PLANNED operations; C2 consent gate at submit; Hub submit **without retry** and status
sync; underwriting states before payment; payment hold/release; materialise with
`proposal_answer` and first `proposal_status_history` row; post-payment history consumers;
read-path switch; repair sweep. Round 2:

| Decision | Effect |
|---|---|
| **E1** | `AWAITING_PAYMENT` only from `UW_APPROVED` (`INV-PRP-03` stands). LLD open item P1 (pay before approval) is **closed: no** |
| **C4** | Insurer decision TAT is 7 calendar days **from proposal receipt** — that clock now runs **before** payment, while this service is in `UNDER_WRITING` (PRP-1) |

## 3. Operations

[openapi.yaml](./openapi.yaml): `getFormSchema`, `createProposal`, `getProposal`, `saveAnswers`,
`submitProposal`, `decideCounterOffer`, `withdrawProposal`, `holdProposalForPayment`,
`releaseProposalPaymentHold`, `getProposalStatusHistory`; `onPaymentReconciled` (built shim).

## 4. Data owned

DynamoDB `{env}-proposal-working` (+ `heldByPaymentId`, per-answer `questionText`,
`schemaVersion`, counter-offer). Aurora schema `proposal` — design
[`09-proposal.sql`](../../data-architecture/schemas/09-proposal.sql): `proposal`, `proposal_answer`
(INSERT-only, `answer_enc` per row), `proposal_status_history` (append-only, `UNIQUE(source_event_id)`),
`uw_requirement`, `uw_document_ref`, `idempotency_record`; grants per LLD §11.

## 5. Rule sets and configuration

`CONSENT.POLICY` via consent-service at `BEFORE_PROPOSAL_SUBMIT` (`CNS-SHR`, 30 days, named
insurer). Issuance mode and form rules are "configuration by (lob, insurer, product)" with **no rule
set defined** (PRP-5) → fail closed; `ISSUANCE_MODE_INVALID` if unresolved.

## 6. Events

Produced: `ProposalCreated`, `ProposalSubmitted`, `ProposalAcknowledged`, `UnderwritingDecision`,
`ProposalWithdrawn`, `ProposalMaterialised` (catalogue); `ProposalBlockedNoConsent` (audit
mandatory), `ProposalExpired`, `ProposalStatusChanged` (LLD §9 — **not in catalogue**, PRP-6);
`AuditRecorded`. Consumed: `PaymentReconciled`, `PaymentDeclined`/`Expired`/`Rejected`,
`QuoteExpired`, `PolicyIssued`, `ProposalDeclined`, `IssuanceTatEscalated`.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| quotation `getQuote` | per quotation contract | Stub per [quotation openapi](../quotation-service/openapi.yaml): `SELECTED` quote, expired quote, `404` |
| consent `checkConsents` (`BEFORE_PROPOSAL_SUBMIT`, `insurerCode`) | 500 ms, fail closed | satisfied / missing `CNS-SHR` / `503` |
| integration-hub `submitProviderRequest` (`PROPOSAL_SUBMIT`), `runProviderQuery` (`PROPOSAL_FORM_SCHEMA`, `APPLICATION_STATUS`) | submit 3 s / 30 s **no retry**; reads bounded retry | `202`, `SUBMISSION_UNCERTAIN`, `FAILED` with `PROPOSAL_REJECTED`, status values |
| administration-config | 300 ms | Sample configuration |

## 8. Invariants

`INV-PRP-01` C2 at submit · `INV-PRP-02` selected offer of a `SELECTED` quote · `INV-PRP-03`
(E1) · `INV-PRP-04` no withdrawal at/after `AWAITING_PAYMENT` · `INV-PRP-05` PII only encrypted ·
`INV-PRP-06` issuance mode mandatory · one open proposal per journey, unique application number ·
post-payment immutability (frozen item, INSERT-only rows, append-only history) · no automatic
re-submit (F-04).

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-PRP-01 | Submit without `CNS-SHR` for the named insurer → `403 CONSENT_REQUIRED`, stays `DRAFT`, `ProposalBlockedNoConsent` | `INV-PRP-01` |
| AT-PRP-02 | Hub submit times out → proposal not re-submitted; poll exhausted → `SUBMISSION_FAILED` + ops task | LLD §4.3, F-04 |
| AT-PRP-03 | `UNDER_WRITING → AWAITING_PAYMENT` directly → `409 ILLEGAL_TRANSITION`; from `UW_APPROVED` → allowed | E1, `INV-PRP-03` |
| AT-PRP-04 | `saveAnswers` stores `questionText` from the schema version shown, never from the caller | D8, LLD §4.2 |
| AT-PRP-05 | Envelope over 350 KB → `413 PAYLOAD_TOO_LARGE` | LLD §5 |
| AT-PRP-06 | Withdraw at `AWAITING_PAYMENT` → `409 WITHDRAWAL_NOT_PERMITTED` | `INV-PRP-04` |
| AT-PRP-07 | Hold when not `AWAITING_PAYMENT` → `409 PROPOSAL_NOT_PAYABLE`; second payment → `409 PAYMENT_ALREADY_IN_PROGRESS` | LLD §4.5 |
| AT-PRP-08 | `PaymentReconciled` (duplicate) → one `proposal`, one `proposal_answer` per question, one history row `PAID`, one `ProposalMaterialised` | D8 |
| AT-PRP-09 | `PolicyIssued` before materialise → retried, never a history row without its parent | LLD §4.7 |
| AT-PRP-10 | On PostgreSQL `app_proposal` cannot UPDATE / DELETE `proposal_answer` or history | LLD §11 grants |
| AT-PRP-11 | Duplicate application number → `409 CONFLICT` | `ux_proposal_application` |
| AT-PRP-12 | Schema assertion: no plaintext PII column in `proposal` | `INV-PRP-05` |

## 10. Build order

**Tier 6** (after quotation). Real dependencies: quotation, consent, Hub. Startable now with stubs.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| PRP-1 | Under E1 + C4 the insurer-decision TAT (7 days from receipt) elapses **before** payment; policy-issuance LLD starts `PROPOSAL.TAT` at `PaymentReconciled`. Which service runs the pre-payment UW-decision clock? | Mahesh (+ Shailja on the start event) | TAT tracking; not the rest of the build |
| PRP-2 | Envelope answer shape + counter-offer field (code change, LLD P2) | Amit | Materialising answers |
| PRP-3 | Accepted counter-offer with a new premium → re-quote rule | Rajal | Counter-offer path |
| PRP-4 | Is a TAT breach terminal (`ISSUANCE_FAILED`) or escalation only | Shailja | That history row |
| PRP-5 | Proposal configuration rule set (issuance mode, form rules) | Mahesh | `createProposal` at go-live |
| PRP-6 | Catalogue entries for `ProposalBlockedNoConsent`, `ProposalExpired`, `ProposalStatusChanged` | Events owner | Publishing them |
| PRP-7 | Register `ISSUANCE_MODE_INVALID` if not present | bank-common-error owner | Exact code |
