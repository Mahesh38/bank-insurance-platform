# 11 — Lead module sequences (R0)

**Status:** `AI-DRAFTED` · T3 · human Board 1 outstanding  
**Origin:** `SUG-20260930-lmd` · `EPIC-005` · `ARCH-027` · `PLAN-007`  
**Companion:** [`10-lead-module-hld.md`](./10-lead-module-hld.md) · [`07-nip-bff-lead-phase-api-lld.md`](./07-nip-bff-lead-phase-api-lld.md)

R0 default actor for create: **Bank RM** (`INV-LED-04`). BRD SP / Non-SP / Insurance RM create paths are illustrated only as *future* variants under `OPEN-LEAD-ACTOR`.

---

## 1. Landing — own pipeline (SCR-02)

```mermaid
sequenceDiagram
  autonumber
  actor RM as Bank RM
  participant APP as NIP-APP
  participant BFF as NIP BFF
  participant LED as Lead #5
  participant AUD as Audit

  RM->>APP: Open assisted workspace
  APP->>BFF: GET /workspace/pipeline?cursor&limit
  BFF->>LED: GET /internal/v1/leads?owner=me&states=working
  LED-->>BFF: page of LeadSummary
  BFF-->>APP: masked pipeline page
  Note over BFF,AUD: list access audited without PII payloads
```

Sync. Cursor pagination. No nested full Lead (EPIC-003 §5).

---

## 2. Search → confirm → product → create (SCR-03…05)

```mermaid
sequenceDiagram
  autonumber
  actor RM as Bank RM
  participant APP as NIP-APP
  participant BFF as NIP BFF
  participant LED as Lead #5
  participant CUST as Customer #4
  participant CBS as CBS via Apigee
  participant JRN as Journey #6
  participant AUD as Audit

  RM->>APP: Search by CUSTOMER_ID|MOBILE|PAN
  APP->>BFF: GET /customers:search
  BFF->>LED: active-leads for caller book (lead-first)
  LED-->>BFF: optional existingLead summary
  alt need CBS
    BFF->>CUST: resolve / search
    CUST->>CBS: Customer 360
    CBS-->>CUST: customer card(s)
    CUST-->>BFF: bank-language projection
  end
  BFF-->>APP: masked cards (+ existingLead if caller-owned)

  RM->>APP: Confirm customer + productClass
  APP->>BFF: POST /leads (Idempotency-Key)
  BFF->>LED: POST /internal/v1/leads
  LED->>LED: ALG-DEDUPE
  alt unfinished duplicate
    LED-->>BFF: 409 CONFLICT + existing leadId
    BFF-->>APP: resume existing
  else create
    LED->>LED: mint leadId, state=NEW
    LED->>JRN: create journey ref (sync)
    JRN-->>LED: journeyId
    LED->>AUD: LeadCreated
    LED-->>BFF: 201 leadId + journeyId
    BFF-->>APP: success
  end
```

Rules:

- Lead-first then CBS (ARCH-025).
- Idempotency-Key on create (S-20).
- Insurer/plan absent at create (`BR-LEAD-005`).
- `productClass` immutable (`BR-LEAD-006`).

---

## 3. Dedupe conflict only

```mermaid
sequenceDiagram
  participant BFF as NIP BFF
  participant LED as Lead #5

  BFF->>LED: POST /internal/v1/leads
  LED->>LED: key = principalId + customerId + productClass
  Note over LED: unfinished = not terminal AND biGenerated=false
  LED-->>BFF: 409 + existingLead {leadId, journeyId, state}
```

After `biGenerated=true`, same key may create a **new** lead (`BR-DEDUPE` table). Another principal’s unfinished lead does not block this principal (`BR-DEDUPE` row “another user”); cross-RM **visibility** remains `OPEN-LEAD-XRM` (absent).

---

## 4. Resume Save & Close

