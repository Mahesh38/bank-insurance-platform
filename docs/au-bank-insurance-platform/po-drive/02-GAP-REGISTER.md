# Gap register — complete (Platform PO)

**ID scheme:** `GAP-xxx`  
**Severity:** P0 block freeze · P1 block pilot · P2 quality/scale · P3 later  
**Status:** Open · In progress · Closed · Deferred

---

## P0 — Block scope / build freeze

| ID | Gap | Area | Owner | Impact if ignored | Exit criteria | Status |
|----|-----|------|-------|-------------------|---------------|--------|
| GAP-001 | First LOB not chosen | Scope | Bancassurance + PO | Wrong fields, insurers, UW model | Signed D-001 | Open |
| GAP-002 | R0 definition of done unclear | Scope | Sponsor + PO | Infinite delivery | Signed D-007 (quote/payment/policy) | Open |
| GAP-003 | R0 channel (RM-only vs hybrid) unclear | Channel | Digital + PO | Wrong auth/UX | Signed D-002 | Open |
| GAP-004 | Customer segment for R0 unclear | Customer | Product + Compliance | CIF vs full KYC scope | Signed D-009 | Open |
| GAP-005 | Insurer panel for pilot unknown | Partners | Bancassurance | Cannot UAT | Named insurers + 1SB enablement | Open |
| GAP-006 | Consent rules not executable | Compliance | Compliance + BA | Legal reject | Consent rule pack v1 approved | Open |
| GAP-007 | Suitability content & override unknown | Compliance | Compliance + BA | Regulatory reject | Suitability pack v1 approved | Open |
| GAP-008 | BR templates lack AC | Requirements | BA + PO | Cannot estimate/test | P0 BRD with AC (this pack → refine) | In progress |
| GAP-009 | Figma not mapped to CJ/RMJ | UX | Digital + BA | Build wrong screens | Inventory + MVP flags | Open |
| GAP-010 | Executive sponsor unnamed | Governance | PO | Slow decisions | Name + RACI | Open |

---

## P1 — Block safe pilot

| ID | Gap | Area | Owner | Exit criteria | Status |
|----|-----|------|-------|---------------|--------|
| GAP-011 | Payment experience undecided | Payments | Payments + Digital | D-006 + failure/retry AC | Open |
| GAP-012 | Quote validity / compare rules missing | Sales | Product + BA | Quote rule pack v1 | Open |
| GAP-013 | Product Matrix dimensions undefined | Products | Product | Matrix v0 for first LOB | Open |
| GAP-014 | AgentId / RM mapping model incomplete | Attribution | Ops + Compliance | Mapping source + fail behaviour | Open |
| GAP-015 | 1SB commercial + sandbox confirmation | Partners | Bancassurance | Keys, distributorId, IP, panel | Open |
| GAP-016 | Information model attributes missing | Data | BA + Architect | Attribute sheets for P0 objects | Open |
| GAP-017 | NFR numbers missing (SLA, retention, RTO) | NFR | Infosec + Ops + PO | NFR sheet signed | Open |
| GAP-018 | Platform vs Integration Hub team boundary | Org | PO + Arch | Layered backlog ownership | Open |
| GAP-019 | Exception / ops handling thin for R0 | Ops | Ops + BA | Ops runbook for top exceptions | Open |
| GAP-020 | Comms templates & triggers undefined | Comms | Product + Ops | Event→template map for R0 | Open |

---

## P2 — Quality / scale

| ID | Gap | Area | Owner | Status |
|----|-----|------|-------|--------|
| GAP-021 | Reporting KPI dictionary incomplete | Analytics | BI | Open |
| GAP-022 | Admin/config UX not specified | Admin | Product + Digital | Open |
| GAP-023 | Self-service journey not detailed | Channel | Digital | Deferred (if R0 RM-only) |
| GAP-024 | Renewals & servicing boundary fuzzy | Scope | PO | Deferred R2+ |
| GAP-025 | Multi-language / vernacular | UX | Digital | Deferred |
| GAP-026 | Dual-aggregator routing | Integration | Arch | Deferred |
| GAP-027 | Embedded insurance in loan flows | Growth | Product | Deferred |
| GAP-028 | Key rotation / retention purge jobs | Tech | Eng | Open (post COMP patterns) |

---

## P3 — Later / watchlist

| ID | Gap | Notes | Status |
|----|-----|-------|--------|
| GAP-029 | Claims administration | Explicitly out (Vol 01) | Closed (out of scope) |
| GAP-030 | Insurer core systems | Explicitly out | Closed (out of scope) |
| GAP-031 | Full direct-insurer fabric | Phase C strategy only | Deferred |

---

## Cross-reference

| Gap cluster | Documents |
|-------------|-----------|
| Scope decisions | `../01-stakeholder-working-session.md`, `../requirements/R0-SCOPE.md` |
| Requirements depth | `../requirements/BRD-P0-CAPABILITIES.md`, `PRD-R0-*.md` |
| SWOT | `01-SWOT-ANALYSIS.md` (W/T rows map to gaps) |
| Discovery Qs | `../03-discovery-backlog.md` |
