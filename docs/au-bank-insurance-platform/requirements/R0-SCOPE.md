# R0 / MVP Scope One-Pager — AU Bank Insurance Distribution Platform

**Version:** 0.3  
**Status:** Aligned to [Business Clarifications & Working Decisions v1](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)  
**Journey focus:** RM-assisted + Self-service + Hybrid (all Day 1)  
**Sold =** Policy issued + confirmation + reconcilable + ops-trackable  
**Executive sponsor perspective:** [Dilip — AI Executive Sponsor Perspective](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/README.md)

---

## 1. Goal of MVP

Deliver AU Bank’s Life Insurance distribution platform for **ETB customers**, supporting **RM-assisted, self-service, and hybrid** journeys, with:

- Mandatory need analysis & suitability before quote  
- Group A insurers fully inside the platform via **1SB**  
- Group B insurers via catalogue + **redirect** (no quote via 1SB)  
- Payment on **customer device** via **AU Bank PG** (and IFT where applicable)  
- Success measured as **policy sold** (issuance), not quote/proposal/payment alone  

**Maps to:** BG-001…BG-006.

For material scope, investment or pilot-outcome questions, use the Dilip AI Executive Sponsor Perspective to challenge the business case before Rajal records the canonical Product decision. This lens does not replace a real AU Bank sponsor, Product authority, specialist gate or mandatory human approval.

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
| A10 | Lead module **in Insurance Platform** — working inbox (create/resume/convert/archive). Opportunity is an alias only. 7-year SoT is Payment, Policy history, Consent, Suitability, Audit |
| A11 | Figma = **reference only** |
| A12 | 1SB = current integration layer (not a temporary hack); no tight coupling |

---

## 3. In scope (MVP)

| Area | In |
|------|-----|
| Identity | Bank SSO / AD path per BRD §1–2 |
| User management | Roles, access, hierarchy (bank; partner depth as needed) |
| Lead | In-platform Lead module (create/update/follow-up/assign/convert/archive). Bulk/campaign stays phased. Name is **Lead** |
| Customer | CBS fetch (Cust ID / Mobile / PAN etc.) for ETB |
| Rules / workflow | Core lead/application gates; configurable where possible |
| Suitability | Check, partner product list, PDF, consent records |
| Catalogue | Bank-owned; includes Group A + Group B |
| Quote (Group A) | List/compare/modify/share/eBI via platform + 1SB |
| Redirect (Group B) | Recommendation → insurer platform |
| Proposal (Group A) | Prefill/masking, consents, submission, status |
| Risk / fraud | PTL/RAG and related flows as partner-ready (configurable) |
| Payment | Dual mandate (as decided), IFT, AU Bank PG; cheque may phase |
| Issuance | Status, confirmation, policy PDF, **historic state transitions**, `issuanceMode` STP / non-STP / Insta |
| Off-platform book | MIS upload of offline / insurer-portal policies (`source=OFF_PLATFORM`); never creates a Lead |
| Reporting | R0 MIS: funnel, sold, on- vs off-platform, products needing onboarding — isolated read path |
| Administration | R0 admin panel: configuration maker-checker and report generation |
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

### Sponsor-level measurement requirement

Before pilot scale-out, the Product/business view should also identify the evidence needed to answer:

- where the funnel drops between lead, suitability, quote, proposal, underwriting, payment and issuance;
- whether assisted/self-service/hybrid journeys are actually being used as intended;
- whether the bank can operationally and financially track an issued policy;
- which insurer/partner dependency materially affects conversion or turnaround;
- which measurable result justifies continuing, changing or scaling the investment.

Numeric targets must come from approved business baselines or explicit assumptions; the AI sponsor lens must not invent them.

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

## 7. Executive sponsor checkpoint

For any material change to R0 scope, a significant `Should` deferral, material new digital capability, build/buy/partner choice or pilot-success decision, request the [Dilip AI sponsor perspective](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/03-invocation-and-decision-contract.md).

The expected result is one of:

- `ENDORSE`
- `ENDORSE_WITH_CONDITIONS`
- `CLARIFY`
- `DEFER`
- `DO_NOT_ENDORSE`

Rajal then records the Product scope/priority decision through the existing governance model. A sponsor-perspective endorsement is **business input**, not permission to bypass Architecture, Security, Compliance/Risk, QA, SRE, Database or human sign-offs.

---

## 8. Sign-off and perspective record

| Role / perspective | Name | Decision | Date |
|------|------|----------|------|
| **AI Executive Sponsor Perspective** | **Dilip lens** | ENDORSE / ENDORSE WITH CONDITIONS / CLARIFY / DEFER / DO NOT ENDORSE | |
| **Platform PO — canonical Product authority** | **Rajal** | Approve / Revise | |
| Accountable human business sponsor, when organizational process requires one | *Per AU Bank process / TBC* | Human approval; AI output cannot satisfy this | |
| Compliance | | Approve / Revise (esp. §8–9, §16) | |
| Architecture (noted) | | Noted | |

> **Identity rule:** a record produced by the AI lens must say `AI Executive Sponsor Perspective`; it must never be represented as the real Dilip Kumar Vidyarthi personally signing or approving this scope.
