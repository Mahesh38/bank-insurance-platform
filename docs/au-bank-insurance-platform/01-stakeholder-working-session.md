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

**Attendees (minimum):** Platform PO, Bancassurance, Digital/UX, Compliance (or delegate), BA  
**Optional:** Architecture, Ops, Infosec

### Agenda

1. **Charter read-through** ([00](./00-project-charter.md)) — 15 min  
2. **Problem validation** — keep / rewrite / drop P1–P5 — 20 min  
3. **Persona & channel** — RM-only vs RM+customer app; who completes payment/OTP — 20 min  
4. **LOB strategy** — first LOB, second LOB, explicit deferrals — 15 min  
5. **Aggregator stance** — 1SB now / replaceability requirement — 15 min  
6. **Decision log** (below) — 15 min  
7. **Parking lot** for tech debates — 5 min  

### Exit criteria

- Charter sections 2–5 have named owners and provisional decisions  
- First-release LOB candidate agreed or explicitly “TBD by date X”  
- Open questions moved into [03](./03-discovery-backlog.md) with owners  

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

| ID | Decision question | Options | Decision | Owner | Date | Notes |
|----|-------------------|---------|----------|-------|------|-------|
| D-001 | First LOB for AU Bank MVP | Term / Health / Motor / Other | *TBD* | Bancassurance + PO | | |
| D-002 | Primary user of MVP UI | RM-assisted only / Customer self / Hybrid | *TBD* | Digital + PO | | |
| D-003 | Aggregator for go-live | 1SB / Other / Dual | *TBD* | Bancassurance + PO | | |
| D-004 | Replaceability required at MVP? | Yes hard / Yes soft / No | *TBD* | PO + Architect | | |
| D-005 | Suitability engine location | Bank only / Aggregator / Hybrid | *TBD* | Compliance + PO | | |
| D-006 | Payment experience | Aggregator URL redirect / Bank PG / Hybrid | *TBD* | Payments + PO | | |
| D-007 | “Done” for first release | Quote only / Through payment / Through policy | *TBD* | PO | | |
| D-008 | Agent attribution source | RM login map / Manual entry / Both | *TBD* | Compliance + Ops | | |

*Add rows freely. Do not delete contested decisions — mark superseded.*

---

## Parking lot (tech — do not solve in Session 1)

- Service boundaries, job polling, encryption key custody, CI coverage floors  
- Exact 1SB path names and sandbox quirks  
- Stack choices already explored in prior engineering spike  

These return only after product scope freeze.
