# SWOT analysis — AU Bank Insurance Distribution Platform

**Role:** Platform Product Owner  
**Scope:** Full programme (L1 channels + L2 platform + L3 integration), not only 1SB adapter  
**Inputs:** Baseline Volumes/Phases, knowledge base, prior 1SB engineering spike, Figma (uninventoried)

---

## SWOT matrix

| | **Helpful** | **Harmful** |
|---|-------------|-------------|
| **Internal** | **Strengths** | **Weaknesses** |
| **External** | **Opportunities** | **Threats** |

---

## Strengths (internal)

| ID | Strength | Evidence | How we exploit |
|----|----------|----------|----------------|
| S1 | Clear strategic vision: bank-owned, insurer-agnostic | Vol 01, Phase 1 | Use as sponsor narrative; reject vendor-UI shortcuts |
| S2 | Complete capability map covering full value stream | Phase 1, Vol 01/03 | Organise epics and ownership by capability |
| S3 | Journey vocabulary (CJ/RMJ/JRN) ready for UX & BA | Vol 06, Phase 3 | Bind Figma + stories to IDs |
| S4 | Explicit replaceability / Integration Hub strategy | Vol 01, Phase 5 | Keep L3 adapters thin; protect L2 contracts |
| S5 | Compliance capabilities first-class (consent, suitability, audit) | Goals BG-006, capabilities | Make them R0 gates, not Phase-2 debt |
| S6 | Prior engineering spike on 1SB quote/proposal/payment/status | `docs/1sb-insurance-integration/` | Seed L3 Phase A; do not pretend platform is done |
| S7 | Stakeholder catalogue exists | Vol 04 | Build RACI and workshop invites quickly |
| S8 | Guiding principles (canonical, config, audit) | Vol 01 | Architecture review checklist |

---

## Weaknesses (internal)

| ID | Weakness | Evidence | How we mitigate |
|----|----------|----------|-----------------|
| W1 | BR catalogues are template-thin (no real AC) | Vol 03, Phase 2 | BA deep-dive pack before sprint commit |
| W2 | Business rules are categories, not executable rules | Phase 4 | Rule pack workshops (consent/suitability/quote) |
| W3 | Information model has objects, not attributes/SoR | Phase 4 | Domain attribute workshops post R0 freeze |
| W4 | Geography / branch cohort for pilot not frozen | WD closed LOB/insurer groups; geography silent | Freeze pilot geography after sponsor validation |
| W5 | Figma not mapped to journeys | Intake | Screen inventory workshop (Figma = reference only) |
| W6 | Dual narrative risk: “integration done” vs “platform” | Eng vs Vol 01 | PO communication; layered backlog |
| W7 | NFRs lack numbers (SLA, availability, retention days) | Phase 5 principles only | NFR workshop with Infosec/Ops |
| W8 | ~~R0 channel unclear~~ → **Resolved working (D-002)**; residual = self/hybrid UX detail thin | WD §2 vs GAP-023 | Design hybrid mode-switch; do not re-open channel debate |
| W9 | Renewals/servicing listed while claims excluded — boundary fuzzy | Phase 1 vs Vol 01 | Explicit R0/R1/R2 out-list |
| W10 | Programme sponsorship **name** not filled (role known) | Stakeholder templates | GAP-010 |

---

## Opportunities (external)

| ID | Opportunity | Why it matters | How we capture |
|----|-------------|----------------|----------------|
| O1 | Bancassurance growth / insurance penetration | BG-003 | R0 proves conversion funnel instrumentation |
| O2 | Multi-insurer comparison as RM selling tool | CJ-07, Quote Comparison | Design compare UX early for first LOB |
| O3 | Progressive independence from aggregator | Roadmap Phases A–C | Contract tests on ports from day 1 |
| O4 | Reuse bank CIF / identity / branch network | Existing customer persona | Prefill as R0 advantage |
| O5 | Configurable Product Matrix for rapid launches | BR-PROD, BG-007 | Invest in catalogue admin early (R1) |
| O6 | Regulatory push for suitability & auditability | BG-006, IRDAI mention | Make evidence pack a selling point to Compliance |
| O7 | Figma already exists for client review | Prototype URL | Compress UX discovery if we inventory fast |
| O8 | Prior 1SB sandbox/docs research shortens L3 | Existing field guides | Parallelize L3 hardening with L2 discovery |

---

## Threats (external)

| ID | Threat | Impact | How we respond |
|----|--------|--------|----------------|
| T1 | 1SB commercial/sandbox/panel delays | Blocks Phase A | Early CONFIRM with Partner RM; contingency date |
| T2 | Scope creep to “full platform + all LOBs + servicing” | No pilot | Ruthless R0 cut; parking lot for R2+ |
| T3 | Compliance rejects weak suitability/consent | Go-live block | Compliance in Session 1–2; rules before UI polish |
| T4 | Vendor lock-in via leaked 1SB shapes into apps | Strategic failure | ArchUnit / API review gates |
| T5 | Dynamic proposal forms ≠ static Figma screens | Rework | Prototype form renderer early |
| T6 | Payment redirect / reconciliation failures | Drop-off, ops pain | Explicit payment AC + retry journey |
| T7 | Multi-LOB UW/medical complexity in first release | Delay | Term-like LOB first if possible; UW tracking lite in R0 |
| T8 | Org confusion between Distribution Platform vs Integration Service teams | Duplicate/gap delivery | Single PO backlog; layered epics |
| T9 | Data residency / PII retention ambiguity | Infosec stop | Q-P0-19 before prod data |
| T10 | RM adoption failure if workspace worse than today’s tools | No conversion | RM co-design; measure TAT |

---

## Strategic implications (PO decisions)

| Theme | Implication |
|-------|-------------|
| **S + O** | Double-down on bank-owned journey + measurable conversion; use Figma + CIF for fast R0 |
| **S + T** | Use principles (canonical, audit) as shields against lock-in and compliance rejection |
| **W + O** | Fund BA/rule workshops now — opportunity window closes if we code on templates |
| **W + T** | Highest danger zone: thin requirements + scope creep + vendor delay → freeze R0 this month |

### Priority responses (next 30 days)

1. **Sponsor formal validation** of Working Decisions + R0-SCOPE (D-001…D-007, D-009 already working).  
2. Inventory Figma → CJ/RMJ (reference only).  
3. Write executable Consent + Suitability **content** packs (gates already locked).  
4. Align PRD / BRD-P0 chapters to BRD Overview + three journeys.  
5. Align L3 eng spike as Integration Hub epic under platform programme — not a parallel product.  
6. Name Sponsor; publish RACI for Open Validation Items.
