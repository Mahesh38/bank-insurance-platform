# BRD Overview — AU Bank Insurance Distribution Platform

**Document type:** Business Requirements Document — Overview / Table of Contents  
**Bank:** AU Bank  
**Owner:** Platform Product Owner  
**Status:** Heading structure **approved by PO**. Detailed chapters for Login, Lead, Suitability (Savings/ULIP), Savings listing/details/quote, Pitch Deck, Customer buying, and Exception Handling are in [brd-detailed/](./brd-detailed/README.md) (`DOC-005`). Remaining chapters still need a detailed BRD.  
**Version:** 1.1  
**Related:** [brd-detailed/](./brd-detailed/README.md) · [alignment](./BRD-ALIGNMENT-2026-09-28.md) · [Working Decisions](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) · [R0-SCOPE.md](./R0-SCOPE.md) · [knowledge-base/](../knowledge-base/README.md) · [po-drive/](../po-drive/00-PO-PROJECT-VIEW.md)

---

## How to use this document

1. This overview is the **binding chapter map** for the BRD.  
2. Do **not** invent parallel top-level modules without PO approval. Detailed behaviour for a numbered chapter is in [brd-detailed/](./brd-detailed/README.md) when a file exists; do not invent a chapter from this TOC alone.  
3. Each numbered section below becomes a BRD chapter with: business objective, actors, functional requirements, rules, exceptions, AC, data, integrations, reports.  
4. Priority tags (R0 / R1 / R2+) are **PO sequencing guidance** for delivery — not a change to the heading list.  
5. Scope constraints from Working Decisions apply across chapters: **Life LOB**, **ETB**, **three journeys Day 1**, **Group A/B insurers**, **Sold = issuance**.

| Tag | Meaning |
|-----|---------|
| **R0** | Needed for MVP / first controlled pilot (Life; RM + self + hybrid) |
| **R1** | Scale / hardening soon after pilot |
| **R2+** | Roadmap after core journey is stable |

---

## Approved BRD overview (PO)

### 1. Login
**Detailed BRD:** [Login_Module_BRD_Detailed_CONTEXT.md](./brd-detailed/Login_Module_BRD_Detailed_CONTEXT.md) (v1.0, 04-Sep-2026). That file **removes Forgot Password** from both login modes; partner recovery is Unlock User. Bank RM uses Employee ID + bank-system password + OTP.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 1.1 | AD Integration | R0 |
| 1.2 | Forgot Password | Removed in Login BRD v1.0 — Unlock User replaces it for partners; Bank RM password is out of platform scope |
| 1.3 | Account Lock / Unlock | R0 |
| 1.4 | SSO redirection | R0 — Login BRD describes bank-system password + OTP, not a named SSO redirect; confirm with Infosec / Mahesh |

---

### 2. User Management Module
| # | Sub-heading | Priority |
|---|-------------|----------|
| 2.1 | Role Rights | R0 |
| 2.2 | User Access & Permission | R0 |
| 2.3 | User Hierarchy Management (Both AU Bank & Partner) | R0 (bank) / R1 (partner depth) |
| 2.4 | User Create / Update & Terminate Module | R0 |

---

