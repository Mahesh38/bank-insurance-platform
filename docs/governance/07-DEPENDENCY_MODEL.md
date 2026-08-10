# 07 — Dependency Model & Execution Ordering

**Layer:** L1 — generic
**Pipeline step:** 7 — Dependency Analysis (and the ordering step after approval)
**Owner:** Tech Lead · Architect (architectural and decision dependencies)

---

## 1. Why this step exists

Priority says *what matters*. Dependencies say *what is possible*. A P1 item whose prerequisite
is missing is not startable, and starting it anyway produces the most expensive kind of
rework — work built on a foundation that later moves.

The output of this step is one of three states:

| State | Meaning |
|-------|---------|
| `READY` | All hard dependencies satisfied — may be scheduled by priority |
| `BLOCKED` | A hard dependency is unsatisfied — the **blocker** inherits urgency, not this item |
| `PARKED-DEPENDENT` | The blocker is itself parked to a later stage — this item parks with it |

`PARKED-DEPENDENT` is the case the blueprint calls out: *"this requirement depends on B or C,
so maybe I can park it"*. It is a legitimate, recorded outcome — not a failure to plan.

---

## 2. Dependency types

| Type | Meaning | Violation cost |
|------|---------|----------------|
| `HARD` | Cannot start, or cannot be correct, until the other completes | Rework or incorrect behaviour |
| `SOFT` | Better after, tolerable before | Friction, minor rework |
| `TECHNICAL` | Code, module, library, or contract prerequisite | Compile/runtime failure |
| `BUSINESS` | A business rule, product decision, or requirement must be settled | Wrong behaviour shipped |
| `EXTERNAL` | Third party: credentials, sandbox access, partner API, another team | Idle work, wasted spend |
| `ARCHITECTURAL` | A boundary, contract, or topology decision must land first | Structural rework |
| `DATA` | Schema, migration, backfill, or reference data must exist | Data loss or corruption |
| `SECURITY` | Control (authn/authz/crypto/secret) must exist first | Exposure window |
| `COMPLIANCE` | Consent, retention, audit, or approval must be in place | Regulatory breach |
| `ENVIRONMENT` | Environment, pipeline, or infrastructure must exist | Cannot verify |
| `DECISION` | An ADR or explicit choice must be recorded | Churn; competing implementations |

`DECISION` is the type teams forget. **A decision is work.** If an item is blocked by an
unmade decision, raise the ADR as its own item with its own priority — do not let a design
question masquerade as an implementation task.

---

## 3. Relation vocabulary

```yaml
dependencies:

  blocked_by:            # HARD — this cannot start until these are Done
    - ARCH-021

  requires:              # must exist and be correct, may already be Done
    - DOMAIN-014

  enables:               # items that become startable when this completes
    - API-031
    - API-032

  related_to:            # informational — no ordering constraint
    - NFR-008

  external:              # outside our control; needs an owner and a date
    - insurer_api_contract

  decision_dependency:   # ADRs / choices that must be recorded first
    - ADR-004
```

Each edge is recorded in [registers/DEPENDENCY-REGISTER.md](./registers/DEPENDENCY-REGISTER.md)
with `DEP-###`, its type, and its state — because edges outlive the items that discovered them.

