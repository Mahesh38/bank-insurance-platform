# Programme TODO — Platform PO master list

**Owner:** Platform Product Owner (driver)  
**How to use:** Check boxes in PRs; move Done items to bottom changelog weekly  
**Rule:** No delivery sprint commit until Wave 0 exit criteria met

---

## Wave 0 — Discover & freeze (NOW)

### Governance
- [ ] Name Business Sponsor (role = Head of Insurance Business / Platform — GAP-010)
- [ ] Confirm Steering cadence (bi-weekly)
- [ ] Publish RACI for remaining Open Validation Items
- [ ] Align org: Distribution Platform backlog owns L1–L3; 1SB eng is L3 epic (GAP-018)

### Scope decisions (Working Decisions Draft v1 — pending formal sign-off)
- [x] D-001 First LOB → **Life** (Term, ULIP, Savings/Investment)
- [x] D-002 Channels → **RM + Self-service + Hybrid** Day 1
- [x] D-007 R0 done / Sold → **policy issued** + confirm + recon + ops
- [x] D-009 Segment → **ETB only**
- [x] D-003 Aggregator → **1SB** (single aggregator MVP; extensible)
- [x] D-005 Suitability / need analysis **mandatory** before quote
- [x] D-006 Payment → customer device; **AU Bank PG**; no RM-device payment
- [ ] Sponsor **sign-off** on [Working Decisions](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) + [R0-SCOPE.md](../requirements/R0-SCOPE.md)

### Artefacts & UX
- [ ] Figma walkthrough recorded (Figma = **reference only** — WD §15)
- [ ] Screen inventory table (screen → CJ/RMJ/JRN → MVP Y/N) (GAP-009)
- [ ] Mark concept-only Figma screens as Out for MVP
- [ ] Detail self-service + hybrid mode-switch UX (GAP-023)

### Requirements depth
- [x] Capture PO-approved [BRD overview headings](../requirements/BRD-OVERVIEW.md)
- [ ] Write detailed BR chapters under BRD overview (start §1–4, §6–8, §10–11)
- [ ] Review & approve [PRD-R0](../requirements/PRD-R0-DISTRIBUTION-PLATFORM.md) against Working Decisions + BRD overview (three journeys)
- [ ] Align [BRD-P0](../requirements/BRD-P0-CAPABILITIES.md) to BRD overview sections
- [ ] Consent rule pack v1 (GAP-006) — sequencing R&D → BRD §6.4 / §8.3
- [ ] Suitability rule pack v1 (GAP-007) → BRD §6
- [ ] Quote/compare rule pack v1 for Group A (GAP-012)
- [ ] Product Catalogue / Matrix v0 for Life + Group A/B flags (GAP-013)
- [ ] Attribute sheets: Lead, Consent, Suitability, Quote, Proposal, Payment, Policy (GAP-016)
- [ ] NFR sheet draft incl. retention/residency placeholders (GAP-017)
- [ ] Group B redirect journey AC

### Wave 0 exit
- [ ] Sponsor sign-off on Working Decisions + R0 scope
- [ ] Compliance provisional OK on consent/suitability approach (configurable until validated)
- [ ] Architecture options workshop scheduled (journey engine hybrid modes; aggregator-agnostic core)

---

## Wave 1 — Design for R0

- [ ] JRN-001 swimlane (RM / Customer / Platform / Integration Hub / Insurer)
- [ ] Wireframes reconciled to Figma MVP set
- [ ] API capability outline (bank-canonical) for P0 capabilities
- [ ] Integration Hub Phase A interface contract (reuse prior 1SB learnings)
- [ ] Security: RM auth mode, roles, agent mapping (GAP-014)
- [ ] Payment failure/resume UX + ops runbook (GAP-011, GAP-019)
- [ ] Comms event map for R0 (GAP-020)
- [ ] Test strategy: journey E2E + compliance evidence samples
- [ ] Pilot ops model: branch cohort, support hours, escalation

### Wave 1 exit
- [ ] Design freeze for R0 screens + APIs
- [ ] Estimation confidence ≥ medium for P0 epics

---

## Wave 2 — Build R0 (epics)

Track as delivery backlog after Wave 1. Epic checklist:

- [ ] E-ID Identity & RM session
- [ ] E-CUST Customer lookup / prefill (CIF)
- [ ] E-LEAD Lead create/resume
- [ ] E-CONSENT Consent capture & gate
- [ ] E-SUIT Suitability & recommendation record
- [ ] E-PROD Product Matrix eligibility (first LOB)
- [ ] E-QUOTE Quote + compare + select
- [ ] E-PROP Proposal dynamic form + submit
- [ ] E-UW Underwriting status tracking (lite)
- [ ] E-PAY Payment URL/session + status
- [ ] E-POL Policy issuance visibility + docs link
- [ ] E-AUDIT Audit trail + attribution
- [ ] E-HUB Integration Hub Phase A (1SB adapter)
- [ ] E-RM RM workspace (pipeline + task list minimum)
- [ ] E-REP Minimal pilot dashboard (funnel counts)

---

## Wave 3 — Pilot & learn

- [ ] Sandbox E2E JRN-001 green
- [ ] UAT with RM cohort
- [ ] Compliance sample audit pack
- [ ] Measure: TAT, drop-off by stage, quote→policy
- [ ] Retro → R1 backlog

---

## Wave 4 — R1 scale (parking until pilot)

- [ ] More insurers / branches
- [ ] Ops exception console
- [ ] Richer UW/docs
- [ ] Reporting pack for branch/exec
- [ ] Admin configuration UI hardening
- [ ] Hybrid direct-insurer spike (if commercially ready)

---

## Wave 5 — Strategic (Phase B/C)

- [ ] Direct insurer adapter pattern proven
- [ ] 1SB deprecation criteria defined
- [ ] Multi-LOB playbook (Health/Motor…)
- [ ] Renewals/servicing decision revisit

---

## PO weekly operating cadence

| Day | Activity |
|-----|----------|
| Mon | Gap register + decision chase |
| Wed | Triad (PO/BA/Arch) requirements quality |
| Fri | Steering notes / risk update |

---

## Changelog

| Date | Note |
|------|------|
| 2026-07-31 | Initial master TODO from SWOT + gap register + KB |
