# quotation-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Amit (engineering) and Mahesh (architecture) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[DynamoDB design](../../data-architecture/schemas/20-dynamodb-working-payload.md) ·
[Scenario 10](../../api-reference/scenarios/10-multi-quote-and-selection.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Persistence built** (47 main / 9 test classes): `WorkingQuote` domain (offers, select, materialise), DynamoDB store with guards and envelope encryption, Aurora materialiser |
| Contract operations | `getSelectedLegacy` (BUILT, deprecated) and `onPaymentReconciled` (BUILT); `createQuote`, `getQuote`, `selectOffer`, `holdForPayment`, `releasePaymentHold` PLANNED |
| Persistence | Flyway `V1__quotation_structured`, vendor `V2__quotation_offer_selected` |
| Gaps | OUTBOX items lack `published` flag / envelope, TTL set at write (catalogue §6.2); no stream publisher / sweeper; `INVALID` offers skipped at materialise; no `product_class` / `selected_offer_id` columns |

Maturity ≈ 30 %.

## 2. Scope for this build

The five PLANNED operations (C1 gate via suitability, consent checkpoint via consent, Hub dispatch,
poll + offer ingestion, select, payment hold/release, lazy expiry), `QuoteConverted` at materialise,
`INVALID` offers kept, OUTBOX publisher + 5-minute sweeper, repair sweep, read-path switch.
Strict posture: **C1** — the multi-quote checkpoint requires `CNS-QSH` and still-valid DP/SOL
(from `CONSENT.POLICY`, not code); **C5 / C6** come through suitability's gate answer only.

## 3. Operations

[openapi.yaml](./openapi.yaml): `createQuote`, `getQuote`, `selectOffer`, `holdForPayment`,
`releasePaymentHold` (to build); `getSelectedLegacy` (deprecated — retire after payment switches,
QUO-1); `onPaymentReconciled` (built, HTTP shim until MSK).

## 4. Data owned

DynamoDB `{env}-quotation-working` (META, IDEM, OUTBOX, `JOURNEY#…/OPEN_QUOTE`; new attribute
`heldByPaymentId`; optional GSI2 expiry). Aurora schema `quotation` — design
[`08-quotation.sql`](../../data-architecture/schemas/08-quotation.sql): `quote`, `offer`
(INSERT-only), `idempotency_record`; LLD §11 deltas (`product_class`, `selected_offer_id`, relaxed
`ck_offer_positive` for `INVALID`, consumer dedupe).

## 5. Rule sets and configuration

Embeds none. Records `configVersionRef` of what it relied on (`SUITABILITY.GATE` via suitability,
`CONSENT.POLICY` via consent, `HUB.ROUTING` via Hub). Quote validity window and product matrix are
"from configuration" (LLD §4.1 step 4) but **no rule set names them** (QUO-2): fail closed
(`CONFIGURATION_UNRESOLVABLE`) until defined.

## 6. Events

Produced: `QuoteRequested`, `QuoteCompleted`, `QuoteFailed`, `QuoteRejectedNoSuitability`
(audit mandatory), `OfferSelected`, `QuoteConverted`, `QuoteExpired` (LLD — **not in catalogue**,
QUO-3), `AuditRecorded`. Consumed: `PaymentReconciled` (materialise), `PaymentDeclined`,
`PaymentExpired`, `PaymentRejected`, `PaymentRefunded` (release; the last two not in catalogue yet).

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| suitability `getUsableEvaluation` (S-08) | 500 ms, no retry, fail closed | Stub per [suitability openapi](../suitability-service/openapi.yaml): `200`, `403 SUITABILITY_REQUIRED` with each `refusalReason`, timeout |
| consent `checkConsents` (`BEFORE_MULTI_QUOTE`) | 500 ms, no retry | satisfied / missing purposes / `503` |
| integration-hub `submitProviderRequest`, `pollProviderRequest` (S-09/S-11) | 3 s / 30 s, **no retry** on submit; poll 1 s → 30 s cap | `202`, `SUBMISSION_UNCERTAIN`, `COMPLETED`, `PARTIALLY_COMPLETED`, `FAILED`, `TIMED_OUT` |
| suitability `lockEvaluation`, lead BI-generated | — | Stub; lead BI endpoint not contracted (QUO-4) |
| payment `listPayments` (repair sweep) | — | Stub per [payment openapi](../payment-service/openapi.yaml) |
| DynamoDB | — | Existing local store / in-memory adapter |

## 8. Invariants

`INV-QUO-01` (D4 wording: outcome for the class ∈ allowed outcomes, read at the instant) ·
`INV-QUO-02/03` offer ingestion · `INV-QUO-04` select inside validity (lazy expiry) · `INV-QUO-05`
single selected offer · one open quote per journey · hold never lost to TTL · frozen after
materialise · `INV-DIS-01` attribution never caller-supplied · C1 gate class 100 % branch coverage.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-QUO-01 | Suitability refuses → META `REJECTED` + OUTBOX `QuoteRejectedNoSuitability`, `403 SUITABILITY_REQUIRED`, Hub not called | `INV-QUO-01`, SUIT-R25 |
| AT-QUO-02 | Suitability times out → same record, `503 UPSTREAM_UNAVAILABLE`, no quote | SUIT-R26 |
| AT-QUO-03 | Consent missing `CNS-QSH` → `403 CONSENT_REQUIRED` with `missingPurposes`, **nothing written**; retry with same key after capture → `202` | C1, LLD §4.1 step 3 |
| AT-QUO-04 | Body carries `distributorId` → `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` | `INV-DIS-01` |
| AT-QUO-05 | Hub submit is called once with `quote:{quoteId}:submit`; a Hub timeout is not retried | LLD §10 |
| AT-QUO-06 | Offers with non-positive premium → `INVALID`, excluded from selection; 0 valid → `FAILED` + `QuoteFailed` | `INV-QUO-02/03` |
| AT-QUO-07 | Select after `validUntil` → `EXPIRED`, marker released, `QuoteExpired`, `409 QUOTE_EXPIRED` | `INV-QUO-04` |
| AT-QUO-08 | Select sets one `SELECTED`, siblings `NOT_SELECTED`, `INVALID` untouched | `INV-QUO-05` |
| AT-QUO-09 | Hold by payment A, then by payment B → `409 PAYMENT_ALREADY_IN_PROGRESS`; held quote never expires; release by A restores TTL | LLD §4.4 |
| AT-QUO-10 | `UNCERTAIN` / `RECONCILIATION_BREAK` payment events do not release the hold | LLD §4.4 table |
| AT-QUO-11 | `PaymentReconciled` twice → one Aurora quote + every offer (incl. `INVALID`), state `CONVERTED`, one `QuoteConverted` | D8, ADR-025 |
| AT-QUO-12 | `getQuote` after materialise reads Aurora (`recordSource=RECORD`) even if the item still exists | LLD §4.7 |
| AT-QUO-13 | OUTBOX item written `published=false`, no TTL; sweeper republishes after 5 min in `eventId` order | catalogue §6.2 |
| AT-QUO-14 | Repair sweep materialises a reconciled payment with no Aurora row; working item gone → alert `QUOTE_MATERIALIZE_LOST` | LLD §4.8 |

## 10. Build order

**Tier 6.** Real dependencies: suitability, consent, Hub (+ 1SB adapter + persistence), payment for
holds. Startable now with stubs; persistence half already exists.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| QUO-1 | Retire `getSelectedLegacy` once payment uses `payment-hold` | Amit | Nothing |
| QUO-2 | Which rule set holds the quote validity window and product matrix (no ADR-024 §7 entry) | Mahesh, Rajal | `createQuote` at go-live (fails closed) |
| QUO-3 | `QuoteExpired`, `PaymentRejected`, `PaymentRefunded` in the catalogue | Events owner | Publishing / consuming them |
| QUO-4 | Lead "BI generated" call on first successful quote — no lead operation contracted | Rajal + Mahesh | Lead `QUALIFIED` transition |
| QUO-5 | Accept optional GSI2 expiry sweep or lazy expiry only | Aarti | Timeliness of `QuoteExpired` only |
| QUO-6 | `consents:check` response: single vs per-purpose consent id | consent owner, Shailja | `consent_id` column semantics |
| QUO-7 | S09: real DynamoDB table, streams and MSK | Shivanshi | Go-live |
