# policy-issuance-service — LLD (contract companion)

**Status:** AI-DRAFTED 2026-10-21, awaiting Amit (engineering), Mahesh (architecture), Aarti (DBA:
new table, nullable columns, grants) and Shailja (`PROPOSAL.TAT` figure and start event).

[OpenAPI](./openapi.yaml) · [API reference page](../../api-reference/services/policy-issuance-service.md) ·
[Decision D8](../../architecture-review/DECISIONS-2026-10-21.md#d8--payment-issuance-tracking-and-the-long-term-record-d-025) ·
[`ADR-024`](../../architecture-review/ADR-024-configuration-driven-rule-engine.md) ·
[`11-policy.sql`](../../data-architecture/schemas/11-policy.sql) ·
[1SB application status](../../../1sb-insurance-integration/field-guides/application-status.md) ·
[payment contract](../payment-service/LLD.md) · [proposal contract](../proposal-service/LLD.md)

## 1. Purpose and ownership

Tracks the insurer's decision after the premium is reconciled and holds the distributor's record of
the issued policy (the insurer is policy administrator). Starts the insurer-decision clock on
`PaymentReconciled`, follows the 1SB application status (poll S-16 or callback), appends every
status change, writes the issued facts on `ISSUED`, raises `PolicyIssued` / `ProposalDeclined` /
`IssuanceTatEscalated` (the D8 "TatBreached" is `level = BREACHED`), and lets Notification inform the customer. Port 8099, `service-id: policy`.
Aurora schema `policy` (`app_policy`) + S3 prefix for PDFs (Object Lock).

**No cross-schema reads.** `insurer_code`, `product_code`, `sum_assured`, `premium`, `frequency`,
`product_class` come from the **structured quote** via `GET quotation /internal/v1/quotes/{quoteId}`
(selected offer); `application_number`, `lob`, `issuanceMode` from `GET proposal
/internal/v1/proposals/{proposalId}`; payment facts from `GET payment /internal/v1/payments/{paymentId}`.
Their read-path switch (DynamoDB before materialise, Aurora after) makes this safe even when
policy-issuance consumes `PaymentReconciled` before quotation has materialised.

## 2. API summary

| Operation | Path | Caller |
|---|---|---|
| (consumer) `PaymentReconciled` | `{env}.payment.payment.v1` | MSK |
| `startIssuanceTracking` | `POST /internal/v1/policies` | repair sweep, ops (same handler as the consumer) |
| `findPolicies` | `GET /internal/v1/policies?proposalId|journeyId` | Journey, BFF |
| `getPolicy` | `GET /internal/v1/policies/{policyId}` | BFF (IPR gated) |
| `getPolicyStatusHistory` | `GET …/{policyId}/status-history` | BFF, ops |
| `receiveStatusCallback` | `POST /internal/v1/policies/status-callbacks` | integration-hub (1SB callback, if offered) |
| `getPolicyDocument` | `GET …/{policyId}/documents/{documentId}` | BFF |
| `ingestOffPlatformPolicy` | `POST /internal/v1/policies:ingest` | MIS ingest (CR-013) |

All PLANNED (skeleton).

## 3. Policy state and issuance status

The policy row is created at `PaymentReconciled` (after `RECONCILED`, so `INV-POL-01` holds) in
`PENDING_ISSUANCE` with `policy_number` null; the D8 status vocabulary lives in
`policy_status_history` and the denormalised `issuance_status` column.

| History status (D8) | Typical 1SB `applicationStatus` (normalised by the Hub status map) | `policy.state` |
|---|---|---|
| `APPLICATION_RECEIVED` | `PROPOSAL_SUBMITTED`, `PAYMENT_SUCCESS` (and the first row at start) | `PENDING_ISSUANCE` |
| `UNDER_REVIEW` | `UNDERWRITING`, `SCRUTINY`, `PRE_CONVERSION`, `REQUIREMENT_VERIFICATION`, `ACCEPTED` | `PENDING_ISSUANCE` |
| `REQUIREMENT_RAISED` | `REQUIREMENTS_PENDING`, `DOCUMENT_UPLOAD_*`, `KYC_FAILED`, `COUNTER_OFFER`, `AWAITING_CLIENT_APPROVAL` | `PENDING_ISSUANCE` |
| `ISSUED` | `POLICY_ISSUED` | `ISSUED` → `CONFIRMED` → `ACTIVE` (docs + audit, `INV-POL-03`) |
| `DECLINED` | `REJECTED`, `CANCELLED` | `ISSUANCE_REJECTED` |
| `REFUNDED` | (event `PaymentRefunded`) | unchanged; `closed_at` set |

The mapping column is a **seed proposal** for the Hub's status map (configuration, `HUB.ROUTING`
owner Mahesh); the raw provider status is always stored (`provider_status`). A post-payment
`COUNTER_OFFER` is open item PI3.

