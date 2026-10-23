# proposal-service — Proposal and underwriting tracking, Life cell (#11)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Owns the **application**: the insurer's dynamic proposal form filled for the selected offer, its submission, and **tracking** of the insurer's underwriting (requirements, counter-offer, decision). The bank distributes; the **insurer underwrites** — this service never decides UW (`TI-03`). Enforces **C2** at submit |
| **Module** | `services/proposal-service` · port 8097 · `service-id: proposal` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/proposals` (HLD §5.3) |
| **Callers** | Journey (create, submit) · BFF (status reads, form schema) · Payment (payable check) |
| **Calls out** | Consent (C2 gate) · Quotation (selected offer) · Integration Hub (form schema, submit S-12 async-poll **no retry on submit**, status/requirements) · Configuration (`issuanceMode`, field rules) |
| **Owns data** | **DynamoDB `{env}-proposal-working`** — working proposal (form answers, nominee, UW requirements in a client-side-encrypted envelope) until payment (`DB-DEC-0005`) · Aurora schema **`proposal`** — `proposal`, `uw_requirement`, `uw_document_ref` structured after `RECONCILED`, `idempotency_record` · role `app_proposal` |
| **Contract** | **Authoritative:** [`openapi.yaml`](../../contracts/proposal-service/openapi.yaml) + [`LLD.md`](../../contracts/proposal-service/LLD.md) (`ADR-027`, AI-DRAFTED, awaiting review). Where this page and the contract differ, the contract wins |
| **Code status** | **PARTIAL — persistence only** (`SUG-20261021-lft`): DynamoDB working-state store, envelope encryption (AES-GCM, KMS data key), materialiser into structured Aurora rows (`proposal`, `uw_requirement`, `uw_document_ref`), `POST /internal/v1/events/payment-reconciled`; tested against embedded DynamoDB Local. Public command API, payment hold/release, `proposal_answer` and `proposal_status_history` (D8) not built |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `GET` | `/internal/v1/proposals/form-schema?quoteId&offerId` | PROPOSED | BFF | Bank-envelope dynamic form for the selected product (via Hub) |
| `POST` | `/internal/v1/proposals` | CONTRACTED · body PROPOSED | Journey | Create `DRAFT` from the selected offer |
| `PUT` | `/internal/v1/proposals/{proposalId}/answers` | PROPOSED | Journey / BFF | Save form answers (encrypted envelope) |
| `POST` | `/internal/v1/proposals/{proposalId}/submit` | CONTRACTED | Journey | C2 gate → submit through Hub; `202` |
| `GET` | `/internal/v1/proposals/{proposalId}` | CONTRACTED | BFF, Payment | State, `applicationNumber`, UW state, requirements |
| `POST` | `/internal/v1/proposals/{proposalId}/withdraw` | PROPOSED | Journey | Only before `AWAITING_PAYMENT` (`INV-PRP-04`) |
| `POST` | `/internal/v1/proposals/{proposalId}/counter-offer-decision` | CONTRACTED | Journey | Customer accepts / declines an insurer counter-offer |
| `POST` | `/internal/v1/proposals/{proposalId}/payment-hold` · `/payment-release` | CONTRACTED | Payment | Hold for `paymentId` while money moves (TTL removed) / release when the payment ends without `RECONCILED` |
| `GET` | `/internal/v1/proposals/{proposalId}/status-history` | CONTRACTED | BFF, ops | Post-payment status history (append-only, D8) |

---

## 2. State model (abridged)

`DRAFT → SUBMITTED → UNDER_WRITING ⇄ REQUIREMENTS_PENDING → UW_APPROVED | UW_COUNTER_OFFER |
UW_DECLINED`; `UW_COUNTER_OFFER → UW_APPROVED | CUSTOMER_DECLINED`; `UW_APPROVED →
AWAITING_PAYMENT → PAID → CONVERTED` (policy issued) or `→ ISSUANCE_FAILED → CONVERTED |
REFUND_REQUIRED`. `SUBMITTED → SUBMISSION_FAILED` when the insurer never acknowledges. Withdrawal is
refused at or after `AWAITING_PAYMENT`. Full diagram: [domain model §4.5](../../ws3-platform/01-domain-model-and-invariants.md).
**From `PAID` on, every state change is a row in the append-only `proposal.proposal_status_history`
(D8)** — the DynamoDB item is frozen and the `proposal` row is INSERT-only, both saying `PAID`.

---

## 3. Endpoints (PROPOSED shapes)

### 3.1 `POST /internal/v1/proposals`

```json
{ "leadId": "01JQ…", "journeyId": "01JQ…", "customerId": "CUST-0433", "quoteId": "01JT…",
  "offerId": "01JU…", "consentId": "01JS…", "lob": "LIFE" }
