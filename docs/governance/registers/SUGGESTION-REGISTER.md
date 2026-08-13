# Suggestion Register

**Every input that could become work gets a row here — admitted, parked, rejected, or escalated.**
Nothing is dropped. Nothing is deleted.

**Owner:** whichever agent or person triaged the input
**ID format:** `SUG-<YYYYMMDD>-<3 chars from 0-9a-z>` — collision-resistant, no shared counter.
Rules: [../state/CURRENT-STATE.yaml](../state/CURRENT-STATE.yaml) `id_allocation`
**Rules:** [08-BACKLOG_RULES.md](../08-BACKLOG_RULES.md) · [09-AI_EXECUTION_RULES.md](../09-AI_EXECUTION_RULES.md)

---

## How to add a row

1. Mint an ID: `SUG-<today>-<3 random chars>`, e.g. `SUG-20260812-a1b`. No counter to
   increment and no merge conflict when two branches triage at once.
2. Run pipeline steps 2–5 ([09 §2](../09-AI_EXECUTION_RULES.md#2-the-mandatory-sequence)).
3. Add the summary row below.
4. For anything beyond a trivial reject, add a detail block in §3 using
   [../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).
5. **Check for duplicates first** (Rule CS-2). A repeat is linked and increments
   `recurrence_count` — it is not a new row.

---

## 1. Status vocabulary

| Status | Meaning |
|--------|---------|
| `ADMITTED` | Entered a backlog for the current stage |
| `ADMIT-BYPASS` | Implemented under a human override of the process ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)) |
| `PARKED` | Real work, later stage — see [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) |
| `ESCALATED` | Awaiting a human decision as `CR-###` |
| `REJECTED` | Will not be done; reason recorded |
| `DUPLICATE` | Already tracked; linked |
| `SUPERSEDED` | Overtaken by another decision; linked |
| `LAPSED` | Idea closed by aging (AS-3) |
| `CLOSED-DELIVERED` | Admitted and shipped; linked to the PR |

---

## 2. Register

