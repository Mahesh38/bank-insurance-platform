# 05 — Priority Model (P1–P5)

**Layer:** L1 — generic
**Pipeline step:** 6 — Priority Scoring
**Owner:** Product Owner (business weight) · Tech Lead (technical weight)

---

## 1. The governing insight

> **Priority is not a property of an item. It is a property of an item *at a stage*.**

Disaster-recovery testing is P4 during MVP and P1 during production readiness. Nothing about
the work changed; the stage did. Every priority in this model therefore comes in a pair:

| Field | Meaning |
|-------|---------|
| `priority_now` | What it is worth **today**, in the current stage. Drives the queue. |
| `priority_at_target` | What it will be worth at its `target_stage`. Drives unpark urgency. |

A parked item with `priority_now: P4, priority_at_target: P1` is an item nobody may forget.
A parked item at `P5 / P5` is an idea.

---

## 2. The five levels

| Level | Name | Meaning | Scheduling |
|-------|------|---------|------------|
| **P1** | Critical — now | Stops the line. Correctness, safety, compliance, or the gate is blocked. | **Preempts** current work. Started immediately. |
| **P2** | High | Must be delivered inside the current stage. | Next in queue after P1s. |
| **P3** | Medium | Belongs to this stage but is not gate-blocking; or is a prerequisite for the next stage. | Scheduled within the stage; may slip one stage with a recorded reason. |
| **P4** | Low / deferred | Real work, wrong time. Parked with a target stage and an unpark trigger. | Re-triaged at the target gate. |
| **P5** | Idea / watch | No committed stage. Kept so it is not re-proposed from scratch. | Reviewed at gate sweeps only. |

**P1 and P2 are current-stage-only.** Assigning P1 to something SF3 is a classification error —
if it is genuinely urgent it is not premature, so re-examine the stage fit.

---

## 3. Hard P1 overrides

