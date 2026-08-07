# Template — Implementation Plan

Guidance: [10-IMPLEMENTATION_PLAN_TEMPLATE.md](../10-IMPLEMENTATION_PLAN_TEMPLATE.md)
Schema: [../schemas/implementation-plan.schema.json](../schemas/implementation-plan.schema.json)

Required for risk tier **T2 and above**. Written **before** code — this document is what the
review boards judge, and `files_expected` / `out_of_scope` are the contract that drift control
measures against.

---

```yaml
implementation_plan:

  id: PLAN-011
  work_item: NFR-011
  origin: SUG-0043
  workstream: WS-1
  risk_tier: T3                       # T1 | T2 | T3 | T4
  author: "agent:claude"
  date: 2026-08-08

  objective: >
    Mutating API calls are idempotent across multiple service instances.

  problem: >
    The idempotency filter stores keys in process memory. With more than one instance,
    a bank retry routed to a second instance creates a duplicate resource. Evidence:
    TD-010; ACTION-PLAN 5.4 requires this before scale-out.

  proposed_solution: >
    Implement the existing IdempotencyStorePort with a Redis-backed adapter. Keys carry
    a TTL matching current in-memory semantics. Fail closed on Redis unavailability:
    return a retryable error rather than allowing an unchecked mutation.

  alternatives:
    - option: "Persist idempotency records via bank-persistence-service"
      rejected_because: "Adds a synchronous hop to every mutating call; the persistence service does not own request-scoped state"
    - option: "Sticky sessions at the load balancer"
      rejected_because: "Correctness by deployment configuration; breaks on rebalance"

  affected_components:
    - 1sb-integration-service (adapter.idempotency)

  files_expected:
    - RedisIdempotencyStoreAdapter.java
    - IdempotencyConfig.java
    - application-uat.yml
    - RedisIdempotencyStoreAdapterTest.java
    - IdempotencyFilterIT.java

  data_changes: none
  api_changes: none
  security_impact: none               # keys must not contain PII — see AC-3
  compliance_impact: none
  backward_compatibility: compatible
  performance_impact: "expected +2–5ms per mutating call; to be measured"
  operational_impact: "new Redis dependency: connection config, health indicator, alert"

  testing:
    unit:
      - "key collision returns the stored response"
      - "TTL expiry allows a new request"
      - "Redis unavailable → fails closed with a retryable error"
    integration:
      - "two application contexts sharing one Redis: second call returns the first response"
    other:
      - "ArchUnit: adapter does not leak Redis types into application or domain"

  rollback: >
    Revert the commit and re-enable the in-memory implementation via configuration.
    No data migration; keys are ephemeral. Rollback is sufficient.

  dependencies: [DEP-006]
  assumptions: [ASM-002]
  risks:
    - risk: "Redis outage blocks all mutating traffic"
      mitigation: "fail-closed is deliberate; alert on Redis health; documented in the runbook"

  acceptance_criteria:
    - "AC-1 multi-instance duplicate suppression"
    - "AC-2 fails closed when Redis is unavailable"
    - "AC-3 no PII in Redis keys or values"

  out_of_scope:
    - "Caching master data in Redis"
    - "Session storage"
    - "Redis clustering / HA topology"
    - "Migrating existing in-memory keys"

  estimate: L

  reviews: []                         # populated by the board (11)
  variance_log: []                    # populated during implementation (10 §6)
```

---

## Short form (T2)

```yaml
plan: PLAN-014 · work_item: BUG-006 · tier: T2 · author: agent:claude
objective:  "Unknown application number returns 404, not 502"
problem:    "FUNC-009 AC-4 fails: empty manufacturer maps to UPSTREAM_BAD_RESPONSE"
solution:   "Distinguish empty-manufacturer from malformed in OneSbStatusAdapter"
files:      [OneSbStatusAdapter.java, OneSbStatusAdapterTest.java]
tests:      ["empty manufacturer → 404", "regression: populated response unchanged"]
rollback:   "revert — no data or contract change"
out_of_scope: ["status snapshot persistence", "retry policy", "other adapters"]
ac:         ["empty manufacturer → 404 RESOURCE_NOT_FOUND", "existing mappings unchanged"]
```
