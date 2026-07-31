# R0 / MVP Scope One-Pager — AU Bank Insurance Distribution Platform

**Version:** 0.2  
**Status:** Aligned to [Business Clarifications & Working Decisions v1](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)  
**Journey focus:** RM-assisted + Self-service + Hybrid (all Day 1)  
**Sold =** Policy issued + confirmation + reconcilable + ops-trackable

---

## 1. Goal of MVP

Deliver AU Bank’s Life Insurance distribution platform for **ETB customers**, supporting **RM-assisted, self-service, and hybrid** journeys, with:

- Mandatory need analysis & suitability before quote  
- Group A insurers fully inside the platform via **1SB**  
- Group B insurers via catalogue + **redirect** (no quote via 1SB)  
- Payment on **customer device** via **AU Bank PG** (and IFT where applicable)  
- Success measured as **policy sold** (issuance), not quote/proposal/payment alone  

**Maps to:** BG-001…BG-006.

---

## 2. Working decisions (locked unless overturned)

| ID | Decision |
|----|----------|
| A1 | LOB = **Life only** (Term, ULIP, Savings/Investment, future life) |
| A2 | Channels = **RM + Self-service + Hybrid** from Day 1 |
| A3 | Customers = **ETB only** (any AU Bank relationship) |
| A4 | Group A connectivity = **1SB** (ICICI Pri, HDFC Life, Bajaj… as applicable) |
| A5 | Group B = Product Catalogue + **redirect** to insurer |
| A6 | Sold = **policy issued** + confirmation + recon + ops track |
| A7 | Suitability + Need Analysis **mandatory** before quote |
| A8 | Consent **mandatory**; sequencing pending compliance R&D |
| A9 | Payment on **customer device**; **no RM-device payment**; AU Bank PG only |
| A10 | Lead module **in Insurance Platform** (future migrate to Sampath) |
| A11 | Figma = **reference only** |
| A12 | 1SB = current integration layer (not a temporary hack); no tight coupling |

---

## 3. In scope (MVP)

| Area | In |
|------|-----|
| Identity | Bank SSO / AD path per BRD §1–2 |
| User management | Roles, access, hierarchy (bank; partner depth as needed) |
| Lead | In-platform Lead module (create/update/follow-up/assign; bulk/campaign may phase) |
| Customer | CBS fetch (Cust ID / Mobile / PAN etc.) for ETB |
| Rules / workflow | Core lead/application gates; configurable where possible |
| Suitability | Check, partner product list, PDF, consent records |
| Catalogue | Bank-owned; includes Group A + Group B |
| Quote (Group A) | List/compare/modify/share/eBI via platform + 1SB |
| Redirect (Group B) | Recommendation → insurer platform |
| Proposal (Group A) | Prefill/masking, consents, submission, status |
| Risk / fraud | PTL/RAG and related flows as partner-ready (configurable) |
| Payment | Dual mandate (as decided), IFT, AU Bank PG; cheque may phase |
| Issuance | Status, confirmation, policy PDF & customer communication |
| Reporting | Funnel + sold definition; full MIS may phase |
| Integration | 1SB UAT → prod; credentials model per Working Decisions §12 |

---

## 4. Out of scope (MVP)

- Health / Motor / Travel and other non-life LOBs  
- New-to-Bank (NTB) onboarding  
- Third-party payment gateways  
- Payment on RM device  
- Multi-aggregator routing (extensibility only)  
- Bank-owned aggregation layer  
- Embedded / loan-disbursement insurance offers  
- Branch kiosk (pending)  
- Treating Figma as SoT  
- Treating quote/proposal/payment as “sold”  

---

## 5. Success metrics (MVP)

| Metric | Target |
|--------|--------|
| Policy **Sold** count (issuance definition) | Primary commercial KPI |
| ETB Life journeys completable (RM / self / hybrid) | Critical path UAT green for Group A |
| Suitability never bypassed before quote | 100% of quote attempts gated |
| Payment never on RM device | Design + test proof |
| Group B redirect path usable | Recommendation → insurer link |
| Consent & suitability evidence | 100% on submitted Group A proposals |

---

## 6. Dependencies

| Dependency | Needed |
|------------|--------|
| 1SB UAT + Distributor ID | Integration start |
| Bank SSO | Login |
| CBS | ETB fetch |
| AU Bank Payment Gateway | Online payment |
| Bank notifications | Customer/RM communications |
| Compliance R&D for consent sequencing | Before hard-coding consent UX |
| Agent identity validation | Attribution model freeze |

---

## 7. Sign-off

| Role | Name | Decision | Date |
|------|------|----------|------|
| Head of Insurance Business / Platform (Sponsor) | *TBC* | | |
| Platform PO | | Approve / Revise | |
| Compliance | | Approve / Revise (esp. §8–9, §16) | |
| Architecture (noted) | | Noted | |
