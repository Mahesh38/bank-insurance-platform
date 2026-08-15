# Suggestion Register

**Every input that could become work gets a row here — admitted, parked, rejected, or escalated.**
Nothing is dropped. Nothing is deleted.

**Owner:** whichever agent or person triaged the input
**ID format:** `SUG-<YYYYMMDD>-<3 chars from 0-9a-z>` — collision-resistant, no shared counter.
Rules: [../state/CURRENT-STATE.yaml](../state/CURRENT-STATE.yaml) `id_allocation`
**Rules:** [08-BACKLOG_RULES.md](../08-BACKLOG_RULES.md) · [09-AI_EXECUTION_RULES.md](../09-AI_EXECUTION_RULES.md)

---

## How to add a row

1. Mint an ID: `SUG-<today>-<3 random chars>`, e.g. `SUG-20260812-a1b`. No counter to
   increment and no merge conflict when two branches triage at once.
2. Run pipeline steps 2–5 ([09 §2](../09-AI_EXECUTION_RULES.md#2-the-mandatory-sequence)).
3. Add the summary row below.
4. For anything beyond a trivial reject, add a detail block in §3 using
   [../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).
5. **Check for duplicates first** (Rule CS-2). A repeat is linked and increments
   `recurrence_count` — it is not a new row.

---

## 1. Status vocabulary

| Status | Meaning |
|--------|---------|
| `ADMITTED` | Entered a backlog for the current stage |
| `ADMIT-BYPASS` | Implemented under a human override of the process ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)) |
| `PARKED` | Real work, later stage — see [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) |
| `ESCALATED` | Awaiting a human decision as `CR-###` |
| `REJECTED` | Will not be done; reason recorded |
| `DUPLICATE` | Already tracked; linked |
| `SUPERSEDED` | Overtaken by another decision; linked |
| `LAPSED` | Idea closed by aging (AS-3) |
| `CLOSED-DELIVERED` | Admitted and shipped; linked to the PR |

---

## 2. Register