These bypass the matrix and the score entirely. If any is true, the item is **P1** and may
interrupt in-flight work ([09 §4](./09-AI_EXECUTION_RULES.md#4-the-interrupt-rule)):

| # | Override | Test |
|---|----------|------|
| O1 | **Build / pipeline failure** | Default branch or the active branch does not build, or the gate job is red |
| O2 | **Security vulnerability** | Reachable and exploitable in code we ship — not a theoretical CVE in an unused path |
| O3 | **Incorrect domain model** | Implemented behaviour contradicts an approved state model, invariant, or business rule |
| O4 | **Missing mandatory API** | A contract the *current deliverable* promises is absent or non-conformant |
| O5 | **Regulatory violation** | Breaches a stated regulatory, consent, retention, audit, or attribution requirement |
| O6 | **Data corruption / loss** | Any path that can corrupt, lose, or leak persisted or in-flight data |
| O7 | **Blocking dependency** | Directly blocks the work item currently in flight |
| O8 | **Acceptance criteria failure** | An item marked Done does not satisfy its AC |

**Discipline on overrides.** An override claim must cite the specific evidence — the failing
job, the CVE and the reachable call path, the approved model it contradicts, the AC ID. An
unevidenced override claim is downgraded to normal scoring and logged in
[18](./18-GOVERNANCE_METRICS.md) as a false-P1. Override inflation is the fastest way to destroy
this model: if everything is P1, the queue is unordered again.

**O2 nuance.** A vulnerability that is real but unreachable at the current stage (e.g. a
production-only component not yet deployed) is P1 *for its stage* — record it, do not preempt
domain work for it. State the reachability finding explicitly.

---

## 4. The scoring model

Used when no hard override applies. Six factors; each is an integer.

| Factor | Symbol | Scale |
|--------|--------|-------|
| **Necessity** | N | MUST = 4 · SHOULD = 2 · COULD = 1 · NOT-NOW = 0 |
| **Stage fit** | S | SF0 = 4 · SF1 = 3 · SF2 = 1 · SF3 = 0 |
| **Blocking factor** | B | 0 blocks nothing · 1 blocks one item · 2 blocks 2–3 · 3 blocks ≥ 4 items **or** a gate criterion |
| **Risk if deferred** | R | 0 negligible · 1 contained · 2 material · 3 severe or irreversible |
| **Decay** (does delay make it costlier?) | D | 0 none · 1 moderate rework later · 2 high — migration, data backfill, or breaking a published contract |
| **Effort** | E | 0 XS/S (≤ 1 day) · 1 M (≤ 3 days) · 2 L (≤ 1 week) · 3 XL (> 1 week or multi-component) |

```text
SCORE = 2N + 2S + 2B + 2R + D − E          range: −3 … 30
```

| Score | Band |
|-------|------|
| ≥ 24 | P1 |
| 17–23 | P2 |
| 11–16 | P3 |
| 5–10 | P4 |
| ≤ 4 | P5 |

Effort is subtracted, not divided: this is a cost-of-delay model with a cost penalty, so a
cheap item never outranks a critical one, but two equally necessary items are ordered by cost.

### Factor coupling (Rule PRI-8)

Stage fit is not independent of blocking factor: the SF codes are *defined* in terms of what
an item blocks ([03 §3](./03-LIFECYCLE.md#3-stage-fit-codes-sf)). Leaving `B` at 0 for an SF0
item contradicts the classification that produced the SF0 in the first place.

| If stage fit is… | Then B is at least… | Because |
|---|---|---|
| **SF0** prerequisite | **2** | By definition it blocks a gate criterion or the current deliverable. `B = 3` if it blocks the gate itself |
| **SF1** on-stage **and** necessity MUST | **1** | A MUST on the current stage's deliverable blocks that deliverable |
| SF1 otherwise, SF2, SF3 | 0 | No implied blocking |

Set `B` from the dependency analysis when it is higher. The floor exists so the two routes to a
priority cannot silently disagree — before this rule, `SF0 + MUST` with `B = 0` scored **P3**
against a matrix default of **P1**, and the consistency check below fired on perfectly
classified work.

### Consistency check (Rule PRI-4)

The action matrix in [00 §6](./00-GOVERNANCE.md#6-the-action-matrix) implies a default band.
Where the matrix gives a range (`P1–P2`), the **lower-urgency end** is the default for this
comparison. The score may adjust that default by **at most one band**.

> If the score lands more than one band away from the matrix default, **the classification is
> probably wrong, not the score.** Re-check necessity, stage fit, and the PRI-8 floors before
> accepting the number.

This is the model's self-check: two independent routes to the same answer, and a required
reconciliation when they disagree. With PRI-8 applied, every cell of the matrix agrees with the
formula to within one band — verified in [18 §5](./18-GOVERNANCE_METRICS.md#5-minimum-viable-measurement)'s
calibration and re-checked by CI whenever the weights change.

### Ordering inside a band

The raw score is the sort key within a priority band. Further ties break in this order:

1. Higher **enablement count** (unblocks more items) — see [07 §5](./07-DEPENDENCY_MODEL.md#5-execution-ordering)
2. Lower **effort**
3. Higher **confidence** ([16 §5](./16-DECISION_MODEL.md#5-confidence-levels))
4. Older `raised_at` (anti-starvation)

---

## 5. Caps — stage fit dominates

Applied **after** scoring. These make it structurally impossible for a premature item to jump
the queue:

| Rule | Condition | Cap |
|------|-----------|-----|
| **PRI-1** | Every item carries both `priority_now` and `priority_at_target` | — |
| **PRI-2** | `stage_fit = SF3` (premature) | `priority_now` ≤ **P4** if `future_necessity = MUST`, else ≤ **P5** |
| **PRI-3** | `stage_fit = SF2` (adjacent) | `priority_now` ≤ **P3** |
| **PRI-4** | Score vs matrix default differ by > 1 band | Re-classify before proceeding |
| **PRI-5** | `scope_fit = SC2` (adjacent value) | `priority_now` = **P5**, bucket = Ideas |
| **PRI-6** | `confidence < C3` | Cannot be P1/P2 as *implementation*; convert to a **SPIKE**, which itself may be P1/P2 |
| **PRI-7** | A hard override (§3) | Ignores every cap above |

PRI-2 is exactly the blueprint's disaster-recovery case: MUST in the abstract, `NOT-NOW` in
context, `P4` today, `P1` at production readiness.

---

## 6. Worked examples

### Blocking dependency for in-flight work

```yaml
input: "JobStorePort lacks a payability check; FUNC-007 cannot satisfy AC-3"
necessity: MUST (N=4)      stage_fit: SF0 (S=4)
blocking: in-flight item (B=1)   risk: material (R=2)
decay: 0    effort: M (E=1)
score: 8+8+2+4+0-1 = 21    →  band P2
override O7 (blocks in-flight item) → **P1**
```

### Redis idempotency adapter during hardening

```yaml
input: "Replace in-memory idempotency with Redis"
necessity: SHOULD (N=2)    stage_fit: SF2 — belongs to Phase 5.4 (S=1)
blocking: 0 (B=0)          risk: contained, single instance today (R=1)
decay: 1 (migration later) effort: L (E=2)
score: 4+2+0+2+1-2 = 7     →  band P4
cap PRI-3 (SF2 ≤ P3) not binding →  **P4**, park to Phase 5
target: priority_at_target = P2   (S=3, B=2 before scale-out)
```

### Kafka for quote events during hardening

```yaml
necessity: COULD (N=1)     stage_fit: SF3 (S=0)
blocking: 0                risk: 0
decay: 1                   effort: XL (E=3)
score: 2+0+0+0+1-3 = 0     →  band P5
cap PRI-2, future_necessity = SHOULD → **P5**, park to Integration Architecture
```

### Compliance review of audit schema (current gate item 4.4)

```yaml
necessity: MUST (N=4)      stage_fit: SF0 — gate criterion (S=4)
blocking: gate (B=3)       risk: regulatory (R=3)
decay: 1                   effort: M (E=1)
score: 8+8+6+6+1-1 = 28    →  **P1**
```

### PII appearing in a log line

```yaml
override O5 (regulatory) + O6 (data exposure) → **P1**, interrupt permitted
```

---

## 7. Anti-starvation

Parking is only honest if parked items actually come back.

| Rule | Effect |
|------|--------|
| **AS-1** | Every P4 item is re-triaged at its `unpark_trigger`, and unconditionally at every gate sweep |
| **AS-2** | A P4 item that has survived **two** gate transitions without being pulled forward is force-reviewed: promote, re-target, or close as `SUPERSEDED`/`WONT-DO` with a reason |
| **AS-3** | A P5 idea untouched for **three** gate transitions is closed as `LAPSED` — still readable, no longer in the queue |
| **AS-4** | Aging is reported in [18](./18-GOVERNANCE_METRICS.md); a growing P4 backlog is a scope signal, not a hygiene problem |
| **AS-5** | `recurrence_count` — an item independently re-proposed ≥ 3 times gains +1 to R on its next scoring. Repeated rediscovery is evidence |

---

## 8. Priority in the triage record

```yaml
priority:
  now: P4
  at_target: P1
  factors: { N: 4, S: 0, B: 0, R: 2, D: 1, E: 2 }
  score: 7
  matrix_default: P4
  consistency: OK              # score band vs matrix default within 1
  overrides_applied: []
  caps_applied: [PRI-2]
  rationale: >
    MUST at production readiness; nothing at hardening stage depends on it;
    deferring costs only the eventual execution, not rework.
```
