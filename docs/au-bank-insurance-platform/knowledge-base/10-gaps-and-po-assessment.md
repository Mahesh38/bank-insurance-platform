# 10 — Gaps & Platform PO assessment

**Date:** Discovery restart after baseline PDF intake; **updated 2026-07-31** after Working Decisions Draft v1  
**Verdict:** Strong **framework**; weak **specificity**. Usable as programme skeleton — **not** yet build-ready requirements.  
**Operational gap tracker:** [../po-drive/02-GAP-REGISTER.md](../po-drive/02-GAP-REGISTER.md) (prefer this for status).  
**Scope SSOT:** [../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)

---

## Maturity scorecard

| Area | Maturity | Comment |
|------|----------|---------|
| Vision & strategy | **High** | Clear bank-owned, insurer-agnostic intent |
| Business goals (BG-001…008) | **High** | Traceable goal set |
| Capability map | **High** | Complete enough to organise work |
| Journey / value stream spine | **Medium-High** | CJ/RMJ/JRN + shared flow are clear |
| Stakeholder inventory | **Medium** | Roles listed; responsibilities templated |
| Process catalogue | **Medium** | Purposes clear; triggers/rules generic |
| Detailed BR + AC | **Low** | Vol 03 / Phase 2 largely repeated templates |
| Concrete business rules | **Low** | Categories only; few executable statements |
| Information model depth | **Low** | Object list without attributes/SoR IDs |
| LOB / product / insurer scope | **Working (WD)** | Life LOB; Group A/B — was Missing at PDF intake |
| Channel / segment / Sold | **Working (WD)** | Three journeys; ETB; Sold=issuance |
| UX (Figma) linkage | **Missing** | Prototype not inventoried into CJ/RMJ |
| Non-functional targets | **Low** | Principles only (no SLOs/numbers) |

---

## What we can treat as adopted (working)

1. Platform owns journey end-to-end (lead → **issuance**), not just connectivity.  
2. **RM + self-service + hybrid** from Day 1 (D-002).  
3. Consent + suitability are first-class; need analysis/suitability mandatory before quote (D-005, D-011).  
4. Product Catalogue / Matrix drives eligibility; Group A quotes vs Group B redirect (D-010).  
5. 1SB current layer, then replaceable via Integration Hub (D-003, D-004).  
6. Canonical model + configuration + audit are non-negotiable.  
7. Claims admin and insurer cores are out of platform scope (for now).  
8. Journeys to elaborate: assisted, self-service, and hybrid Life paths for ETB.

---

## Critical gaps still blocking formal build freeze

| Gap | Why it blocks | Owner | Gap register |
|-----|---------------|-------|--------------|
| Suitability **content** & override rules | Regulatory + UX | Compliance + BA | GAP-007 |
| Consent sequencing, wording, TTL | Legal gate | Compliance | GAP-006 |
| Quote validity / compare rules (Group A) | Sales behaviour | Product | GAP-012 |
| Payment failure/retry AC | Ops pain | Payments + Digital | GAP-011 (partial) |
| Sponsor **name** + formal WD sign-off | Governance | Sponsor + PO | GAP-010 |
| Figma → journey mapping | Avoid building demo screens | Digital + BA | GAP-009 |
| Replace BR templates with real AC under BRD Overview | Eng cannot estimate or test | BA | GAP-008 |
| Attribute-level information model | Data & API design | BA + Architect | GAP-016 |
| Agent identity model | Attribution | Ops + Compliance | GAP-014 |
| NFR numbers (retention, residency) | Infosec | Compliance + Infosec | GAP-017 |

**Closed at Working Decisions (do not re-open as “missing scope”):** LOB (Life), channel (three journeys), ETB, Sold=issuance, Group A/B, AU Bank PG / customer-device payment, 1SB replaceability stance.

---

## Source-doc quality note (transparent)

Volume 03 and Phase 2 use the **same structural paragraph** for nearly every BR-* capability (CRUD + audit + configurable rules). Phase 3 JRN-* entries largely reuse one shared flow with different titles.  

**PO interpretation:** these are **placeholders proving coverage of the map**, not finished requirements. Next BA revision must expand per capability: priorities, AC, data ownership, alternate/exception detail, UI expectations, regulatory refs.

---

## Recommended next artefacts (BA backlog)

1. **R0 Scope One-Pager** — LOB, insurers, journeys in/out, success metric  
2. **JRN-001 detailed process** — swimlanes, screens, data, rules, exceptions  
3. **Consent & Suitability rule pack** — executable BR-RULE-xxx  
4. **Product Matrix v0** — eligibility dimensions for first LOB  
5. **Figma inventory** — screen → CJ/RMJ → MVP flag  
6. **BR deep-dive** for: Lead, Consent, Suitability, Quote, Proposal, Payment, Policy (P0 only)  
7. **Platform vs Integration Hub boundary** workshop (align with prior 1SB eng spike)

---

## Alignment with prior engineering work

| Prior eng focus | Platform KB position |
|-----------------|----------------------|
| Thin 1SB integration (quote/proposal/payment/status) | Belongs under **Integration Hub / Phase A adapter** |
| Bank-canonical APIs for those ops | Compatible — keep |
| Missing lead/consent/suitability/catalogue/RM workspace | **Must be product backlog for the Distribution Platform**, not assumed done |

---

## Decision prompts for Session 1 (carry forward)

Reuse D-001…D-008 in `../01-stakeholder-working-session.md`, now informed by this KB:

- D-001 First LOB  
- D-002 RM-only vs hybrid  
- D-003 1SB confirmed for AU Bank  
- D-007 R0 definition of done (recommend: **through policy issuance visibility**, payment mandatory)  
- Add **D-009**: Existing-customer-only for R0? (docs list new/prospective too)