## 4. Flows

### 4.1 Start tracking on `PaymentReconciled`

1. Dedupe on `eventId` (`processed_event`). 2. `GET payment` → `RECONCILED` (else
`409 PAYMENT_NOT_RECONCILED`). 3. `GET proposal`, `GET quote` (selected offer). 4. Resolve
`PROPOSAL.TAT` (fail closed): `decisionDays` = **7** (IRDAI Master Circular on Protection of
Policyholders' Interests, 5 Sep 2024 — D8; Shailja to confirm), `warningAt` (configurable, **no seed
value** in any source — unset means no warning event), start event = payment reconciliation (owner
D8 sequence; Shailja confirms receipt vs payment), day basis unset in the sources — the tracker
counts calendar days, the reading that breaches earliest, until Shailja confirms.
5. One transaction: INSERT `policy` (`PENDING_ISSUANCE`, `tat_started_at = reconciledAt`,
`tat_due_at`, `tat_warn_at`, ids, product facts) ON CONFLICT (`proposal_id`) DO NOTHING; INSERT
history `APPLICATION_RECEIVED`; outbox `IssuanceTrackingStarted`.

### 4.2 Poll (S-16) or callback

A scheduler (ShedLock, one leader) selects `PENDING_ISSUANCE` policies whose `next_poll_at <= now`
and calls Hub `POST /internal/v1/provider-queries {operation: APPLICATION_STATUS, applicationNumber,
insurerCode}` (3 s / 15 s, bounded retry — a read). The poll interval is configuration
(`PROPOSAL.TAT.parameters.statusPollInterval`, **no value in any source**; until Mahesh sets it the
tracker raises `CONFIGURATION_UNRESOLVABLE` and pages ops rather than guessing). For an insurer
whose 1SB route supports a callback, `receiveStatusCallback` delivers the same normalised status
and polling for that insurer stops (`HUB.ROUTING` flag).

Per result: same status as the last row → update `last_polled_at` only; changed → append a history
row (never UPDATE a sold fact) and update `issuance_status`; `ISSUED` → one-time UPDATE of
`policy_number`, `issued_at`, `risk_commencement_date`, `maturity_date`, `state = ISSUED`,
`tat_decided_at` (guard `policy_number IS NULL`, `INV-POL-02`) + outbox `PolicyIssued`; then
document download via Hub → S3 → `policy_document_ref` → audit confirmed → `ACTIVE`.
`DECLINED` → `state = ISSUANCE_REJECTED`, `tat_decided_at`, outbox `ProposalDeclined`.

### 4.3 TAT

`now ≥ tat_warn_at` (if set) and undecided → `IssuanceTatEscalated {level: WARNING}` once.
`now ≥ tat_due_at` and undecided → `tat_breached_at`, outbox `IssuanceTatEscalated {level: BREACHED}` (ops escalation, Compliance MIS, Audit). Tracking continues
after a breach — the insurer may still decide.

