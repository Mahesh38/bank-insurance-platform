# Gap register — complete (Platform PO)

**ID scheme:** `GAP-xxx`  
**Severity:** P0 block freeze · P1 block pilot · P2 quality/scale · P3 later  
**Status:** Open · In progress · Closed · Deferred  
**Aligned to:** [Working Decisions Draft v1](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)

---

## P0 — Block scope / build freeze

| ID | Gap | Area | Owner | Impact if ignored | Exit criteria | Status |
|----|-----|------|-------|-------------------|---------------|--------|
| GAP-001 | First LOB not chosen | Scope | Bancassurance + PO | Wrong fields, insurers, UW model | Life only (Term/ULIP/Savings) | **Closed** (WD §1) |
| GAP-002 | R0 definition of done unclear | Scope | Sponsor + PO | Infinite delivery | Sold = issued + confirm + recon + ops | **Closed** (WD §4) |
| GAP-003 | R0 channel (RM-only vs hybrid) unclear | Channel | Digital + PO | Wrong auth/UX | RM + Self + Hybrid Day 1 | **Closed** (WD §2) |
| GAP-004 | Customer segment for R0 unclear | Customer | Product + Compliance | CIF vs full KYC scope | ETB only; any bank relationship | **Closed** (WD §3) |
| GAP-005 | Insurer panel for pilot unknown | Partners | Bancassurance | Cannot UAT | Group A named; Group B redirect | **Closed** (WD §§5–6) |
| GAP-006 | Consent rules not executable | Compliance | Compliance + BA | Legal reject | Consent rule pack v1 approved | **CONTENT-COMPLETE, RATIFICATION-PENDING** — [consent rule pack](../rule-packs/consent-rule-pack.md), 38 testable rules; [S02 evidence](../../application-lifecycle-bible/evidence/S02-regulatory-evidence.md). **Still OPEN**: S02-G3 needs Shailja's E2 signature |
| GAP-007 | Suitability content & override unknown | Compliance | Compliance + BA | Regulatory reject | Suitability pack v1 (mandatory gate locked) | **CONTENT-COMPLETE, RATIFICATION-PENDING** — [suitability rule pack](../rule-packs/suitability-rule-pack.md), 48 testable rules, `SUIT-ALGO-LIFE-v1.0`, override policy defined (no override in R0). **Still OPEN**: S02-G4 needs Shailja's E2 signature |
| GAP-008 | ~~BR templates lack AC~~ → **restated**: ACs exist but are assertion bullets, carry 8 unresolved `confirm` markers, and omit failure paths | Requirements | BA + PO | Cannot estimate/test | Given/When/Then AC on every R0 requirement + exception behaviour | **CLOSED on content** — [S03 evidence §4](../../application-lifecycle-bible/evidence/S03-requirements-evidence.md): 60 G/W/T criteria + 12 exception criteria; 5 of 8 `confirm` markers resolved. Gate S03-G1 needs QA + Compliance E2 signature. *The original wording was stale and is corrected here rather than closed against a description that no longer matched the artefact* |
| GAP-009 | Figma not mapped to CJ/RMJ | UX | Digital + BA | Build wrong screens | Inventory + MVP flags; Figma = reference | **CLOSED on mapping** — [S05 evidence §4.3](../../application-lifecycle-bible/evidence/S05-experience-evidence.md): 18-screen inventory mapped to requirement IDs and acceptance criteria, plus flow and 18×4 state catalogue. Screens are **built** in [`apps/rm-workspace-app`](../../../apps/rm-workspace-app/README.md). **Residual**: existing Figma material still to be mapped onto the inventory (S05-OPEN-01) and visual design not applied (S05-OPEN-02) |
| GAP-010 | Executive sponsor unnamed | Governance | PO | Slow decisions | Name + RACI (role known) | **OPEN — structure closed, name outstanding.** [S00 evidence §4.2/§4.3/§5](../../application-lifecycle-bible/evidence/S00-ideation-evidence.md) closes the role definition, decision rights, escalation path, steering design and the `FRI-001` funding line. **The named individual is not mine to write**, and the Dilip lens is an AI perspective, not a sponsor. Owner: Rajal → Bancassurance leadership. Target **2026-08-29**. Blocks `FRI-001` approval and therefore the recovery increment |

---

## P1 — Block safe pilot

