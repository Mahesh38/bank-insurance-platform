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
| SUG-20260816-ba7 | 2026-08-16 | human:Mahesh | Add a senior end-to-end bancassurance BA AI persona for existing R11 and link it to current personas/context | SF2 | SC1 | SHOULD | GOV | P2 / P2 | ADMIT-BYPASS | [Principal BA package](../../context/roles/principal-insurance-platform-business-analyst/README.md) |
| SUG-20260816-ap1 | 2026-08-16 | human:Mahesh | Build a reusable context module and safe evidence-driven autopilot; reconcile semantic governance drift and documentation structure | SF1 | SC1 | MUST | GOV | P2 / P2 | ADMITTED | [CR-010](../change-requests/CR-010-context-module-and-safe-autopilot.md) |
| SUG-20260816-w3s | 2026-08-16 | agent:claude | Extend `current-state.schema.json` with `depends_on` / `entry_conditions` on a workstream and `parent_workstream` / `delivers_bounded_contexts` on `lifecycle`, so workstream relationships are validated rather than held in comments | SF2 | SC1 | SHOULD | GOV | P3 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) |
| SUG-20260820-n5t | 2026-08-20 | human:Mahesh | Redraw `docs/hdl.svg` as the release-coded North Star HLD — boundary descriptions, LOB segregation, aggregation/provider layer, mature-platform capabilities, R0→RN phasing — and preserve the R0 view alongside it | SF2 | SC1 | SHOULD | DOC | P3 / P3 | ADMIT-BYPASS | [architecture diagrams](../../architecture/README.md) |
| SUG-20260820-r1t | 2026-08-20 | agent:claude | Produce the R0 → R1 → R2 transition and dependency map the North Star does not answer: the order in which target components must appear and which are prerequisites for which | SF3 | SC1 | SHOULD | ARCH | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) |

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
  status: CLOSED-DELIVERED
  closed_reason: null

resumed: "WS-1 P4-UAT-SIGNOFF remains the governing delivery objective after this documentation PR"
```

**Bypass risk:** this user-directed documentation work consumes repository capacity that could otherwise advance the open WS-1 `GATE-P4`, especially consumer/UAT enablement work. It does **not** change, waive or mark any gate criterion complete.

### SUG-20260816-ba7 · Principal Insurance Platform Business Analyst / R11

```yaml
id: SUG-20260816-ba7
raised_at: "2026-08-16"
raised_by: "human:Mahesh"
source: >
  Direct user instruction identifying the change as Mahesh's governance decision, explicitly
  requesting immediate bypass, a fresh branch, reference to the Mahesh/Deepali/Aarti/Shivanshi
  personas, and creation of an improved end-to-end bancassurance BA persona.
not_an_ai_suggestion: true

context:
  workstream: "cross-cutting governance/persona context; runtime workstreams unchanged"
  current_phase: "WS-1 Phase 4 hardening; WS-2 foundation"
  active_work_item: "user-directed R11 Principal BA persona integration"

stage_fit:
  code: SF2
  rationale: >
    Documentation/governance-context change only. It introduces no runtime/API/schema/configuration
    change and marks no delivery gate criterion complete.

scope:
  code: SC1
  serves:
    - "end-to-end bancassurance Product and requirement decision preparation"
    - "R11 requirement readiness, AC quality and traceability"
    - "business handoffs to Product, Architecture, Security, Database, SRE and other authorities"
  minimal: true

necessity:
  now: SHOULD
  target_stage: "before further material journey/requirement elaboration"
  evidence_tier: E1
  evidence:
    - "direct Mahesh/repository-owner instruction"
    - "existing AIGEM R11 role and Application Lifecycle business-analysis responsibilities"
  confidence: C5

action: ADMIT-BYPASS
action_rationale: >
  Mahesh explicitly instructed immediate governance creation and bypassed normal AI intake/queue
  ordering. The register ID is retained only because 09-AI_EXECUTION_RULES §8 requires a bypass
  record; it must not be represented as an AI-originated suggestion.
process_skipped:
  - "normal suggestion deferral and single-in-flight queue ordering"
  - "separate CR preparation before documentation"
authorised_by: "Mahesh / repository owner — direct instruction, 2026-08-16"