`enables` is not decorative: the count of items an item enables is a **priority tie-breaker**
([05 §4](./05-PRIORITY_MODEL.md#ordering-inside-a-band)) and the main driver of ordering inside
a band. An enabler with three dependants beats a leaf item of equal score.

---

## 4. Analysis procedure

```text
1. List candidate edges from: the plan's affected components, the contracts it touches,
   the data it reads/writes, the decisions it assumes, the environments it needs.
2. Type each edge (§2).
3. For each edge, resolve the other end's state:
      DONE            → satisfied
      READY/IN-FLIGHT → this item is BLOCKED; record expected release
      PARKED          → this item becomes PARKED-DEPENDENT (inherits the blocker's
                        target_stage unless it has a later one of its own)
      DOES NOT EXIST  → create it as a work item, then re-run this step
4. Detect cycles (§6).
5. Compute: state, earliest-start, enablement count.
6. Write the result into the triage record and the dependency register.
```

> **Rule DEP-1 — An unsatisfied dependency that is not a work item does not exist.** "We'll need
> the schema first" is not a dependency until the schema has an ID.

> **Rule DEP-2 — Inherit the park, not the priority.** A P2 item blocked by a P4 parked item
> becomes `PARKED-DEPENDENT` at the blocker's target stage. It keeps `priority_at_target: P2`
> so it reactivates at the right urgency. This is how one premature prerequisite correctly
> parks a whole branch without anyone losing the branch.

> **Rule DEP-3 — External dependencies get a name and a date.** `external` edges must carry an
> owner and a follow-up date, or they are not tracked, they are hoped for. Work blocked
> externally is parked, and the *chase* becomes its own item.

---

## 5. Execution ordering

Once items are approved and READY, order is computed — not chosen:

```text
1. Build the graph from blocked_by + requires + decision_dependency edges.
2. Topologically sort. Cycles → §6.
3. Take the READY set (all predecessors Done).
4. Sort the READY set by:
      a. priority_now                (P1 → P5)
      b. score                       (05 §4, descending)
      c. enablement count            (descending — unblock the most)
      d. effort                      (ascending — clear cheap enablers)
      e. raised_at                   (ascending — anti-starvation)
5. Take the head. That is the next item. Do not re-derive per developer preference.
6. On completion, recompute the READY set — do not reuse a stale queue.
```

**Cross-workstream:** each workstream has its own queue. An agent works one workstream's queue
at a time; edges that cross workstreams are `HARD` only if a contract is genuinely shared,
otherwise `SOFT`.

Worked ordering example (WS-1, Phase 4 gate):

| Item | P | Score | Enables | Effort | Order |
|------|---|-------|---------|--------|-------|
| 4.4 Compliance review of audit schema | P1 | 28 | 2 (go-live checklist, 4.3) | M | **1** |
| 4.1 Sandbox E2E in CI | P1 | 26 | 3 (4.3, 4.6, Phase 5 regression) | L | **2** |
| 4.2 Publish OpenAPI + collection | P2 | 20 | 1 (4.3) | S | **3** |
| 4.3 Bank consumer UAT spike | P2 | 19 | 0 | M | 4 — `blocked_by` 4.2, `external` (bank app team) |
| 4.6 Performance smoke | P2 | 18 | 0 | M | 5 — `blocked_by` 4.1 |
| 4.5 Ops runbook | P3 | 14 | 0 | S | 6 |

4.4 precedes 4.1 despite similar scores: it enables two items and is unblocked. 4.3 sits below
its priority because it is externally blocked — and per DEP-3 the *chase* for the bank app team
is a separate item that is **not** blocked.

---

## 6. Cycles

A cycle means the decomposition is wrong, not that the work is impossible.

```text
A blocked_by B, B blocked_by A
   │
   ├─ 1. Is one edge actually SOFT?      → downgrade, cycle resolves
   ├─ 2. Can A be split into A1 (what B needs) and A2 (what needs B)?  → split
   ├─ 3. Is the real blocker a DECISION both share?  → raise the ADR first;
   │                                                    both then depend on it, not each other
   └─ 4. None of the above → ESCALATE to Architecture board. Do not "just start somewhere".
```

Every resolved cycle is recorded in the dependency register with the technique used —
recurring cycles between the same components are an architecture signal.

---

## 7. Dependency-driven parking (worked)

```yaml
item: FUNC-016 "Health quote handler"
priority_now: P2
dependencies:
  blocked_by: [FUNC-012]          # Health LOB enablement, parked to Phase 5
  requires:   [TECH-006]          # job/poller infra — DONE
  decision_dependency: []
resolution:
  FUNC-012: PARKED (target Phase 5 — Expand LOBs)
outcome:
  state: PARKED-DEPENDENT
  target_stage: "Phase 5 — Expand LOBs"
  unpark_trigger: "Phase 4 gate PASSED"
  priority_now: P4                # capped by PRI-2 through inheritance
  priority_at_target: P2          # restored on unpark
  parked_because: "blocked_by FUNC-012 (parked)"
```

---

## 8. Dependency block in the triage record

```yaml
dependencies:
  edges:
    - { id: DEP-031, type: HARD,     target: FUNC-012, relation: blocked_by, state: PARKED }
    - { id: DEP-032, type: TECHNICAL,target: TECH-006, relation: requires,   state: DONE }
    - { id: DEP-033, type: EXTERNAL, target: "bank app team UAT slot",
        relation: external, owner: "Bank app TL", follow_up: 2026-08-21, state: OPEN }
  state: PARKED-DEPENDENT
  enablement_count: 2
  earliest_start: "Phase 5 entry"
  cycles: none
```