```mermaid
sequenceDiagram
    participant PAY as payment
    participant ISS as policy-issuance
    participant PAYAPI as payment API
    participant PRP as proposal API
    participant Q as quotation API
    participant H as integration-hub
    participant N as notification
    PAY-->>ISS: PaymentReconciled {paymentId, quoteId, proposalId, journeyId, applicationNumber}
    ISS->>PAYAPI: GET /payments/{id} (RECONCILED?)
    ISS->>PRP: GET /proposals/{id}
    ISS->>Q: GET /quotes/{id} (selected offer: insurer, product, premium, sum assured)
    ISS->>ISS: resolve PROPOSAL.TAT (decisionDays 7) -> INSERT policy PENDING_ISSUANCE + history APPLICATION_RECEIVED + outbox IssuanceTrackingStarted
    loop every statusPollInterval until ISSUED / DECLINED
        ISS->>H: provider-queries APPLICATION_STATUS (S-16)
        H-->>ISS: bankStatus + raw status
        alt changed
            ISS->>ISS: append policy_status_history
        end
        opt now >= tat_due_at and undecided
            ISS->>ISS: tat_breached_at + outbox IssuanceTatEscalated BREACHED (once)
        end
    end
    Note over H,ISS: callback route: POST /internal/v1/policies/status-callbacks replaces the poll for that insurer
    alt ISSUED
        ISS->>ISS: UPDATE policy number, issued_at, risk_commencement_date, maturity_date; outbox PolicyIssued
        ISS-->>N: PolicyIssued -> customer informed
    else DECLINED
        ISS->>ISS: state ISSUANCE_REJECTED; outbox ProposalDeclined
        ISS-->>N: ProposalDeclined -> customer informed
        ISS-->>PAY: ProposalDeclined -> refund task (maker-checker)
    end
```

### 4.4 Refund and closure

`PaymentRefunded` for the policy's payment → history `REFUNDED`, `closed_at = now`. `closed_at` is
also set on free-look cancellation, surrender or maturity (modelled; not built in R0).

### 4.5 Repair

Daily: `GET payment /internal/v1/payments?state=RECONCILED&since=` → any `proposalId` without a
policy row → run §4.1. Policies `PENDING_ISSUANCE` with `last_polled_at` older than three intervals
→ ops alert.

## 5. Validation rules

| Field / condition | Rule | Error code |
|---|---|---|
| payment | `RECONCILED` | `PAYMENT_NOT_RECONCILED` (409) |
| `Idempotency-Key` | on POST | `MISSING_IDEMPOTENCY_KEY` |
| callback caller | integration-hub mesh identity | `SERVICE_IDENTITY_REJECTED` (403) |
| callback | known `applicationNumber` + `insurerCode` | `RESOURCE_NOT_FOUND` |
| callback `ISSUED` | `policyNumber`, `riskCommencementDate` present | `VALIDATION_ERROR` |
| transition after terminal | only `REFUNDED` after `ISSUED`/`DECLINED` | `ILLEGAL_TRANSITION` (recorded for ops) |
| configuration | `PROPOSAL.TAT` resolvable | `CONFIGURATION_UNRESOLVABLE` |
| `policyNumber` | unique per insurer, immutable | store constraint (`INV-POL-02`) |

## 6. Rule-engine usage

`PROPOSAL.TAT` (ADR-024 §7, content approver Shailja): parameters only — `decisionDays` (7),
`warningAt` (unset), `startEvent` (`PAYMENT_RECONCILED`, to confirm), `dayBasis` (unset),
`statusPollInterval` (unset). Resolved at tracking start; the version is stored on the policy
(`tat_rule_set_version`) so a later change never moves an existing clock.

## 7. Mapping

| Source | Field | `policy.policy` column |
|---|---|---|
| event / payment API | `paymentId`, `journeyId`, `reconciledAt` | `payment_id`, `journey_id`, `tat_started_at` |
| proposal API | `proposalId`, `leadId`, `customerId`, `applicationNumber`, `lob` | `proposal_id`, `lead_id`, `customer_id`, **DDL** `application_number`, `lob` |
| quotation API | `quoteId`, selected offer `insurerCode`, `productCode`, `sumAssured`, `premiumAmount`, `premiumFrequency`, `productClass` | **DDL** `quote_id`, `insurer_code`, `product_code`, `sum_assured`, `premium`, `frequency`, `product_class` |
| status `ISSUED` | `policyNumber`, `issuedAt`, `riskCommencementDate`, `maturityDate` | `policy_number`, `issued_at`, `risk_commencement_date`, `maturity_date` |
| `PaymentRefunded` / lifecycle end | — | **DDL** `closed_at` |
| `PROPOSAL.TAT` | due, warn, breach, decided, version | **DDL** `tat_due_at`, `tat_warn_at`, `tat_breached_at`, `tat_decided_at`, `tat_rule_set_version` |
| poll | `last_polled_at`, `next_poll_at`, `issuance_status` | **DDL** |

