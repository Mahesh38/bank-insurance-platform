# 10 — Implementation Plan

**Layer:** L1 — generic
**Pipeline step:** 9 — Implementation Plan
**Owner:** Author of the change (agent or developer)

---

## 1. Purpose

The plan is the artefact the review boards judge. It exists so that seven reviewers can reach
an informed verdict **without reading the diff** — because the diff does not exist yet.

A plan is also a **contract with yourself**: `files_expected` and `out_of_scope` are what drift
detection ([17](./17-DRIFT_CONTROL.md)) measures the eventual change against.

> **Rule IP-1 — No plan, no code**, above risk tier T1. A plan written after the code is not a
> plan, it is a summary; label it honestly and expect a Technical rework verdict.

---

## 2. When a plan is required

| Risk tier | Examples | Plan required |
|-----------|----------|---------------|
| **T1 Trivial** | Docs, comments, test-only additions, config typo | Not required — a triage record suffices |
| **T2 Standard** | A story inside existing architecture; a bug fix with a known cause | **Yes** — short form (§3) |
| **T3 Significant** | New component, new public API, new data structure, new dependency, security surface | **Yes** — full form |
| **T4 Critical** | Regulatory, PII, money movement, authn/authz, migration, production topology | **Yes** — full form + human sign-off |

Tier is assigned in the triage record and drives the mandatory review boards
([11 §3](./11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory)).

---

## 3. Required fields

Copy [templates/IMPLEMENTATION-PLAN.md](./templates/IMPLEMENTATION-PLAN.md). Validated by
[schemas/implementation-plan.schema.json](./schemas/implementation-plan.schema.json).

```yaml
implementation_plan:

  id: PLAN-011
  work_item: NFR-011
  origin: SUG-0043
  risk_tier: T3
  author: "agent:claude"

  objective: >
    One sentence. What is true after this change that is not true now.

  problem: >
    The current state and why it is insufficient. Cite evidence: a failing test,
    a gate criterion, an AC, a regulation, a measurement. Not "it would be better if".

  proposed_solution: >
    The approach, and — where a choice exists — why this one. Note the alternatives
    considered in `alternatives`.

  alternatives:
    - option: "..."
      rejected_because: "..."

  affected_components:
    - journey-domain
    - journey-application

  files_expected:                # the drift contract — be specific
    - JourneyAggregate.java
    - JourneyState.java
    - JourneyCommandHandler.java

  data_changes: none             # none | schema | migration | backfill (+ detail)
  api_changes: none              # none | additive | breaking (+ contract detail)
  security_impact: none          # none | authn | authz | secrets | crypto | PII | attack-surface
  compliance_impact: none        # none | consent | retention | audit | attribution | reporting
  backward_compatibility: compatible   # compatible | deprecating | breaking (+ migration path)
  performance_impact: none       # none | measured | expected (+ numbers)
  operational_impact: none       # config, env vars, runbook updates, alerts

  testing:
    unit:
      - aggregate transition tests
    integration:
      - mutation command tests
    other:
      - ArchUnit boundary rules still green

  rollback: >
    How this is undone. If it cannot be cleanly undone, say so — that is a
    material input to the Architecture and Operations verdicts.

  dependencies: [DOMAIN-012]
  assumptions: [ASM-004]         # unvalidated beliefs this plan rests on
  risks:
    - risk: "invalid state transitions accepted"
      mitigation: "exhaustive transition table test"

  acceptance_criteria:
    - all valid transitions supported
    - invalid transitions rejected
    - tests cover lifecycle

  out_of_scope:                  # equally binding — the anti-creep contract
    - persistence
    - Kafka
    - REST endpoints

  estimate: M                    # XS | S | M | L | XL
  reviews: []                    # filled by 11-REVIEW_GATES
```

---

## 4. Field discipline

| Field | Common failure | What good looks like |
|-------|----------------|----------------------|
| `objective` | Restating the title | A verifiable end state |
| `problem` | "Current design is not ideal" | "`FUNC-007` AC-3 fails on retry: a second call creates a second payment session" |
| `proposed_solution` | Describing the *what* only | The mechanism, and why not the alternatives |
| `files_expected` | "various files" | Named files. Unknown files ⇒ you are not ready to plan; spike first |
| `out_of_scope` | Left empty | The three most tempting adjacent changes — this is the field reviewers check hardest |
| `rollback` | "revert the commit" | Whether revert is *sufficient* — data written, contracts published, consumers migrated |
| `assumptions` | Silent | Each one an `ASM-###`, each with a validation method |
| `acceptance_criteria` | Restating the tests | Observable outcomes; tests are how they are proven |

> **Rule IP-2 — `out_of_scope` is mandatory and non-empty** for T2+. If genuinely nothing
> adjacent was tempting, the change is probably T1.

---

## 5. Short form (T2)

```yaml
plan: PLAN-014 · work_item: BUG-006 · tier: T2
objective:  "GET /v1/status/{applicationNumber} returns 404 rather than 500 when 1SB has no product data"
problem:    "AC-4 fails: upstream empty-manufacturer response maps to UPSTREAM_BAD_RESPONSE (502)"
solution:   "Map empty manufacturer/product to RESOURCE_NOT_FOUND in OneSbStatusAdapter"
files:      [OneSbStatusAdapter.java, OneSbStatusAdapterTest.java]
tests:      [unit: empty-manufacturer → 404; regression: populated response unchanged]
rollback:   "revert — no data or contract change"
out_of_scope: [status snapshot persistence, retry policy, other adapters]
ac:         ["empty manufacturer → 404 RESOURCE_NOT_FOUND", "existing mappings unchanged"]
```

---

## 6. The plan is a live document

The plan is updated when reality diverges — it is not fiction preserved for audit.

| Event | Action |
|-------|--------|
| Board returns conditions | Fold them into `acceptance_criteria`; note the source verdict |
| Implementation reveals a needed file outside `files_expected` | Add it **with a variance note**; if it changes a component, re-review ([14 §4](./14-CHANGE_CONTROL.md#4-changing-an-approved-plan)) |
| An assumption proves false | Update `assumptions`, re-check `risks`, and re-check whether the objective still holds |
| Scope must grow | Stop. That is a new work item or a change request — not an edit to this plan |

Every post-approval edit appends to a `variance_log`:

```yaml
variance_log:
  - date: 2026-08-12
    change: "added PaymentSessionMapper.java to files_expected"
    reason: "mapping logic could not live in the adapter without breaking ArchUnit rule"
    re_review: "Technical only — no boundary change"
```

The variance log is the honest record of the gap between plan and reality, and it is the
primary input to [18](./18-GOVERNANCE_METRICS.md)'s plan-accuracy metric.
