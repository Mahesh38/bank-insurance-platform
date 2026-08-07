# Template — Epic

An epic exists when **two or more** epic triggers hold
([06 §5](../06-WORK_CLASSIFICATION.md#epic-triggers--any-two-force-an-epic)): multiple stories ·
multiple components · multiple acceptance outcomes · multiple owners · multiple dependencies ·
delivery spanning multiple increments.

`completion_definition` and `not_included` are **mandatory**. An epic without an explicit
boundary is a scope-creep container.

---

```yaml
epic:
  id: EPIC-005
  title: "Health LOB enablement"
  workstream: WS-1
  origin: ["PRODUCT-BACKLOG E09", "SUG-0052"]
  owner: "Tech Lead"
  stage: "Phase 5 — Expand LOBs"

  outcome: >
    Bank apps can run the full quote → proposal → payment → status journey for
    lob=HEALTH using the same public API contracts as Term.

  epic_triggers_met:
    - multiple_stories
    - multiple_acceptance_outcomes
    - multiple_dependencies

  completion_definition: >
    Health sandbox path green end to end AND Term regression suite still green AND
    no change to QuoteService orchestration AND Health field-guide mappings verified
    against the sandbox.

  not_included:                       # the anti-creep boundary — be specific
    - "Motor LOB (EPIC-006)"
    - "Health-specific pricing or underwriting rules"
    - "Redis idempotency (NFR-011)"
    - "Health-specific UI or BFF work"

  stories:
    - id: FUNC-012
      title: "Health quote handler"
      state: PARKED
      priority_at_target: P2
    - id: FUNC-016
      title: "Health proposal schema handling"
      state: PARKED
      priority_at_target: P2
    - id: FUNC-017
      title: "Health payment + status mapping"
      state: PARKED
      priority_at_target: P3
    - id: QA-011
      title: "Health sandbox regression suite"
      state: PARKED
      priority_at_target: P2

  story_order: [FUNC-012, FUNC-016, FUNC-017, QA-011]   # from 07 §5, not preference

  dependencies:
    blocked_by: ["Phase 4 gate"]
    requires: [TECH-006, TECH-007]     # job + poller infra, already Done
    enables: []
    decision_dependency: []

  risks: [RISK-004]
  assumptions: [ASM-007]

  progress:
    stories_total: 4
    stories_done: 0
    completion_definition_met: false
```

---

## Tracking rules

| Rule | Detail |
|------|--------|
| An epic is Done only when **all child stories are Done and `completion_definition` is met** | Story completion alone is not epic completion — the definition catches integration gaps |
| A story may not be added to an epic without checking `not_included` | If it is listed there, it needs a change request, not an edit |
| An epic that grows past ~8 stories should be split | Beyond that, the completion definition stops being verifiable |
| Epic priority is derived, not set | It is the highest `priority_now` among its ready children |
| An epic whose children are all parked is itself parked | With the earliest child trigger |