| ID | Gap | Area | Owner | Exit criteria | Status |
|----|-----|------|-------|---------------|--------|
| GAP-011 | Payment failure/retry AC incomplete | Payments | Payments + Digital | Customer-device + AU Bank PG locked (D-006); failure/retry AC still needed | **Partial** |
| GAP-012 | Quote validity / compare rules missing | Sales | Product + BA | Quote rule pack v1 (Group A) | **CLOSED on content** — [S04 evidence §4.3](../../application-lifecycle-bible/evidence/S04-product-definition-evidence.md): QR-01…QR-12, including validity (shorter of insurer window and 7 days), re-quote triggers, comparison basis, and the disclosed ranking rule with commission excluded as an input (QR-07). Ranking rule implemented and asserted in `apps/rm-workspace-app` |
| GAP-013 | Product Matrix dimensions undefined | Products | Product | Matrix v0 for Life catalogue | **CLOSED on dimensions** — [S04 evidence §4.2](../../application-lifecycle-bible/evidence/S04-product-definition-evidence.md): 11 matrix dimensions, the R0 population rule (one Group A / Life / Term slot) and catalogue rules CAT-R01…R06. **Residual**: the insurer, product and eligibility *values* are commercial facts — UNKNOWN, owner Bancassurance, S04-OPEN-01 |
| GAP-014 | AgentId / RM mapping model incomplete | Attribution | Ops + Compliance | Mapping source + fail behaviour | **HALF-CLOSED** — behaviour defined: `AC-PROP-030-2` (expired SP certification → `403 AGENT_CERTIFICATION_EXPIRED`, no insurer call, ops task) in [S03 evidence §4.5](../../application-lifecycle-bible/evidence/S03-requirements-evidence.md). **Open**: where `agentId` and certification expiry are sourced from (S03-OPEN-03, depends on WS-2) |
| GAP-015 | 1SB commercial + sandbox confirmation | Partners | Bancassurance | Keys, distributorId, IP, panel | **In progress** (agreement signed; UAT provisioning; Distributor ID soon) |
| GAP-016 | Information model attributes missing | Data | BA + Architect | Attribute sheets for P0 objects | **CLOSED on content** — [S03 evidence §5](../../application-lifecycle-bible/evidence/S03-requirements-evidence.md): business attribute sheets for Customer, Lead/Journey, Quote/Offer, Proposal, Payment and Policy (name, type, optionality, validation, classification, source of truth). Consent and Suitability are in their rule packs. **Residual**: Aarti's physical mapping (S03-OPEN-05) |
| GAP-017 | NFR numbers missing (SLA, retention, RTO) | NFR | Infosec + Ops + PO | NFR sheet signed | Open (WD §16) |
| GAP-018 | Platform vs Integration Hub team boundary | Org | PO + Arch | Layered backlog ownership | Open |
| GAP-019 | Exception / ops handling thin for R0 | Ops | Ops + BA | Ops runbook for top exceptions | Open |
| GAP-020 | Comms templates & triggers undefined | Comms | Product + Ops | Event→template map for R0 | Open |

---

## P2 — Quality / scale

| ID | Gap | Area | Owner | Status |
|----|-----|------|-------|--------|
| GAP-021 | Reporting KPI dictionary incomplete | Analytics | BI | Open — Sold definition locked |
| GAP-022 | Admin/config UX not specified | Admin | Product + Digital | Open |
| GAP-023 | Self-service + hybrid journey detail | Channel | Digital | **RE-SCOPED to an R1 entry condition** — no longer blocks R0. R0 is assisted-first per [DEC-20260816-03](../../governance/registers/DECISION-REGISTER.md#7-product-decisions--ws-3-realignment-increment-cr-010); DIY revisits at R1, hybrid at R2, both with named triggers in the [WS-3 charter §3.2](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#32-out-of-scope-with-revisit-triggers) |
| GAP-024 | Renewals & servicing boundary fuzzy | Scope | PO | Deferred R2+ |
| GAP-025 | Multi-language / vernacular | UX | Digital | Deferred |
| GAP-026 | Dual-aggregator routing | Integration | Arch | Deferred (extensibility only) |
| GAP-027 | Embedded insurance in loan flows | Growth | Product | Deferred |
| GAP-028 | Key rotation / retention purge jobs | Tech | Eng | Open (post COMP patterns) |

---

## P3 — Later / watchlist

| ID | Gap | Notes | Status |
|----|-----|-------|--------|
| GAP-029 | Claims administration | Explicitly out | Closed (out of scope) |
| GAP-030 | Insurer core systems | Explicitly out | Closed (out of scope) |
| GAP-031 | Full direct-insurer / bank-owned agg layer | Future | Deferred |
| GAP-032 | NTB onboarding | Out of MVP | Deferred |
| GAP-033 | Branch kiosk | Pending business decision | Deferred |

---

## Cross-reference

| Gap cluster | Documents |
|-------------|-----------|
| Scope decisions | `../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md`, `../requirements/R0-SCOPE.md` |
| Requirements depth | `../requirements/BRD-OVERVIEW.md`, `BRD-P0-CAPABILITIES.md`, `PRD-R0-*.md` |
| SWOT | `01-SWOT-ANALYSIS.md` (W/T rows map to gaps) |
| Discovery Qs | `../03-discovery-backlog.md` |
| Decision log | `../DECISION-LOG.md` |