classification:
  type: GOV
  also: [DOC]
  risk_tier: T1

authority_effect:
  existing_role: "R11 — Business Analyst / Product delegate"
  new_board: false
  new_aigem_role: false
  named_human_roster_growth: false
  product_authority: "unchanged — Rajal"
  specialist_authority: "unchanged — canonical matrix owners"
  human_approval_impersonation: false

priority:
  now: P2
  at_target: P2
  rationale: "explicitly requested now; no P1 override or runtime gate closure"

outcome:
  decision: GOV-008
  destination: "docs/context/roles/principal-insurance-platform-business-analyst/"
  branch: "codex/principal-business-analyst-persona"
  status: CLOSED-DELIVERED

resumed: "WS-1 P4-UAT-SIGNOFF remains the governing runtime delivery objective after this docs-only branch"
```

**Bypass risk:** this governed documentation work consumes capacity while `GATE-P4` remains open.
The mitigation is bounded scope: one R11 package plus canonical links, with no application code,
stage state, gate evidence or specialist authority changed.

### SUG-20260816-ap1 · Reusable Context Module and Safe Autopilot

```yaml
id: SUG-20260816-ap1
raised_at: "2026-08-16"
raised_by: "human:Mahesh"
source: "direct acceptance of the 2026-08-16 governance/context validation recommendations"
input: >
  Synchronise main, accept the review recommendations, create an autopilot operating mode,
  remove proven documentation redundancy, improve folder abstraction, and make the context
  module reusable for projects with different problem statements and domains.

context:
  workstream: "cross-cutting governance/context; supports WS-1 and WS-2 and prepares proposed WS-3"
  current_phase: "WS-1 Phase 4 hardening; WS-2 Phase 1 foundation"
  canonical_stage: "L7 for WS-1; L4/L6 for WS-2"
  current_objective: "remove structural delivery blockers without auto-approving a stage or regulated decision"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: SUG-20260816-ap1

stage_fit:
  code: SF1
  rationale: >
    Application CI, correct routing, machine-verifiable evidence and non-blocking scheduling
    support current gates; reusable context packaging is absorbed as the same bounded control-plane change.

scope:
  code: SC1
  business_scope: "cross-cutting delivery-enablement and context portability"
  serves: ["GATE-P4 4.1/4.7", "GATE-IAM-P1", "future WS-3 foundation recovery"]
  failure_without_it: "semantic drift remains invisible and blocked work can stall the whole programme"
  minimal: true
  authority: "direct repository-owner instruction; specialist and stage-transition approvals remain separate"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "current governance hardening"
  binds_when: "before unattended/autopilot execution is enabled"
  evidence_tier: E1
  evidence:
    - "2026-08-16 validation: mechanical checks green while routing paths and semantic statements conflict"
    - "GATE-P4 0/7 and GATE-IAM-P1 0/6 criteria closed"
  confidence: C5
  assumptions: []

action: ADMIT
action_rationale: "The recommendation was raised in the previous turn and explicitly accepted by the human in this turn."
conflicts:
  - "Automation may prepare CANDIDATE evidence but cannot supply PASSED or mandatory human approvals."

classification:
  type: GOV
  also: [DOC, INFRA, QA]
  breakdown: EPIC
  epic: CR-010
  risk_tier: T3
  destination: "governance change request + existing workstream backlog"

priority:
  now: P2
  at_target: P2
  rationale: "High enablement value and current control-plane defects; no evidenced O1-O8 override."

dependencies:
  edges: []
  state: READY
  enablement_count: 5
  earliest_start: "now"
  cycles: none

breakdown:
  children:
    - "context framework and project manifest"
    - "semantic validation"
    - "safe autopilot evidence controller"
    - "application CI foundation"
    - "documentation consolidation"
  completion_definition: "PLAN-001 acceptance criteria pass with no automatic human approval or stage transition."
  not_included:
    - "marking a lifecycle stage PASSED"
    - "importing the full proposed Bible backlog into Jira"
    - "production deployment or risk acceptance"

outcome:
  registered_in: "SUGGESTION-REGISTER.md + CR-010 + PRODUCT-BACKLOG.md"
  work_item_id: SUG-20260816-ap1
  plan_id: PLAN-001
  status: ADMITTED
  closed_reason: null

