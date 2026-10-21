# journey-orchestration-service — Journey (#9)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The **saga orchestrator** and the one record of *where a sale is and why it stopped*. Owns the journey stage machine and the **references** (`suitabilityId`, `consentId`, `quoteId`, `offerId`, `proposalId`, `paymentId`, `policyId`, `applicationNumber`, `policyNumber`). It **coordinates, it never decides**: eligibility, price, UW outcome and payment state stay in their owning services (`INV-JRN-02`, `OR-01`) |
| **Module** | `services/journey-orchestration-service` · port 8095 · `service-id: journey-orchestration` · layer **L6** · INTERNAL |
| **Base path** | `/internal/v1/journeys` (HLD §5.3) |
| **Callers** | BFF only (S-03, 3 s) · event consumers (Payment, Policy, Audit) |
| **Calls out** | Customer, Lead, Consent, Suitability, Catalogue, Quotation, Proposal, Payment, Policy, Administration-config — **never** the Integration Hub, never an adapter, never another schema |
| **Owns data** | Aurora schema **`journey`** (`journey`, `journey_ref`, `journey_transition`, `outbox_event`, `idempotency_record`) · role `app_journey` |
| **Code status** | **SKELETON**. Paths CONTRACTED (HLD §5.3 + BFF §5.1); command bodies **PROPOSED** from [06 journey and working payload](../../data-architecture/06-journey-orchestrator-and-working-payload.md) |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/journeys` | CONTRACTED | BFF (after lead assignment, process-further) | Start a journey for a lead → `INITIATED` |
| `GET` | `/internal/v1/journeys/{journeyId}` | CONTRACTED | BFF | Stage + references + next action |
| `POST` | `/internal/v1/journeys/{journeyId}/transitions` | CONTRACTED | BFF | Generic advance: `{command, payload refs}` |
| `POST` | `/internal/v1/journeys/{journeyId}/need-analysis` | PROPOSED (BFF §5.1 maps here) | BFF | → Suitability start/save |
| `POST` | `/internal/v1/journeys/{journeyId}/suitability` | PROPOSED | BFF | → Suitability evaluate → `SUITABILITY_COMPLETE` |
| `POST` | `/internal/v1/journeys/{journeyId}/consent/challenge` | PROPOSED | BFF | → Consent create |
| `POST` | `/internal/v1/journeys/{journeyId}/consent/verify` | PROPOSED | BFF | → Consent verify → `CONSENT_CAPTURED` |
| `POST` | `/internal/v1/journeys/{journeyId}/quotes` | PROPOSED | BFF | → Quotation create → `QUOTING` |
| `POST` | `/internal/v1/journeys/{journeyId}/quotes/{quoteId}/selection` | PROPOSED | BFF | → Quotation select → `QUOTE_SELECTED` |
| `POST` | `/internal/v1/journeys/{journeyId}/proposals` | PROPOSED | BFF | → Proposal create → `PROPOSAL_IN_PROGRESS` |
| `POST` | `/internal/v1/journeys/{journeyId}/proposals/{proposalId}/submit` | PROPOSED | BFF | → Proposal submit → `UNDERWRITING` |
| `POST` | `/internal/v1/journeys/{journeyId}/payments` | PROPOSED | BFF | → Payment initiate → `PAYMENT_PENDING` |
| (event) | consumes `PaymentReconciled`, `PolicyIssued`, `PolicyIssuanceFailed`, audit acks | PROPOSED | MSK | Event-driven stage moves |

Reads of quote / proposal / payment / policy status go BFF → owning service directly; Journey is
not a read proxy for other contexts' state.

---

## 2. Stage machine

```text
INITIATED → NEED_ANALYSIS → SUITABILITY_COMPLETE (C1) → CONSENT_CAPTURED (C2) → QUOTING
→ QUOTE_SELECTED → PROPOSAL_IN_PROGRESS → UNDERWRITING → PAYMENT_PENDING (C4)
→ PAYMENT_SETTLED (RECONCILED) → ISSUANCE_PENDING → ISSUED
→ SOLD  (policy ACTIVE + payment RECONCILED + issuance confirmed + audit complete — INV-JRN-05)

side exits: ABANDONED (inactivity / quote expiry / payment window) · DECLINED (UW / customer)
            COMPENSATING → SOLD | COMPENSATED | MANUAL_INTERVENTION   (never silently closed, INV-JRN-04)
```

Every non-terminal stage has a `next_action_due_at` (inactivity horizon, `INV-JRN-03`). Unknown
transitions → `409 ILLEGAL_TRANSITION` + integrity alert (`INV-JRN-01`). Full diagram:
[domain model §5](../../ws3-platform/01-domain-model-and-invariants.md).

---

## 3. The command pattern every advancing endpoint follows

```text
1  Load journey + refs (app_journey), SELECT … FOR UPDATE / version check
2  Reject an illegal stage for this command                         → 409 ILLEGAL_TRANSITION
3  Re-check the actor (PDP; SP certification for regulated steps)   → 403
4  HTTP to the ONE owning service (S-04 … S-16), with a server-derived Idempotency-Key
       journey:{journeyId}:{command}
