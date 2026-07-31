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
| **MVP / R0** | ETB customer completes Life sale (RM / self / hybrid) to **policy issued** (Sold definition); Group A in-platform via 1SB; Group B redirect; consent/suitability/audit intact |
| **R1 Scale** | More RMs/branches; ops dashboards; hardening; exception handling; MIS depth |
| **R2 Expand** | NTB (if sponsored); richer UW/docs; Product Catalogue maturity |
| **Strategic** | Aggregator replaceable (other external or bank-owned layer) **without** rewriting channel apps |

Trace every epic to **BG-001…BG-008** (see knowledge-base/02).

---

## 4. What is already decided (Working Decisions Draft v1)

SSOT: [../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)

1. Bank owns journey state, consent, suitability, catalogue, reporting.  
2. Integrations are replaceable (1SB = current layer, not temporary hack, not the product).  
3. Canonical bank language in UX and public APIs.  
4. Configuration + audit are release gates; compliance items use **configurable policies** until validated.  
5. Claims administration and insurer cores are out of scope.  
6. MVP LOB = **Life**; channels = **RM + Self + Hybrid**; segment = **ETB**; Sold = **issuance**.  
7. Group A (1SB) vs Group B (catalogue + redirect); payment on **customer device** only.

---

## 5. What still blocks formal freeze / hard-coding

| # | Decision | Why I will not hard-code without it |
|---|----------|-------------------------------------|
| Consent sequencing | IRDAI / RBI / Corporate Agency R&D | Illegal UX if wrong |
| Agent identity model | IRDAI + insurer onboarding | Attribution fail |
| PII / audit retention & residency | Compliance pack | NFR wrong |
| Sponsor name | Governance | Slow decisions |
| Suitability / consent **content** packs | Compliance + BA | Gate locked; content open |

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
