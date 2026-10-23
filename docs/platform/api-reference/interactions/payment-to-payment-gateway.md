# Payment ↔ AU Bank Payment Gateway (seams S-13 / S-14 / S-15)

[← API reference home](../README.md)

| | |
|---|---|
| **Parties** | `payment-service` · AU Bank PG · customer device · `notification-service` |
| **Three channels, deliberately separate** | Session create (egress, sync) · authorisation callback (ingress, at-least-once) · settlement file (batch) |
| **Control** | **C4** — payment happens only on the customer's own device; no API path issues a link into an RM session; the platform never sees card, account or PG credentials |
| **Code status** | **CONTRACTED / PROPOSED** (payment-service is a skeleton) |

## 1. Legs

| Leg | Seam | Direction | Auth | Timeout / retry | On failure |
|---|---|---|---|---|---|
| Session create | S-13 | payment → PG | PG credential (Secrets Manager) | 3 s / 15 s, **no retry** | `REJECTED`; proposal stays `AWAITING_PAYMENT` |
| Link delivery | S-18 | payment → outbox → notification → customer phone/email | — | at-least-once | Ops task; journey not blocked |
| Customer pays | — | customer device → PG hosted page (3-D Secure) | — | — | Not our traffic |
| Authorisation callback | S-14 | PG → Istio `/callbacks/pg/*` → payment | **PG signature** + source-IP allowlist + replay window | at-least-once; dedupe on `pgTxnId` | Missing → `UNCERTAIN` |
| Settlement | S-15 | PG file → payment reconciliation | separate from the API path | daily + on demand | Unmatched past SLA → `RECONCILIATION_BREAK` |

## 2. End to end

```mermaid
sequenceDiagram
    participant JO as journey-orchestration
    participant PAY as payment-service
    participant QS as quotation-service
    participant PG as AU Bank PG
    participant N as notification
    participant C as Customer phone
    JO->>PAY: POST /internal/v1/payments {proposalId, deviceChannel}
    PAY->>QS: GET /quotes/{quoteId}/selected (premium + tax)
    PAY->>PG: create session (amount, ref = paymentId)
    PG-->>PAY: session URL
    PAY->>PAY: INSERT payment LINK_ISSUED + outbox PaymentLinkIssued
    PAY-->>JO: {paymentId} → stage PAYMENT_PENDING
    PAY-->>N: PaymentLinkIssued (via MSK)
    N->>C: SMS / email with the PG URL
    C->>PG: pays (3-D Secure)
    PG->>PAY: callback /callbacks/pg/* (signed)
    PAY->>PAY: verify signature, dedupe pgTxnId → AUTHORISED/CAPTURED; outbox
    Note over PAY: later — settlement file
    PAY->>PAY: match pgTxnId AND amount → RECONCILED; outbox PaymentReconciled
```

## 3. What the RM sees

Only status: `LINK_ISSUED`, `AWAITING_AUTHORISATION`, `CAPTURED`, `UNCERTAIN`, `RECONCILED`,
`DECLINED`, `EXPIRED`. Never the URL. If the callback is lost the RM sees `UNCERTAIN` and **cannot
start a new attempt** (`INV-PAY-04`) until reconciliation decides.

## 4. Sources

- [External call catalogue §3](../../../journey-execution/05-EXTERNAL-CALL-CATALOGUE.md) · [payment-service](../services/payment-service.md) · [R0 for developers §7C](../../../architecture/R0-E2E-FOR-DEVELOPERS.md)
