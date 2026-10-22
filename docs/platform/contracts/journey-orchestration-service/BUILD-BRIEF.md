# journey-orchestration-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture) and Rajal (journey behaviour) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Orchestration pattern](../../api-reference/interactions/journey-orchestration-pattern.md) ·
[Scenario 15](../../api-reference/scenarios/15-failure-and-compensation.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes; no migration). 0 / 15 operations. Maturity ≈ 5 %.
The widest fan-out of any service: it calls lead, customer, consent, suitability, quotation,
proposal, payment and consumes most domain events.

## 2. Scope for this build

Stage machine (LLD §2) with the transition table §2.1, the command pattern (§4: lock, PDP, one
owning call, one local transaction with stage + ref + transition + outbox + idempotency), event
consumers (§7), inactivity sweep, compensation bookkeeping. Round-2 corrections the builder applies:

| Decision | Effect |
|---|---|
| **C1** (strict) | `startNeedAnalysis` checks `BEFORE_SUITABILITY` (DP + SOL required) and captures them if missing before suitability; `BEFORE_MULTI_QUOTE` re-checks DP/SOL and captures `CNS-QSH` |
| **E1** | `PAYMENT_PENDING` only after `UnderwritingDecision` approved (already LLD §2.1) |
| **E4** | `HALTED_CONSENT_WITHDRAWN` accepted with `JOURNEY_HALTED_CONSENT_WITHDRAWN` |
| **C2** | `verifyConsentRelayed` refuses unless `RM_RELAYED` is configured (it is not) |
| **E2** | `SOLD` only when audit completeness says complete (`INV-JRN-05`) |

The checkpoint-to-command mapping (which consent checkpoint each command checks) must come from
the stage → checkpoint wiring in the LLD; checkpoint purposes come only from `CONSENT.POLICY`.

## 3. Operations

[openapi.yaml](./openapi.yaml): `startJourney`, `getJourney`, `applyTransition`,
`retryPartySnapshot`, `startNeedAnalysis`, `saveNeedAnalysisAnswers`, `evaluateSuitability`,
`acknowledgeSuitabilityDisclosure`, `checkOrCaptureConsent`, `verifyConsentRelayed`,
`requestQuote`, `selectOffer`, `createProposal`, `submitProposal`, `initiatePayment`.

## 4. Data owned

Schema `journey` — design [`12-journey.sql`](../../data-architecture/schemas/12-journey.sql) with
LLD §8 deltas: `journey` (nullable `party_snapshot_ref` + CHECK, `product_class`, stage CHECK incl.
`HALTED_CONSENT_WITHDRAWN`), `journey_ref`, `journey_transition` (INSERT-only), `outbox_event`,
`processed_event`, `idempotency_record`; partial unique `ux_journey_lead_open` (PostgreSQL proof).

## 5. Rule sets

| Rule set | Use |
|---|---|
| `JOURNEY.POLICY` | `inactivityHours.<stage>` → `next_action_due_at` — **no seed, values unset** (JRN-4). Unset → `INV-JRN-03` cannot be satisfied → `startJourney` fails closed with `CONFIGURATION_UNRESOLVABLE` until configured |
| `CONSENT.POLICY` (version pin only) | Stored in `config_version_ref`; evaluated by consent-service |

## 6. Events

Produced (topic `journey.journey.v1`): `JourneySold`, `JourneyCompensating` (catalogue);
`JourneyStarted`, `JourneyStageChanged`, `JourneyHalted`, `JourneyAbandoned` (LLD §7 — **not in
catalogue**, JRN-6); `AuditRecorded` per transition (`INV-AUD-02`).
Consumed (idempotent, `processed_event`): `ConsentGranted`, `ConsentWithdrawn`, `ConsentExpired`,
`SuitabilitySuperseded`/`Invalidated`, `QuoteCompleted`/`QuoteFailed`/`QuoteExpired`,
`ProposalSubmitted`/`ProposalAcknowledged`, `UnderwritingDecision`, `PaymentCaptured`,
`PaymentReconciled`, `PolicyIssued`, `PolicyIssuanceFailed`, `ProposalDeclined`.

## 7. Synchronous dependencies (stub each per its own openapi.yaml until BUILT)

