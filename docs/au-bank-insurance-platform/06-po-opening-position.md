# Platform PO opening position — restarting from zero

**Voice:** Platform Product Owner (with BA facilitation)  
**Date:** Discovery restart (historical kickoff stance)  
**Bank:** AU Bank  
**Supersession:** Scope hypotheses below are **superseded** by [Working Decisions Draft v1](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) and [DECISION-LOG.md](./DECISION-LOG.md). Keep this file as kickoff context only.

---

## What I asked the room to agree (kickoff)

We are **not** continuing the previous engineering narrative as the product truth.  
We are building an **AU Bank Insurance Distribution Platform** for bancassurance — starting with shared understanding of customers, journeys, and release outcomes.

Prior 1SB research and code spikes are valuable. They answer “can we connect?”  
They do **not** yet answer “what must AU Bank ship for RMs and customers?”

---

## Stakeholder positions (still valid for validation sessions)

### Bancassurance / Product

- Confirm Working Decisions on Life LOB + Group A/B insurers.  
- Is the first win “more policies” or “controlled compliant pilot”?

### Digital / Channel / UX (Figma owners)

- Walk Figma as **reference** (MVP vs concept).  
- Detail self-service + hybrid mode-switch UX.

### Compliance / Risk / Infosec

- Non-negotiables: consent, suitability, attribution, logging.  
- Close Open Validation Items (sequencing, retention, residency).

### Branch / RM ops

- Who starts / finishes / chases docs; payment only on customer device.

### Architecture / Engineering (advisory)

- Options after Working Decisions formal validation — journey engine hybrid modes; aggregator-agnostic core.

---

## Working hypotheses — status after Working Decisions

| # | Original hypothesis | Status |
|---|---------------------|--------|
| 1 | AU Bank brand owns UX; aggregator invisible | **Working — keep** (D-004) |
| 2 | RM-assisted is the default / only MVP channel | **Superseded** — RM + Self-service + Hybrid Day 1 (D-002) |
| 3 | Existing customers only for R0 | **Working — keep** ETB only (D-009) |
| 4 | One LOB first (Term candidate not approved) | **Superseded** — Life LOB: Term, ULIP, Savings/Investment (D-001) |
| 5 | 1SB current connectivity; bank APIs survive replacement | **Working — keep** (D-003, D-004) |
| 6 | Payment + status in first release; Sold unclear | **Superseded** — Sold = issuance (D-007); payment on customer device (D-006) |

---

## Discovery phase “done” — current honesty

| Criterion | Status |
|-----------|--------|
| Charter drafted / aligned to WD | Done as draft |
| Figma reconciled into journey canvas | **Open** (inventory pending) |
| P0 discovery questions answered or deferred | **Mostly done** (Working); Open Validation remains |
| R0 scope in product language | Done as draft ([R0-SCOPE](./requirements/R0-SCOPE.md)) |
| Architecture options workshop with constraints | **Not yet** — after sponsor validation |

---

## Immediate next steps (post Working Decisions)

| # | Action | Who |
|---|--------|-----|
| 1 | Management validation of Working Decisions ([04-MANAGEMENT-READINESS](./po-drive/04-MANAGEMENT-READINESS.md)) | Sponsor + PO |
| 2 | Figma walkthrough / inventory (reference only) | Digital + BA |
| 3 | Consent / suitability content + Open Validation R&D | Compliance + BA |
| 4 | Use [DECISION-LOG.md](./DECISION-LOG.md) as only `D-xxx` source | All |

**Working product SSOT:** [07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md).  
Engineering under `docs/1sb-insurance-integration/` stays parked as research / Integration Hub candidate.
