# How I see this project — Platform PO (driving view)

**Author:** Platform Product Owner  
**Bank:** AU Bank  
**Product:** Insurance Distribution Platform  
**Date:** 2026-07-31  
**Status:** Working doctrine for programme steering

---

## 1. One-line truth

We are **not** building “a 1SB connector.”  
We are building **AU Bank’s insurance distribution operating system** — RM + customer journeys, compliance gates, catalogue, and fulfilment tracking — with **1SB as the first Integration Hub adapter**.

---

## 2. The complete project (three layers)

```text
L1  CHANNELS
    RM Workspace · Customer journeys · Ops consoles
    (Figma = UX hypothesis)

L2  DISTRIBUTION PLATFORM (bank-owned source of truth)
    Lead · Consent · Suitability · Product Matrix
    Quote · Proposal · UW tracking · Payment tracking
    Policy · Communications · Reporting · Admin
    Identity · Audit · Configuration

L3  INTEGRATION HUB
    Canonical bank APIs → adapters
    Phase A: 1SB · Phase B: Hybrid · Phase C: Direct insurers
```

| Layer | Business owns? | Today’s maturity |
|-------|----------------|------------------|
| L1 Channels | Digital + Sales | Figma exists; not inventoried; not tied to CJ/RMJ |
| L2 Platform | Product + BA + Compliance | Vision/capability map strong; BR detail weak |
| L3 Hub | Architecture + Integration | Prior eng spike on quote/proposal/payment/status exists as **adapter candidate** |

**Risk if we only ship L3:** AU Bank still does not “own the journey.”  
**Risk if we build all of L1+L2 at once:** multi-year programme with no pilot revenue proof.

---

## 3. North star outcomes (what “good” looks like)

| Horizon | Outcome |
|---------|---------|
| **R0 Pilot** | Controlled RM cohort completes **JRN-001** for **one LOB** through payment + policy visibility, with consent/suitability/audit intact |
| **R1 Scale** | More RMs/branches; ops dashboards; hardening; exception handling |
| **R2 Expand** | Next LOB + richer UW/docs; Product Matrix maturity |
| **Strategic** | Direct insurer adapters can be added **without** rewriting RM/customer apps |

Trace every epic to **BG-001…BG-008** (see knowledge-base/02).

---

## 4. What is already decided (I will defend these)

1. Bank owns journey state, consent, suitability, catalogue, reporting.  
2. Integrations are replaceable (1SB ≠ product).  
3. Canonical bank language in UX and public APIs.  
4. Configuration + audit are release gates, not polish.  
5. Claims administration and insurer cores are out of scope.  
6. First journey to elaborate: **JRN-001 RM Assisted New Policy Purchase**.

---

## 5. What I will force a decision on (next 2 workshops)

| # | Decision | Why I will not proceed without it |
|---|----------|-----------------------------------|
| D-001 | First LOB | Field models, insurers, UW intensity all change |
| D-007 | R0 definition of done | Without this, eng builds forever |
| D-002 | Channel for R0 (RM-only vs hybrid) | Auth, UX, staffing |
| D-009 | Existing customer only? | CIF vs full onboarding |
| D-006 | Payment experience | Redirect UX + reconciliation |
| D-005 | Suitability mandatory before quote? | Compliance path |

Until these are signed, delivery work is **discovery / spike**, not committed roadmap.

---

## 6. How I will drive the programme

| Mode | Cadence | Output |
|------|---------|--------|
| Steering | Bi-weekly | Scope, risks, decisions |
| Product triad (PO+BA+Architect) | Weekly | Requirements quality gate |
| Journey workshop | Until JRN-001 locked | Screen + process + rules |
| Delivery | After R0 freeze | Epics → stories with AC |

**Definition of Ready (story):** AC, happy + alternate + exception, data ownership, audit expectation, UX reference (or explicit “API-only”).  
**Definition of Done (release):** Traceable to BG + JRN; compliance sign-off on consent/suitability/audit; pilot metrics wired.

---

## 7. Honest assessment

| Strength of current pack | Weakness |
|--------------------------|----------|
| Excellent programme skeleton | Template BRs cannot be estimated or tested |
| Clear strategic independence story | No LOB/insurer/pilot geography |
| Journey vocabulary ready for Figma mapping | Figma not yet linked |
| Prior 1SB eng work usable as L3 seed | Easy to confuse that seed with “platform done” |

**PO stance:** Protect the vision; cut R0 ruthlessly; deepen requirements before scaling team size.

---

## 8. Related artefacts in this pack

| Doc | Purpose |
|-----|---------|
| [01-SWOT-ANALYSIS.md](./01-SWOT-ANALYSIS.md) | SWOT |
| [02-GAP-REGISTER.md](./02-GAP-REGISTER.md) | Full gap register |
| [03-PROGRAMME-TODO.md](./03-PROGRAMME-TODO.md) | Master TODO |
| [../requirements/](../requirements/README.md) | R0 scope + PRD + P0 BRD |
| [../knowledge-base/](../knowledge-base/README.md) | Application knowledge base |
