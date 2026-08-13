# Dependency Register

Edges of the dependency graph. Edges outlive the items that discovered them, so they are
recorded once and reused by every later ordering computation.

**Owner:** Tech Lead · Architect (architectural and decision edges)
**Model:** [07-DEPENDENCY_MODEL.md](../07-DEPENDENCY_MODEL.md)

---

## 1. Edges

| ID | From | Relation | To | Type | State | Notes |
|----|------|----------|----|------|-------|-------|
| DEP-001 | Gate 4.3 (bank consumer UAT) | `blocked_by` | Gate 4.2 (OpenAPI published) | HARD | OPEN | A consumer cannot integrate against an unpublished contract |
| DEP-002 | Gate 4.3 | `external` | Bank app team UAT slot | EXTERNAL | OPEN | ⚠️ Needs a named owner and a follow-up date (Rule DEP-3) |
| DEP-003 | Gate 4.6 (performance smoke) | `blocked_by` | Gate 4.1 (sandbox E2E in CI) | SOFT | OPEN | Smoke reuses the E2E harness; could be built standalone at higher cost |
| DEP-004 | TD-014 (integration ↔ persistence E2E) | `enables` | Gate 4.1 | TECHNICAL | OPEN | Parked item whose trigger has fired |
| DEP-005 | Phase 5 (Expand LOBs) | `blocked_by` | Phase 4 gate | HARD | OPEN | "Do not start Health/Motor until Phase 3/4 exit is met" |
| DEP-006 | TD-010 (Redis idempotency) | `blocked_by` | Horizontal scale-out decision | DECISION | OPEN | Needs an ADR before implementation, not just capacity |
| DEP-007 | TD-006 (AWS Secrets Manager) | `blocked_by` | AWS deployment target confirmed | ENVIRONMENT | OPEN | Prod profile fails fast until then |
| DEP-008 | Gate 4.4 (compliance review) | `enables` | TD-023 scope (raw payload capture breadth) | COMPLIANCE | OPEN | The review decides how far capture must extend |
| DEP-009 | WS-2 Phase 2 (production IdP) | `blocked_by` | WS-2 Phase 1 gate | HARD | OPEN | Deliberate deferral behind the adapter |
| DEP-010 | WS-2 Phase 2 (AD federation) | `external` | Bank confirms AD technology | EXTERNAL | OPEN | ⚠️ Needs a named owner and a follow-up date |
| DEP-011 | TD-007 (tighten ArchUnit) | `requires` | Packages populated by LOB expansion | TECHNICAL | OPEN | Cannot tighten rules against empty packages |

## 2. External dependencies

Every `EXTERNAL` edge needs an owner and a follow-up date, or it is not tracked — it is hoped
for (Rule DEP-3).

| ID | Dependency | Owner | Follow-up | State | Impact if late |
|----|------------|-------|-----------|-------|----------------|
| → [DEP-002](#1-edges) | Bank app team UAT integration slot | ⚠️ **unassigned** | ⚠️ **unset** | OPEN | Phase 4 gate criterion 4.3 cannot close |
| → [DEP-010](#1-edges) | Bank AD technology confirmation | ⚠️ **unassigned** | ⚠️ **unset** | OPEN | WS-2 Phase 2 design cannot start |

> Both rows need an owner and a date at the next gate review. Per DEP-3, the **chase** for each
> is its own work item and is *not* itself blocked.

## 3. Resolved cycles

| Date | Cycle | Technique | Outcome |
|------|-------|-----------|---------|
| — | — | *none detected* | — |

Recurring cycles between the same components are an architecture signal, not a planning one —
escalate to the Architecture board ([07 §6](../07-DEPENDENCY_MODEL.md#6-cycles)).

## 4. Current ordered READY queue — WS-1 Phase 4

Computed per [07 §5](../07-DEPENDENCY_MODEL.md#5-execution-ordering). Recompute on every
completion; do not reuse a stale queue.

| # | Item | P | Enables | Effort | State |
|---|------|---|---------|--------|-------|
| 1 | **SUG-20260813-a1c — persist audit events (RISK-012)** | **P1** | 1 | M | **READY** — surfaced by the 4.4 pack; may block 4.4 |
| 2 | Gate 4.4 — compliance review of audit schema | P1 | 2 | S | READY — pack assembled; needs a **human Compliance verdict** |
| 3 | Gate 4.3 — bank consumer UAT | P2 | 0 | M | **BLOCKED by DEP-002** (no named team). Enablement pack done; DEP-001 now partial |
| 4 | Gate 4.2 — publish OpenAPI to the internal portal | P2 | 1 | S | BLOCKED — needs a portal URL + credentials |
| 5 | Gate 4.6 — ratify a p95 target and re-measure on UAT | P2 | 0 | S | BLOCKED on a PO decision (ASM-009) |
| — | ~~Gate 4.1 — sandbox E2E suite in CI~~ | — | — | — | ✅ **Done** 2026-08-13 (absorbed TD-014) |
| — | ~~Gate 4.5 — operations runbook~~ | — | — | — | ✅ **Done** 2026-08-13 |
| — | ~~Gate 4.7 — close or waive QA-001~~ | — | — | — | ✅ **Done** 2026-08-13 — closed, not waived |

> This ordering is **derived, not a commitment.** It follows from the ratified gate criteria in
> [04](../04-STAGE_GATES.md) and the state file. The PO and Tech Lead own the actual sequence
> and re-order it at the weekly Governance Sync.
