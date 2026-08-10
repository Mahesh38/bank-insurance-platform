# 19 — Porting AIGEM to Another Repository

**Layer:** L1 — generic
**Purpose:** install this framework in a new repo in under an hour

---

## 1. What travels and what does not

| Copy unchanged (L1) | Rewrite (L3) | Adapt once per org (L2) |
|---------------------|--------------|--------------------------|
| `00-GOVERNANCE.md` (§3 routing table needs editing) | `01-CURRENT_STATE.md` | `ORG-STANDARDS.md` |
| `03-LIFECYCLE.md` (§6 stage map) | `02-PROJECT_SCOPE.md` | |
| `05`, `06`, `07`, `08`, `09` | `04-STAGE_GATES.md` | |
| `10`–`19` | `state/CURRENT-STATE.yaml` | |
| `templates/`, `schemas/` | `registers/*` (start empty) | |

Rule of thumb: **anything naming a phase, a service, a person, or a backlog file is L3.**

---

## 2. Installation

```bash
# 1. Copy the framework
mkdir -p <target-repo>/docs/governance
cp -r docs/governance/{00..19}-*.md templates schemas <target-repo>/docs/governance/
mkdir -p <target-repo>/docs/governance/{registers,state}

# 2. Start empty registers (headers only)
cp docs/governance/registers/*.md <target-repo>/docs/governance/registers/
#    then delete the example rows

# 3. Seed project state
cp docs/governance/state/CURRENT-STATE.yaml <target-repo>/docs/governance/state/
```

Then, in order:

1. **`state/CURRENT-STATE.yaml`** — workstreams, current stage, objective, in/out of scope,
   gate criteria. This is the single most important file; everything else degrades gracefully
   without it, and nothing works with it wrong.
2. **`01-CURRENT_STATE.md`** — narrative form, standing constraints, known debt.
3. **`02-PROJECT_SCOPE.md`** §6+ — the actual in/out lists.
4. **`03-LIFECYCLE.md`** §6 — map the project's phases onto L0–L10.
5. **`04-STAGE_GATES.md`** §6 — exit criteria per stage, with evidence.
6. **`00-GOVERNANCE.md`** §3 — the routing table: which existing backlog files receive which
   work types. Do not create new backlogs; point at the ones that exist.
7. **`08-BACKLOG_RULES.md`** §3 — the same routing, from the backlog side.
8. **Agent entry point** — add the governance block to `AGENTS.md` / `CLAUDE.md` / `.cursorrules`
   (§4).

---

## 3. Calibrating for project size

The framework scales down. Do not run a seven-board review on a two-person project.

| Project size | Adjustment |
|--------------|------------|
| Solo / prototype | Boards collapse to **Technical + Product**, both agent-run. Keep triage, park registers, and drift control — they carry most of the value. |
| Small team (2–5) | Boards: Architecture, Technical, Product, QA. Security and Compliance only when their impact fields are non-`none`. |
| Regulated / financial | Full seven boards. T4 human sign-off on Security and Risk & Compliance is non-negotiable. |
| Multi-team platform | Add a workstream per team; keep one shared registers folder so cross-team dependencies stay visible. |

What must **never** be dropped, at any size:

1. Context resolution before triage
2. Stage-fit classification
3. Park-with-target-and-trigger
4. One-active-item
5. The P1 interrupt list

Those five deliver the anti-drift benefit. Everything else is refinement.

---

## 4. Agent entry-point snippet

Add to `AGENTS.md`, `CLAUDE.md`, `.cursorrules`, or the equivalent:

```markdown
## Governance — read before suggesting or implementing anything

This repository runs the AIGEM governance model. Before acting on any requirement,
bug, suggestion, or finding:

1. Read `docs/governance/state/CURRENT-STATE.yaml` for the current stage and scope.
2. Follow `docs/governance/09-AI_EXECUTION_RULES.md` — the binding agent contract.
3. Triage every input through the pipeline in `docs/governance/README.md` §5
   before writing code.

Core rules:
- A suggestion is never implemented in the turn it is raised. Triage, record, schedule.
- Exactly one work item in flight. Only the P1 override classes may interrupt it
  (`05-PRIORITY_MODEL.md` §3).
- Parked is not deleted: every deferral records a target stage and an unpark trigger.
- Every TODO carries a work item ID.
- Never mark work Done without evidence.
```

---

## 5. Bootstrapping into an existing project mid-flight

Most adoptions happen partway through delivery. Do not attempt a retrospective triage of the
whole backlog.

```text
Day 1  Write state/CURRENT-STATE.yaml. Nothing else matters until this is right.
Day 1  Add the agent entry-point snippet.
Day 2  Seed the parked backlog from known deferrals — in this repo, the TECH-DEBT
       ledger's "Deferred" rows are already parked items in all but name.
Day 2  Seed the risk register from existing known risks.
Week 1 Triage new inputs only. Do not backfill.
Week 2 First gate scorecard → baseline metrics (18 §3).
Gate 1 First unpark sweep. This is where the model proves itself: parked items
       actually return.
```

Backfilling old decisions costs days and teaches nothing. The framework earns trust on
*new* inputs, and the registers fill themselves within a stage.

---

## 6. Adoption smoke test

Run these five inputs through the model. If the answers match, the installation is sound:

| Test input | Expected verdict |
|------------|------------------|
| "Add a message broker" during early-stage work | SF3 → PARK with a target stage |
| "Fix a null check that makes an AC fail" | SF0 / MUST → P1 ADMIT |
| "Rewrite this module in a nicer style" | SC2/E7 → Ideas or REJECT |
| "Encrypt PII at rest" when it is not yet stored | NOT-NOW, `future_necessity: MUST`, target = the stage where it is stored |
| "Add retries because the upstream might fail" with no observed failure | X9 fails → COULD, likely park |

If any of these comes back "sure, let's do it now", the current-state file is wrong or the
agent entry point is not being read.

---

## 7. Common installation mistakes

| Mistake | Consequence | Fix |
|---------|-------------|-----|
| Leaving the example project data in `01`/`02`/`04` | Agents triage against the wrong project — confidently | Rewrite all L3 files before first use |
| Creating a new backlog instead of routing to existing ones | Two sources of truth; both rot | Fix the routing table ([00 §3](./00-GOVERNANCE.md#3-scope-of-authority)) |
| Parking without an unpark trigger | Parked backlog becomes a graveyard | Schema enforces it — validate records |
| Running all seven boards on everything | Rubber-stamping within two weeks | Apply the tier matrix ([11 §3](./11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory)) |
| No one owns `CURRENT-STATE.yaml` | It goes stale; Rule CS-1 halts triage | Name an owner; set `review_due` |
| Agents allowed to approve their own T3/T4 plans | Approval theatre | Enforce [11 §2](./11-REVIEW_GATES.md#2-who-may-sit-on-a-board) |
