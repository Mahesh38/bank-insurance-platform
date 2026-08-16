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
| SUG-20260816-d8v | 2026-08-16 | human:Mahesh | Add Dilip AI executive-sponsor perspective for bancassurance business/value decisions and wire it into P0/R0 | SF2 | SC1 | SHOULD | GOV | P2 / P2 | ADMIT-BYPASS | [1SB backlog governance/decision-quality enablers](../../1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md#governance--decision-quality-enablers) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

### SUG-20260816-d8v · Dilip AI Executive Sponsor Perspective

```yaml
id: SUG-20260816-d8v
raised_at: "2026-08-16"
raised_by: "human:Mahesh"
source: "direct user instruction after accepting the executive-sponsor recommendation"
input: >
  Accept the recommendation, create the persona on the GitHub repository, push the code,
  put references wherever required, and make the perspective available before the remaining
  Phase/P0 stories. The requested AI should primarily provide the executive sponsor perspective
  and may give clarity/approval in bounded cases.

context:
  workstream: "cross-cutting Product/business context; WS-1 remains in Phase 4 hardening"
  current_phase: "WS-1 Phase 4 — Hardening & consumer enablement"
  canonical_stage: "L7 — Hardening for WS-1; AU platform R0/P0 requirements remain active business context"
  current_objective: "P4-UAT-SIGNOFF for WS-1; improve decision quality before remaining AU-platform P0 story refinement"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: "user-directed SUG-20260816-d8v documentation integration"

stage_fit:
  code: SF2
  rationale: >
    Small documentation/context integration with no runtime change, no new dependency and no
    gate-criterion change. It can be absorbed while making the sponsor perspective available
    before material P0/R0 business decisions.
  absorption_test:
    small: true
    no_new_dependency: true
    no_new_decision: true
    gate_neutral: true

scope:
  code: SC1
  business_scope: >
    Cross-cutting decision-quality enabler for Product/R0 rather than 1SB runtime functionality.
  serves:
    - "AU platform P0/R0 scope and business-value decisions"
    - "material Should-deferral and investment decisions"
    - "pilot success and benefits-realization definition"
  failure_without_it: >
    Executive sponsor reasoning remains implicit and future agents can miss the business-value,
    investment and measurable-outcome perspective before Product finalizes material P0 decisions.
  minimal: true
  authority: "direct user instruction + existing Rajal Product package extension model"

necessity:
  now: SHOULD
  future_necessity: SHOULD
  target_stage: "before material P0/R0 scope/value decisions"
  binds_when: "BRD/R0 sponsor-perspective trigger fires"
  evidence_tier: E1
  evidence:
    - "direct project-owner/user instruction"
    - "R0-SCOPE already contains a Business Sponsor sign-off slot"
    - "BRD-P0 previously allowed Should slip with sponsor OK but had no reusable AI sponsor lens"
  confidence: C5
  assumptions: []

action: ADMIT-BYPASS
action_rationale: >
  The user explicitly accepted the recommendation and directed immediate repository creation,
  references and push. The bypass is limited to documentation/context queue ordering; it does not
  bypass any Architecture, Security, Compliance, QA, SRE, Database or mandatory-human decision.
conflicts:
  - >
    CR-009 closes the canonical persona roster. Resolved by implementing Dilip as an auxiliary
    Product-side executive sponsor perspective under Rajal, not as a tenth canonical persona,
    new board or parallel authority.

classification:
  type: GOV
  also: [DOC]
  breakdown: STORY
  epic: null
  risk_tier: T1
  destination: "1SB PRODUCT-BACKLOG.md governance / decision-quality enablers"

priority:
  now: P2
  at_target: P2
  rationale: >
    Explicitly requested before remaining P0 business decisions, but it does not supersede open
    runtime hardening blockers or a P1 safety/regulatory override.

dependencies:
  edges: []
  state: READY
  enablement_count: 3
  earliest_start: "now by explicit user direction"
  cycles: none

breakdown:
  children: []
  completion_definition: >
    Dilip sponsor lens documented under Rajal; P0 BRD/R0/stakeholder references wired; governance
    trace recorded; branch pushed and draft PR opened.
  not_included:
    - "new AIGEM persona or board"
    - "change to canonical persona authority matrix"
    - "human sponsor impersonation/signature"
    - "runtime application changes"

outcome:
  registered_in: "registers/SUGGESTION-REGISTER.md + PRODUCT-BACKLOG.md"
  work_item_id: SUG-20260816-d8v
  plan_id: null
  status: IN_REVIEW
  closed_reason: null

resumed: "WS-1 P4-UAT-SIGNOFF remains the governing delivery objective after this documentation PR"
```

**Bypass risk:** this user-directed documentation work consumes repository capacity that could otherwise advance the open WS-1 `GATE-P4`, especially consumer/UAT enablement work. It does **not** change, waive or mark any gate criterion complete.

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
