# policy-issuance-service — Policy and issuance (#13)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Holds the **distributor's record of the issued policy** (the insurer is policy administrator): policy number, dates, premium, documents. Requests / confirms issuance with the insurer through the Hub and refuses to exist before the payment is `RECONCILED` (`INV-POL-01`). A policy becomes `ACTIVE` only when its documents are persisted **and** the issuance audit event is confirmed (`INV-POL-03`) — the fourth limb of `SOLD` |
| **Module** | `services/policy-issuance-service` · port 8099 · `service-id: policy` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/policies` (HLD §5.3) |
| **Callers** | Journey / event consumer (`PaymentReconciled`) · BFF (read, IPR gated) · MIS ingest (off-platform policies, `CR-013`) |
| **Calls out** | Payment (state check) · Integration Hub (`ISSUANCE_CONFIRM`, `APPLICATION_STATUS`, document download) · S3 (policy PDFs) · Notification (policy issued) |
| **Owns data** | Aurora schema **`policy`** (`policy`, `policy_document_ref`, `idempotency_record`) + S3 prefix for PDFs · role `app_policy` |
| **Contract** | **Authoritative:** [`openapi.yaml`](../../contracts/policy-issuance-service/openapi.yaml) + [`LLD.md`](../../contracts/policy-issuance-service/LLD.md) (`ADR-027`, AI-DRAFTED, awaiting review; `PROPOSAL.TAT` figure awaits Shailja) |
| **Code status** | **SKELETON** — nothing in the contract is built |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/policies` | CONTRACTED · body PROPOSED | Journey on `PaymentReconciled` | Create `PENDING_ISSUANCE`; request / confirm issuance |
| `GET` | `/internal/v1/policies/{policyId}` | CONTRACTED | BFF | Policy + document refs |
| `GET` | `/internal/v1/policies/{policyId}/documents/{documentId}` | PROPOSED | BFF | Short-lived signed URL to the PDF (RM may view; never a payment artefact) |
| `POST` | `/internal/v1/policies:ingest` | PROPOSED (`CR-013`) | MIS ingest | `source = OFF_PLATFORM`, `leadId` null (`INV-POL-05`) — never `lead.create` |
| `GET` | `/internal/v1/policies/{policyId}/status-history` | CONTRACTED | BFF, ops | Append-only issuance status history (D8) |
| `POST` | `/internal/v1/policies/status-callbacks` | CONTRACTED | Integration Hub | 1SB / insurer status callback; replaces polling where offered |

## 2. State model

`PENDING_ISSUANCE → ISSUED → CONFIRMED → ACTIVE` (R0), plus `ISSUANCE_REJECTED`,
`CONFIRMATION_OVERDUE → CONFIRMED | ISSUANCE_DISPUTED`, `FREE_LOOK_CANCELLED`. `LAPSED`,
`SURRENDERED`, `MATURED` modelled, not built in R0.

> **D8 (2026-10-21):** on `PaymentReconciled` the service starts the insurer-decision clock
> (`PROPOSAL.TAT`, default 7 days — IRDAI Master Circular 2024, Compliance to confirm), polls the
> 1SB application status (S-16) or receives a callback, appends every change to the new
> `policy.policy_status_history` (`APPLICATION_RECEIVED`, `UNDER_REVIEW`, `REQUIREMENT_RAISED`,
> `ISSUED`, `DECLINED`, `REFUNDED`), writes `issued_at`, `risk_commencement_date`, `maturity_date`
> (and later `closed_at`) on `ISSUED`, and emits `PolicyIssued` / `ProposalDeclined` /
> `IssuanceTatEscalated`. Insurer, product, premium and sum assured come from the quotation and
> proposal APIs — never a cross-schema read. The flow below is superseded by
> [LLD §4](../../contracts/policy-issuance-service/LLD.md#4-flows).

## 3. `POST /internal/v1/policies` (PROPOSED)

```json
{ "leadId": "01JQ…", "journeyId": "01JQ…", "customerId": "CUST-0433", "proposalId": "01JV…", "paymentId": "01JY…" }
```

```text
1  GET payment → state must be RECONCILED            → else 409 PAYMENT_NOT_RECONCILED
2  INSERT policy (PENDING_ISSUANCE) + idempotency + outbox (one txn)
3  Hub ISSUANCE_CONFIRM / APPLICATION_STATUS (S-16, 3 s / 15 s) → policyNumber, dates
     issued      → ISSUED (+ PolicyIssued)            policy_number unique per insurer, immutable (INV-POL-02)
     rejected    → ISSUANCE_REJECTED (+ PolicyIssuanceFailed → Journey COMPENSATING, F-05)
     no answer   → scheduled re-check; SLA → CONFIRMATION_OVERDUE → ISSUANCE_DISPUTED (F-06)
4  Download policy document via Hub → S3 (Object Lock class) → INSERT policy_document_ref
5  Audit event for issuance confirmed written → ACTIVE (INV-POL-03) → Journey may reach SOLD
6  201 { policyId, state }
```

`GET` response: `{ policyId, policyNumber, insurerCode, productCode, sumAssured, premium,
frequency, riskCommencementDate, maturityDate, state, freeLookExpiresAt, documents:[{documentId, documentType}] }`.

## 4. Data — schema `policy`

`policy` (`policy_id`, `lead_id`, `journey_id`, `customer_id`, `proposal_id`, `payment_id`,
`policy_number`, `insurer_code`, `product_code`, `sum_assured`, `premium`, `frequency`,
`risk_commencement_date`, `maturity_date`, `state`, `issued_at`, `confirmed_at`,
`free_look_expires_at`, `lob`, `product_class`, `config_version_ref`, `version`) ·
`policy_document_ref` (`object_ref` to S3, `document_type`). Retention `RET-POLICY+7Y`.
DDL [`11-policy.sql`](../../data-architecture/schemas/11-policy.sql).

## 5. Sources

- [Domain model §4.7, §5.1](../../ws3-platform/01-domain-model-and-invariants.md) · [DATA-002 CR-013 alignment](../../data-architecture/DATA-002-cr013-alignment.md) · [R0 HLD](../../../architecture/R0-HLD.md)
