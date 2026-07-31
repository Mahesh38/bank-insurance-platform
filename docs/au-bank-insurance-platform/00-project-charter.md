# Project charter — AU Bank Insurance Platform

**Document owner:** Platform Product Owner  
**Status:** Draft aligned to baseline Volumes/Phases (see knowledge-base)  
**Bank:** AU Bank  
**Version:** 0.2 (post document intake)

---

## 1. One-sentence intent

AU Bank will build a **bank-owned, insurer-agnostic Insurance Distribution Platform** that owns the journey from **lead → consent → suitability → quote → proposal → payment → policy**, with **RM-assisted** (and later self-service) channels, using **1SilverBullet first** and evolving to direct insurer integrations — without rewriting the bank business model.

---

## 2. Business problem (to validate)

| # | Problem statement (draft) | Validate with |
|---|---------------------------|---------------|
| P1 | Insurance is sold ad hoc; RM lacks a guided, auditable journey inside bank systems | Bancassurance / RM ops |
| P2 | Multi-insurer comparison and proposal capture is slow / error-prone without a platform | Product + RM |
| P3 | Compliance needs clear agent/distributor attribution, consent, and audit trail | Compliance / Risk |
| P4 | Bank does not want permanent lock-in to a single aggregator’s APIs in every app | Architecture + PO |
| P5 | Customer is a **bank customer first** — CIF/KYC should prefill; bank remains identity SoT | Digital + Data |

*Stakeholder action: confirm, rewrite, or drop each problem in Session 1.*

---

## 3. Who we serve (personas — draft)

| Persona | Primary need | Channel (hypothesis) |
|---------|--------------|----------------------|
| **RM / Branch staff** | Originate quote → proposal → track status for a customer sitting with them | RM app / assisted web (Figma) |
| **Customer (existing)** | Understand offers, complete OTP/payment/docs, receive policy | Customer app / assisted + self steps |
| **Bancassurance ops** | Product catalog, insurer enablement, exception handling | Ops console (later?) |
| **Compliance / Audit** | Reconstruct who did what, with which agent/distributor | Reports / audit store |
| **Platform / Integration** | Stable bank APIs; hide aggregator protocol | Internal services |

---

## 4. In scope / out of scope (discovery defaults — not frozen)

### Proposed **in scope** for first release discussion

- RM-assisted journey for **at least one LOB** (candidate: Term Life — not locked)
- Customer identification via bank CIF (read/prefill)
- Quote → select → proposal → payment handoff → status visibility
- Suitability / disclosures as **bank-owned** steps (content TBD)
- Audit + agent attribution for regulated sale

### Proposed **out of scope** until explicitly pulled in

- Claims, renewals, servicing as full product suites
- Building AU Bank’s own multi-carrier connectivity fabric on day 1
- Replacing bank CRM / CIF / core banking
- Non-bank customer (walk-in / open market) acquisition (unless product asks for it)
- Full insurer panel for every LOB at once

---

## 5. Strategic constraints (working assumptions)

| Constraint | Assumption | Owner to confirm |
|------------|------------|------------------|
| Aggregator now | 1SB used as current insurance gateway | Bancassurance + PO |
| Replaceability | Bank journey + APIs must survive aggregator change | PO + Architect |
| Brand | Experience is **AU Bank**, not 1SB white-label as primary UX | Digital + Brand |
| Data | Customer SoT remains bank; aggregator holds transactional insurance refs | Data + Compliance |
| Attribution | Distributor id from bank config; agent id mandatory on regulated submit | Compliance |

---

## 6. Success (draft — measurable later)

Until metrics are approved, success means:

1. An RM can complete a **happy-path Term (or chosen LOB) sale** for an existing customer in the bank UI.
2. Compliance can answer: **which agent, which distributor, which consent, which steps**.
3. Engineering can change aggregator behind a bank API **without rewriting RM/customer apps**.
4. Product can add the next LOB **without a new “platform”**.

---

## 7. Relationship to prior work

| Artefact | Treatment in this reset |
|----------|-------------------------|
| `docs/1sb-insurance-integration/**` | Prior research + engineering spike — **reference only** |
| Figma “For Client Review” | Primary UX/process hypothesis — **to be inventoried** |
| Uploaded baseline docs (pending) | Source material for BA synthesis |

---

## 8. Charter approval (blank)

| Role | Name | Decision | Date |
|------|------|----------|------|
| Platform PO | | Approve / Revise | |
| Bancassurance lead | | Approve / Revise | |
| Digital / Channel | | Approve / Revise | |
| Compliance | | Approve / Revise | |
| Architecture (advisory) | | Noted | |
