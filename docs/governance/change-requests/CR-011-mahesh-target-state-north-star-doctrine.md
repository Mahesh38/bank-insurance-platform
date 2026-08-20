# CR-011 — Mahesh Target-State / North Star Architecture Doctrine

**Date:** 2026-08-20
**Type:** GOV
**Raised by:** Repository owner → Mahesh — Architecture owner (Board 1)
**Branch:** `claude/mahesh-persona-training-ktwomp`
**Decision:** PENDING RATIFICATION

## 1. Current position

The Mahesh persona package (v1.1, files `01`–`08`) trains the Architecture persona to decide **the
next change**: authority classes, decision workflow, review contract, compliance collaboration and
escalation. It contains no doctrine for the different question *"design the system we are trying to
become."*

Consequences observed:

- A target-state request has no defined answer format, so it is answered with a larger diagram.
- Line-of-business and journey segregation have no stated rules, so each is re-argued.
- `docs/hdl.svg` exists as an R0 rendering with no documented canvas contract, update procedure or
  consistency checks, making every edit a re-derivation.
- Stakeholder vision material has no intake path, so it is either ignored or silently absorbed as
  if it were an accepted decision.

## 2. Proposed change

1. Transcribe the stakeholder North Star session (`VIN-001`) into
   `docs/au-bank-insurance-platform/references/2026-08-20-north-star-architecture-brainstorming-notes.md`
   as an attributed, **non-binding** reference.
2. Add eight target-state modules to the Mahesh package (`09`–`16`), raising it to **v1.2**:
   - `09` — horizons `H0`–`H3`, target-state invariants `TI-01`–`TI-18`, variation axes `VA-1`–`VA-6`,
     vision answer format, vision intake register and rules `VI-01`–`VI-03`;
   - `10` — the North Star capability model: the six-question capability definition contract,
     capability ≠ microservice (`NS-03`/`NS-04`), the five planes, the capability catalogue, and the
     full `VIN-001` reconciliation;
   - `11` — line-of-business segregation: LOB cells, shared-versus-LOB test, isolation verification,
     LOB onboarding checklist;
   - `12` — journey segregation: Opportunity/Journey/Policy lifecycle, registry versus execution,
     channel continuity, journey variants, actor model;
   - `13` — orchestration: coordination ownership, sync/async, compensation, events, process-engine
     test;
   - `14` — shared capabilities: qualification test, delivery forms, availability posture,
     integration boundaries, configuration, data ownership;
   - `15` — actor identity and authorization, carrying the **Bank Active Directory invariant**
     forward unchanged;
   - `16` — HLD authoring and update protocol, including the canvas contract for `docs/hdl.svg`.
3. Record two reconciliations against existing decisions rather than absorbing them silently
   (`09 §5.1`, `§5.2`), each with the ADR it will require.
4. Update the package README (contents, loading order, operating rules 14–17, version and date).

## 3. Driver

The repository owner asked that the Architecture persona be able to answer target-state design
questions and update the existing HLD without re-deriving the platform's intent each time. That
requires the target state to be written down as **invariants, axes and capability contracts** —
which is also the form that makes it reviewable.

The stakeholder session's own framing is adopted as the governing method: *before another
architecture diagram, produce a North Star capability model*; a capability is not automatically a
microservice; ownership boundaries first, deployable boundaries later.

## 4. Evidence

- `VIN-001` transcribed source, attributed and dated.
- `docs/platform/ws3-platform/03-solution-architecture-r0.md` — R0 service set, seams, resilience,
  fitness functions.
- `docs/platform/architecture-review/02` and `08` — target service catalogue and `ARCH-*` log.
- `docs/platform/authentication-authorization/README.md` — accepted workforce identity baseline
  (`ARCH-018`–`ARCH-022`).
- `docs/au-bank-insurance-platform/knowledge-base/` — vision, capability map, integration strategy.
- `docs/1sb-insurance-integration/journeys/universal-lob-journey.md` — the LOB difference evidence
  base.
- `DECISION-REGISTER` rows `DEC-20260816-03`, `-04`, `-05` — assistance-mode and LOB sequencing.
- `docs/hdl.svg` — the R0 rendering whose canvas contract file `16` documents.

## 5. Impact

