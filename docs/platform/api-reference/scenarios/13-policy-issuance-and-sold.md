# Scenario 13 — Policy issuance and the four-part `SOLD`

[← API reference home](../README.md) · Prev: [12](./12-payment-and-reconciliation.md) · Next: [14](./14-provider-status-sync.md)

**Status: CONTRACTED / PROPOSED.** Triggered by events, not by an RM click.

**`SOLD` requires all four** (`INV-JRN-05`): policy `ACTIVE` · payment `RECONCILED` · issuance
confirmed by the insurer · all required audit events persisted. A sale is never inferred from a
quote, a proposal or a payment alone.

## 1. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant BUS as outbox → MSK
    participant JO as journey-orchestration
    participant POL as policy-issuance-service
    participant PAY as payment-service
    participant HUB as integration-hub
    participant A as 1SB adapter
    participant S3 as S3 (Object Lock)
    participant AUD as audit
    participant LS as lead-service
    BUS-->>JO: PaymentReconciled
    JO->>JO: PAYMENT_SETTLED → ISSUANCE_PENDING
    JO->>POL: POST /internal/v1/policies {proposalId, paymentId, journeyId, leadId, customerId}
    POL->>PAY: GET payment → must be RECONCILED (else 409 PAYMENT_NOT_RECONCILED)
    POL->>POL: INSERT policy PENDING_ISSUANCE + outbox
    POL->>HUB: ISSUANCE_CONFIRM / APPLICATION_STATUS (S-16)
    HUB->>A: GET /v1/status/{applicationNumber}
    A-->>HUB: bankStatus ISSUED + policyNumber
    HUB-->>POL: issued
    POL->>POL: ISSUED (policy_number unique, immutable) · outbox PolicyIssued
    POL->>HUB: document download → S3 (Object Lock) · INSERT policy_document_ref
    POL->>POL: CONFIRMED
    BUS-->>AUD: PolicyIssued … → audit_event + WORM
    AUD-->>POL: issuance audit persisted
    POL->>POL: ACTIVE (INV-POL-03) · outbox
    BUS-->>JO: PolicyIssued / ACTIVE → stage ISSUED
    JO->>AUD: completeness check (four evidence events)
    JO->>JO: SOLD · outbox JourneySold
    BUS-->>LS: JourneySold → POST /leads/{id}/convert → CONVERTED → archive
    BUS-->>AUD: JourneySold
```

## 2. Failure exits

| Failure | Path |
|---|---|
| Insurer rejects issuance | `ISSUANCE_REJECTED`, `PolicyIssuanceFailed` → journey `COMPENSATING` → bounded retry → `REFUND_REQUIRED` + maker-checked refund (F-05) |
| Insurer silent | `CONFIRMATION_OVERDUE` → re-check → `ISSUANCE_DISPUTED` (F-06) |
| Audit write lagging | Journey stays `ISSUED` until the evidence lands (F-10) — never `SOLD` on a topic ack |

## 3. Pages

[Policy issuance](../services/policy-issuance-service.md) · [Journey](../services/journey-orchestration-service.md) · [Audit](../services/audit-compliance-service.md) · [Outbox and events](../interactions/outbox-and-events.md)
