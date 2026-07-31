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
| GAP-006 | Consent rules not executable | Compliance | Compliance + BA | Legal reject | Consent rule pack v1 approved | Open (WD §9 pending) |
| GAP-007 | Suitability content & override unknown | Compliance | Compliance + BA | Regulatory reject | Suitability pack v1 (mandatory gate locked) | Open (gate locked; content TBD) |
| GAP-008 | BR templates lack AC | Requirements | BA + PO | Cannot estimate/test | P0 BRD with AC under BRD Overview | In progress |
| GAP-009 | Figma not mapped to CJ/RMJ | UX | Digital + BA | Build wrong screens | Inventory + MVP flags; Figma = reference | Open (WD §15) |
| GAP-010 | Executive sponsor unnamed | Governance | PO | Slow decisions | Name + RACI (role known) | Open (role confirmed) |

---

## P1 — Block safe pilot

| ID | Gap | Area | Owner | Exit criteria | Status |
|----|-----|------|-------|---------------|--------|
| GAP-011 | Payment experience undecided | Payments | Payments + Digital | Customer device; AU Bank PG; no RM-device pay | **Closed** (WD §§6,14) — failure/retry AC still Open |
| GAP-012 | Quote validity / compare rules missing | Sales | Product + BA | Quote rule pack v1 (Group A) | Open |
| GAP-013 | Product Matrix dimensions undefined | Products | Product | Matrix v0 for Life catalogue | Open |
| GAP-014 | AgentId / RM mapping model incomplete | Attribution | Ops + Compliance | Mapping source + fail behaviour | Open (WD §10) |
| GAP-015 | 1SB commercial + sandbox confirmation | Partners | Bancassurance | Keys, distributorId, IP, panel | **In progress** (agreement signed; UAT provisioning; Distributor ID soon) |
| GAP-016 | Information model attributes missing | Data | BA + Architect | Attribute sheets for P0 objects | Open |
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
| GAP-023 | Self-service + hybrid journey detail | Channel | Digital | Open (now Day 1 — detail required) |
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