```yaml
scope: "Persona grounding and one non-binding reference document; no product runtime behaviour changes"
stage: "No lifecycle-stage change; WS-3 remains at S08"
dependencies:
  - "AIGEM Board 1"
  - "Mahesh persona package"
  - "docs/hdl.svg and the WS-3 architecture documents it renders"
  - "ARCH-004 (Aarti) and SC-W3-5 (Deepali, Shivanshi) — reconciliations named, not applied"
parked_items: "None"
effort: "M"
risk_if_rejected: >
  Target-state questions continue to be answered with diagrams rather than ownership boundaries;
  LOB and journey segregation stay re-argued per request; HLD edits stay re-derivations; and
  stakeholder vision material continues to have no path between "ignored" and "silently treated as
  decided".
```

**Explicitly not changed by this CR:** no invariant is relaxed, no accepted decision is superseded,
no scope is added, no service is proposed for build, and `docs/hdl.svg` is **not** edited — file
`16` rule `HA-01` requires the capability model to precede a diagram change, and `VIN-001` says the
same.

## 6. Alternatives considered

### A — Extend `01-persona.md` and `02-capability-model.md` in place

Rejected. Target-state doctrine is a different mode of work with a different answer format and a
different evidence bar. Folding it into the change-review persona files makes both harder to load
selectively, which is the property the modular package exists to provide.

### B — Write a target-state architecture document under `docs/platform/` instead

Rejected as the primary vehicle, though likely correct later. A target-state *document* asserts a
design; this CR delivers the *method and constraints* for producing one. Producing the design first
would repeat the failure the session identified — drawing before the capability model is defensible.

### C — Ingest `VIN-001` directly into the persona files without transcribing the source

Rejected. Un-transcribed material has no provenance and cannot be re-checked, and the repository's
standing practice is that unsourced statements are assumptions with owners, not facts.

### D — Adopt `VIN-001` as accepted architecture

Rejected. It is stakeholder input, not a ratified decision. Two points refine existing accepted
decisions (`ARCH-004`, `SC-W3-5`); those are recorded as requiring ADRs with the accountable
personas, not applied by transcription.

### E — Add modules `09`–`16` as grounding, with `VIN-001` as non-binding reference

**Recommended.** Delivers the method, preserves every invariant, names the conflicts, and leaves
every architecture decision to its accountable owner.

## 7. Authority and safeguards

- All eight modules are **grounding context**, subordinate to the precedence order in `08 §5`.
- No invariant, control (`C1`–`C10`), structural constraint (`SC-W3-*`) or fitness function is
  weakened; `TI-01`–`TI-18` restate existing obligations with citations.
- Bank Active Directory remains the authoritative workforce identity source at every horizon
  (`TI-01`); the accepted `ARCH-018`–`ARCH-022` baseline is carried forward unchanged.
- Two reconciliations are **named, not applied**: `ARCH-004` (with Aarti) and `SC-W3-5` (with
  Deepali and Shivanshi). Neither is treated as decided by this CR.
- Seven open items are recorded in `10 §9.1`; five belong to Rajal, Shailja, Aarti or Deepali and
  are explicitly not Mahesh's to close.
- AIGEM T4 human Architecture sign-off, the Security veto and the Risk & Compliance veto are
  unchanged.
- `A0–A3`, `D0–D3`, `R0–R3` and `P1–P5` remain separate vocabularies.

## 8. Decision and approvers

```yaml
decision: PENDING
requested_by: "Repository owner, 2026-08-20"
approvers:
  architecture:
    status: AI_DRAFTED_FOR_REVIEW_BRANCH
    approver: "Mahesh — Principal Insurance Platform Architect (human signature outstanding)"
  product:
    status: PENDING
    approver: "Rajal — Principal Insurance Platform Product Owner"
  security:
    status: PENDING
    approver: "Deepali — Principal Security Architect (SC-W3-5 reconciliation, identity modules)"
  data:
    status: PENDING
    approver: "Aarti — Principal Insurance Data & Database Architect (ARCH-004 reconciliation)"
conditions:
  - "Do not treat this as ratified/binding until the required human approvals are recorded."
  - "Do not apply the ARCH-004 or SC-W3-5 reconciliations without their own ADRs."
  - "Do not treat any H1-H3 capability described in modules 09-16 as approved scope."
  - "Do not edit docs/hdl.svg on the basis of these modules alone (HA-01, HA-03)."
```

## 9. Post-approval actions

1. Mark CR-011 `APPROVED` and add the Decision Register row.
2. Raise the two ADRs named in `09 §5.1` and `§5.2` with their accountable personas.
3. Route the seven open items in `10 §9.1` to their named owners.
4. Confirm the H0 design constraint that no contract assumes `customerId == cifId` (open item 3),
   verifiable at S11.
5. Re-check the modules whenever `DEC-20260816-03` / `-05`, `ARCH-004`, `ARCH-018`–`ARCH-022` or
   `SC-W3-*` change.