| ID | Date | Source | Summary | SF | SC | Necessity | Type | P now / target | Action | Ref |
|----|------|--------|---------|----|----|-----------|------|----------------|--------|-----|
| SUG-20260813-a1c | 2026-08-13 | agent:claude (while preparing the gate 4.4 compliance pack) | Persist audit events to `audit_event` via the existing `POST /internal/v1/audit-events`, instead of emitting them to the application log only | SF0 | SC0 | MUST | COMP | P1 / P1 | ADMITTED | [detail](#sug-20260813-a1c--persist-audit-events) · [RISK-012](./RISK-REGISTER.md#2-open-risks) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

<!-- ### SUG-0001 · <title>  ... full triage record ... -->

### SUG-20260813-a1c · Persist audit events

```yaml
# schema: triage-record
id: SUG-20260813-a1c
raised_at: "2026-08-13"
raised_by: "agent:claude"
source: "assembling the compliance review pack for gate criterion 4.4"
input: >
  The audit trail is documented as an immutable, 7-year, queryable compliance log backed by the
  audit_event table. In fact the only AuditEventPublisher wired into 1sb-integration-service is
  LoggingAuditEventPublisher, which writes to the application log. Nothing writes to the table.
  The persistence endpoint POST /internal/v1/audit-events exists and works; no adapter calls it.
  TD-021 documented that endpoint for an audit-consumer service that was scoped as "Phase 2+ /
  separate story" and does not exist.

# ---- STEP 1: CONTEXT RESOLUTION (01) ----
context:
  workstream: WS-1
  current_phase: "Phase 4 — Hardening & consumer enablement"
  canonical_stage: "L7 — Hardening"
  current_objective: "Term path signed off for UAT use by at least one bank caller"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: "gate-4.4 compliance review pack"

# ---- STEP 2: LIFECYCLE VALIDATION (03) ----
stage_fit:
  code: SF0
  rationale: >
    Criterion 4.4 requires a compliance review of the audit schema and log samples. The schema is
    sound but unpopulated, so the criterion cannot honestly pass against the implementation as it
    stands. Blocking a gate criterion is the definition of SF0.
  target_stage: null
  unpark_trigger: null
  absorption_test:
    small: null
    no_new_dependency: null
    no_new_decision: null
    gate_neutral: null

# ---- STEP 3: SCOPE VALIDATION (02) ----
scope:
  code: SC0
  business_scope: "in scope — 'Compliance review of audit schema and log samples' is a Phase 4 in_scope item"
  serves: []
  failure_without_it: >
    No durable, immutable record of who did what to which application. A dispute, a regulator
    request, or an incident investigation would have only rotating application logs to draw on.
  minimal: true
  authority: "CURRENT-STATE.yaml current_scope.in_scope; ACTION-PLAN.md 4.4"

# ---- STEP 4: NECESSITY (16) ----
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "Phase 4 — Hardening & consumer enablement"
  binds_when: "first UAT exposure to real customer journeys"
  failure_without_it: >
    Criterion 4.4 would pass against a documented control that does not exist. A dispute, a
    regulator request, or an incident investigation would have only rotating application logs
    where an immutable 7-year trail was promised.
  evidence_tier: E1
  evidence:
    - "grep 'implements AuditEventPublisher' over services/ and libs/ returns only LoggingAuditEventPublisher and a test double"
    - "No HTTP adapter for /internal/v1/audit-events exists in 1sb-integration-service"
    - "TD-021 closed as doc-only; the audit-consumer service it described was never built"
  confidence: C5
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true

# ---- STEP 5: ACTION MATRIX (00 §6) ----
action: ADMIT
action_rationale: "SF0 + MUST -> ADMIT at P1. A prerequisite cannot be parked."
duplicate_of: null
conflicts:
  - "architecture and V1__init_schema.sql describe audit_event as the compliance log; the wiring does not exist. Resolved in favour of the code as the statement of fact, and the gap is raised rather than the documents trusted."

# ---- STEP 6: CLASSIFICATION (06) ----
classification:
  type: COMP
  also: [INFRA]
  breakdown: STORY
  epic: null
  risk_tier: T4
  destination: "PRODUCT-BACKLOG.md + RISK-REGISTER.md (RISK-012)"

# ---- STEP 7: PRIORITY (05) ----
priority:
  now: P1
  at_target: P1
  factors: { N: 3, S: 3, B: 2, R: 3, D: 0, E: 1 }
  score: 12
  matrix_default: P1
  consistency: OK
  overrides_applied: []
  caps_applied: []
  rationale: >
    A compliance control that is documented but absent is worse than one known to be missing,
    because it invites reliance. T4 because it is a control a regulator can ask about.

# ---- STEP 8: DEPENDENCIES (07) ----
dependencies:
  edges: []
  state: READY
  enablement_count: 1
  earliest_start: "immediately — the persistence endpoint and schema already exist"
  cycles: none

# ---- STEP 9: BREAKDOWN (06 §5) ----
breakdown:
  children: []
  completion_definition: >
    Audit events emitted by 1sb-integration-service are persisted to audit_event and readable
    through the persistence API, with capture failure degrading like raw-payload capture rather
    than failing the customer's transaction.
  not_included:
    - "Database-enforced insert-only grants (RISK-013 — separate control)"
    - "Audit event retention job (no retention period defined yet — see the 4.4 review pack)"

# ---- Outcome ----
outcome:
  registered_in: "registers/RISK-REGISTER.md (RISK-012); compliance pack Finding 1"
  work_item_id: null
  plan_id: null
  status: ADMITTED
  closed_reason: null

# ---- Return to task ----
resumed: "gate-4.4 compliance review pack"
```

> **Deliberately not implemented in the same change as the pack.** An HTTP audit adapter is new
> production code on a compliance path at risk tier T4: it needs its own work item, plan, and
> Security + Risk & Compliance verdicts. Slipping it into a documentation change is the
> unreviewed scope growth [00 §1](../00-GOVERNANCE.md#1-problem-this-solves) exists to prevent.

---

## 4. Seeded from existing artefacts

AIGEM was adopted mid-flight. Rather than backfilling every past decision
([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)), the
already-deferred items in [TECH-DEBT.md](../../1sb-insurance-integration/service-ssot/TECH-DEBT.md)
were seeded directly into [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) as pre-existing parked work.
They keep their `TD-###` IDs; no `SUG-####` was minted retrospectively.

**Do not re-triage or re-report these** — they are known
([01 §6](../01-CURRENT_STATE.md#6-known-open-debt-affecting-triage)).

---

## 5. Register row convention (machine-enforced)

> **A table row whose first cell is a bare ID is that ID's DEFINITION.** Exactly one definition
> may exist per ID, across every register. `FreshnessCheck` enforces this and halts on a
> duplicate — that is how a cross-branch ID collision is caught after a merge.
>
> Cross-reference rows — the same item shown again in another view, such as an external
> dependency also listed under its edge, or an open risk repeated under accepted risks — must
> **point at** the definition rather than restate the bare ID — for example a leading cell
> of `→ [DEP-002](./DEPENDENCY-REGISTER.md#1-edges)` instead of a bare `DEP-002`.
