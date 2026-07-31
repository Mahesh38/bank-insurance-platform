# Stakeholder working sessions — AU Bank Insurance Platform

**Facilitator:** Platform Product Owner (+ BA)  
**Mode:** Product discovery restart — decisions before design freeze  
**Prerequisite inputs:** Figma walkthrough + uploaded baseline docs ([05](./05-figma-and-artefact-intake.md))

---

## Session 0 — Artefact intake (async / short sync)

**Goal:** Get all prior materials into one place before debating scope.

| Step | Action | Owner |
|------|--------|-------|
| 1 | Upload earlier basic docs; log each in [05](./05-figma-and-artefact-intake.md) | PO / requester |
| 2 | Export or walk Figma screens; list screens in intake | Digital / PO |
| 3 | Mark each prior “decision” as **Adopt / Reopen / Reject** | PO + BA |

**Exit:** Intake log complete enough to run Session 1.

---

## Session 1 — Problem, personas, boundaries (90–120 min)

**Current use (post Working Decisions):** Run as a **validation** session — confirm or revise Working Decisions — not a greenfield freeze.

**Attendees (minimum):** Platform PO, Bancassurance, Digital/UX, Compliance (or delegate), BA  
**Optional:** Architecture, Ops, Infosec · **Sponsor** for formal sign-off if ready

### Agenda

1. **Working Decisions read-through** ([07](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)) — 20 min  
2. **Confirm D-001…D-007, D-009, D-010** (LOB, journeys, ETB, Sold, insurers, payment, suitability gate) — 25 min  
3. **Open Validation Items** — consent sequencing, agent identity, retention; configurable-controls approach — 20 min  
4. **Figma = reference**; inventory ask — 10 min  
5. **Decision Log** ([DECISION-LOG](./DECISION-LOG.md)) — confirm IDs / mark revisions — 10 min  
6. **Management ask** ([04-MANAGEMENT-READINESS](./po-drive/04-MANAGEMENT-READINESS.md)) — 10 min  

### Exit criteria

- Working Decisions marked **Validated** or revised with new version  
- Sponsor name confirmed or dated follow-up  
- Open Validation owners assigned  
- Remaining open questions updated in [03](./03-discovery-backlog.md)  
- Parking lot for tech debates only  

---

## Session 2 — Journey & process freeze (120 min)

**Input:** Figma prototype + [04](./04-process-and-journey-canvas.md)

### Agenda

1. Walk Figma end-to-end as RM (and customer steps)  
2. Map each screen → journey stage → bank system vs aggregator  
3. Mark mandatory vs optional steps for MVP  
4. Identify compliance gates (suitability, consent, disclosure, attribution)  
5. Agree “definition of sold” (policy issued? payment success? proposal submitted?)  

### Exit criteria

- Journey canvas stages marked **MVP / Later / Out**  
- Screen → stage mapping table started in [04](./04-process-and-journey-canvas.md)  
- No unresolved “who owns this step?” for MVP stages  

---

## Session 3 — Scope freeze & epic outline (90 min)

### Agenda

1. In / out of scope for Release 0 / Release 1  
2. Non-functional must-haves (audit, masking, auth) as product gates  
3. Draft epic list (product language — not sprint tickets yet)  
4. Dependencies on bank systems (CIF, auth, payments landing, notifications)  
5. Sign-off path to architecture brief  

### Exit criteria

- Written scope freeze note (append to decision log)  
- Discovery backlog P0 questions closed or deferred with date  
- Architecture invited for options workshop **after** this session  

---

## Decision log

Working decisions recorded in [DECISION-LOG.md](./DECISION-LOG.md) and [07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md).

| ID | Decision question | Options | Decision | Owner | Date | Notes |
|----|-------------------|---------|----------|-------|------|-------|
| D-001 | First LOB for AU Bank MVP | Term / Health / Motor / Other | **Life** (Term, ULIP, Savings/Investment) | Bancassurance + PO | 2026-07-31 | Working draft |
| D-002 | Primary channels of MVP UI | RM-assisted only / Customer self / Hybrid | **All three Day 1** | Digital + PO | 2026-07-31 | Working draft |
| D-003 | Aggregator for go-live | 1SB / Other / Dual | **1SB** (single; extensible later) | Bancassurance + PO | 2026-07-31 | Working draft |
| D-004 | Replaceability required at MVP? | Yes hard / Yes soft / No | **Yes hard** (no tight coupling) | PO + Architect | 2026-07-31 | Working draft |
| D-005 | Suitability before quote | Bank only / Aggregator / Hybrid | **Mandatory bank-owned** need analysis + suitability | Compliance + PO | 2026-07-31 | Working draft |
| D-006 | Payment experience | Aggregator URL / Bank PG / Hybrid | **AU Bank PG**; customer device; no RM-device pay | Payments + PO | 2026-07-31 | Working draft |
| D-007 | “Done” / Sold for first release | Quote / Payment / Policy | **Policy issued** + confirm + recon + ops | PO | 2026-07-31 | Working draft |
| D-008 | Agent attribution source | RM login map / Manual / Both | *Pending IRDAI validation* | Compliance + Ops | | WD §10 |
| D-009 | Customer segment | ETB / NTB / Both | **ETB only** (any bank relationship) | Product | 2026-07-31 | Working draft |

*Add rows freely. Do not delete contested decisions — mark superseded.*

---

## Parking lot (tech — do not solve in Session 1)

- Service boundaries, job polling, encryption key custody, CI coverage floors  
- Exact 1SB path names and sandbox quirks  
- Stack choices already explored in prior engineering spike  

These return only after product scope freeze.
