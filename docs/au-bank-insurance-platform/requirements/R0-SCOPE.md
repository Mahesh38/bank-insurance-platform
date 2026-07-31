# R0 Scope One-Pager — AU Bank Insurance Distribution Platform

**Version:** 0.1 (PO draft)  
**Journey focus:** JRN-001 RM Assisted New Policy Purchase  
**Status:** ASSUMPTIONS pending Session 1 sign-off

---

## 1. Goal of R0

Prove that an AU Bank RM can take an **existing customer** through a **compliant, auditable** insurance sale on the bank platform — from lead to **payment completion and policy issuance visibility** — for **one LOB**, via **Integration Hub → 1SB**, without the RM using a third-party insurer portal as the primary workspace.

**Maps to:** BG-001, BG-002, BG-005, BG-006 (primary); BG-003 measured.

---

## 2. Working assumptions (confirm or replace)

| ID | Assumption | Confirm with |
|----|------------|--------------|
| A1 | First LOB = **Term Life** (or nearest bancassurance priority) | Bancassurance |
| A2 | Channel = **RM-assisted only** (customer may do OTP/payment steps) | Digital |
| A3 | Customers = **existing bank customers** (CIF lookup) | Product + Compliance |
| A4 | Connectivity = **1SB Phase A** only | Partners |
| A5 | Done = **Payment success + Policy issued/visible** (not quote-only) | Sponsor |
| A6 | Suitability **mandatory** before quote | Compliance |
| A7 | UW/docs = **status tracking lite** (not full document DMS in R0) | Ops + PO |
| A8 | Self-service full journey = **Out** of R0 | Digital |
| A9 | Renewals / claims / multi-LOB = **Out** of R0 | PO |

---

## 3. In scope (R0)

| Area | In |
|------|-----|
| Identity | RM authentication + basic roles |
| Customer | Search/retrieve existing customer; prefill |
| Lead | Create, assign to RM, resume, status |
| Consent | Capture, version, gate journey, audit evidence |
| Suitability | Capture assessment; record recommendation; block if fail (per rules) |
| Product | Read-only catalogue + eligibility matrix for first LOB |
| Quote | Request, poll/async, compare, select |
| Proposal | Dynamic form render, save/resume, submit, status |
| UW | Show insurer/application status & pending requirements (read) |
| Payment | Payment URL/session, HTTPS redirect, status, retry |
| Policy | Show policy number/status/docs link when issued |
| RM workspace | My leads / in-progress journeys / tasks (minimum) |
| Audit | Actor, action, journey id, agent/distributor attribution |
| Integration Hub | 1SB adapter for quote/proposal/payment/status (+ masters as needed) |
| Reporting | Funnel counts for pilot (lead→quote→proposal→pay→policy) |

---

## 4. Out of scope (R0)

- Full customer self-serve purchase journey  
- New-to-bank customer acquisition onboarding (unless A3 overturned)  
- Health / Motor / other LOBs  
- Direct insurer adapters (Phase B/C)  
- Claims  
- Full renewals suite  
- Rich ops MIS / executive BI suite  
- Admin UI for arbitrary rule authoring (seed config OK)  
- Multi-aggregator routing  
- Perfect offline / branch-kiosk modes  

---

## 5. Success metrics (pilot)

| Metric | Target (draft) |
|--------|----------------|
| JRN-001 happy path completable in UAT | 100% critical path scripts green |
| RM processing time vs baseline | Measure baseline first; improve directionally |
| Quote → proposal conversion (pilot) | Instrument; target TBD after 2 weeks data |
| Consent + suitability evidence completeness | 100% of submitted proposals |
| Sev-1 production defects in pilot month | 0 unresolved > 48h |
| PII in app logs | 0 confirmed incidents |

---

## 6. Dependencies

| Dependency | Needed by |
|------------|-----------|
| 1SB sandbox credentials + distributorId + IP allowlist | Wave 1 |
| Bank SSO / RM identity | Wave 1 |
| CIF/customer API access | Wave 1 |
| Payment landing URL hosting | Wave 1 |
| Compliance approval of consent/suitability text | Wave 0 |
| Named pilot branches / RM cohort | Wave 2 |

---

## 7. Sign-off

| Role | Name | Decision | Date |
|------|------|----------|------|
| Business Sponsor | | Approve / Revise | |
| Insurance Business Head | | Approve / Revise | |
| Compliance | | Approve / Revise | |
| Platform PO | | Approve / Revise | |
| Architecture (noted) | | Noted | |
