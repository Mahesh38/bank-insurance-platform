# 09 — AI Execution Rules (The Agent Contract)

**Layer:** L1 — generic
**Status:** Binding on every AI agent operating in this repository
**Read this file before acting on anything.**

---

## 0. The contract in five lines

1. Resolve current state before forming any opinion.
2. Triage every input through the pipeline; emit a record; never implement on impulse.
3. Hold exactly one active work item; queue everything else.
4. Interrupt only for the P1 override classes — and say so explicitly.
5. Leave a written trail: record, plan, verdicts, evidence, register lines.

---

## 1. When this applies

| Situation | Pipeline required? |
|-----------|--------------------|
| A user asks for a new feature or change | ✅ Full pipeline |
| A user reports a bug | ✅ Full pipeline (usually short — O8/O3 override) |
| An agent notices an improvement while doing other work | ✅ Triage only, then **return to the current item** |
| A code review or scan produces a finding | ✅ Full pipeline |
| A user explicitly says "just do X, skip the process" | ⚠️ Honour it, and record the bypass ([§8](#8-when-a-human-overrides-the-process)) |
| Executing an already-approved work item | ❌ No — you are past the gate; follow the plan |
| Answering a question with no change implied | ❌ No |

> **Rule AE-1 — Answering is not implementing.** An agent may explain, analyse, and recommend
> freely. The pipeline governs *changes to the repository*, not conversation.

---

## 2. The mandatory sequence

```text
STEP 0  Load context           → 01-CURRENT_STATE.md + state/CURRENT-STATE.yaml
                                  Stale or missing → STOP and report (Rule CS-1)
STEP 1  Assign SUG-####        → templates/TRIAGE-RECORD.md
STEP 2  Stage fit    (03)      → SF0…SF4  + target_stage/unpark_trigger if SF3
STEP 3  Scope fit    (02)      → SC0…SC4  + serves[] if SC1
STEP 4  Necessity    (16)      → MUST/SHOULD/COULD/NOT-NOW/REJECT + confidence
STEP 5  Action matrix (00 §6)  → ADMIT | PARK | REJECT | ESCALATE
        ── PARK/REJECT/ESCALATE: write the register line and STOP here ──
STEP 6  Classify     (06)      → work type + also[]
STEP 7  Score        (05)      → priority_now, priority_at_target, factors, caps
STEP 8  Dependencies (07)      → edges, state, enablement count
STEP 9  Break down   (06 §5)   → EPIC / STORY / TASK / SPIKE  (+ children)
STEP 10 Plan         (10)      → implementation plan  (skip only for tier T1)
STEP 11 Review       (11)      → mandatory boards for the risk tier
STEP 12 Gate                   → APPROVED → READY (12) · REWORK → back to STEP 10
STEP 13 Order        (07 §5)   → is this actually next? If not, queue it
STEP 14 Implement              → follow the plan; run drift checks (17)
STEP 15 Validate     (13)      → evidence, not assertion
STEP 16 Register               → close the loop in registers/ + backlog
```

Steps 2–5 are cheap and must always run. Most inputs stop at step 5.

---

## 3. One active item

> **Rule AE-2 — An agent holds exactly one `IN-FLIGHT` work item.**

While an item is in flight:

- Do not start a second item, however small.
- Do not "quickly fix" something noticed in passing — write `SUG-####`, continue.
- Do not extend the current item's scope beyond its plan's `files_expected` and
  `affected_components` without re-review ([14 §4](./14-CHANGE_CONTROL.md#4-changing-an-approved-plan)).
- Do not refactor code you are merely reading.

The single most valuable behaviour this framework buys is **finishing the thing you started.**

---

## 4. The interrupt rule

Current work may be preempted **only** by a P1 hard override
([05 §3](./05-PRIORITY_MODEL.md#3-hard-p1-overrides)):

```text
O1 build/pipeline failure · O2 exploitable security vulnerability ·
O3 incorrect domain model · O4 missing mandatory API for the current deliverable ·
O5 regulatory violation   · O6 data corruption or loss ·
O7 blocking dependency for the in-flight item · O8 acceptance criteria failure
```

Interrupt protocol:

```text
1. State the override class and its evidence, out loud, before switching.
2. Snapshot the in-flight item: what is done, what remains, where the code sits.
3. Handle the P1 through the pipeline — a P1 is fast-tracked, not un-governed:
   it still needs a plan and the boards mandatory for its tier.
4. Return to the snapshotted item. Say that you are returning.
```

Nothing else interrupts. Not an elegant refactor, not a missing test on unrelated code, not a
newer library version, not "while we're in here".

---

## 5. Handling your own suggestions

This is the highest-frequency case in practice: mid-task, the agent sees something.

```text
Notice something
  │
  ├─ Is it a P1 override for the CURRENT item (O3/O7/O8)?
  │     └─► It is part of the current item. Handle it. Note it in the plan's variance log.
  │
  ├─ Is it a P1 override for the SYSTEM (O1/O2/O5/O6)?
  │     └─► Interrupt per §4.
  │
  └─ Anything else
        └─► Write SUG-####  (stage fit, scope fit, necessity, one-line rationale)
            Add to the suggestion register
            SAY one line to the user: "Noted as SUG-0043 (parked, Phase 5); continuing FUNC-011."
            RETURN to the current item.
```

The one-line acknowledgement matters: it proves the idea was captured, so neither the user nor
the agent feels the need to act on it now to avoid losing it.

**Batching.** During a long task, collect suggestions and write them to the register in one
pass at the end of the task — but keep the running list visible and never let it change what
you build.

---

## 6. Output discipline

Every governed action produces a written artefact.

| Action | Artefact | Where |
|--------|----------|-------|
| Any input triaged | Triage record | [registers/SUGGESTION-REGISTER.md](./registers/SUGGESTION-REGISTER.md) |
| Parked | Parked entry with target + trigger | [registers/PARKED-BACKLOG.md](./registers/PARKED-BACKLOG.md) |
| Rejected | Closed record with reason | Suggestion register |
| Escalated | `CR-###` | [14-CHANGE_CONTROL.md](./14-CHANGE_CONTROL.md) |
| Admitted | Backlog entry with `origin: SUG-####` | Routed per [08 §3](./08-BACKLOG_RULES.md#3-routing-table-l3) |
| Planned | Implementation plan | Plan file or PR body |
| Reviewed | One verdict per board | Plan `reviews[]` |
| Implemented | Commits referencing the work item ID | Git |
| Validated | Evidence artefacts | Per [13](./13-DEFINITION_OF_DONE.md) |

**Minimum viable triage** for a small input is six lines — this is not heavyweight:

```yaml
SUG-0043: "Add Redis-backed idempotency store"
stage_fit: SF2 (Phase 5.4)      scope_fit: SC0
necessity: SHOULD               priority: P4 now / P2 at Phase 5
action: PARK                    unpark_trigger: "Phase 4 gate PASSED"
```

---

## 7. Prohibited behaviours

| # | Never | Because |
|---|-------|---------|
| 1 | Implement a suggestion in the same turn it is raised | That is the failure this framework exists to prevent |
| 2 | Add a dependency, framework, or infrastructure component not in the approved plan | Architecture verdict required ([00 §8](./00-GOVERNANCE.md#8-non-negotiables)) |
| 3 | Create an abstraction with exactly one implementation "for future flexibility" | [16 §6](./16-DECISION_MODEL.md#6-anti-over-engineering-tests) |
| 4 | Change a public contract, schema, or error code outside plan scope | Downstream consumers |
| 5 | Widen an epic to absorb an adjacent idea | Epics have `not_included` for this reason |
| 6 | Mark anything Done without evidence | [13](./13-DEFINITION_OF_DONE.md) |
| 7 | Write `TODO`/`FIXME` without a work item ID | Rule BL-1 |
| 8 | Self-approve a board that requires a human ([11 §2](./11-REVIEW_GATES.md#2-who-may-sit-on-a-board)) | Approval theatre |
| 9 | Edit `state/CURRENT-STATE.yaml` stage fields | Stage transitions are human ([04 §5](./04-STAGE_GATES.md#5-who-may-declare-a-transition)) |
| 10 | Delete a parked or rejected register entry | Nothing is forgotten |
| 11 | Silently drop part of a requested scope | Say what you left out and why |
| 12 | Re-litigate a closed decision without new evidence | Cite the ADR; new evidence → change request |

---

## 8. When a human overrides the process

A human may say "skip the process, just do it". Honour it — humans outrank the framework
([README §7](./README.md#7-precedence)) — and:

```text
1. Do it.
2. Record: SUG-#### with action: ADMIT-BYPASS, who authorised it, and what was skipped.
3. Name the risk in one sentence ("no Security board ran on an auth-path change").
4. If the bypass touches a non-negotiable (00 §8) — secrets, PII, public contract,
   data integrity — say so once, clearly, before acting. Then follow the instruction.
```

Bypasses are counted in [18](./18-GOVERNANCE_METRICS.md). A high bypass rate means the process
is too heavy for the work, and the process should change — not be quietly ignored.

---

## 9. Session start checklist

At the beginning of any working session:

```text
[ ] Run scripts/governance/freshness-check.py — act on the exit code
    (2 = do not admit new work; park and reject only, and say the state is stale)
[ ] Read RUNBOOK.md §8 — the ten facts you must be able to state, and §8.3 for the
    posture your current lifecycle stage requires
[ ] Read state/CURRENT-STATE.yaml — stage, objective, scope, gate
[ ] Read registers/PARKED-BACKLOG.md — do not re-propose parked items
[ ] Read TECH-DEBT.md open items — do not re-report known debt
[ ] Identify the workstream you are in
[ ] Identify the one work item you are picking up, and confirm it is head of the
    ordered READY queue (07 §5). If it is not, say so and ask.
```

## 10. Session end checklist

```text
[ ] Current item state recorded (done / in-flight with a snapshot / blocked with a blocker ID)
[ ] All suggestions raised during the session written to the register
[ ] Any drift detected and its resolution recorded (17)
[ ] Evidence attached for anything claimed Done
[ ] Registers and backlog updated; TODOs carry IDs
[ ] Uncommitted work either committed or explicitly flagged
```

---

## 11. Standard response shape

When an agent triages an input, its reply to the user follows this shape — short, decisive, and
traceable:

```text
SUG-0043 · "Use Redis for idempotency"

Stage:      Phase 4 (Hardening) — this belongs to Phase 5.4
Scope:      In scope (SC0), not in this increment
Necessity:  SHOULD now → MUST before scale-out
Verdict:    PARK → Phase 5, unparks at the Phase 4 gate
Priority:   P4 now · P2 at target
Recorded:   registers/PARKED-BACKLOG.md

Continuing with FUNC-011.
```

Five facts and a next action. No essay, no hedging, and — critically — **the last line is the
return to the actual task.**
