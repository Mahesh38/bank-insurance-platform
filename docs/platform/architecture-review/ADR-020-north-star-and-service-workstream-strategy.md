# ADR-020 — North Star program workstream + per-microservice service workstreams

**Status:** Accepted (Architecture) — Mahesh, 2026-09-13 · Product / Delivery countersign outstanding for state transcription  
**Date:** 2026-09-13  
**Deciders:** Mahesh (Principal Insurance Platform Architect / governance custodian)  
**Consulted (required countersign):** Rajal (Product), Kalpana (Delivery)  
**Workstream:** Cross-cutting governance (amends ADR-002 operating posture)  
**Stage:** S08 — Engineering Foundation (program posture unchanged)  
**Origin:** SUG-20260913-wss / CR-016

## Context

ADR-002 established three program workstreams: WS-3 (platform), WS-1 (supplier), WS-2 (enabler).
That fixed GAP-D (platform work had no home) and remains historically correct.

What it did **not** specify is how **many agents** take ownership of **many microservices**
while the programme still chases a single North Star. Without a service-level lane:

- progress and “waiting on X” blockers live in chat, not in governed artefacts;
- dependency sync between producer and consumer services is informal;
- agents collide on the same programme gate without a clear ownership boundary.

Constraints that remain real:

- Rule LC-1 — stage fit evaluates against a workstream.
- Agents must not mint dozens of independent lifecycle stages (FreshnessCheck / gate hygiene).
- Capability ≠ bounded context ≠ deployable ≠ code module (Mahesh card).
- Delivery urgency does not invent Product or Architecture content (Kalpana PA-1).

## Decision

We will operate a **two-tier workstream model**:

1. **Program workstream — `WS-NS` (North Star Continuity).**  
   One continuous programme lane the team works until the North Star outcome is reached.
   Lifecycle stage, objective, in/out of scope and open gates live here (today’s WS-3 S08 /
   GATE-S08 content continues until Product + Delivery countersign a transcription).

2. **Service workstreams — `SWS-<module>`.**  
   Exactly one execution/ownership lane per microservice in the backend service catalogue.
   Independent agents may own a service workstream. Each keeps a progress markdown board with:
   - active work items,
   - completed work items (with evidence links),
   - waiting / blocked on another service (named `SWS-*` + DEP id when known).

3. **Dependency sync check.**  
   When work crosses service boundaries, both sides record the dependency and may not mark the
   cross-service outcome Done until a sync check passes (contract agreement, shared AC, or
   DEPENDENCY-REGISTER edge cleared). Kalpana still owns critical-path sequencing; the boards
   make ageing visible.

4. **ADR-002 amendment (not deletion).**  
   WS-1 / WS-2 / WS-3 remain **domain cluster labels** and historical gate carriers.
   ADR-002’s rejection of “a fourth workstream for the Flutter client” still holds for
   *programme* workstreams. Service workstreams are **execution lanes**, not additional
   programme stages — they do not violate that clause.

5. **Explicit non-goals.**  
   - No independent `current_phase` / `stage_status` per `SWS-*`.  
   - No bypass of standing constraints, boards, or T4 human sign-offs.  
   - No agent transcription of `CURRENT-STATE.yaml` workstream topology in this decision’s
     raise turn — that remains a human R12 act after countersign.

## Alternatives considered

| Option | Why not |
|--------|---------|
| Do nothing | Multi-agent ownership stays ungoverned |
| Full AIGEM lifecycle workstream per microservice | Stage-fit and gate explosion; rejects Rule LC-1 practicality |
| Informal wiki notes per service | No sync-check contract; boards rot without register linkage |
| Collapse everything into WS-3 only | Erases supplier/enabler clarity ADR-002 fixed |

## Consequences

**Positive**

- Parallel agents have a named ownership lane per service.
- North Star remains one continuous programme chase.
- Cross-service waits become visible and governable.
- ADR-002 history and WS-1/WS-2/WS-3 evidence stay intact.

**Negative / accepted costs**

- More markdown hygiene (one board per service).
- R12 must watch dependency ageing across more lanes (`DL*` windows still apply).
- Until Product/Delivery countersign, `CURRENT-STATE.yaml` still lists WS-1/2/3 only — agents
  follow this ADR + `WORKSTREAM-STRATEGY.md` for operating posture meanwhile.

**Constrains future work**

- Proposals to add a second *programme* workstream competing with `WS-NS` are SF4 unless CR’d.
- Proposals to give `SWS-*` its own lifecycle stage are REJECT without a new CR/ADR.
- Flutter / NIP client remains part of the North Star programme surface — not a separate
  programme workstream (ADR-002 preserved).

## Reversibility

| Question | Answer |
|----------|--------|
| Cost to reverse | medium |
| What makes it expensive | progress-board corpus and agent habits |
| Point of no return | `CURRENT-STATE.yaml` topology transcription + autopilot routing on `WS-NS` |

## Revalidation triggers

- North Star outcome achieved or replaced by Product.
- Catalogue adds/retires a microservice (board must be added/retired in the same change).
- Evidence that service lanes are being triaged as independent lifecycle stages (defect).
- Delivery forecast shows sync-check overhead exceeding parallelization gain (Kalpana).

## Compliance and security impact

- Regulatory obligations touched: none directly.
- Security posture change: none — Deepali’s trust boundaries unchanged.
- Audit or attribution implications: clearer ownership attribution per service board.

## Conditions closure (2026-09-13)

AIGEM board simulation raised HOLDs and `must_fix` conditions. Architecture accepts them as
**real** and requires the controls in
[`CR-016/CONDITIONS-CLOSURE.md`](../../governance/change-requests/CR-016/CONDITIONS-CLOSURE.md)
before treating CR-016 as operationally closed:

- Rules WS-NS-1…8 in `WORKSTREAM-STRATEGY.md` (dual-topology routing, work-item IDs, claim gate, no PII, standing constraints, boards ≠ gate/regulatory evidence).
- `SYNC-CHECK-EVIDENCE-BAR.md` (E1–E3 or dated waiver).
- `PROGRAMME-ROLLUP.md` (programme green ≠ service-board green; DL ageing).
- Ready-to-sign briefs for Rajal (H1) and Kalpana (H2). Human signatures remain outstanding.
