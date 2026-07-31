# Programme TODO — Platform PO master list

**Owner:** Platform Product Owner (driver)  
**How to use:** Check boxes in PRs; move Done items to bottom changelog weekly  
**Rule:** No delivery sprint commit until Wave 0 exit criteria met

---

## Wave 0 — Discover & freeze (NOW)

### Governance
- [ ] Name Business Sponsor + Programme Sponsor (GAP-010)
- [ ] Confirm Steering cadence (bi-weekly)
- [ ] Publish RACI for D-001…D-009
- [ ] Align org: Distribution Platform backlog owns L1–L3; 1SB eng is L3 epic (GAP-018)

### Scope decisions (Session 1)
- [ ] D-001 First LOB
- [ ] D-002 R0 channel (recommend: RM-assisted only)
- [ ] D-007 R0 done definition (recommend: payment + policy visibility)
- [ ] D-009 Existing-customer-only for R0? (recommend: Yes)
- [ ] D-003/ D-016 1SB commercial confirmation path
- [ ] D-005 Suitability mandatory before quote? (recommend: Yes)
- [ ] D-006 Payment experience decision
- [ ] Sign [R0-SCOPE.md](../requirements/R0-SCOPE.md)

### Artefacts & UX
- [ ] Figma walkthrough recorded
- [ ] Screen inventory table (screen → CJ/RMJ/JRN → MVP Y/N) (GAP-009)
- [ ] Mark concept-only Figma screens as Out for R0

### Requirements depth
- [ ] Review & approve [PRD-R0](../requirements/PRD-R0-DISTRIBUTION-PLATFORM.md)
- [ ] Expand [BRD-P0](../requirements/BRD-P0-CAPABILITIES.md) AC with Compliance
- [ ] Consent rule pack v1 (GAP-006)
- [ ] Suitability rule pack v1 (GAP-007)
- [ ] Quote/compare rule pack v1 (GAP-012)
- [ ] Product Matrix v0 dimensions for first LOB (GAP-013)
- [ ] Attribute sheets: Lead, Consent, Suitability, Quote, Proposal, Payment, Policy (GAP-016)
- [ ] NFR sheet draft (GAP-017)

### Wave 0 exit
- [ ] Sponsor sign-off on R0 scope
- [ ] Compliance provisional OK on consent/suitability approach
- [ ] Architecture options workshop scheduled (not before scope freeze)

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
