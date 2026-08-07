# Template — Triage Record

The pipeline's output for **one input**. Produced for every requirement, bug, suggestion, or
finding, before any code is written.

Schema: [../schemas/triage-record.schema.json](../schemas/triage-record.schema.json)

---

## Full form

```yaml
triage_record:

  id: SUG-0001
  raised_at: 2026-08-08
  raised_by: "agent:claude"          # agent:<name> | human:<name> | scan:<tool>
  source: "code review of PaymentSessionController"
  input: >
    Verbatim statement of what was suggested or reported. Do not paraphrase into
    something more reasonable than what was said.

  # ---- STEP 1: CONTEXT RESOLUTION (01) ----
  context:
    workstream: WS-1
    current_phase: "Phase 4 — Hardening & consumer enablement"
    canonical_stage: "L7 — Hardening"
    current_objective: "Term path signed off for UAT use by at least one bank caller"
    state_as_of: 2026-08-07
    state_provisional: true
    active_work_item: FUNC-011        # what the agent was doing when this arrived

  # ---- STEP 2: LIFECYCLE VALIDATION (03) ----
  stage_fit:
    code: SF3                          # SF0 | SF1 | SF2 | SF3 | SF4
    rationale: "Belongs to production readiness; no current gate criterion needs it"
    target_stage: "Phase 6 — Production readiness"     # required for SF3
    unpark_trigger: "Phase 5 gate PASSED"              # required for SF3
    absorption_test:                   # required for SF2 only
      small: null
      no_new_dependency: null
      no_new_decision: null
      gate_neutral: null

  # ---- STEP 3: SCOPE VALIDATION (02) ----
  scope:
    code: SC0                          # SC0 | SC1 | SC2 | SC3 | SC4
    business_scope: "in scope — go-live checklist obligation"
    serves: []                         # required for SC1: the work items that fail without it
    failure_without_it: "no verified recovery path for a regional outage"
    minimal: true
    authority: "ACTION-PLAN.md Phase 6.3/6.5"

  # ---- STEP 4: NECESSITY (16) ----
  necessity:
    now: NOT-NOW                       # MUST | SHOULD | COULD | NOT-NOW | REJECT
    future_necessity: MUST
    target_stage: "Phase 6 — Production readiness"
    binds_when: "first production deployment"
    evidence_tier: E2                  # E1 strongest … E7 weakest
    evidence:
      - "ACTION-PLAN Phase 6.3 / 6.5"
    confidence: C4                     # C1 … C5
    assumptions: []                    # ASM-### ids
    anti_over_engineering:
      X1_named_consumer: false
      X3_cheap_later: true
      X5_stage_necessity: false
      X9_problem_observed: false

  # ---- STEP 5: ACTION MATRIX (00 §6) ----
  action: PARK                         # ADMIT | ADMIT-BYPASS | PARK | REJECT | ESCALATE
  action_rationale: "Scheduled MUST at a later stage — not a rejection"
  duplicate_of: null
  conflicts: []                        # documents that disagreed, and how it was resolved

  # ---- Steps 6–9 run only when action == ADMIT ----

  # ---- STEP 6: CLASSIFICATION (06) ----
  classification:
    type: OPS                          # FUNC|BUG|NFR|ARCH|SEC|COMP|INFRA|DEBT|REFACTOR|QA|OPS|SPIKE|DOC|MIGRATION|GOV|IDEA
    also: []
    breakdown: STORY                   # EPIC | STORY | TASK | SPIKE | ADR
    epic: null
    risk_tier: T2                      # T1 | T2 | T3 | T4
    destination: "PARKED-BACKLOG.md §2"

  # ---- STEP 7: PRIORITY (05) ----
  priority:
    now: P4
    at_target: P1
    factors: { N: 0, S: 0, B: 0, R: 2, D: 1, E: 2 }
    score: 3
    matrix_default: P4
    consistency: OK                    # score band vs matrix default within one band
    overrides_applied: []              # O1…O8
    caps_applied: [PRI-2]
    rationale: "MUST at production readiness; nothing at hardening depends on it"

  # ---- STEP 8: DEPENDENCIES (07) ----
  dependencies:
    edges: []
    state: READY                       # READY | BLOCKED | PARKED-DEPENDENT
    enablement_count: 0
    earliest_start: "Phase 6 entry"
    cycles: none

  # ---- STEP 9: BREAKDOWN (06 §5) ----
  breakdown:
    children: []
    completion_definition: null
    not_included: []

  # ---- Outcome ----
  outcome:
    registered_in: "registers/PARKED-BACKLOG.md"
    work_item_id: null                 # assigned when ADMITTED
    plan_id: null
    status: PARKED
    closed_reason: null

  # ---- Return to task ----
  resumed: FUNC-011                    # what the agent went back to. Never omit.
```

---

## Short form (most inputs)

Six lines, and enough to be auditable:

```yaml
SUG-0044: "Extract a shared status mapper"
context:   WS-1 · Phase 4 · working on FUNC-011
stage/scope: SF2 (Phase 5 — needed when the second LOB exists) / SC0
necessity: SHOULD · confidence C4 · X2 fails (one implementation today)
action:    PARK → Phase 5, unparks at the Phase 4 gate
priority:  P4 now / P3 at target
resumed:   FUNC-011
```

---

## Rejection form

```yaml
SUG-0045: "Rewrite the poller reactively for throughput"
context:   WS-1 · Phase 4
stage/scope: SF4 / SC3
necessity: REJECT
evidence:  E7 (preference) — no measured throughput problem; X9 fails
action:    REJECT
reason:    >
  No throughput requirement exists and none is measured. A reactive rewrite would
  change the concurrency model of delivered, approved Term-path code during hardening.
reopen_if: "a measured p95 or throughput target fails (E3 evidence)"
resumed:   FUNC-011
```

`reopen_if` is mandatory on rejections: it converts "no" into "not unless", which is both more
honest and less likely to be re-argued.
