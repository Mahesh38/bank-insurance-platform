# 08 — Backlog Rules

**Layer:** L1 (buckets, states) + L3 (destinations)
**Owner:** Product Owner · Delivery Lead

---

## 1. One rule above all

> **Every input ends somewhere.** Admitted, parked, rejected, escalated, or duplicated — but
> never dropped, and never "mentioned in a reply and forgotten".

The register *is* the agent's long-term memory. An agent that answers "good idea, but not now"
without writing a register line has deleted the idea.

---

## 2. The six buckets

| Bucket | Contains | Lives in | Exit |
|--------|----------|----------|------|
| **READY** | Approved, unblocked, ordered — may be picked up now | Product/test backlog with `state: READY` | Picked up → IN-FLIGHT |
| **BLOCKED** | Approved but a hard dependency is unsatisfied | Same backlog, `state: BLOCKED` + blocker ID | Blocker Done → READY |
| **PARKED** | Real work, wrong stage | [registers/PARKED-BACKLOG.md](./registers/PARKED-BACKLOG.md) | Unpark trigger fires → re-triage |
| **IDEAS** | Out of scope, plausible value, no committed stage | Same file, Ideas section | Gate sweep or scope change |
| **REJECTED** | Will not be done | [registers/SUGGESTION-REGISTER.md](./registers/SUGGESTION-REGISTER.md), closed | Only via a change request |
| **ESCALATED** | Exceeds agent authority; awaiting a human | [14-CHANGE_CONTROL.md](./14-CHANGE_CONTROL.md), `CR-###` | Human decision |

Bucket assignment is fully determined by the triage verdict — there is no discretionary step
between "verdict" and "bucket".

---

## 3. Routing table (L3)

Once ADMITTED, the work type decides the destination. AIGEM does **not** keep a parallel
backlog.

| Type | Destination |
|------|-------------|
The machine authority is `state/CURRENT-STATE.yaml routing[workstream][type]`. Every active
workstream is closed over the same 16 canonical types and every destination must exist.

| Type | Destination rule |
|---|---|
| `FUNC`, `BUG`, `NFR`, `INFRA`, `OPS`, `SPIKE` | Owning workstream backlog |
| `DEBT`, `REFACTOR` | Owning workstream debt/backlog destination |
| `QA` | Owning workstream quality backlog/destination |
| `ARCH`, `MIGRATION` | Architecture decision log plus owning backlog |
| `SEC`, risk-bearing `COMP` | Owning backlog plus risk register |
| `DOC` | Applied in place and recorded in the suggestion register |
| `GOV` | Governance change control plus owning backlog (see §3.1) |
| `IDEA` | Parked backlog → Ideas |

`TECH-*` and `SHARED-*` are legacy item-ID namespaces, not work types. Classify them as
`INFRA`, `NFR` or `ARCH` before routing.

### 3.1 Governance work is work