| ID | Date | Source | Summary | SF | SC | Necessity | Type | P now / target | Action | Ref |
|----|------|--------|---------|----|----|-----------|------|----------------|--------|-----|
| SUG-20260814-k3p | 2026-08-14 | human:Mahesh | Close the open GATE-P4 criteria so WS-1's gate can be closed as approved | SF1 | SC0 | MUST | OPS | P2 / P2 | ESCALATED | [§3](#sug-20260814-k3p--close-gate-p4-so-ws-1-can-be-approved) · [GATE-P4-EVIDENCE](../../1sb-insurance-integration/service-ssot/phase-4/GATE-P4-EVIDENCE.md) |
| SUG-20260815-r7m | 2026-08-15 | human:Mahesh | Realign the programme: audit L0–L10 coverage, restructure phases/sub-phases, define epics, stories, priorities and sequence | SF0 | SC0 | MUST | GOV | P1 / P1 | ESCALATED | [§3](#sug-20260815-r7m--programme-realignment-l0l10-audit) · [CR-010](../change-requests/CR-010-programme-realignment.md) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

<!-- ### SUG-0001 · <title>  ... full triage record ... -->

### SUG-20260814-k3p · Close GATE-P4 so WS-1 can be approved

```yaml
# schema: triage-record
id: SUG-20260814-k3p
raised_at: "2026-08-14"
raised_by: "human:Mahesh"
source: "direct request to the agent"
input: >
  "Pick up the ready task now. We need to close the criteria gate, which is open for
  the work stream so that we can close it as app."

context:
  workstream: WS-1
  current_phase: "Phase 4 — Hardening & consumer enablement"
  canonical_stage: "L7 — Hardening"
  current_objective: "Term path signed off for UAT use by at least one bank caller"
  state_as_of: "2026-08-10"
  state_provisional: false          # FreshnessCheck exit 0 — FRESH
  active_work_item: null            # nothing in flight; branch was level with main

stage_fit:
  code: SF1
  rationale: >
    Closing GATE-P4's criteria IS the current stage. L7 posture is "prove it, don't
    extend it" — runbooks, evidence and compliance review are exactly the bias.

scope:
  code: SC0
  business_scope: "in scope — current_scope.in_scope lists the operations runbook explicitly"
  authority: "CURRENT-STATE.yaml current_gate GATE-P4; ACTION-PLAN Phase 4"
  minimal: true

necessity:
  now: MUST
  future_necessity: MUST
  binds_when: "Phase 4 cannot exit until every criterion carries evidence"
  failure_without_it: >
    Phase 4 never exits. WS-1 stays at L7 with the Term path unproven for UAT, and
    every item parked on "Phase 4 gate PASSED" (TD-022, TD-010) stays parked.
  evidence_tier: E1                 # the gate definition itself
  confidence: C5

action: ESCALATE
action_rationale: >
  The request splits in two, and the halves get different verdicts.

  (a) "Close the gate / mark it approved" — ESCALATE. 04 §5 reserves PASSED to
      Architect + PO jointly and states an AI agent may NEVER mark a gate PASSED.
      Independently of authority, the evidence does not exist: 0 of 7 criteria are
      closed. 4.3 needs an external bank caller, 4.4 and 4.5 need human board
      verdicts, 4.1 and 4.6 need a working sandbox (CONFIRM-01 §D still open).
      Per RUNBOOK §9, a criterion that cannot be met goes to Architect + PO for a
      waiver or a moved criterion — BEFORE CANDIDATE.

  (b) "Do the ready work that closes a criterion" — executed, not re-admitted.
      Criterion 4.5's runbook is already in current_scope.in_scope and is already a
      gate criterion, so it is admitted work, not a new suggestion. It was the only
      OPEN criterion fully ownable without an external party or environment, and its
      blocker (an approver named "Ops", with no persona) was cleared by CR-008.
conflicts:
  - >
    04-STAGE_GATES.md §6 named 4.5's approver as "Ops"; CURRENT-STATE.yaml named
    "Shivanshi / SRE (4.5)" under CR-008. Resolved in favour of the state file, which
    is authoritative (Rule CS-1). 04-STAGE_GATES.md corrected in this change.

outcome:
  registered_in: "registers/SUGGESTION-REGISTER.md"
  work_item_id: null
  plan_id: null
  status: ESCALATED
  closed_reason: null

resumed: "none — this was the session's only work item"
```

**Artefacts produced**

| Artefact | Purpose |
|---|---|
| [`phase-4/OPERATIONS-RUNBOOK.md`](../../1sb-insurance-integration/service-ssot/phase-4/OPERATIONS-RUNBOOK.md) | Criterion 4.5 evidence — **DRAFT, unsigned** |
| [`phase-4/GATE-P4-EVIDENCE.md`](../../1sb-insurance-integration/service-ssot/phase-4/GATE-P4-EVIDENCE.md) | Criterion-by-criterion evidence table for GATE-P4 |
| [`04-STAGE_GATES.md`](../04-STAGE_GATES.md) | Approver drift corrected ("Ops" → Shivanshi); 4.5 → Partial |

**Awaiting a human decision**

- **Architect + PO** — waiver, move, or hold for 4.3 (external bank caller) and 4.7 (QA-001).
- **Shivanshi / Board 7** — verdict on `OPERATIONS-RUNBOOK.md`; an `APPROVED` closes 4.5.

**Raised but deliberately not actioned**

- **TD-014's unpark trigger has fired** (overlaps criterion 4.1). Left for R1/R2 triage —
  one work item in flight ([RUNBOOK §8.4](../RUNBOOK.md#84-the-five-behaviours-that-matter-most)).

---

### SUG-20260815-r7m · Programme realignment (L0–L10 audit)

```yaml
# schema: triage-record
id: SUG-20260815-r7m
raised_at: "2026-08-15"
raised_by: "human:Mahesh"
source: "direct request to the agent, reasoning in the Rajal / R1 persona"
input: >
  "Act as Rajal, rethink on the phases and services the way project going right now.
  Are we on right track? There are l0 to l10 stages are defined, what we started with
  l0 or jumped directly to l4 or l5. In all stages there will be multiple smaller task
  and sub phases will be there, have we covered all? Or have we skipped anything and we
  need to rethink on it? I want to realign the project. Define the the phases, sub
  phases, create epics and stories under that epics. Make sure he have we decide the
  priorities and right sequence."

context:
  workstream: WS-1                  # the input's nominal home; the finding is that it needs WS-3
  current_phase: "Phase 4 — Hardening & consumer enablement"
  canonical_stage: "L7 — Hardening"
  current_objective: "Term path signed off for UAT use by at least one bank caller"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null

stage_fit:
  code: SF0
  rationale: >
    A programme whose scope of record covers 2 of 29 epics cannot exit any gate
    meaningfully. GATE-P4 is currently unpassable by construction (criterion 4.3 needs a
    journey, consent gate and suitability gate that no workstream owns). Correcting the
    baseline is a prerequisite to closing the current stage, not adjacent to it.

scope:
  code: SC0
  business_scope: "in scope — R0-SCOPE.md and PRD-R0 are approved programme authority"
  authority: "R0-SCOPE.md; PRD-R0-DISTRIBUTION-PLATFORM.md; 03-PROGRAMME-TODO.md"
  minimal: false                    # deliberately: a partial realignment reproduces the gap

necessity:
  now: MUST
  future_necessity: MUST
  binds_when: "immediately — three shipped/locked contradictions are already live (S4, S5, S6)"
  failure_without_it: >
    Programme L1/L2 work keeps being evaluated against WS-1's L7 posture and auto-parks as
    premature. 27 epics stay ungoverned. The A7 suitability gate and A2 self-service channel
    remain contradicted by delivered code and governed scope respectively.
  evidence_tier: E1                 # direct contradiction between existing approved documents
  confidence: C5

action: ESCALATE
action_rationale: >
  Adding a workstream, re-scoping a stage gate and editing routing are all CR-triggering
  changes under 14 section 1, approvable only by PO + Architect. An agent may not approve a
  change request. The analysis and the proposal are delivered; ratification is human.

  Two findings are additionally reserved to other authorities and are NOT pre-judged here:
  S4 (customer identity trust boundary) is Deepali/R8, and S6 (is the shipped quote path
  permissible without suitability and consent gates) is Shailja/R9 alone.
conflicts:
  - >
    R0-SCOPE A2 (RM + Self-service + Hybrid Day 1) vs CURRENT-STATE.yaml WS-2 out_of_scope
    (retail-customer authentication deferred). Unresolved — raised as decision D-C.
  - >
    R0-SCOPE A7 (suitability mandatory before quote) vs D11 (consentRef WARN-only in P0) and
    a delivered quote path with no suitability gate. Unresolved — raised as decision D-D.
  - >
    CURRENT-STATE.yaml routing.ARCH points at a non-existent path. Factual error; correction
    proposed in CR-010 section 5.

outcome:
  registered_in: "registers/SUGGESTION-REGISTER.md"
  work_item_id: null
  plan_id: null
  status: ESCALATED
  closed_reason: null

resumed: "none — this was the session's work item"
```

**Artefacts produced**

| Artefact | Purpose |
|---|---|
| [`po-drive/05-PROGRAMME-REALIGNMENT.md`](../../au-bank-insurance-platform/po-drive/05-PROGRAMME-REALIGNMENT.md) | L0–L10 audit, 8 findings, 6 phases, 20 sub-phases, 29 epics, scored stories, critical path |
| [`CR-010`](../change-requests/CR-010-programme-realignment.md) | The governance vehicle — 4 separable changes, PENDING RATIFICATION |
| `state/CURRENT-STATE.yaml` | CR counter advanced to 11 (ID hygiene only — **no stage field edited**) |

**Awaiting a human decision** — 8 decisions listed in the proposal §9. The two urgent ones are
**D-C** (S4, customer identity) and **D-D** (S6, quote-path compliance); both are contradictions
between locked decisions and shipped work, and D-D carries O5 regulatory exposure.

---

## 4. Seeded from existing artefacts

AIGEM was adopted mid-flight. Rather than backfilling every past decision
([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)), the
already-deferred items in [TECH-DEBT.md](../../1sb-insurance-integration/service-ssot/TECH-DEBT.md)
were seeded directly into [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) as pre-existing parked work.
They keep their `TD-###` IDs; no `SUG-####` was minted retrospectively.

**Do not re-triage or re-report these** — they are known
([01 §6](../01-CURRENT_STATE.md#6-known-open-debt-affecting-triage)).

---

## 5. Register row convention (machine-enforced)

> **A table row whose first cell is a bare ID is that ID's DEFINITION.** Exactly one definition
> may exist per ID, across every register. `FreshnessCheck` enforces this and halts on a
> duplicate — that is how a cross-branch ID collision is caught after a merge.
>
> Cross-reference rows — the same item shown again in another view, such as an external
> dependency also listed under its edge, or an open risk repeated under accepted risks — must
> **point at** the definition rather than restate the bare ID — for example a leading cell
> of `→ [DEP-002](./DEPENDENCY-REGISTER.md#1-edges)` instead of a bare `DEP-002`.
