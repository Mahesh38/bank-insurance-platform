# BFF → journey-orchestration → owning services (seams S-03 … S-16)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | BFF → `journey-orchestration-service` → exactly one owning service per command (Customer, Suitability, Consent, Quotation, Proposal, Payment, Policy) |
| **Style** | Synchronous command (S-03, 3 s); callee seams per [budgets](../conventions/idempotency-timeouts-retries.md#2-seam-budgets--every-hop-its-timeout-retry-and-failure) |
| **Consistency** | Callee commits first, then the journey commits — **no 2PC** |
| **Idempotency** | Client key at the BFF; server-derived `journey:{journeyId}:{command}` on every internal hop |
| **Code status** | **CONTRACTED / PROPOSED** — journey-orchestration is a skeleton |

## 1. Who calls whom, per stage

| Journey stage after success | BFF route | Journey calls | Callee writes | Journey writes |
|---|---|---|---|---|
| `INITIATED` | start (after assignment) | Customer snapshot (S-04) | `customer.customer_snapshot` | `journey` + `party_snapshot_ref` |
| `NEED_ANALYSIS` → `SUITABILITY_COMPLETE` | `/need-analysis`, `/suitability` | Suitability (S-07) | `suitability.*` (structured) | ref `suitabilityId` |
| `CONSENT_CAPTURED` | `/consent/challenge`, `/consent/verify` | Consent (S-06) | `consent.consent` (write-once) | ref `consentId` |
| `QUOTING` | `/quotes` | Quotation (→ Hub, S-09) | DynamoDB `quotation-working` item | ref `quoteId` |
| `QUOTE_SELECTED` | `/quotes/{id}/selection` | Quotation | DynamoDB item `selected*` attributes | ref `offerId` |
| `PROPOSAL_IN_PROGRESS` → `UNDERWRITING` | `/proposals`, `/proposals/{id}/submit` | Proposal (→ Hub, S-12) | DynamoDB `proposal-working` item | refs `proposalId`, `applicationNumber` |
| `PAYMENT_PENDING` | `/payments` | Payment (→ PG, S-13) | `payment.payment` (structured) | ref `paymentId` |
| `PAYMENT_SETTLED` | — (event `PaymentReconciled`) | — | Quotation/Proposal materialise | stage only |
| `ISSUANCE_PENDING` → `ISSUED` → `SOLD` | — (events) | Policy (→ Hub, S-16) | `policy.policy` + S3 | refs `policyId`, `policyNumber` |

**Journey never calls the Integration Hub or an adapter**, never opens another schema, and never
copies a decision (eligibility, price, UW outcome, payment state) onto its own row (`INV-JRN-02`).

## 2. One command, step by step

```mermaid
sequenceDiagram
    participant BFF
    participant JO as journey-orchestration
    participant J as schema journey
    participant S as owning service
    participant SS as owning schema
    BFF->>JO: POST /internal/v1/journeys/{id}/quotes (Idempotency-Key K)
    JO->>J: SELECT journey FOR UPDATE (version)
    JO->>JO: legal transition? actor certified? (PDP)
    JO->>S: POST /internal/v1/quotes (Idempotency-Key journey:{id}:quote:1, refs from journey_ref)
    S->>SS: BEGIN; INSERT business row + idempotency + outbox; COMMIT
    S-->>JO: 202 {quoteId}
    JO->>J: BEGIN; UPDATE stage/version; UPSERT journey_ref; INSERT transition; INSERT outbox; INSERT idempotency(K); COMMIT
    JO-->>BFF: 202 {quoteId, stage: QUOTING}
```

| Failure point | Result |
|---|---|
| Callee refuses (e.g. `403 SUITABILITY_REQUIRED`) | Journey writes nothing; refusal propagated with origin; stage unchanged |
| Callee unreachable | `503`, stage unchanged; RM may retry with the same key |
| Callee committed, journey commit failed | Retry: callee replays the same id (same key); journey upserts the same ref |
| Event arrives twice | Consumers dedupe on `eventId`; stage moves are idempotent |

## 3. Event-driven moves

Some stages move on **events**, not BFF clicks: `PaymentCaptured` (recorded as a transition, stage
stays `PAYMENT_PENDING`), `PaymentReconciled` (→ `PAYMENT_SETTLED`), `PolicyIssued`,
`PolicyIssuanceFailed` (→ `COMPENSATING`), audit write confirmations (→ `SOLD` once all four
evidence events persist). See [outbox and events](./outbox-and-events.md).

## 4. Sources

- [06 Journey orchestrator §2–§4, §7](../../data-architecture/06-journey-orchestrator-and-working-payload.md) · [R0 solution architecture §5](../../ws3-platform/03-solution-architecture-r0.md) · [journey-orchestration-service](../services/journey-orchestration-service.md)
