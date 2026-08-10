---
name: aigem-triage
description: Triage a requirement, bug, suggestion, AI recommendation, review comment, or scan finding through the AIGEM governance pipeline before any code is written — decides whether it fits the current lifecycle stage and approved scope, how necessary it is (MUST/SHOULD/COULD/NOT-NOW/REJECT), what work type it is, its P1–P5 priority now and at its target stage, its dependencies, and whether to ADMIT, PARK, REJECT or ESCALATE it. Use whenever a new idea or change request arrives, whenever you notice an improvement while working on something else, whenever asked "should we do X" or "is this the right time for X", and before starting any implementation. Also use when parking or unparking work, building an implementation plan, or running the seven-board review gate.
---

# AIGEM Triage

Governance model: [`docs/governance/`](../../../docs/governance/README.md).
Binding agent contract: [`09-AI_EXECUTION_RULES.md`](../../../docs/governance/09-AI_EXECUTION_RULES.md).

## The one rule

**A suggestion is never implemented in the turn it is raised.** Triage it, record it, schedule
it, then return to the work item you were on. Only the P1 override classes interrupt.

## Before anything: know where you are

Operating manual: [`RUNBOOK.md`](../../../docs/governance/RUNBOOK.md).
**[§8](../../../docs/governance/RUNBOOK.md#8-what-the-ai-agent-must-know-about-this-project)
is your role card** — the ten facts you must be able to state before acting, and **§8.3, how
your posture must change at the current lifecycle stage**. Read §8.3's row for your workstream
before every triage: the same suggestion is right at L6 and wrong at L7.

```bash
java scripts/governance/FreshnessCheck.java
# exit 0 fresh · 1 warn (disclose it) · 2 do NOT admit new work — park/reject only
```

## Run this

```text
STEP 0  Read docs/governance/state/CURRENT-STATE.yaml
        → workstream, current phase, objective, in/out of scope, open gate.
        Missing, malformed, or past review_due → STOP and report (Rule CS-1).

STEP 1  Mint SUG-<YYYYMMDD>-<3 chars>, e.g. SUG-20260812-a1b (no shared counter).
        Check registers/SUGGESTION-REGISTER.md and TECH-DEBT.md first —
        a repeat is a link plus recurrence_count, not a new row.

STEP 2  Stage fit  → 03-LIFECYCLE.md §3
        SF0 prerequisite · SF1 on-stage · SF2 adjacent · SF3 premature · SF4 invalid
        SF3 MUST carry target_stage + unpark_trigger.
        SF2 must pass the absorption test (small, no new dependency, no new
        decision, gate-neutral) or it parks.

STEP 3  Scope fit  → 02-PROJECT_SCOPE.md §3
        SC0 explicit · SC1 derived (must name `serves`) · SC2 adjacent → Ideas ·
        SC3 out → REJECT · SC4 externally mandated → ESCALATE.

STEP 4  Necessity  → 16-DECISION_MODEL.md §2
        MUST / SHOULD / COULD / NOT-NOW / REJECT, plus evidence tier (E1–E7)
        and confidence (C1–C5). Confidence < C3 ⇒ spike, not implementation.
        Run the anti-over-engineering tests (§6) for anything adding structure.

STEP 5  Action matrix → 00-GOVERNANCE.md §6 → ADMIT | PARK | REJECT | ESCALATE
        PARK/REJECT/ESCALATE: write the register line and STOP.

── ADMIT only, from here ──

STEP 6  Classify   → 06-WORK_CLASSIFICATION.md §2–§3 (type + risk tier T1–T4)
STEP 7  Score      → 05-PRIORITY_MODEL.md
        Hard P1 overrides first (§3, evidence required); else
        SCORE = 2N + 2S + 2B + 2R + D − E, then caps PRI-2/PRI-3/PRI-5/PRI-6.
        Always record priority_now AND priority_at_target.
STEP 8  Dependencies → 07-DEPENDENCY_MODEL.md (edges, state, enablement count)
        Blocked by a parked item ⇒ PARKED-DEPENDENT, inherit the park not the priority.
STEP 9  Break down → 06 §5. One owner + one acceptance outcome = story.
        Two or more epic triggers = epic (needs completion_definition + not_included).
STEP 10 Plan       → 10-IMPLEMENTATION_PLAN_TEMPLATE.md (T2+; files_expected and
        out_of_scope are the drift contract)
STEP 11 Review     → 11-REVIEW_GATES.md, boards mandatory for the tier, in role,
        one at a time, evidence per verdict
STEP 12 Gate → READY (12-DEFINITION_OF_READY.md) → order (07 §5) → implement
STEP 13 Drift checks throughout → 17-DRIFT_CONTROL.md
STEP 14 Done only with evidence → 13-DEFINITION_OF_DONE.md, then close the registers
```

## Reply shape

```text
SUG-20260812-a1b · "Use Redis for idempotency"

Stage:      Phase 4 (Hardening) — this belongs to Phase 5.4
Scope:      In scope (SC0), not in this increment
Necessity:  SHOULD now → MUST before scale-out
Verdict:    PARK → Phase 5, unparks at the Phase 4 gate
Priority:   P4 now · P2 at target
Recorded:   docs/governance/registers/PARKED-BACKLOG.md

Continuing with FUNC-011.
```

Five facts and a return to the task. The last line is not optional.

## Templates and schemas

| Need | File |
|------|------|
| Triage record | `docs/governance/templates/TRIAGE-RECORD.md` |
| Work item / bug / spike | `docs/governance/templates/WORK-ITEM.md` |
| Epic | `docs/governance/templates/EPIC.md` |
| Implementation plan | `docs/governance/templates/IMPLEMENTATION-PLAN.md` |
| Review verdict | `docs/governance/templates/REVIEW-VERDICT.md` |
| ADR | `docs/governance/templates/ADR.md` |
| Validation | `docs/governance/schemas/` |

## Never

- Implement a suggestion in the turn it is raised
- Hold more than one work item in flight
- Park without a target stage and an unpark trigger
- Claim a P1 override without naming its evidence
- Edit stage fields in `CURRENT-STATE.yaml`, or approve a change request
- Delete a parked or rejected register entry
- Mark anything Done without evidence