> **Rule BR-4 — `GOV` items are triaged, queued and counted like any other work type.**
> A change to the governance framework, a persona package, a register format or this file gets a
> `SUG-####`, a stage-fit and necessity verdict, a priority, a backlog entry and a place in the
> queue — exactly as a `FUNC` item does. It also consumes the **single in-flight slot**
> ([00 §2](./00-GOVERNANCE.md#2-principles), [09](./09-AI_EXECUTION_RULES.md)).

Governance work previously routed only to `docs/governance/**` through change control. Change
control asks *"is this change correct?"* — it never asks *"should we be spending this week's
capacity on governance instead of on the open gate?"*. Nothing compared governance work against
product work, because governance work never entered the queue where that comparison happens.

The measured consequence in this repository: **61 consecutive commits of governance and persona
documentation, zero product commits, while GATE-P4 held at 0 of 7 exit criteria closed and
GATE-IAM-P1 at 0 of 6.** Every one of those changes was individually well-formed and correctly
change-controlled. None of them was ever weighed against the delivery it displaced.

Concretely, a `GOV` item must now:

1. carry a `SUG-####` and a triage verdict like any other input;
2. appear as a work item in the owning workstream's backlog, not only as a CR;
3. occupy its owner's one in-flight slot while it is being written — governance work pauses that
   owner's product lane, but it is not a global repository mutex; independent, dependency-safe
   owners may continue, and the displacement must be visible on the board;
4. state, in its CR `impact` block, which gate criterion or delivery outcome it defers;
5. be reported in the gate scorecard under [18 §2](./18-GOVERNANCE_METRICS.md#2-governance-metrics).

**Exempt from the queue** (hygiene, not change): correcting a broken link or typo, appending a
row to a register during normal triage, recording a decision that has already been made, and the
freshness acknowledgements in [17](./17-DRIFT_CONTROL.md). These are bookkeeping the framework
already requires; they are not framework changes.

> **The test:** if it adds a rule, a persona, a board, a document or a required artefact, it is
> `GOV` work and it queues. If it only records something the framework already decided, it is
> hygiene and it does not.

---

## 4. Item states

```text
TRIAGED ──► READY ──► IN-FLIGHT ──► IN-REVIEW ──► DONE
   │          ▲            │
   │          │            └──► BLOCKED ──┘
   ├──► PARKED ┘ (on unpark trigger, re-triage)
   ├──► ESCALATED ──► (CR approved) ──► TRIAGED
   └──► CLOSED: REJECTED | DUPLICATE | SUPERSEDED | LAPSED | WONT-DO
```

| State | Invariant |
|-------|-----------|
| `TRIAGED` | Has a `SUG-####` with stage fit, scope fit, necessity, type, priority |
| `READY` | Meets [12-DoR](./12-DEFINITION_OF_READY.md); has an approved plan for tier T2+ |
| `IN-FLIGHT` | Exactly one per agent/owner ([09 §3](./09-AI_EXECUTION_RULES.md#3-one-active-item)) |
| `BLOCKED` | Names the blocker ID, owner and follow-up date; it does not consume an implementation WIP slot |
| `IN-REVIEW` | Board verdicts pending or conditions outstanding |
| `DONE` | Meets [13-DoD](./13-DEFINITION_OF_DONE.md) with evidence |
| `PARKED` | Has `target_stage` **and** `unpark_trigger` |
| Closed states | Have a reason and, for `DUPLICATE`/`SUPERSEDED`, a link |

---

## 5. Unparking

Parked work returns through a defined sweep, never through someone remembering.

**Triggers**
1. Stage gate `PASSED` — sweep every item whose `unpark_trigger` names that transition
2. Scope change approved (CR) — sweep items whose rejection reason was scope
3. `recurrence_count` reaches 3 — the idea keeps arriving; re-evaluate
4. Aging rules AS-2 / AS-3 ([05 §7](./05-PRIORITY_MODEL.md#7-anti-starvation))
5. A dependency that caused `PARKED-DEPENDENT` becomes Done

**Sweep procedure**

```text
For each candidate parked item:
  1. Re-run pipeline steps 2–7 against the NEW current state.
     (Do not auto-admit. Six months of delivery may have solved it, obsoleted it,
      or made it a different problem.)
  2. Outcomes:
       still SF3      → re-park with a NEW target stage and a reason for the roll
       now SF0/SF1    → ADMIT: score fresh, plan, review
       now SF4 / SC3  → close as SUPERSEDED or WONT-DO with a reason
  3. Record the sweep result on the item: sweep date, previous target, new state.
```

An item re-parked **twice** is force-reviewed by the PO: either it is genuinely a later-stage
must, or it is an idea pretending to be scheduled work.

---

## 6. Traceability

Every admitted change is traceable in both directions:

```text
SUG-0042  ──►  NFR-011  ──►  EPIC-004  ──►  PLAN-011  ──►  verdicts[7]  ──►  PR #61  ──►  DONE
   ▲                                                                                        │
   └────────────────── register back-link, evidence, and closure reason ────────────────────┘
```

Required links per item:

| Link | Where |
|------|-------|
| Suggestion → work item | Suggestion register `admitted_as` |
| Work item → suggestion | Backlog entry `origin: SUG-####` |
| Work item → epic | `epic:` field |
| Work item → plan | `plan:` field |
| Plan → verdicts | Plan `reviews[]` |
| Work item → requirement / gate criterion | `serves:` |
| Work item → commits / PR | Commit message includes the ID |
| Debt → expiry + owner | Tech debt ledger |
| Decision → ADR | Decision register |

> **Rule BL-1 — Every `TODO`, `FIXME`, or `HACK` in code carries a work item ID on the same
> line.** `// TODO(TD-023): wire raw payload capture for status calls`. An unreferenced TODO
> is an untracked decision, and CI may reject it.

---

## 7. Hygiene

| Cadence | Action | Owner |
|---------|--------|-------|
| Per input | Triage record written | Agent |
| Per work item close | Registers updated, evidence attached | Owner |
| Per gate | Unpark sweep; aging review; metrics snapshot | Delivery Lead |
| Per gate | Parked backlog reconciled with the debt ledger (no item in both) | TL |
| Monthly | Duplicate merge pass on the suggestion register | Delivery Lead |
| Per stage change | Priorities recomputed for READY + PARKED items | PO + TL |

Recomputation at stage change is not optional: priority is stage-relative
([05 §1](./05-PRIORITY_MODEL.md#1-the-governing-insight)), so a stage change silently invalidates
every stored `priority_now`.
