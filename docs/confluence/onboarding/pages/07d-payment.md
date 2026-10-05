# Sequence — Payment and issuance

> **Teaching pack** · `SUG-20261005-cfp` · not a source of truth.
>
> **Confluence parent:** Sequence diagrams  
> **This page:** grandchild 7.4

**C4:** the payment link is sent to the **customer's device**. There is no API path that issues a payment link into an RM or bank-employee session.

A policy is issued only against a **RECONCILED** payment. `CAPTURED` is not enough. `UNCERTAIN` blocks a second attempt.

![Payment and issuance](../diagrams/13-seq-payment.png)

## Money path

1. RM asks the platform to create a payment for the journey.
2. Platform refuses if the destination would be the RM session.
3. Notification delivers the link to the customer's phone (`#17`).
4. Customer pays on the **AU Bank Payment Gateway** hosted page — outside the platform.
5. PG calls back on a **separate API Gateway route**, IP-allowlisted.
6. Payment stays `UNCERTAIN` until the settlement file says otherwise. Never guess.
7. `#13` Policy issues only when payment is `RECONCILED`.
8. Audit events go out via the transactional outbox. Journey reaches `SOLD` only when policy is `ACTIVE`, payment is `RECONCILED`, issuance is confirmed, and required audit events are acknowledged.

```mermaid
sequenceDiagram
  autonumber
  actor RM as Bank RM
  participant Pay as Payment #12
  actor Cust as Customer device
  participant PG as AU Bank PG
  participant Pol as Policy #13
  participant Aud as Audit #16

  RM->>Pay: POST /journeys/{id}/payments
  Note over RM,Cust: C4 — refuse if destination is an RM session
  Pay->>Cust: SMS / link
  Cust->>PG: pay on hosted PG
  PG->>Pay: authorisation callback (allowlisted)
  Pay->>Pay: CAPTURED / UNCERTAIN
  Pay->>Pay: settlement file → RECONCILED
  Note over Pay,Pol: issue blocked until RECONCILED
  Pay->>Pol: issue policy
  Pol->>Aud: append-only + outbox
  Aud-->>Pol: required events acknowledged
  Note over RM,Aud: SOLD = ACTIVE + RECONCILED + confirmed + audit complete
```

Reconciliation break (`F-07`) is a **manual finance procedure**. A timeout must not auto-resolve money.

Source: [`R0-HLD.md` §1 steps 6–9](../../../architecture/R0-HLD.md) · `INV-PAY-04` · `INV-JRN-05`.