| Dependency | Seam / budget | Stub must cover |
|---|---|---|
| identity-authorization PDP | S-02 300 ms | allow, deny reasons, timeout, `ASSIST_ONLY` |
| lead-service `getLead` | S-20 2 s | assigned + cleared lead; held lead; `404`; SP not certified |
| customer-service `takeSnapshot` | S-04 2 s, 1 retry | `201`, `200` replay, `503` (journey holds at `INITIATED`) |
| consent-service `checkConsents`, `startCapture`, `verifyChallenge` | S-06 2 s | satisfied / missing / `503` / `429` |
| suitability-service create / save / evaluate / acknowledge / usages | S-07 3 s | outcomes per class incl. `NOT_SUITABLE`, `INSUFFICIENT_DATA`, `422 SUITABILITY_INCOMPLETE` |
| quotation `createQuote`, `selectOffer` | per quotation contract | `202`, `403 SUITABILITY_REQUIRED`, `403 CONSENT_REQUIRED`, `409 QUOTE_EXPIRED`, `503` |
| proposal `createProposal`, `submitProposal` | no retry on submit | `201`, `202`, `403 CONSENT_REQUIRED`, `409` |
| payment `createPaymentSession` | no retry | `201`, `409 PROPOSAL_NOT_PAYABLE` |
| audit `getJourneyCompleteness` | 2 s, 1 retry | complete / missing steps / `503` |

## 8. Invariants

`INV-JRN-01` legal transitions only (+ integrity alert) · `INV-JRN-02` no decision columns
(ArchUnit) · `INV-JRN-03` horizon on every non-terminal stage · `INV-JRN-04` `COMPENSATING` never
silently closed · `INV-JRN-05` `SOLD` gate · `INV-IDM-01` · one open journey per lead · consent is
re-checked at the point of use, never trusted from the stage.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-JRN-01 | Every (stage, command) pair outside LLD §2.1 → `409 ILLEGAL_TRANSITION` (table-driven test over all pairs) | `INV-JRN-01` |
| AT-JRN-02 | Customer snapshot `503` at start → journey created at `INITIATED`, `nextAction = RETRY_PARTY_SNAPSHOT`; `startNeedAnalysis` → `409` reason `PARTY_SNAPSHOT_MISSING` | LLD §3.1 |
| AT-JRN-03 | `startNeedAnalysis` with DP/SOL missing → capture started, stage unchanged; suitability not called | C1 |
| AT-JRN-04 | `requestQuote` re-checks consent; consent withdrawn since `CONSENT_CAPTURED` → refused, quotation not called | LLD §3.2, C1 |
| AT-JRN-05 | `ConsentWithdrawn(CNS-QSH)` while `QUOTING` → `HALTED_CONSENT_WITHDRAWN`; any later command → `403 JOURNEY_HALTED_CONSENT_WITHDRAWN` | E4, CNS-R29 |
| AT-JRN-06 | `CNS-SHR` withdrawn after submission → no stage change, ops task `CONSENT_WITHDRAWN_POST_SUBMISSION` | CNS-R30 |
| AT-JRN-07 | `initiatePayment` before `UnderwritingDecision` approved → `409` | E1, `INV-PRP-03` |
| AT-JRN-08 | `PolicyIssued` with audit completeness `false` → stays `ISSUED`; with `true` → `SOLD` + `JourneySold` | `INV-JRN-05`, ADR-026 |
| AT-JRN-09 | Duplicate delivery of any consumed event → processed once | ADR-025 §4 |
| AT-JRN-10 | Callee committed, journey transaction fails → retry with the same key upserts the same ref | LLD §4 step 6 |
| AT-JRN-11 | Inactivity sweep never abandons `PAYMENT_*`, `ISSUANCE_PENDING`, `ISSUED`, `COMPENSATING`, `MANUAL_INTERVENTION` | LLD §6 |
| AT-JRN-12 | `PolicyIssuanceFailed` → `COMPENSATING`; exits only to `SOLD` / `COMPENSATED` / `MANUAL_INTERVENTION` | `INV-JRN-04` |
| AT-JRN-13 | Partner rep issues any command → `403 ASSIST_ONLY_ACTOR` | `INV-ACT-02` |
| AT-JRN-14 | `JOURNEY.POLICY` unresolvable → `startJourney` refused, nothing written | `INV-JRN-03`, ADR-024 |

## 10. Build order

**Tier 5** for real integration (after lead, customer, consent, suitability); **startable now**
with stubs for all eight dependencies.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| JRN-1 | S-03 3 s budget vs chained calls (S-07 alone is 3 s) | Mahesh | Budget tuning; not build |
| JRN-2 | S-04 2 s vs S-05 3 s + retry (customer CUS-2) | Mahesh | Nothing (hold at `INITIATED`) |
| JRN-3 | LLD still describes D5 (DP/SOL at multi-quote, `BEFORE_SUITABILITY` empty); strict C1 applies | Contract owner | LLD update |
| JRN-4 | `JOURNEY.POLICY` inactivity horizons per stage — no source value | Rajal | `startJourney` at go-live |
| JRN-5 | BFF has no journey routes (BFF-1) | Mahesh | RM app driving journeys |
| JRN-6 | Catalogue entries for `JourneyStarted`, `JourneyStageChanged`, `JourneyHalted`, `JourneyAbandoned` | Events owner | Publishing them; lead's journey placeholder |
| JRN-7 | Register `JOURNEY_HALTED_CONSENT_WITHDRAWN` in `ErrorCodes` if not present (E4 accepted) | bank-common-error owner | Exact code |
