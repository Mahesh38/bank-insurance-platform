# Project charter — AU Bank Insurance Platform

**Document owner:** Platform Product Owner  
**Status:** Draft aligned to [Working Decisions v1](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)  
**Bank:** AU Bank  
**Version:** 0.3 (Working Decisions Draft v1)

---

## 1. One-sentence intent

AU Bank will build a **bank-owned, insurer-agnostic Insurance Distribution Platform** that owns the journey from **lead → need analysis → consent → suitability → quote → proposal → payment → policy issuance**, supporting **RM-assisted, customer self-service, and hybrid** journeys from Day 1 for **ETB Life** customers, using **1SilverBullet as the current integration layer** (Group A) while remaining replaceable — without rewriting the bank business model.

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

| Persona | Primary need | Channel |
|---------|--------------|---------|
| **RM / Branch staff** | Assist / complete Life sale; share links; never take payment on RM device | RM app / assisted web |
| **Customer (ETB)** | Self-serve or hybrid purchase; complete payment on personal device; receive policy | Bank digital channels + assisted steps |
| **Bancassurance ops** | Product catalog, insurer enablement, exception handling | Ops console (later?) |
| **Compliance / Audit** | Reconstruct who did what, with which agent/distributor | Reports / audit store |
| **Platform / Integration** | Stable bank APIs; hide aggregator protocol | Internal services |

---

## 4. In scope / out of scope

**SSOT:** [07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) (working draft pending formal validation).

### **In scope** — MVP (working freeze)

- **LOB:** Life only — Term, ULIP, Savings / Investment (+ future life as required)
- **Journeys:** RM-assisted + Self-service + Hybrid (seamless mode switch) from Day 1
- **Segment:** ETB only (any AU Bank relationship)
- Bank-owned Product Catalogue (Group A + Group B)
- Group A: in-platform quote → proposal → payment → issuance via 1SB
- Group B: recommendation → redirect to insurer (no 1SB quote)
- Mandatory need analysis + suitability before quote; mandatory consent (sequencing TBD)
- SSO, bank notifications, AU Bank PG only; Lead module in-platform (→ Sampath later)
- Sold = policy issued + confirmation + reconcilable + ops-trackable

### **Out of scope** — MVP

- Health, Motor, Travel, other non-life LOBs
- NTB customers
- Embedded / loan-disbursement insurance offers
- Multi-aggregator routing; bank-owned aggregation layer
- Branch kiosk (pending business decision)
- Claims, renewals, servicing as full product suites
- Replacing bank CRM / CIF / core banking
- Third-party payment gateway; payment on RM device

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

1. An RM or ETB customer can complete a **happy-path Life sale** (assisted, self, or hybrid) through **policy issuance**.
2. Compliance can answer: **which agent, which distributor, which consent, which suitability steps**.
3. Engineering can change aggregator behind a bank API **without rewriting** channel apps.
4. Product can extend Life variants / Group B redirect **without a new “platform”**.
5. Quotes, proposals, and payments alone are **not** counted as Sold.

---

## 7. Relationship to prior work

| Artefact | Treatment in this reset |
|----------|-------------------------|
| `docs/1sb-insurance-integration/**` | Prior research + engineering spike — **reference only** |
| Figma “For Client Review” | UX reference only — **not SoT** (Working Decisions §15) |
| Working Decisions Draft v1 | **Working SSOT** for MVP scope until formally validated |
| Uploaded baseline docs | Source material for BA synthesis (KB) |

---

## 8. Charter approval (blank)

| Role | Name | Decision | Date |
|------|------|----------|------|
| Platform PO | | Approve / Revise | |
| Bancassurance lead | | Approve / Revise | |
| Digital / Channel | | Approve / Revise | |
| Compliance | | Approve / Revise | |
| Architecture (advisory) | | Noted | |
