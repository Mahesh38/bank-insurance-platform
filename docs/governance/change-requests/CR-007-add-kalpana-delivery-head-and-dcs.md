# CR-007 — Assign R12 Delivery Lead to Kalpana and Add Delivery Control System

**Change request:** CR-007  
**Date raised:** 2026-08-14  
**Status:** PENDING RATIFICATION  
**Revision:** 1.1 — revised on user direction before merge of PR #44 to consolidate Kalpana into existing R12  
**Change type:** Persona assignment / governance orchestration / segregation of duties  
**Runtime impact:** None  
**AIGEM board count:** Unchanged — remains seven

## 1. Request

Assign the repository's already-defined **R12 — Delivery Lead** role to **Kalpana — Principal Insurance Platform Delivery Head / Delivery Lead**, and mature that existing role with a **Delivery Control System (DCS)** that composes existing AIGEM lifecycle, scope, priority, dependency, planning, review, readiness, DoD, change-control and metrics mechanisms into one integrated delivery orchestration view.

This revision deliberately replaces the earlier framing that appeared to add a separate Delivery Head authority.

Canonical identity rule:

> **Kalpana = Delivery Head = Delivery Lead = Program Delivery Director = Enterprise Delivery Head = R12.**

No second Delivery persona may be instantiated from those aliases.

## 2. Problem being solved

The AIGEM Runbook already defines **R12 — Delivery Lead** and assigns it important operating responsibilities, including:

- `CURRENT-STATE.yaml` freshness/stewardship;
- `01-CURRENT_STATE.md` freshness;
- parked-backlog and risk-register hygiene;
- gate cadence and `CANDIDATE` orchestration;
- governance metrics/scorecards;
- freshness checks, unpark sweeps and calibration.

However, R12 is currently marked **unassigned**, which the Runbook itself identifies as a major governance risk.

Separately introducing a new Delivery Head would create ambiguity over whether R12 or Kalpana owns Delivery. The correct model is therefore to **fill and mature R12 with Kalpana**, not add a parallel Delivery persona.

The matured R12 role also needs explicit enterprise delivery capability across:

- workstream sequencing;
- critical path;
- dependency ageing;
- parallelization;
- milestone/forecast confidence;
- delivery RAID and decision latency;
- release orchestration;
- schedule recovery and hypercare closure.

## 3. Proposed changes

### Canonical persona assignment

Add/update `docs/context/roles/kalpana-delivery-head/` so that:

- Kalpana is the named R12 Delivery Lead;
- Delivery Head / Delivery Lead / Program Delivery Director / Enterprise Delivery Head are aliases for one persona;
- the original R12 Runbook duties are explicitly inherited;
- the deeper insurance/bancassurance delivery capability extends, rather than competes with, R12.

### AIGEM Runbook

Update `docs/governance/RUNBOOK.md` so R12 is no longer unassigned and resolves directly to Kalpana. Preserve the existing R12 authority boundary: R12 may prepare/mark a gate `CANDIDATE` but does not replace Product + Architecture or specialist/human approval.

### Delivery Control System

Add `docs/governance/DELIVERY-CONTROL-SYSTEM.md` as the **operating mechanism of R12**, not a second governance framework or role. It references existing AIGEM SSOT instead of duplicating lifecycle state, scope, priority, dependencies, reviews, readiness or metrics.

### Canonical authority matrix

Integrate Delivery directly into `docs/governance/PERSONA-AUTHORITY-MATRIX.md` as the R12 orchestration jurisdiction. Remove the separate `PERSONA-AUTHORITY-MATRIX-DELIVERY-ADDENDUM.md` so there is only one authority matrix.

### Cross-persona protocol

Keep `delivery-cross-persona-decision-protocol.md`, but make it explicit that Kalpana is R12 and that delivery criticality never transfers another authority's decision rights.

### Discovery

Update repository agent/persona discovery so all Delivery aliases route to Kalpana/R12 and no stale addendum reference remains.

## 4. Authority decision

Kalpana / R12 owns or accountably coordinates:

- existing R12 current-state/register/gate-cadence/metrics responsibilities;
- integrated delivery plan;
- milestones and evidence-based forecast;
- sequencing/workstreams;
- critical path;
- dependency flow/ageing;
- delivery decision deadlines/escalation;
- capacity/bottleneck coordination;
- delivery health;
- release-readiness orchestration;
- recovery/fast-track scenario analysis;
- hypercare coordination.

