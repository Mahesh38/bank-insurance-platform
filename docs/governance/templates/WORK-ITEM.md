# Template — Work Item (Story / Task / Bug / Spike)

Schema: [../schemas/work-item.schema.json](../schemas/work-item.schema.json)
Readiness bar: [12-DEFINITION_OF_READY.md](../12-DEFINITION_OF_READY.md)

---

```yaml
work_item:
  id: NFR-011
  origin: SUG-0043                  # traceability back to triage
  title: "Redis-backed idempotency store for multi-instance safety"
  type: NFR                         # 06 §2
  breakdown: STORY                  # EPIC | STORY | TASK | SPIKE | BUG
  epic: EPIC-005
  workstream: WS-1
  owner: "Dev A"                    # exactly one
  risk_tier: T3

  # Why
  serves: ["Phase 5 exit criterion 5.4", "TD-010"]
  problem: >
    In-memory idempotency is correct for a single instance. Horizontal scale-out
    would allow duplicate mutating calls to pass the filter.

  # What
  description: >
    Replace the in-memory idempotency store with a Redis-backed implementation
    behind the existing port, preserving current semantics.

  acceptance_criteria:
    - id: AC-1
      given: "two service instances sharing a Redis store"
      when: "the same Idempotency-Key is submitted to both"
      then: "the second returns the first response and creates no second resource"
    - id: AC-2
      given: "Redis is unavailable"
      when: "a mutating request arrives"
      then: "the request fails closed with a retryable error; no duplicate is created"
    - id: AC-3
      given: "any request"
      when: "keys are inspected in Redis"
      then: "no PII appears in key names or values"

  out_of_scope:
    - "Caching master data in Redis"
    - "Session storage"
    - "Redis clustering / HA topology"

  # State
  state: PARKED                     # TRIAGED|READY|BLOCKED|IN-FLIGHT|IN-REVIEW|DONE|PARKED|closed
  priority_now: P4
  priority_at_target: P2
  target_stage: "Phase 5.4"
  unpark_trigger: "Phase 4 gate PASSED"

  # Dependencies
  dependencies:
    blocked_by: [DEP-006]
    requires: []
    enables: ["horizontal scale-out"]
    related_to: [TD-010]
    external: []
    decision_dependency: ["ADR — scale-out topology"]

  # Delivery
  plan: null                        # PLAN-### once written
  estimate: L
  assumptions: [ASM-002]
  risks: [RISK-004]
  evidence: []                      # attached at DoD
  variance_log: []
```

---

## Bug form

A bug additionally requires the **violated specification** (Rule WC-1) and a regression test
that fails before the fix.

```yaml
work_item:
  id: BUG-006
  origin: SUG-0051
  title: "Status lookup returns 502 instead of 404 for unknown application"
  type: BUG
  violates: "FUNC-009 AC-4"                 # mandatory — no spec, no bug
  reproduction: >
    POST /LifeTerm/prostat/ returns an empty manufacturer block; the adapter maps it
    to UPSTREAM_BAD_RESPONSE, surfacing 502 rather than 404.
  root_cause: "Empty-manufacturer response not distinguished from a malformed response"
  regression_test: "OneSbStatusAdapterTest#emptyManufacturer_returnsNotFound"
  fails_before_fix: true                    # mandatory
  same_class_elsewhere: "checked OneSbPaymentAdapter and OneSbQuoteAdapter — not present"
  acceptance_criteria:
    - "empty manufacturer/product → 404 RESOURCE_NOT_FOUND"
    - "existing mappings unchanged (regression suite green)"
```

## Spike form

```yaml
work_item:
  id: SPIKE-007
  type: SPIKE
  question: "Can a Redis-backed idempotency store preserve current at-least-once semantics?"
  timebox: "2 days"
  deliverable: "ADR draft + prototype branch + measured latency delta"
  decision_it_unblocks: "ADR — scale-out topology"
  exit: "question answered either way; a NO is a successful spike"
  ships_production_code: false              # always false
```