resumed: null
```

### SUG-20260820-n5t · North Star HLD with release phasing

```yaml
id: SUG-20260820-n5t
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  Update docs/hdl.svg to the final vision for the insurance platform, with reference to the
  recorded target-state design discussion. The SVG must carry a detailed description of each
  boundary, LOB segregation, the aggregation/provider layer and the capabilities a mature
  architecture must have, and must show the phase-wise release: what is in R0, R1, R2 and RN,
  RN being the final targeted system for now.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH"

stage_fit:
  code: SF2
  rationale: >
    A documentation artefact with no runtime change. Architecture intent at S08 is legitimate
    and cheap: R0 is being built now, and the value of drawing the target now is precisely that
    R0 leaves the right seams behind. It does not admit R1+ work, move a gate or create a
    dependency.
  absorption_test:
    small: true
    no_new_dependency: true
    no_new_decision: true
    gate_neutral: true

scope:
  code: SC1
  business_scope: >
    Architecture communication artefact for WS-3. Not a scope change: the diagram renders
    CURRENT-STATE's own in_scope / out_of_scope split rather than proposing a different one.
  serves:
    - "R0 build decisions that are cheap now and expensive later — the seams"
    - "stakeholder answer to 'why is R0 so small' and 'why build a registry for one product'"
    - "Delivery (R12) sequencing input and the follow-on transition map"
  failure_without_it: >
    R0 gets built without its seams, and adding Health becomes a redesign rather than a cell.
  minimal: true
  authority: >
    Board 1 Architecture owns the HLD. AI may draft and simulate Mahesh's reasoning; the
    mandatory human T4 Architecture sign-off in 11-REVIEW_GATES.md is NOT satisfied by this.

necessity:
  verdict: SHOULD
  evidence_tier: E5
  confidence: C4
  note: >
    Target-state content is drawn from the recorded design discussion plus the repository's own
    architecture registration and current state. Where the discussion and CURRENT-STATE disagree
    on release numbering, CURRENT-STATE wins — non-Life LOBs are 'R2+' there, which the diagram
    realises as R3 (Health) and R4 (General/Motor).

action:
  verdict: ADMIT-BYPASS
  rationale: >
    Implemented in the turn it was raised, which the one rule normally forbids. Recorded as
    ADMIT-BYPASS rather than ADMITTED because it was a direct human instruction from the owning
    authority for a documentation artefact, not an agent-originated suggestion.
  priority_now: P3
  priority_at_target: P3
  type: DOC
  risk_tier: T1

decisions_taken:
  - id: "keep both diagrams"
    decision: >
      docs/hdl.svg becomes the North Star; the previous R0 HLD is preserved unchanged at
      docs/architecture/r0-reference-architecture.svg.
    rationale: >
      The two answer different questions. Publishing only the target invites 'why has the team
      not built Health?'; publishing only R0 invites 'why are we building a Journey Registry for
      one product?'. Overwriting the R0 view would have destroyed the executable architecture.
  - id: "label the unadmitted"
    decision: >
      R1–R4 elements are stamped with their CURRENT-STATE out_of_scope revisit_at; RN elements
      are marked as having no governance record; a separate dashed band marks integrations
      (CKYC, V-KYC, e-sign, TPA, IRDAI/IIB reporting, channel vendors) as RN candidates that are
      explicitly NOT admitted scope.
    rationale: >
      A target diagram is the easiest artefact in a programme to misread as a plan. Naming the
      gaps is more useful than omitting them, but only if the diagram says they are gaps.

not_included:
  - "any change to CURRENT-STATE.yaml scope, stage or gate state"
  - "T4 Architecture sign-off, which remains outstanding and human"
  - "an ADR for any RN technology choice (event bus, direct insurer routes, analytics)"
  - "the R0 -> R1 -> R2 transition and dependency map (SUG-20260820-r1t, parked)"

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-n5t
  status: ADMIT-BYPASS
  evidence:
    - "docs/hdl.svg — North Star, 11 described boundaries, release-coded R0..RN"
    - "docs/architecture/r0-reference-architecture.svg — R0 view preserved"
    - "docs/architecture/README.md — which diagram answers which question"
    - "scripts/governance/ci-checks.py — PASSED, 24 checks"
  closed_reason: null

resumed: null
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