```

Guards: the offer's quote is `SELECTED` and this is that offer (`422 INVALID_OFFER_REFERENCE`,
`INV-PRP-02`); `issuanceMode` resolved (`STP` | `NON_STP` | `INSTA`, `INV-PRP-06`).
One DynamoDB `TransactWriteItems`: `PROPOSAL#{id}/META` (`DRAFT`, control attributes, empty encrypted envelope) + `IDEM` + `JOURNEY#{journeyId}/OPEN_PROPOSAL` marker. The quote is **not** converted here — it stays `SELECTED` until materialise (D8). Response `201 { proposalId, state: "DRAFT", schemaId, schemaVersion }`.

### 3.2 `PUT /internal/v1/proposals/{proposalId}/answers`

`{ "schemaId", "schemaVersion", "values": { "proposer.panNumber": "…", "nominee.name": "…" } }`.
Validated against the form schema (field types, mandatory, patterns, visibility rules resolved by
`(lob, formId)`). PII values (PAN, nominee, health answers) live **only inside `payload_enc`**
(`INV-PRP-05`). Each save is a conditional `UpdateItem` (new encrypted envelope, `version + 1`). Response `200 { proposalId, state: "DRAFT", missingMandatoryFields: [] }`.

### 3.3 `POST /internal/v1/proposals/{proposalId}/submit`

```text
1  Read PROPOSAL#{id}/META (strongly consistent); state must be DRAFT; updates conditional on version
2  C2 gate: GET consent /consents/{consentId}: GRANTED, same customer, purposes cover submission,
   now < validUntil   → else 403 CONSENT_REQUIRED, stay DRAFT, outbox PROPOSAL_BLOCKED_NO_CONSENT
3  Quote still inside validity → else proposal held in DRAFT, journey back to QUOTING (F-03)
4  POST Hub /internal/v1/provider-requests {operation: PROPOSAL_SUBMIT, …}
     Idempotency-Key = proposal:{proposalId}:submit      ← NO automatic retry
5  state SUBMITTED; outbox ProposalSubmitted
6  202 { proposalId, state: SUBMITTED }
then  poll Hub (S-12) until the insurer acknowledges:
     applicationNumber received → UNDER_WRITING (journey → UNDERWRITING)
     insurer rejected          → SUBMISSION_FAILED
     poll budget exhausted     → SUBMISSION_FAILED + operations task (F-04) — never re-submit blind
```

### 3.4 `GET /internal/v1/proposals/{proposalId}`

```json
{ "proposalId": "01JV…", "state": "REQUIREMENTS_PENDING", "applicationNumber": "APP123456",
  "underwritingState": "REQUIREMENTS_PENDING", "issuanceMode": "NON_STP",
  "requirements": [ { "requirementId": "01JW…", "type": "DOCUMENT", "subType": "INCOME_PROOF",
                      "description": "Last 3 months salary slips", "status": "PENDING", "dueDate": "2026-10-28" } ] }
```

UW status comes from the insurer through Hub status sync (`bankStatus`, never insurer wire codes) —
see [Scenario 14](../scenarios/14-provider-status-sync.md). `UnderwritingDecision` events go to
Journey (`UW_APPROVED` → `PAYMENT_PENDING` path), Audit and Notification.

### 3.5 On `PaymentReconciled`

Read the DynamoDB item, decrypt, and in ONE Aurora transaction insert `proposal.proposal` (with `nominee_enc`, `issuance_mode`), one **`proposal.proposal_answer`** row per question asked (`question_id`, `question_text` as shown, `schema_version`, `answer_enc` — D8, replaces `form_values_ref`), `uw_requirement`, `uw_document_ref`, and the first `proposal_status_history` row `PAID` (`ON CONFLICT DO NOTHING`); then set `materializedAt` on the item; state `PAID`. Built today: `proposal`, `uw_requirement`, `uw_document_ref` only. Later states (`CONVERTED` on `PolicyIssued`, `UW_DECLINED`/`REFUND_REQUIRED` on `ProposalDeclined`) are appended to the history. Detail: [LLD §4.6–4.7](../../contracts/proposal-service/LLD.md#46-materialise-on-paymentreconciled).

## 4. Errors

`403 CONSENT_REQUIRED` · `422 INVALID_OFFER_REFERENCE` · `422 ISSUANCE_MODE_INVALID` · `409
ILLEGAL_TRANSITION` · `409 WITHDRAWAL_NOT_PERMITTED` · `422 PROPOSAL_REJECTED` (insurer) ·
`422 AGENT_ATTRIBUTION_MISSING` (adapter) · `503 UPSTREAM_UNAVAILABLE`.

## 5. Sources

- [Domain model §4.5, §6](../../ws3-platform/01-domain-model-and-invariants.md) · [`DB-DEC-0005`](../../data-architecture/DB-DEC-0005-dynamodb-working-payload.md) · [DynamoDB design](../../data-architecture/schemas/20-dynamodb-working-payload.md) · [`09-proposal.sql`](../../data-architecture/schemas/09-proposal.sql) · [Proposal and dynamic forms field guide](../../../1sb-insurance-integration/field-guides/proposal-and-dynamic-forms.md) · [Application status field guide](../../../1sb-insurance-integration/field-guides/application-status.md)