```mermaid
sequenceDiagram
  actor RM as Bank RM
  participant APP as NIP-APP
  participant BFF as NIP BFF
  participant LED as Lead #5
  participant JRN as Journey #6

  RM->>APP: Open lead from pipeline
  APP->>BFF: GET /leads/{leadId}
  BFF->>LED: GET /internal/v1/leads/{leadId}
  LED-->>BFF: Lead + journeyId
  BFF->>JRN: GET journey stage refs
  JRN-->>BFF: resume point
  BFF-->>APP: prefilled resume payload
```

`BR-LEAD-004`: resume from last applicable journey point with stored data prefilled — Journey owns the point; Lead owns the inbox row.

---

## 5. Assignment / reassignment (pre-BI)

```mermaid
sequenceDiagram
  actor RM as Bank RM
  participant BFF as NIP BFF
  participant LED as Lead #5
  participant PDP as AuthZ PDP
  participant AUD as Audit

  RM->>BFF: POST /leads/{id}/assignments
  BFF->>LED: POST /internal/v1/leads/{id}/assignments
  LED->>PDP: may assign? target certified?
  PDP-->>LED: allow / deny
  alt biGenerated
    LED-->>BFF: 422 REASSIGN_AFTER_BI (VAL-016)
  else
    LED->>LED: append assignment history
    LED->>AUD: AssignmentChanged
    LED-->>BFF: 200 Lead
  end
```

`OPEN-D1` still owns SLA reset and conversion credit semantics; storage always keeps history (`BR-OWN-002/003`).

---

## 6. First BI → Eligible / QUALIFIED

```mermaid
sequenceDiagram
  participant QTE as Quotation #10
  participant LED as Lead #5
  participant AUD as Audit

  QTE->>LED: POST /internal/v1/leads/{id}/bi-generated (or event)
  Note over QTE,LED: Only after successful BI response (BR-BI-001/002)
  LED->>LED: if first: biGenerated=true, reportingClass=ELIGIBLE, state→QUALIFIED
  LED->>AUD: LeadQualified
  LED-->>QTE: 204
```

Multiple BIs stay on Quote; Lead does not mint a new `leadId` (`BR-BI-003/005`).

---

## 7. Convert → archive (ADR-014)

```mermaid
sequenceDiagram
  participant JRN as Journey #6
  participant LED as Lead #5
  participant AUD as Audit

  JRN->>LED: JourneySold (payment RECONCILED + policy ACTIVE)
  LED->>LED: QUALIFIED → CONVERTED (INV-LED-02)
  LED->>LED: archiveWorkingInbox → ARCHIVED
  LED->>AUD: LeadConverted + LeadArchived
```

Off-platform MIS policy ingest never calls lead create (`INV-LED-09`).

---

## 8. Close (pre-conversion)

```mermaid
sequenceDiagram
  actor RM as Bank RM
  participant BFF as NIP BFF
  participant LED as Lead #5

  RM->>BFF: POST /leads/{id}/close {reason, remarks?}
  BFF->>LED: POST /internal/v1/leads/{id}/close
  LED->>LED: → DISQUALIFIED/Closed terminal
  Note over LED: BR-CLOSE-001 — no reopen; new lead needs fresh create + dedupe
  LED-->>BFF: 200
```

---

## 9. Deferred — BRD role variants (`OPEN-LEAD-ACTOR`)

| BRD flow | Sequence status |
|---|---|
| §8.1 Insurance RM creates (picks branch + SP) | Documented in BRD only; **not** R0 API until Product overturns INV-LED-04 |
| §8.2 Bank SP creates (picks Insurance RM) | Same |
| §8.3 Bank Non-SP creates (picks SP; RM derived) | Same |
| Meeting schedule + SMS/email | Parked `SUG-20260907-fig` / BOOT notification scope |

---

## 10. Sync vs async

| Interaction | Mode |
|---|---|
| Pipeline, search, get, create, assign, close | Sync |
| BI mark from Quote | Sync command or durable event — same idempotent handler |
| JourneySold | Event (async) with idempotent convert |
| Assignment notification | Async outbox → Notification (R0 thin) |
| Meeting customer SMS/email | Deferred |

---

## 11. Done for this document

- [x] Core R0 sequences
- [x] Explicit deferral of BRD multi-actor create
- [ ] Human Board 1 signature