### 3. Lead Management
**Detailed BRD:** [Lead_Module_BRD_Detailed_CONTEXT.md](./brd-detailed/Lead_Module_BRD_Detailed_CONTEXT.md) (v1.0). CBS search is in this BRD (overview §4). Bulk / campaign stay out unless Rajal issues a formal scope change. Assignment timing conflicts with Customer Buying Journey v1.2 — see [alignment C3](./BRD-ALIGNMENT-2026-09-28.md#c3--insurance-rm-assignment-timing-and-bank-rm-vs-bank-sp).

| # | Sub-heading | Priority |
|---|-------------|----------|
| 3.1 | Lead Create | R0 |
| 3.2 | Lead Update | R0 |
| 3.3 | Lead follow-up & Reminder (Meeting) | R0 |
| 3.4 | Lead Bulk upload | R1 |
| 3.5 | Lead Assignment & Reassignment (Individual & bulk) | R0 (individual) / R1 (bulk) |
| 3.6 | Campaign & Drive Lead module | R1 |
| 3.7 | Lead duplicate flagging, Lead Expiry & Lead Deletion | R0 |

---

### 4. Customer Data Fetch
**Covered in** the Lead BRD (Customer ID / mobile / PAN search). No separate September 2026 BRD.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 4.1 | CBS Integration (Fetch data using Cust ID, Mobile No, PAN etc.) | R0 |

---

### 5. Lead / Application Rules and workflow
**Partial:** [Exception_Handling_BRD_V1.0_CONTEXT.md](./brd-detailed/Exception_Handling_BRD_V1.0_CONTEXT.md) covers block / approval / hierarchy. A configurable rule designer is **not** in this pack.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 5.1 | Lead Rules | R0 |
| 5.2 | New Rule creation and updating existing rules | R1 (seed config in R0) |
| 5.3 | Workflow management | R0 (core gates) / R1 (configurable designer) |
| 5.4 | Approval management and records | R0 (mandatory approvals) / R1 (full matrix) |

---

### 6. Suitability Check Module
**Detailed BRD (Savings / ULIP only):** [Suitability_Module_BRD_Detailed_CONTEXT.md](./brd-detailed/Suitability_Module_BRD_Detailed_CONTEXT.md). Term suitability is **not** in this pack. Tentative premium / PPT / PT formulas are TBD.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 6.1 | Suitability Check | R0 |
| 6.2 | List product based on suitability for each partner | R0 |
| 6.3 | Suitability PDF creation | R0 |
| 6.4 | Suitability Consent Capture and storing records | R0 |

---

### 7. Quote list & Compare
**Detailed BRDs (Savings only):** [listing/compare](./brd-detailed/Savings_Listing_and_Compare_Plans_BRD_Detailed_CONTEXT.md) · [product details](./brd-detailed/Product_Details_Savings_Plan_BRD_Detailed_CONTEXT.md) · [quote / riders / final BI](./brd-detailed/Quote_Finalisation_Riders_and_Final_BI_BRD_Detailed_CONTEXT.md) · [pitch deck](./brd-detailed/Create_Pitch_Deck_BRD_Detailed_CONTEXT.md). Term / ULIP listing and details are **not** in this pack.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 7.1 | Quote listing logic | R0 |
| 7.2 | Sorting and Filter options | R0 |
| 7.3 | View more and product details | R0 |
| 7.4 | Downloads (Brochure, Policy wording etc.) | R0 |
| 7.5 | Compare Quote | R0 |
| 7.6 | Share Quote | R0 / R1 |
| 7.7 | Modify Quote (Change SA, PT, PPT, Paymode etc.) | R0 |
| 7.8 | Add-on covers (Addition & Deletion) | R0 (LOB-dependent) |
| 7.9 | Discounting (Credit Score, Existing customer discount, DIY Discount etc.) | R1 (simple existing-customer in R0 if mandated) |
| 7.10 | eBI sharing and acceptance | R0 |

---

### 8. Proposal journey
**Detailed BRD:** [Customer_Buying_Journey_BRD_V1.2_CONTEXT.md](./brd-detailed/Customer_Buying_Journey_BRD_V1.2_CONTEXT.md) (also covers §10 Payment and §11 Submission). Seller completes proposal; customer controls verification, consent, PIVC handoff, payment and submission.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 8.1 | Data prefill & Masking | R0 |
| 8.2 | Document waiver (Fetching & passing KYC, Credit score, Income proof etc.) | R0 / R1 |
| 8.3 | Proposal acceptance & Consent on eBI, CIS, Proposal form, Suitability etc. | R0 |
| 8.4 | ACR Process | R0 / R1 (confirm with Compliance) |

---

### 9. Insurance Risk and fraud detection module
| # | Sub-heading | Priority |
|---|-------------|----------|
| 9.1 | PTL API & RAG API | R0 / R1 (confirm partner readiness) |
| 9.2 | Pre-issuance verification process | R0 |
| 9.3 | Customer journey | R0 |
| 9.4 | SP journey | R0 |
| 9.5 | Both in a single call | R0 / R1 |
| 9.6 | Storing Video, PDF & sharing it with Insurance partners | R0 |
| 9.7 | Updating FR in Insurance system | R0 |

---

### 10. Payment
**Covered in** Customer Buying Journey v1.2 (IFT / PG / Cheque / eMandate). Cheque API lifecycle remains open until the integration contract is finalised.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 10.1 | Dual Payment Mandate | R0 / R1 |
| 10.2 | Payment Process | R0 |
| 10.2.i | Internal Fund Transfer | R0 |
| 10.2.ii | Online Payment options (Payment Gateway) | R0 |
| 10.2.iii | Cheque Payment Process | R1 |
| 10.3 | Payment & Mandate communication to customers | R0 |

---

### 11. Proposal Submission
**Covered in** Customer Buying Journey v1.2. Application ID is created only when Proposal Form initiation succeeds.

| # | Sub-heading | Priority |
|---|-------------|----------|
| 11.1 | Status update | R0 |
| 11.2 | Policy Issuance confirmation | R0 |
| 11.3 | Policy PDF & communication to customers | R0 |

---

### 12. Welcome Calling process
| # | Sub-heading | Priority |
|---|-------------|----------|
| 12.1 | Calling customer | R1 |
| 12.2 | Follow-up | R1 |
| 12.3 | Cancellation & Update to customer | R1 |
| 12.4 | Query resolution and update | R1 |

---

### 13. Post issuance policy status check
| # | Sub-heading | Priority |
|---|-------------|----------|
| 13.1 | Post issuance policy status check | R0 (read status) / R1 (full servicing views) |

---

### 14. Data Storage & Push to DWH
| # | Sub-heading | Priority |
|---|-------------|----------|
| 14.1 | Data Storage & Push to DWH | R0 (store) / R1 (DWH push) |

---

### 15. Commission calculation
| # | Sub-heading | Priority |
|---|-------------|----------|
| 15.1 | Commission calculation | R1 / R2+ |

---

### 16. Report, MIS & Dashboard
| # | Sub-heading | Priority |
|---|-------------|----------|
| 16.1 | Report, MIS & Dashboard | R0 (pilot funnel) / R1 (full MIS) |

---

## End-to-end flow (PO narrative)

These headings follow the bancassurance sale spine:

```text
Login / User Management
        → Lead Management + CBS Customer Fetch
        → Lead/Application Rules & Workflow
        → Suitability (+ PDF + Consent)
        → Quote list & Compare (+ eBI)
        → Proposal journey (+ consents / ACR)
        → Risk & fraud (PTL/RAG, video/PDF, FR update)
        → Payment (+ mandate & customer communication)
        → Proposal Submission → Policy issuance & PDF
        → Welcome calling (ops)
        → Post-issuance status
        → DWH / Commission / MIS
```

---

## Traceability to earlier platform language

| BRD overview (this doc) | Earlier KB / capability language |
|-------------------------|----------------------------------|
| §1–2 Login & User Mgmt | Identity & Access, RM Workspace |
| §3 Lead Management | Lead / RMJ-01… |
| §4 CBS Customer Fetch | Customer / CIF prefill |
| §5 Rules & workflow | Configuration, approvals |
| §6 Suitability | Suitability & Recommendation |
| §7 Quote list & Compare | Quote + Quote Comparison |
| §8 Proposal journey | Proposal |
| §9 Risk & fraud | UW / partner verification (bank-owned orchestration) |
| §10 Payment | Payment |
| §11 Proposal Submission / Issuance | Policy lifecycle |
| §12 Welcome Calling | Ops / Communications |
| §13 Post issuance | Policy status / servicing lite |
| §14–16 DWH / Commission / MIS | Reporting & Analytics |

Integration providers (e.g. 1SB) remain **behind** these business chapters — not separate top-level BRD modules unless PO adds them.

---

## Next BA work (per chapter)

For each approved heading, produce:

1. Business objective & actors  
2. Functional requirements (BR-xxx)  
3. Business rules  
4. Happy / alternate / exception flows  
5. Acceptance criteria  
6. Data elements & retention  
7. Upstream/downstream systems (CBS, AD/SSO, PG, insurer/partner APIs, DWH)  
8. UI references (Figma screen IDs when available)  
9. Priority confirmation (R0/R1/R2+) with Compliance / Ops sign-off where marked  

Start deep-dive order (PO recommendation): **§1 → §2 → §3 → §4 → §6 → §7 → §8 → §10 → §11**, then §5/§9, then §12–§16.

September 2026 pack covers §1, §3, §4 (inside Lead), §6 (Savings/ULIP), §7 (Savings), §8/§10/§11, plus Pitch Deck and Exception Handling. **Still missing a detailed BRD:** §2 User Management, Term suitability, ULIP listing/details, §9 Risk/fraud, §12–§16.

---

## Change control

| Version | Date | Change | Owner |
|---------|------|--------|-------|
| 1.0 | 2026-07-31 | PO-approved overview headings captured as BRD TOC | Platform PO |
| 1.1 | 2026-09-28 | Linked September 2026 detailed BRDs; annotated Forgot Password removal and open conflicts | Platform PO (pack) / BA ingest |

Any addition, rename, or removal of a top-level section requires **PO approval** and an update to this table.
