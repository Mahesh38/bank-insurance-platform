# CR-007 — Add Kalpana Delivery Head and Delivery Control System

**Change request:** CR-007  
**Date raised:** 2026-08-14  
**Status:** PENDING RATIFICATION  
**Change type:** Persona / governance orchestration / segregation of duties  
**Runtime impact:** None  
**AIGEM board count:** Unchanged — remains seven

## 1. Request

Introduce **Kalpana — Enterprise Delivery Head / Program Delivery Director** as the repository's canonical Delivery persona and add a **Delivery Control System (DCS)** that composes existing AIGEM lifecycle, scope, priority, dependency, planning, review, readiness, DoD, change-control and metrics mechanisms into one integrated delivery orchestration view.

## 2. Problem being solved

The repository has mature Product, Architecture, Engineering, Security, Database, QA and Compliance/Risk personas and a strong admission/review governance framework. It does not yet have one named persona accountable for the integrated path from approved work to predictable release across:

- workstream sequencing;
- critical path;
- dependency ageing;
- parallelization;
- milestone/forecast confidence;
- delivery RAID and decision latency;
- release orchestration;
- schedule recovery and hypercare closure.

Without a named Delivery authority, these concerns can become implicit, split across specialist roles or reduced to status reporting.

## 3. Proposed changes

### Persona

Add `docs/context/roles/kalpana-delivery-head/` with modular guidance for:

- persona identity and behavioural rules;
- insurance/bancassurance/B2B/B2C/B2B2C delivery capability;
- authority/decision rights;
- workstream/critical-path/parallelization planning;
- dependency, RAID, decision and escalation control;
- release, recovery and fast-track control;
- delivery health, cadence, metrics and maintenance.

### Delivery Control System

Add `docs/governance/DELIVERY-CONTROL-SYSTEM.md` as an orchestration overlay. It references existing AIGEM SSOT instead of duplicating lifecycle state, scope, priority, dependencies, reviews, readiness or metrics.

### Segregation of duties

Add `PERSONA-AUTHORITY-MATRIX-DELIVERY-ADDENDUM.md` and `delivery-cross-persona-decision-protocol.md` to make Delivery interactions explicit.

### Discovery

Update repository agent/persona discovery so delivery questions route to Kalpana and her DCS.

## 4. Authority decision

Kalpana would own/accountably coordinate:

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

She would **not** own or override:

- Rajal's Product intent/scope/priority/acceptance;
- Mahesh's Architecture decisions;
- Amit's implementation Engineering;
- Deepali's Security outcome/Board 4 conclusion;
- Aarti's persistence/database integrity authority;
- Swapnali's QA evidence/test-strategy authority;
- Shailja's Compliance/Risk permissibility/control authority;
- mandatory human sign-offs or material risk acceptance.

## 5. No eighth board

This change does **not** introduce another AIGEM review board.

Kalpana is an orchestration authority operating across the existing seven-board lifecycle. A Delivery dependency on a specialist decision does not transfer that specialist's review/approval/block authority to Kalpana.

## 6. Relationship to existing governance

DCS reuses:

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

1. Kalpana cannot alter `CURRENT-STATE.yaml` stage/scope authority independently.
2. Delivery urgency cannot waive Security, Compliance, QA, data-integrity or Architecture controls.
3. `READY_TO_CONVENE_GO_NO_GO` is not approval.
4. `APPROVED_FOR_COORDINATED_RELEASE` requires all actually required recorded approvals/sign-offs.
5. AI simulation of Kalpana cannot impersonate mandatory human approval or material risk acceptance.
6. Product criticality such as P0/P1/P2/P3 remains distinct from AIGEM P1–P5 priority.
7. Kalpana-local `DL0–DL3` is delivery-impact severity only and cannot overwrite another persona's severity.
8. Mocks/stubs/simulators may remove waiting during build but never substitute for required real integration/release evidence.

## 8. Expected benefit

The change should improve:

- end-to-end delivery ownership without authority overlap;
- critical-path visibility;
- early external/provider/environment planning;
- safe parallel execution;
- decision and dependency latency;
- realistic timeline confidence;
- release-readiness transparency;
- recovery speed when forecasts slip;
- clean separation between “code complete”, “deployed” and truly “delivered”.

## 9. Ratification requirement

This CR is raised by the agent/user-directed change process but is **not self-approved by the AI**. Any governance binding effect that requires human ratification remains subject to the repository's existing change-control and mandatory approval rules.

## 10. Acceptance criteria

- [x] Kalpana has a canonical modular persona package.
- [x] DCS explicitly composes existing AIGEM rather than replacing it.
- [x] No eighth board is created.
- [x] Delivery authority and `Not Authorised` boundaries are explicit.
- [x] Rajal/Mahesh/Amit/Deepali/Aarti/Swapnali/Shailja handoffs are covered.
- [x] Critical path, dependencies, decision latency, parallelization, release, recovery and hypercare are covered.
- [x] Delivery authority extension is documented separately from the existing canonical matrix to avoid silently rewriting specialist rights.
- [x] Repository discovery points agents to Kalpana for delivery questions.
- [x] No runtime code, API, schema or production configuration is changed.