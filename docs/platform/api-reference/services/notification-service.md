# notification-service — Notification (#17)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Delivers messages to the **customer's device** through the bank's SMS / email gateways. R0 scope (D8, 2026-10-21) is **OTP (consent), payment link, policy issued and proposal declined** customer notices; everything else is R1. A delivery failure **never blocks the journey** — it raises an operations task (S-18) |
| **Module** | `services/notification-service` · port 8102 · `service-id: notification` · layer L5 · INTERNAL |
| **Contract** | [`openapi.yaml`](../../contracts/notification-service/openapi.yaml) · [LLD](../../contracts/notification-service/LLD.md) — authoritative per [ADR-027](../../architecture-review/ADR-027-openapi-contract-authority.md); AI-DRAFTED; `SENT`/`FAILED` published as `NotificationSent`/`NotificationFailed`; templates are configuration |
| **Base path** | `/internal/v1/notifications` (HLD §5.3) |
| **Callers** | Consent (OTP, direct call) · events `PaymentLinkIssued`, `PolicyIssued`, `ProposalDeclined` via outbox → MSK |
| **Calls out** | Bank SMS / email gateway (egress inspected; via Apigee where the bank exposes it) |
| **Owns data** | **PROPOSED:** minimal schema `notification` (idempotency + delivery status, no message body or contact data) — DDL addition pending. Evidence remains the audit event |
| **Code status** | **SKELETON**. `POST /internal/v1/notifications` CONTRACTED; body **PROPOSED** |

## 1. `POST /internal/v1/notifications` (PROPOSED)

```json
{ "eventId": "01K…", "template": "PAYMENT_LINK", "channel": "SMS", "lob": "LIFE",
  "recipientRef": { "customerId": "CUST-0433" }, "parameters": { "url": "https://pg…", "expiresAt": "…" } }
```

- The recipient is resolved from **Customer #4** by `customerId` — a raw phone number or email from
  a caller is refused, so a link can never be redirected to an RM device (C4).
- Idempotent on `eventId`. Response `202 { notificationId, status: "QUEUED" }`.
- Templates come from configuration (`#19`); no message text in code.
- Outcomes (`SENT`, `FAILED`) are published as events for Audit; `FAILED` → operations task.
- Logs never contain the URL, OTP, phone number or email.

## 2. Sources

- [R0 HLD Boundary 4, S-18](../../../architecture/R0-HLD.md#42-seam-catalogue-s-01--s-22) · [External call catalogue §1](../../../journey-execution/05-EXTERNAL-CALL-CATALOGUE.md)
