# payment-service — Payment (#12)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Owns money movement for the premium: create a payment session at the **AU Bank Payment Gateway**, send the link to the **customer's own device** (C4), ingest the PG authorisation callback, reconcile against the PG settlement file, and handle `UNCERTAIN`, reconciliation breaks and maker-checked refunds. `RECONCILED` — not `CAPTURED` — is the state that permits issuance |
| **Module** | `services/payment-service` · port 8098 · `service-id: payment` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/payments` · PG callbacks arrive on Istio route `/callbacks/pg/*` (IP-allowlisted to the PG, **not** on the RM session path) |
| **Callers** | Journey (initiate) · BFF (status read) · AU Bank PG (callback) · settlement-file job |
| **Calls out** | Quotation (selected premium, `INV-PAY-03`) · Proposal (payable check) · AU Bank PG (session create, S-13, via Apigee where applicable) · Notification (payment link to customer, S-18) |
| **Owns data** | Aurora schema **`payment`** — `payment`, `payment_attempt`, `refund`, `outbox_event`, `idempotency_record` · role `app_payment` · **always structured; never a JSON envelope; no cardholder data anywhere** |
| **Code status** | **SKELETON**. Paths CONTRACTED (HLD §5.1/§5.3: `POST /payments`, `POST /payments/pg-callbacks`, `GET …/{id}`); bodies **PROPOSED** |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/payments` | CONTRACTED · body PROPOSED | Journey | Initiate; issue link to customer device |
| `GET` | `/internal/v1/payments/{paymentId}` | CONTRACTED | BFF, Journey | State incl. `UNCERTAIN` / `RECONCILED` — **never the payment URL** |
| `POST` | `/internal/v1/payments/pg-callbacks` (edge: `/callbacks/pg/*`) | CONTRACTED | AU Bank PG | Authorisation result (signature-verified) |
| `POST` | `/internal/v1/payments/reconciliations` | PROPOSED | Settlement job | Ingest settlement file / run match |
| `POST` | `/internal/v1/payments/{paymentId}/refunds` | PROPOSED | Ops (maker) | Request refund (`REFUND_INITIATED`) |
| `POST` | `/internal/v1/payments/refunds/{refundId}/approve` | PROPOSED | Ops (checker) | Maker-checker above threshold (`INV-PAY-06`) |

**Never exists:** any endpoint that returns a payment URL into an RM or bank-employee session
(`FF-14`, `PAYMENT_DEVICE_ISOLATION`), or that accepts a card, account or PG credential.

---

## 2. State model

`INITIATED → LINK_ISSUED → AWAITING_AUTHORISATION → AUTHORISED → CAPTURED → RECONCILED`
with `DECLINED`, `EXPIRED`, `REJECTED`; `AUTHORISED → UNCERTAIN` (PG response lost) resolved only by
reconciliation; `CAPTURED → RECONCILIATION_BREAK` past SLA → manual; `RECONCILED → REFUND_INITIATED
→ REFUNDED`. [Domain model §4.6](../../ws3-platform/01-domain-model-and-invariants.md).

---

## 3. Endpoints (PROPOSED shapes)

### 3.1 `POST /internal/v1/payments` — initiate

Headers: `X-Actor-Id`, `Idempotency-Key` = `proposal:{proposalId}:payment:{attempt}`.

```json
{ "leadId": "01JQ…", "journeyId": "01JQ…", "proposalId": "01JV…", "deviceChannel": "SMS_LINK" }
```

There is **no amount in the request** — Payment fetches it.

```text
1  Proposal must be UW_APPROVED / AWAITING_PAYMENT          → else 409 PROPOSAL_NOT_PAYABLE
2  Amount := Quotation GET /quotes/{quoteId}/selected → premium + tax, to the paise (INV-PAY-03)
3  One non-terminal payment per proposal (ux_payment_active_per_proposal)
       → 409 PAYMENT_ALREADY_IN_PROGRESS (INV-PAY-02)
   prior attempt AUTHORISED or UNCERTAIN → 409 PAYMENT_STATE_UNCERTAIN (INV-PAY-04)
4  deviceChannel ∈ SMS_LINK | EMAIL_LINK | QR_SCAN; target contact = customer's registered contact
   from Customer #4 — never an RM/employee contact    → else 403 PAYMENT_DEVICE_ISOLATION
5  PG session create (S-13: 3 s / 15 s, NO retry) with a server-derived key from paymentId
       PG rejects → REJECTED; proposal stays AWAITING_PAYMENT
6  ONE payment transaction: INSERT payment (LINK_ISSUED, amount, currency, device_channel,
   link_issued_to_enc, link_expires_at) + payment_attempt + idempotency + outbox PaymentLinkIssued
7  Notification (async, S-18) sends the PG URL to the customer's phone/email; failure never blocks
   the journey — it raises an ops task
8  201 { paymentId, state: LINK_ISSUED, deviceChannel, linkExpiresAt, maskedDestination }
```

### 3.2 `GET /internal/v1/payments/{paymentId}`

`{ paymentId, proposalId, amount, currency, state, deviceChannel, linkExpiresAt, authorisedAt, capturedAt, reconciledAt }`.
No URL, no PG token, no card data.

### 3.3 PG callback (S-14)

```text
1  Arrives on Istio /callbacks/pg/*; source IP ∉ PG allowlist → dropped at the edge
2  Verify PG signature over the body → invalid: 401 + SECURITY_EVENT
3  Replay window check on the PG timestamp → outside: 401
4  Dedupe on pgTxnId → seen: return the stored result (at-least-once delivery)
5  Apply AUTHORISED / CAPTURED / DECLINED to the aggregate; append payment_attempt; outbox
   PaymentAuthorised / PaymentCaptured
6  A callback that never arrives does NOT become paid or unpaid — the payment becomes UNCERTAIN
   and only reconciliation decides (F-08)
```

### 3.4 Reconciliation (S-15)

Daily + on demand. Match each settlement record on **`pgTxnId` AND amount** (either alone is not a
match) → `RECONCILED` + outbox **`PaymentReconciled`** (→ Journey `PAYMENT_SETTLED`, Quotation and
Proposal materialise, Policy issues). Unmatched inside SLA → stays `CAPTURED`; past SLA →
`RECONCILIATION_BREAK` + `PaymentReconciliationBreak` alert → Finance manual procedure (F-07).
**No timeout ever resolves a money question.**

### 3.5 Refunds

`REFUND_INITIATED` only from `RECONCILED`; above the configured threshold a second person must
approve (`INV-PAY-06`); `refund` rows record initiator, approver, decision time.

## 4. Data — schema `payment`

| Table | Content |
|---|---|
| `payment` | `payment_id`, `lead_id`, `journey_id`, `proposal_id`, `amount`, `currency`, `state`, `device_channel`, `link_issued_to_enc`, `link_expires_at`, `pg_txn_id`, `authorised_at`, `captured_at`, `settlement_ref`, `reconciled_at`, `lob`, `config_version_ref`, `version` |
| `payment_attempt` | Append-only: `outcome`, `pg_txn_id`, `detail` |
| `refund` | Maker-checker |
| `outbox_event`, `idempotency_record` | Platform pattern |

Retention `RET-7Y-IMMUTABLE` from the first write. Payment never writes `journey.*`, never reads
the quote/proposal working items in DynamoDB (no DynamoDB permission at all). DDL [`10-payment.sql`](../../data-architecture/schemas/10-payment.sql).

## 5. Sources

- [Domain model §4.6, §6](../../ws3-platform/01-domain-model-and-invariants.md) · [06 §6 Payment on the journey](../../data-architecture/06-journey-orchestrator-and-working-payload.md#6-payment--on-the-journey-and-independent) · [External call catalogue §3](../../../journey-execution/05-EXTERNAL-CALL-CATALOGUE.md) · [R0 HLD §2.6, §5](../../../architecture/R0-HLD.md#26-the-journey-saga--the-one-record-of-where-a-sale-is)
