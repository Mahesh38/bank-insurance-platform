# 03 — Lifecycle Model & Stage Fit

**Layer:** L1 (model) + L3 (stage list per workstream)
**Pipeline step:** 2 — Lifecycle Validation
**Owner:** Architect

---

## 1. The idea in one line

> Almost every bad AI suggestion is a **good suggestion delivered at the wrong stage**.

Kafka is not wrong. Autoscaling is not wrong. Disaster-recovery testing is not wrong. They are
wrong *during domain modelling*, because they consume the one resource the current stage needs
— attention — and they lock in decisions before the information needed to make them exists.

Stage fit is therefore evaluated **before** merit, and it can override merit
([00 §6](./00-GOVERNANCE.md#6-the-action-matrix)).

---

## 2. Canonical lifecycle (L1)

Any software platform passes through these stages. Project phase names map onto them (§6).

| # | Stage | Central question | Typical work | Typically premature here |
|---|-------|------------------|--------------|--------------------------|
| L0 | **Discovery** | What problem, for whom? | Capability map, requirement catalogue, stakeholder map | Anything with a class file |
| L1 | **Business Design** | What behaviour do we promise? | Journeys, business rules, acceptance definitions | Technology selection |
| L2 | **Domain / Aggregate Design** | What are the concepts and their lifecycles? | Aggregates, state models, invariants, ubiquitous language | Messaging, persistence tuning, caching, observability stacks |
| L3 | **Technical / Solution Design** | How will it be built? | Component boundaries, contracts, ADRs, NFR targets | Production capacity planning |
| L4 | **Foundation** | Can we build safely? | Scaffolds, shared libs, CI, secrets, migrations, arch tests | Feature breadth |
| L5 | **Connectivity / Integration** | Can we talk to the outside? | Clients, auth, error normalisation, async infra | Multi-LOB expansion |
| L6 | **Vertical Slice (MVP)** | Does one path work end to end? | One journey, fully working, thin | Second journey, generalisation, frameworks |
| L7 | **Hardening** | Is it correct, safe, and provable? | E2E evidence, compliance review, perf smoke, runbooks | New features |
| L8 | **Expansion** | Does it generalise? | Additional LOBs/journeys reusing the same orchestration | Rearchitecting the thing that works |
| L9 | **Production Readiness** | Can we run it? | Dashboards, alerts, DR, retention, go-live checklist, autoscaling | Broad new scope |
| L10 | **Operate & Evolve** | Is it healthy and improving? | SLOs, incident learning, debt repayment, evolution | Unbounded rewrites |

**Direction rule:** work belonging to stage *n* may be pulled forward into stage *n−1* only when
it is a prerequisite (SF0) or absorbable-adjacent (SF2). Work from *n+2* onward is always
premature.

---

## 3. Stage-fit codes (SF)

| Code | Name | Definition | Action |
|------|------|------------|--------|
| **SF0** | PREREQUISITE | The current stage **cannot exit** without it; it blocks a gate criterion | ADMIT — may preempt current work |
| **SF1** | ON-STAGE | Directly serves the current stage's deliverable | ADMIT |
| **SF2** | ADJACENT | Belongs to the next stage, but is absorbable now (see the absorption test) | ADMIT if absorbable, else PARK |
| **SF3** | PREMATURE | Belongs to a later stage; the information or the need does not exist yet | **PARK** with a target stage |
| **SF4** | STAGE-INVALID | No stage on the roadmap will need it, or it contradicts a standing constraint | **REJECT** with reason |

### The SF2 absorption test

SF2 is the only discretionary code. Admit only if **all four** hold:

1. **Small** — one story or less; fits inside the current work item's plan without extending it.
2. **No new dependency** — no new library, service, infrastructure component, or contract.
3. **No new decision** — it does not require an ADR or a choice we lack information to make.
4. **Gate-neutral** — it cannot delay or endanger the current stage's exit criteria.

Fail any one → PARK. When in doubt, PARK: parking costs a register line, wrongly absorbing costs
a stage.

### SF3 carries three mandatory fields

A park is worthless if nobody knows when to unpark. Every SF3 record must state:

```yaml
stage_fit:
  code: SF3
  target_stage: L5-Integration          # where it becomes on-stage
  unpark_trigger: "entry to Phase 5 (Expand LOBs)"   # the observable event
  future_necessity: MUST                # what it will be worth then
```

Without `target_stage` + `unpark_trigger`, the item is not parked — it is lost. Enforced by
[schemas/triage-record.schema.json](./schemas/triage-record.schema.json).

### SF4 requires an argument, not a shrug

REJECT is permanent and therefore needs the strongest justification of any verdict. Valid SF4
grounds:

- contradicts a standing constraint ([01 §5](./01-CURRENT_STATE.md#5-standing-constraints-apply-to-every-triage-in-this-repo));
- solves a problem the roadmap has decided not to have;
- superseded by an existing decision (cite the ADR);
- duplicates an existing item (link it — this is `DUPLICATE`, a softer close).

"I don't think we need it" is not SF4. If no stage claims it but value is plausible, it is
SC2 → Ideas, not SF4.

---

## 4. Worked examples

### Kafka during domain design

```yaml
input:          "Publish aggregate state changes to Kafka"
workstream:     WS-1
current_stage:  L2 Domain/Aggregate Design
relevance:      "Integration architecture concern"
stage_fit:      SF3            # premature
target_stage:   L5 Connectivity / Integration
unpark_trigger: "event-backbone ADR accepted"
future_necessity: SHOULD
action:         PARK
```

> **The unpark trigger on this example has partly fired, and the example is still correct.**
> `ADR-012` was accepted for **WS-3** on 2026-08-24, so a broker exists in the R0 platform estate.
> This row is `workstream: WS-1` at `L2`, and both facts still hold there: the adapter is at
> Phase 4, publishes to no topic, and would still be premature. An unpark trigger fires for the
> workstream whose stage it names — which is the same rule (`LC-1`) the scope example in
> [`02 §2`](./02-PROJECT_SCOPE.md#2-scope-is-not-one-thing) now illustrates.

### Production autoscaling during domain modelling

```yaml
input:            "Configure production autoscaling"
current_stage:    L2 Domain modelling
stage_fit:        SF3
necessity_now:    NOT-NOW
future_necessity: MUST
target_stage:     L9 Production Readiness
priority_now:     P4          # capped by rule PRI-2
priority_at_target: P1
action:           PARK
```

### Disaster-recovery testing during MVP

```yaml
input:            "Disaster recovery testing"
current_lifecycle: L6 MVP / vertical slice
required_stage:    L9 Production Readiness
necessity_now:     NOT-NOW
future_necessity:  MUST
priority_now:      P4
action:            PARK
```

### The same item, at production readiness

```yaml
input:            "Disaster recovery testing"
current_lifecycle: L9 Production Readiness
stage_fit:         SF1
necessity:         MUST
priority_now:      P1
action:            ADMIT
```

**Same suggestion. Same merit. Opposite verdict.** That is the model working, not failing.

### Duplicate payment prevention during vertical slice

```yaml
input:        "Prevent duplicate payment processing"
current_stage: L6 Vertical slice (payment story in flight)
scope_fit:     SC1            # FUNC-007 is incorrect without it
stage_fit:     SF0            # blocks the story's acceptance criteria
necessity:     MUST
priority_now:  P1
action:        ADMIT
```

---

## 5. Multi-workstream evaluation

This repository runs two lifecycles concurrently ([01 §4](./01-CURRENT_STATE.md#4-workstreams)).

> **Rule LC-1 — Evaluate stage fit against the input's own workstream.** Never against the
> repository's "most advanced" stage, and never against an average.

If an input spans both workstreams (e.g. "propagate the workforce actor ID into 1SB audit
events"), split it: one work item per workstream, each triaged in its own context, linked by
`related_to`. A single item that is SF1 in one workstream and SF3 in another is unschedulable.

If an input maps to **no** workstream, its scope fit is SC2 at best.

---

## 6. Project stage map (L3)

### WS-1 · 1SB Insurance Integration

| Project phase ([ACTION-PLAN.md](../1sb-insurance-integration/service-ssot/ACTION-PLAN.md)) | Canonical stage | Status |
|---|---|---|
| Phase 0 — Access & alignment | L0/L1 | Done |
| Phase 1 — Foundations (shared libs + scaffold) | L4 | Done |
| Phase 2 — Connectivity + async infra | L5 | Done |
| Phase 3 — Term vertical slice | L6 | Done |
| **Phase 4 — Hardening & consumer enablement** | **L7** | **← current** |
| Phase 5 — Expand LOBs (Health → Motor) | L8 | Next |
| Phase 6 — Production readiness | L9 | Future |
| Later (P2 LOBs, routing flag, version freeze) | L10 | Future |

### WS-2 · Workforce Auth & Authorization

| Phase | Canonical stage | Status |
|---|---|---|
| Architecture baseline (SSOT accepted decisions) | L3 | Done |
| **Phase 1 — Foundation implementation** | **L4/L6** | **← current** |
| Phase 2 — Bank AD federation + production IdP decision | L5/L3 | Next |
| Phase 3 — Retail customer identity | L1→ | Future |

---

## 7. Stage-fit quick reference for common suggestion families

Use as a prior, not a substitute for the analysis. Columns are WS-1's current stage (L7).

| Suggestion family | Earliest legitimate stage | Verdict at L7 Hardening |
|---|---|---|
| Domain invariant / state-model correction | L2 | SF0 if it breaks an AC, else SF1 |
| New public API contract | L3 | SF2/SF3 — needs Architecture verdict |
| Shared library extraction | L4 | SF1 if ≥ 2 consumers exist today, else SF3 |
| Retry / circuit breaker | L5 | SF1 if an upstream failure is observed; SF3 if speculative |
| Caching layer | L5–L7 | SF3 unless a measured latency gate fails |
| Message broker / event backbone | L5 | SF3 — out of current topology. **Workstream-dependent:** SF1 for WS-3 since `CR-012` (`ADR-012`) put one in the R0 estate |
| Test coverage for delivered code | L6 | SF0/SF1 — always welcome at hardening |
| E2E / sandbox suite | L7 | **SF1 — this is the current stage's work** |
| Compliance evidence, log-sample review | L7 | SF1 |
| Runbook, incident playbook | L7 | SF1 |
| Performance smoke test | L7 | SF1 |
| Second LOB / journey | L8 | SF2 — only after the gate |
| Dashboards, alerting, SLOs | L9 | SF3 |
| Autoscaling, DR drills, retention jobs | L9 | SF3 |
| Multi-region, cost optimisation | L9/L10 | SF3 |
| Reactive/framework rewrite | — | SF4 unless evidence-backed |