`premium` is the selected offer's premium (excluding tax), matching the insurer's policy premium;
the amount paid (premium + tax) stays in `payment.payment`.

## 8. Invariants

`INV-POL-01` (start), `-02` (unique + one-time guarded UPDATE), `-03` (`ACTIVE` only after documents
and audit), `-04` (append-only history — enforced by grants), `-05` (ingest). One policy per
proposal (UNIQUE `proposal_id`).

## 9. Events

Produced (outbox `policy.outbox_event`, topic `{env}.policy.policy.v1`): `IssuanceTrackingStarted`,
`PolicyStatusChanged` (one per history row), `PolicyIssued` (policyId, policyNumber, insurerCode,
proposalId, journeyId), `ProposalDeclined` (proposalId, paymentId, applicationNumber, reason),
`IssuanceTatEscalated` (`level` `WARNING`|`BREACHED`; policyId, proposalId, applicationNumber, decisionDays,
clockStartedAt). `ProposalDeclined` replaces the older
`PolicyIssuanceFailed` for an insurer decline. Consumed: `PaymentReconciled`, `PaymentRefunded`.
Notification consumes `PolicyIssued` / `ProposalDeclined` to inform the customer (S-18).

## 10. Idempotency, timeouts, failures

Consumer dedupe by `eventId`; callbacks by `(provider, providerEventId)`; history by
`UNIQUE (policy_id, source, source_ref)`. Hub reads: 3 s / 15 s, bounded retry, failure → next poll.
Quote/proposal API unavailable at start → consumer retry, then DLQ; the repair sweep re-drives.

## 11. Required DDL changes (not edited here)

| Target | Change |
|---|---|
| `policy.policy` | `policy_number`, `risk_commencement_date` **nullable** until `ISSUED` + CHECK (`state IN ('PENDING_ISSUANCE','ISSUANCE_REJECTED') OR policy_number IS NOT NULL`); ADD `quote_id CHAR(26) NOT NULL`, `application_number VARCHAR(50) NOT NULL`, `issuance_status VARCHAR(24) NOT NULL`, `closed_at`, `tat_started_at`, `tat_warn_at`, `tat_due_at`, `tat_breached_at`, `tat_decided_at`, `tat_rule_set_version`, `last_polled_at`, `next_poll_at`; UNIQUE `(proposal_id)`; INDEX `(state, next_poll_at)` |
| NEW `policy.policy_status_history` | `history_id CHAR(26) PK`, `policy_id FK`, `status` CHECK (`APPLICATION_RECEIVED, UNDER_REVIEW, REQUIREMENT_RAISED, ISSUED, DECLINED, REFUNDED`), `provider_status`, `provider_sub_status`, `source`, `source_ref`, `occurred_at`, `recorded_at`; UNIQUE `(policy_id, source, source_ref)` |
| NEW `policy.outbox_event`, `policy.processed_event` | ADR-025 pattern (the review found policy has no outbox) |
| `91-grants.sql` — **`app_policy` has no grants today** | `USAGE` on schema; `SELECT, INSERT, UPDATE` on `policy` (REVOKE DELETE); `SELECT, INSERT` on `policy_status_history`, `policy_document_ref` (REVOKE UPDATE, DELETE); `SELECT, INSERT, UPDATE` on `outbox_event`; full on `idempotency_record`, `processed_event` |

## 12. Open items

| # | Item | Owner |
|---|---|---|
| PI1 | Confirm 7 days, start event (receipt vs payment), day basis, warning point | Shailja |
| PI2 | Status poll interval; does 1SB offer a status callback | Mahesh, 1SB |
| PI3 | Post-payment counter-offer handling (refund + re-quote?) | Rajal |
| PI4 | Hub status map seed (§3) | Mahesh |