5  Owning service writes ITS store (working JSON or structured evidence) and returns an id
6  ONE local transaction in schema journey:
       UPDATE journey.stage, next_action_due_at, version
       UPSERT journey_ref (ref_type, ref_value)
       INSERT journey_transition (from, to, trigger, actor, correlationId)
       INSERT outbox_event
       INSERT idempotency_record
7  Callee failure → stage unchanged (S-03); no compensating write into the callee's schema
```

No 2PC. If step 6 fails after step 5 committed, a retry returns the same callee id (same key) and
the journey upserts the same reference. Detail:
[journey orchestration pattern](../interactions/journey-orchestration-pattern.md).

---

## 4. Endpoints (PROPOSED shapes)

### 4.1 `POST /internal/v1/journeys`

```json
{ "leadId": "01JQ…", "customerId": "CUST-0433", "lob": "LIFE", "channel": "ASSISTED", "rmId": "SP-1001" }
```

Guards: lead visible, assigned, exception cleared, `accountableSpId` certified (`INV-LED-10`).
Behind the scenes: S-04 Customer snapshot (2 s, 1 retry) → `party_snapshot_ref`; on Customer
failure the journey is still created and **holds at `INITIATED`** with a retryable error (S-04).
Then `INSERT journey` (`INITIATED`) + transition + outbox `JourneyStarted`. Response
`201 { "journeyId", "stage": "INITIATED", "nextActionDueAt" }`. The lead's `journeyId` placeholder
is then replaced by this id (Lead update via its API).

### 4.2 `GET /internal/v1/journeys/{journeyId}`

```json
{
  "journeyId": "01JQ…", "leadId": "01JQ…", "customerId": "CUST-0433", "lob": "LIFE",
  "stage": "QUOTE_SELECTED", "nextActionDueAt": "2026-10-28T00:00:00Z",
  "refs": { "suitabilityId": "01JR…", "consentId": "01JS…", "quoteId": "01JT…", "offerId": "01JU…" },
  "compensation": null
}
```

Partner reps get the journey only when it is partner-visible (`AC-4`), otherwise `404`.

### 4.3 Example advance — `POST /internal/v1/journeys/{journeyId}/quotes`

```json
{ "mode": "MULTI", "category": "SUM_ASSURED", "requestedCover": 10000000,
  "members": [ { "role": "LIFE_ASSURED", "dob": "1990-04-12", "gender": "MALE", "tobacco": false,
                 "annualIncome": 1500000, "pincode": "400001" } ],
  "preferences": { "paymentFrequency": "YEARLY" } }
```

Requires stage `CONSENT_CAPTURED` (or a re-quote from `QUOTING`); forwards with
`suitabilityId` and `consentId` taken **from `journey_ref`** — never from the request body.
Response `202 { "quoteId", "state": "IN_PROGRESS", "stage": "QUOTING" }`. Detail:
[Scenario 10](../scenarios/10-multi-quote-and-selection.md).

---

## 5. Data — schema `journey`

| Table | Write | Purpose |
|---|---|---|
| `journey` | INSERT; UPDATE stage, `next_action_due_at`, assist fields, `compensation`, `version` | Saga header — **no decision columns** |
| `journey_ref` | UPSERT `(journey_id, ref_type)` | The only place Journey remembers other contexts' ids |
| `journey_transition` | INSERT-only | Reconstruction history |
| `outbox_event` | Same transaction as the stage write | `ADR-012` |
| `idempotency_record` | Same transaction | `INV-IDM-01` |

`compensation` JSONB is saga bookkeeping only (`failurePoint`, `attempts`, `ownerTaskId`).
DDL [`12-journey.sql`](../../data-architecture/schemas/12-journey.sql).

## 6. Events

| Produces | Consumes |
|---|---|
| `JourneyStarted`, stage-change events, `JourneyVisibleToPartner`, `PartnerAssistanceRecorded`, `JourneyCompensating`, **`JourneySold`** (→ Lead convert, Audit, Reporting) | `SuitabilityCompleted`, `ConsentGranted`, `QuoteCompleted`, `ProposalSubmitted`, `UnderwritingDecision`, `PaymentLinkIssued`, `PaymentCaptured` (transition only, stage stays `PAYMENT_PENDING`), **`PaymentReconciled`** (→ `PAYMENT_SETTLED`), `PolicyIssued`, `PolicyIssuanceFailed` (→ `COMPENSATING`), audit write confirmations |

## 7. Sources

- [06 Journey orchestrator and working payload](../../data-architecture/06-journey-orchestrator-and-working-payload.md) · [Domain model §5](../../ws3-platform/01-domain-model-and-invariants.md) · [R0 HLD §2.6, §5](../../../architecture/R0-HLD.md#26-the-journey-saga--the-one-record-of-where-a-sale-is)
