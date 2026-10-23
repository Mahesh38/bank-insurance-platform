# Scenario 12 — Payment link to the customer's device, callback, reconciliation

[← API reference home](../README.md) · Prev: [11](./11-proposal-and-underwriting.md) · Next: [13](./13-policy-issuance-and-sold.md)

**Status: CONTRACTED / PROPOSED** (payment-service is a skeleton). Control **C4**: the premium is
paid only on the **customer's own device** through the **AU Bank Payment Gateway**.

## 1. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP (RM)
    participant BFF as BFF
    participant JO as journey-orchestration
    participant PAY as payment-service
    participant QS as quotation-service
    participant PG as AU Bank PG
    participant BUS as outbox → MSK
    participant NT as notification
    participant Phone as Customer phone
    App->>BFF: POST /api/v1/journeys/{jid}/payments {deviceChannel: SMS_LINK}
    BFF->>JO: payments (stage UNDERWRITING with UW_APPROVED)
    JO->>PAY: POST /internal/v1/payments {proposalId, journeyId, leadId, deviceChannel}
    PAY->>QS: GET /quotes/{qid}/selected → premium + tax
    PAY->>PAY: one active payment per proposal · no AUTHORISED/UNCERTAIN attempt
    PAY->>PG: create session (3 s / 15 s, no retry)
    PG-->>PAY: session URL
    PAY->>PAY: INSERT payment LINK_ISSUED + attempt + idempotency + outbox PaymentLinkIssued
    PAY-->>JO: {paymentId}
    JO->>JO: stage PAYMENT_PENDING · ref paymentId
    BFF-->>App: 201 {paymentId, state: LINK_ISSUED, maskedDestination}  ← no URL
    BUS-->>NT: PaymentLinkIssued
    NT->>Phone: SMS with PG URL
    Phone->>PG: open link · 3-D Secure · pay
    PG->>PAY: callback /callbacks/pg/* (signed, IP-allowlisted)
    PAY->>PAY: verify signature · replay window · dedupe pgTxnId · AUTHORISED → CAPTURED · outbox
    BUS-->>JO: PaymentCaptured → transition only (stage stays PAYMENT_PENDING)
    Note over PAY: daily / on-demand settlement file
    PAY->>PAY: match pgTxnId AND amount → RECONCILED · outbox PaymentReconciled
    BUS-->>JO: PaymentReconciled → stage PAYMENT_SETTLED
    BUS-->>QS: materialise quote/offer
    App->>BFF: GET /api/v1/journeys/{jid}/payments/{paymentId}
    BFF-->>App: {state: RECONCILED}
```

## 2. Sample amounts (illustrative)

Payment never accepts an amount from the caller. After `GET /quotes/{qid}/selected` (or
payment-hold), the hold returns e.g.:

| Field | Example (INR) | Source |
|---|---|---|
| `premiumAmount` | `"25000.00"` | Selected offer |
| `taxAmount` | `"4500.00"` | Selected offer |
| **Amount payable / PG session** | `"29500.00"` | `premiumAmount + taxAmount` to the paise |
| `sumAssured` | `"25600000.00"` | Selected offer (control attribute) |

Figures follow `SUIT-TC-REF-01` ([MISSING-PRICES.md](../../contracts/MISSING-PRICES.md)). A PG
callback whose amount ≠ `"29500.00"` is `422 PREMIUM_MISMATCH`.

## 3. The four money rules developers break

| Rule | Why |
|---|---|
| No endpoint ever returns the payment URL to the RM | Otherwise an RM could pay with a customer's card on a bank device (mis-selling hole C4) |
| Amount comes from Quotation, never from the request | `INV-PAY-03` — to the paise; mismatch → `422 PREMIUM_MISMATCH` + financial alert |
| A lost callback means `UNCERTAIN`, not paid or unpaid | `F-08`; new attempts blocked (`409 PAYMENT_STATE_UNCERTAIN`) until reconciliation |
| Issuance waits for `RECONCILED`, not `CAPTURED` | `INV-POL-01`; a reconciliation break never auto-resolves (`F-07`) |

## 4. Errors

`409 PROPOSAL_NOT_PAYABLE` · `409 PAYMENT_ALREADY_IN_PROGRESS` · `409 PAYMENT_STATE_UNCERTAIN` ·
`422 PREMIUM_MISMATCH` · `403 PAYMENT_DEVICE_ISOLATION` · PG reject → payment `REJECTED`, proposal
stays `AWAITING_PAYMENT` · callback bad signature → `401` + security event.

## 5. Data

`payment.payment` (structured from the first write; `link_issued_to_enc` holds the customer contact,
encrypted), `payment_attempt` (append-only), `outbox_event`, `idempotency_record`; journey stage +
`paymentId`. **No cardholder data anywhere on the platform.** Retention `RET-7Y-IMMUTABLE`.

## 6. Pages

[Payment service](../services/payment-service.md) · [Payment and the PG](../interactions/payment-to-payment-gateway.md) · [Failure and compensation](./15-failure-and-compensation.md)
