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
| SUG-20260812-lld | 2026-08-12 | human:Solution Architect | Stakeholder LLD baseline with platform and layer diagrams | SF1 | SC0 | SHOULD | DOC (+ ARCH) | P3 / P3 | CLOSED-DELIVERED | [detail](#sug-20260812-lld--stakeholder-lld-architecture-baseline) |
| SUG-20260812-bps | 2026-08-12 | agent:codex | Persistence architecture contract says poll-attempt/raw-payload APIs are not exposed, but both are implemented | SF1 | SC1 | SHOULD | DOC | P3 / P3 | ADMITTED (QUEUED) | [detail](#sug-20260812-bps--persistence-contract-api-drift) |
| SUG-20260812-cto | 2026-08-12 | human:Solution Architect | CTO approval presentation and target-system workflow diagrams derived from DOC-005 | SF1 | SC0 | SHOULD | DOC (+ ARCH) | P3 / P3 | CLOSED-DELIVERED | [detail](#sug-20260812-cto--cto-architecture-approval-deck) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

<!-- ### SUG-0001 · <title>  ... full triage record ... -->

### SUG-20260812-lld · Stakeholder LLD architecture baseline

```yaml
id: SUG-20260812-lld
raised_at: "2026-08-12"
raised_by: "human:Solution Architect"
input: >
  Create a stakeholder-facing LLD baseline containing one complete platform diagram,
  layer-level drill-down diagrams, service/component responsibilities, backend and cache
  choices, communication media, and a controlled approval/evolution process before further
  application development.
context:
  workstream: "Cross-cutting platform architecture baseline"
  current_phase: "Architecture recommendation awaiting PO/Compliance/Sponsor approval"
  canonical_stage: "L3 — Technical design / approval baseline"
  current_objective: "Make the recorded target architecture reviewable and governable"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null
stage_fit:
  code: SF1
  rationale: >
    The cross-cutting architecture review is already the recorded platform baseline and is
    explicitly awaiting stakeholder approval; a consolidated LLD approval pack directly serves
    that existing design-stage outcome.
scope:
  code: SC0
  business_scope: "Explicit cross-cutting platform architecture documentation"
  serves: []
  failure_without_it: >
    Stakeholders lack one reviewable baseline that distinguishes implemented, approved, proposed,
    and future components before authorising broader platform development.
  minimal: true
  authority: "docs/governance/01-CURRENT_STATE.md §4 Cross-cutting; docs/platform/architecture-review/README.md"
necessity:
  now: SHOULD
  future_necessity: SHOULD
  target_stage: "L3 — Technical design / approval baseline"
  binds_when: "before target-platform architecture approval or broader application development"
  evidence_tier: E2
  evidence:
    - "Requester explicitly requires an approval baseline before application development"
    - "Architecture review status is recommendation pending PO/Compliance/Sponsor approval"
  confidence: C4
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true
action: ADMIT
action_rationale: >
  Documentation-only consolidation of recorded architecture is on-stage and introduces no runtime
  dependency, contract, or implementation change.
duplicate_of: null
conflicts: []
classification:
  type: DOC
  also: [ARCH]
  breakdown: STORY
  epic: null
  risk_tier: T2
  destination: "docs/platform/architecture-review/09-stakeholder-lld-approval-baseline.md"
priority:
  now: P3
  at_target: P3
  factors: {N: 2, S: 3, B: 1, R: 1, D: 1, E: 2}
  score: 13
  matrix_default: P3
  consistency: OK
  overrides_applied: []
  caps_applied: []
  rationale: "On-stage approval aid; important but not a current P1/P2 delivery-gate override"
dependencies:
  edges: []
  state: READY
  enablement_count: 1
  earliest_start: "2026-08-12"
  cycles: none
breakdown:
  children: []
  completion_definition: >
    A versioned draft contains one end-to-end diagram, five layer diagrams, current-service
    component LLDs, responsibility/data/cache/communication matrices, and approval/change-control
    instructions with proposal-versus-implementation status labels.
  not_included:
    - "Approving any proposed ARCH decision"
    - "Changing application code, deployment topology, runtime dependencies, APIs, or schemas"
    - "Implementing Kafka, Redis, EKS, new services, or future LOBs"
outcome:
  registered_in: "docs/governance/registers/SUGGESTION-REGISTER.md"
  work_item_id: DOC-005
  plan_id: PLAN-DOC-005
  status: CLOSED-DELIVERED
  closed_reason: "DOC-005 version 0.1 created and indexed; stakeholder approval remains Pending"
resumed: DOC-005
```

#### PLAN-DOC-005 · Short implementation plan (T2)

```yaml
objective: "Create a stakeholder-reviewable LLD baseline without changing architecture decisions"
solution: >
  Consolidate authoritative business, platform, identity, and 1SB module documentation plus the
  implemented repository structure into one versioned Markdown approval pack with Mermaid diagrams.
files_expected:
  - "docs/platform/architecture-review/09-stakeholder-lld-approval-baseline.md"
  - "docs/platform/architecture-review/README.md"
  - "docs/platform/README.md"
  - "docs/README.md"
  - "docs/au-bank-insurance-platform/DECISION-LOG.md"
  - "docs/governance/registers/SUGGESTION-REGISTER.md"
testing:
  - "FreshnessCheck passes"
  - "all relative Markdown links resolve"
  - "Mermaid blocks have balanced fences and unique diagram identifiers"
  - "service counts and status labels reconcile with the architecture review and repository"
rollback: "Revert DOC-005 documentation changes; no application or data rollback is required"
out_of_scope:
  - "architecture approval"
  - "application development"
  - "new technology decisions"
acceptance_criteria:
  - "one large platform diagram"
  - "one drill-down diagram for each of the five architecture layers"
  - "implemented services have internal component diagrams"
  - "every service has responsibility, datastore/cache, and communication details"
  - "approval roles, decision states, version history, and evolution workflow are explicit"
reviews:
  - board: Architecture
    reviewer_type: AGENT
    self_review: true
    verdict: APPROVED_WITH_CONDITIONS
    evidence:
      - "A1/A2: plan only consolidates boundaries already recorded in architecture-review/02 and module SSOTs"
      - "A4: standing constraints are explicit acceptance criteria"
      - "A5/A6: no decision or infrastructure is introduced; proposed choices remain proposed"
    conditions:
      - "Every diagram and table must distinguish implemented/approved from proposed/future"
  - board: Technical
    reviewer_type: AGENT
    self_review: true
    verdict: APPROVED
    evidence:
      - "Repository source and configuration will be used for implemented-component detail"
      - "No code, schema, API, or dependency change is in scope"
  - board: Product
    reviewer_type: AGENT
    self_review: true
    verdict: APPROVED_WITH_CONDITIONS
    evidence:
      - "Business SSOT remains higher precedence than the architecture recommendation"
    conditions:
      - "Document must not present pending business or architecture decisions as approved"
  - board: QA
    reviewer_type: AGENT
    self_review: true
    verdict: APPROVED
    evidence:
      - "Acceptance criteria are observable through link, count, fence, and governance checks"
```

Delivery evidence:

- [`DOC-005` version 0.1](../../platform/architecture-review/09-stakeholder-lld-approval-baseline.md)
  contains 12 Mermaid diagrams, including one end-to-end platform diagram and all five layer
  drill-downs.
- Repository inventory confirms 5 implemented service directories; the source target catalogue
  contains 19 rows; the reconciled 21–22 target range is explicitly approval-dependent.
- All relative Markdown targets in changed files resolve; Mermaid fences/declarations and
  subgraph identifiers pass structural checks; `git diff --check` passes.
- `java scripts/governance/FreshnessCheck.java` returns `VERDICT: FRESH` with unique register IDs.
- Full `ci-checks.py` was not runnable locally because no Python interpreter is installed; its
  link concern was covered by the read-only PowerShell link check above.

### SUG-20260812-cto · CTO architecture approval deck

```yaml
id: SUG-20260812-cto
raised_at: "2026-08-12"
raised_by: "human:Solution Architect"
input: >
  Coordinate Product Owner and Solution Architect perspectives on DOC-005 and create a clean,
  CTO-ready PowerPoint with complete target-system workflows expressed in Mermaid and in the deck.
context:
  workstream: "Cross-cutting platform architecture baseline"
  current_phase: "Architecture recommendation awaiting PO/Compliance/Sponsor approval"
  canonical_stage: "L3 — Technical design / approval baseline"
  current_objective: "Obtain an informed executive decision on the target architecture baseline"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null
stage_fit:
  code: SF1
  rationale: "The presentation is an approval aid for the existing DOC-005 design baseline."
scope:
  code: SC0
  business_scope: "Cross-cutting platform architecture documentation and stakeholder approval"
  serves: []
  failure_without_it: "The CTO lacks a concise decision narrative and reviewable workflow view."
  minimal: true
necessity:
  now: SHOULD
  future_necessity: SHOULD
  target_stage: "L3 — Technical design / approval baseline"
  evidence_tier: E2
  confidence: C4
action: ADMIT
action_rationale: >
  Documentation-only repackaging of the governed baseline; it introduces no runtime, contract,
  topology, data, or technology decision.
classification:
  type: DOC
  also: [ARCH]
  breakdown: STORY
  risk_tier: T2
  destination: "docs/platform/architecture-review/presentations/bank-insurance-platform-cto-architecture-approval.pptx"
priority:
  now: P3
  at_target: P3
  rationale: "Directly supports the pending architecture approval, without overriding delivery gates."
dependencies:
  edges: ["DOC-005"]
  state: READY
  cycles: none
breakdown:
  completion_definition: >
    A rendered and visually verified executive deck explains the business case, current-to-target
    architecture, service count, end-to-end workflows, data/cache/comms, risks, phased evolution,
    and explicit approval decisions; editable Mermaid source is included.
  not_included:
    - "Human PO, Architect, CTO, Security, Compliance, QA, or Ops approval"
    - "Changing DOC-005 decision states"
    - "Application, infrastructure, API, event, schema, or runtime implementation"
outcome:
  registered_in: "docs/governance/registers/SUGGESTION-REGISTER.md"
  work_item_id: DOC-007
  plan_id: PLAN-DOC-007
  status: CLOSED_DELIVERED
```

#### PLAN-DOC-007 · Short implementation plan (T2)

```yaml
objective: "Turn DOC-005 into a concise, evidence-based CTO approval narrative"
solution: >
  Use the repository's PO and Solution Architect role contracts for two independent agent-simulated
  reviews, then create a PowerPoint and Mermaid workflow source that preserve all status qualifiers.
files_expected:
  - "docs/platform/architecture-review/10-cto-approval-workflows.md"
  - "docs/platform/architecture-review/presentations/bank-insurance-platform-cto-architecture-approval.pptx"
  - "docs/platform/architecture-review/README.md"
  - "docs/governance/registers/SUGGESTION-REGISTER.md"
testing:
  - "FreshnessCheck passes"
  - "Mermaid blocks have valid declarations and balanced fences"
  - "PowerPoint renders completely with no overflow or unintended overlap"
  - "every slide has source notes and proposal status is never presented as approval"
rollback: "Revert DOC-007 documentation assets; no application or data rollback is required"
reviews:
  - board: Product
    reviewer_type: AGENT_SIMULATION
    verdict: APPROVED_WITH_CONDITIONS
    evidence:
      - "Deck anchors the decision in the Life/ETB, RM/self/hybrid, mandatory suitability, customer-device payment and sold-definition business rules"
      - "Implemented capability is kept separate from the complete target-state view"
    conditions:
      - "PO must still confirm the business working decisions and sponsor-validation path"
      - "Conditional direction approval must not be presented as authorization to build every proposed component"
  - board: Architecture
    reviewer_type: AGENT_SIMULATION
    verdict: APPROVED_WITH_CONDITIONS
    evidence:
      - "Service/data/provider boundaries and status markers reconcile to DOC-005"
      - "Workflow diagrams preserve synchronous, asynchronous, external and mandatory-gate semantics"
    conditions:
      - "Q-01 through Q-10 remain open until named human approvers record decisions"
      - "DOC-005 remains authoritative over any presentation simplification"
  - board: Technical
    reviewer_type: AGENT
    verdict: APPROVED_WITH_CONDITIONS
    conditions:
      - "Render and inspect every slide before delivery"
      - "Keep the detailed DOC-005 baseline authoritative over presentation simplification"
```

Delivery evidence:

- The CTO deck contains 15 slides covering the decision ask, business outcome, five implemented
  services, five-layer target architecture, 21–22 deployable range, three system workflows,
  data/cache/communications, trust boundaries, phased evolution, Q-01…Q-10 and approval options.
- Every slide was rendered and inspected at full resolution; connector direction, one-line titles,
  footer fit and content density were corrected before delivery.
- The PPTX package contains 15 slide parts and 15 speaker-note parts; every note contains a
  `[Sources]` block pointing to repository authority.
- [`DOC-007` Mermaid source](../../platform/architecture-review/10-cto-approval-workflows.md)
  contains five balanced Mermaid blocks with recognized declarations.
- Product and Architecture verdicts above are explicitly agent-simulated preparation reviews,
  not human PO, Solution Architect or CTO approval.

### SUG-20260812-bps · Persistence contract API drift

```yaml
SUG-20260812-bps: >
  docs/1sb-insurance-integration/architecture/bank-persistence-service.md still says poll-attempt
  and raw-payload resources are not HTTP-exposed, while JobController and RawPayloadController
  implement those APIs and the service README documents them.
context: "WS-1 · Phase 4 hardening · active item DOC-005"
stage/scope: "SF1 / SC1; serves accurate implemented API evidence and consumer documentation"
necessity: "SHOULD · E1 source-code evidence · confidence C5"
action: "ADMIT as DOC-006, P3 now / P3 target; READY but queued behind DOC-005"
not_included: "No contract, controller, schema, or runtime change"
resumed: DOC-005
```

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
