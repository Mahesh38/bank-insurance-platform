# 12 — Definition of Ready

**Layer:** L1 — generic
**Owner:** Tech Lead (technical readiness) · PO (business readiness)

---

## 1. Purpose

A work item is **READY** when the next person — human or agent — can start it without asking a
question. Ready is not "we want it"; ready is "nothing prevents it and nothing about it is
ambiguous".

Picking up an item that is not ready is the second-largest source of rework after skipping
stage-fit ([03](./03-LIFECYCLE.md)).

---

## 2. The checklist

An item is READY only when **all** apply:

| # | Criterion | Source |
|---|-----------|--------|
| R1 | Has an ID and a work type | [06](./06-WORK_CLASSIFICATION.md) |
| R2 | Traceable to its origin `SUG-####` and to a requirement, gate criterion, or defect | [08 §6](./08-BACKLOG_RULES.md#6-traceability) |
| R3 | Stage fit is SF0/SF1 (or SF2 that passed the absorption test) **against the current stage** | [03](./03-LIFECYCLE.md) |
| R4 | Scope fit is SC0 or SC1; SC1 names its beneficiary in `serves` | [02](./02-PROJECT_SCOPE.md) |
| R5 | Necessity is MUST, SHOULD, or COULD — never NOT-NOW | [16](./16-DECISION_MODEL.md) |
| R6 | `priority_now` computed against the **current** stage, with factors recorded | [05](./05-PRIORITY_MODEL.md) |
| R7 | Acceptance criteria written, observable, and testable | §3 |
| R8 | Dependencies mapped; state is `READY` (no unsatisfied HARD edge) | [07](./07-DEPENDENCY_MODEL.md) |
| R9 | Sized: fits one owner and one increment, or has been decomposed | [06 §5](./06-WORK_CLASSIFICATION.md#5-work-breakdown--epic--story--task--spike) |
| R10 | Implementation plan exists and is **approved** (tier T2+) | [10](./10-IMPLEMENTATION_PLAN_TEMPLATE.md), [11](./11-REVIEW_GATES.md) |
| R11 | Confidence ≥ C3; otherwise a spike precedes it | [16 §5](./16-DECISION_MODEL.md#5-confidence-levels) |
| R12 | Test approach agreed; test data available and PII-free | [11 §8](./11-REVIEW_GATES.md#8-board-5--qa) |
| R13 | Environment / access / credentials available (no open `ENVIRONMENT` or `EXTERNAL` edge) | [07 §2](./07-DEPENDENCY_MODEL.md#2-dependency-types) |
| R14 | Owner assigned — exactly one | [09 §3](./09-AI_EXECUTION_RULES.md#3-one-active-item) |
| R15 | `out_of_scope` stated, so the item has a boundary | [10 §4](./10-IMPLEMENTATION_PLAN_TEMPLATE.md#4-field-discipline) |

---

## 3. Acceptance criteria quality bar

AC are the contract. They must be:

| Property | Test |
|----------|------|
| **Observable** | Someone other than the author can tell whether it holds |
| **Binary** | True or false — never "improved", "faster", "cleaner" |
| **Behavioural** | Describes outcome, not implementation |
| **Bounded** | Covers happy path, at least one error path, and stated edge cases |
| **Independent** | Does not require another unfinished item to be verified |

Preferred form:

```text
Given <context>
When  <action>
Then  <observable outcome>
```

Worked, from this repository:

```text
AC-1  Given a proposal in status SUBMITTED,
      When POST /v1/payments is called with its jobId,
      Then a payment session is created and an HTTPS paymentUrl is returned.

AC-2  Given a proposal not in a payable state,
      When POST /v1/payments is called,
      Then 409 PROPOSAL_NOT_PAYABLE is returned and no session is created.

AC-3  Given any payment response,
      When logs and audit events are inspected,
      Then paymentUrl appears in neither.
```

AC-3 is the pattern worth copying: **a negative, verifiable compliance criterion**. Those are
the ones that get skipped when they are not written down.

---

## 4. Not-ready signals

| Signal | Real cause | Fix |
|--------|------------|-----|
| "We'll figure out the approach as we go" | Confidence < C3 | Spike |
| "It depends on how X turns out" | Open dependency or decision | Resolve or park |
| AC contain "etc." or "and so on" | Scope not bounded | Enumerate or split |
| Two people could implement it differently and both be right | Under-specified | Add AC or an ADR |
| Estimate spans more than one increment | Too big | Decompose |
| Nobody can say who owns it | Not scheduled | Assign or leave in the backlog |
| "Ready except for the tests" | Not ready | R12 |

---

## 5. Ready is stage-scoped

> **Rule DR-1 — READY expires at a stage boundary.** When the stage changes, every READY item is
> re-checked against R3 and R6. An item that was ready for hardening may be out of stage for
> expansion, and its priority is certainly stale ([08 §7](./08-BACKLOG_RULES.md#7-hygiene)).

---

## 6. Agent pre-flight

Before writing a single line of code:

```text
[ ] I can state the work item ID and its type
[ ] I can state the acceptance criteria from the item, not from memory of the conversation
[ ] I can name the approved plan and its verdicts
[ ] I have checked this item is head of the ordered READY queue (07 §5)
[ ] I know the files I expect to touch, and what is out of scope
[ ] I know what evidence will prove it Done (13)
```

Any unchecked box → stop and resolve it. Starting anyway is how a two-day story becomes a
two-week one.