She does **not** own or override:

- Rajal's Product intent/scope/priority/acceptance;
- Mahesh's Architecture decisions or stage-transition approval authority;
- Amit's implementation Engineering;
- Deepali's Security outcome/Board 4 conclusion;
- Aarti's persistence/database integrity authority;
- Swapnali's QA evidence/test-strategy authority;
- Shailja's Compliance/Risk permissibility/control authority;
- mandatory human sign-offs or material risk acceptance.

## 5. No eighth board and no duplicate Delivery persona

This change does **not** introduce another AIGEM review board.

Kalpana is the named operating persona for existing **R12**, working across the existing seven-board lifecycle. A Delivery dependency on a specialist decision does not transfer that specialist's review/approval/block authority to Kalpana.

Likewise, `Delivery Lead`, `Delivery Head`, `Program Delivery Director` and `Enterprise Delivery Head` are aliases, not separate agents.

## 6. Relationship to existing governance

DCS reuses:

- the R12 role/cadence in `RUNBOOK.md`;
- current-state/lifecycle/stage gates;
- project scope;
- P1–P5 priority;
- work classification;
- dependency model/register;
- AI execution rules;
- implementation-plan template;
- seven-board review gates;
- Definition of Ready/Done;
- change control and technical-debt rules;
- decision/evidence model;
- drift control;
- governance/quality metrics.

Where DCS conflicts with binding AIGEM or authoritative policy/regulation, existing precedence wins.

## 7. Safeguards

1. R12/Kalpana cannot independently alter approved Product scope or self-approve an AIGEM stage transition.
2. R12 may mark a gate `CANDIDATE` only as the Runbook permits; candidate is not approval.
3. Delivery urgency cannot waive Security, Compliance, QA, data-integrity or Architecture controls.
4. `READY_TO_CONVENE_GO_NO_GO` is not approval.
5. `APPROVED_FOR_COORDINATED_RELEASE` requires all actually required recorded approvals/sign-offs.
6. AI simulation of Kalpana cannot impersonate mandatory human approval or material risk acceptance.
7. Product criticality such as P0/P1/P2/P3 remains distinct from AIGEM P1–P5 priority.
8. Kalpana-local `DL0–DL3` is delivery-impact severity only and cannot overwrite another persona's severity.
9. Mocks/stubs/simulators may remove waiting during build but never substitute for required real integration/release evidence.
10. The canonical authority source is `PERSONA-AUTHORITY-MATRIX.md`; no separate Delivery addendum remains.

## 8. Expected benefit

The change should:

- close the Runbook's explicit R12 unassigned gap;
- eliminate Delivery Head vs Delivery Lead ambiguity;
- improve end-to-end delivery ownership without authority overlap;
- improve critical-path visibility;
- start external/provider/environment work earlier;
- support safe parallel execution;
- reduce decision and dependency latency;
- improve realistic timeline confidence;
- improve release-readiness transparency;
- accelerate recovery when forecasts slip;
- separate “code complete”, “deployed” and truly “delivered”.

## 9. Ratification requirement

This CR is revised on explicit user direction before PR #44 is merged. The AI records that direction but does **not** impersonate any mandatory Product, Architecture, Security, Compliance or other human sign-off required by AIGEM.

Any remaining formal governance ratification required by repository policy must still be recorded by the correct authority.

## 10. Acceptance criteria

- [x] Kalpana has a canonical modular persona package.
- [x] Kalpana is explicitly the existing **R12 Delivery Lead**, not a parallel Delivery authority.
- [x] Delivery Head / Delivery Lead / Program Delivery Director / Enterprise Delivery Head aliases resolve to Kalpana.
- [x] The Runbook R12 slot is assigned to Kalpana in this PR.
- [x] DCS is explicitly the R12 operating mechanism and composes existing AIGEM rather than replacing it.
- [x] No eighth board is created.
- [x] Delivery authority and `Not Authorised` boundaries are explicit.
- [x] Rajal/Mahesh/Amit/Deepali/Aarti/Swapnali/Shailja handoffs are covered.
- [x] Critical path, dependencies, decision latency, parallelization, release, recovery and hypercare are covered.
- [x] Delivery is integrated directly into the canonical Persona Authority Matrix.
- [x] The separate Delivery authority addendum is removed.
- [x] Repository discovery points all Delivery questions to Kalpana/R12.
- [x] No runtime code, API, schema or production configuration is changed.