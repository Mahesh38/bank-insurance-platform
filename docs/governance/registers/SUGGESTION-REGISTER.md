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
| SUG-20260820-hr0 | 2026-08-20 | human:Mahesh | HLD review round: correct the R0 actor model to two actors with SP as a certification attribute, gate and insurer-scope the Insurance Partner Representative, make the opportunity the single RM-only origination point, make LOB first-class from release 1 and make the configuration layer ship in R0 independently of any admin UI | SF0 | SC0 | MUST | ARCH | P1 / P1 | ADMIT-BYPASS | [ADR-004…007](../../platform/architecture-review/08-architecture-decision-log.md) |
| SUG-20260820-r1t | 2026-08-20 | agent:claude | Produce the R0 → R1 → R2 transition and dependency map the North Star does not answer: the order in which target components must appear and which are prerequisites for which | SF3 | SC1 | SHOULD | ARCH | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) |
| SUG-20260820-al7 | 2026-08-20 | human:Mahesh | Reconcile the North Star and R0 diagrams: one naming and layer convention across both files, the R0 view redrawn on the North Star's boundary bands so it reads as a release-zero cut of the same picture, and the Life LOB cell visually separated from the shared platform | SF1 | SC1 | MUST | ARCH | P2 / P2 | ADMITTED | [detail](#sug-20260820-al7--hld-and-r0-diagram-alignment) |
| SUG-20260820-dc4 | 2026-08-20 | human:Mahesh | Resolve OPEN-A1 and OPEN-D10: physical database topology is an evidence-led decision, not a principle — R0 starts as one cluster with a schema per context and splits later along the LOB-cell / shared-platform seam; and context #5 is named Opportunity, because a lead is too thin to carry renewal, lapse and cross-sell demand | SF1 | SC1 | MUST | ARCH | P2 / P2 | ADMITTED | [ADR-008](../../platform/architecture-review/08-architecture-decision-log.md) |
| SUG-20260820-hl1 | 2026-08-20 | human:Mahesh | Act as Mahesh: turn the R0 reference architecture SVG into a detailed HLD (domain, boundary, communication, API, business logic, phases/waves/what-to-do-when) and an LLD for the CTO and AWS platform team (e2e components, services, AWS, VPC, reverse proxy, PVC, DB, cache) | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMIT-BYPASS | [R0-HLD](../../architecture/R0-HLD.md) · [R0-LLD](../../architecture/R0-LLD.md) |
| SUG-20260820-ls1 | 2026-08-20 | human:Mahesh | Create an SVG rendering of the R0 LLD for the CTO and AWS platform team | SF1 | SC0 | SHOULD | ARCH | P2 / P1 | ADMIT-BYPASS | [r0-lld.svg](../../architecture/r0-lld.svg) |
| SUG-20260820-pt9 | 2026-08-20 | human:Mahesh | Draw the AWS platform-team application view: what the application is, the service inventory, availability-zone placement, the DR bill of materials, the reverse-proxy and egress chain, and **when** each resource is needed — as a deployment topology in the style of a landing-zone request diagram | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMIT-BYPASS | [R0-LLD §2.1/§11.1/§12.1](../../architecture/R0-LLD.md) · rendering superseded by [SUG-20260820-ic3](#sug-20260820-ic3--icon-notation-generated-from-code) |
| SUG-20260820-ic3 | 2026-08-20 | human:Mahesh | Redraw the platform-team views in AWS / Kubernetes icon notation instead of labelled rectangles, and generate them from code rather than hand-authoring SVG | SF1 | SC0 | SHOULD | DOC | P3 / P2 | ADMIT-BYPASS | [diagrams/](../../architecture/diagrams/README.md) |
| SUG-20260820-lay4 | 2026-08-20 | human:Mahesh | Keep the icons, drop the layout engine: place every element on a chosen grid and route every connector orthogonally, so the views are aligned and the links are straight | SF1 | SC0 | SHOULD | DOC | P3 / P2 | ADMIT-BYPASS | [diagrams/](../../architecture/diagrams/README.md) |
| SUG-20260820-cm2 | 2026-08-20 | human:Mahesh | Close the context-architecture gap found by audit: 20 documents unreachable by any link and 96 more at 3+ hops, 22 persona-package files no card routed to, and no CI guard against either. Add a generated document-routing map (`DOC-MAP.yaml`) with a `find` query path, complete the persona `Load deeper` tables, consolidate the READMEs that carry no unique content, and fail CI on an unrouted document | SF1 | SC1 | MUST | GOV | P2 / P2 | ADMIT-BYPASS | [DOC-MAP](../../context/DOC-MAP.yaml) · [doc_routing](../../context/AGENT-CONTEXT-INDEX.yaml) · continues [CR-010](../change-requests/CR-010-context-module-and-safe-autopilot.md) |
| SUG-20260821-jx1 | 2026-08-21 | human:Mahesh | Produce an end-to-end Journey Execution Specification for R0: every actor use case, the hop-by-hop route of each request across edge/BFF/service/aggregate/persistence, the validation performed at each layer with its algorithm, every external API call, and every possible outcome — assembled for the dev and QA teams | SF1 | SC1 | SHOULD | DOC | P3 / P1 | ADMITTED | [detail](#sug-20260821-jx1--r0-journey-execution-specification) |
| SUG-20260821-jx2 | 2026-08-21 | human:Mahesh | Extend the Journey Execution Specification beyond R0 to the whole application — DIY/customer journey, hybrid mode switching, Group B insurers, ULIP/Savings, Health/Motor/Travel, renewals and servicing, admin UI, operations and reporting surfaces | SF3 | SC2 | NOT-NOW | DOC | P5 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) |

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

### SUG-20260820-hr0 · HLD review round — R0 actors, LOB boundary, configuration

```yaml
id: SUG-20260820-hr0
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction — HLD review comments issued by the owning Board 1 Architecture authority"
input: >
  Five review comments on the R0 HLD. (1) There are two actors, not one: the Bank RM is the
  certified Specified Person, and SP certification is an attribute on the RM, not a standalone
  actor row or channel; the Insurance Partner Representative is an insurer employee who assists the
  RM or the customer and is not an SP. (2) Lead/opportunity origination is RM-only; the opportunity
  is the single origination point that every downstream module consumes; the IPR has no create
  rights; no parallel origination path in MVP. (3) IPR visibility is gated — nothing is visible
  until the RM has created the opportunity and completed suitability and need analysis — and is
  scoped to the IPR's own insurer at the data/query layer, not the UI; because the IPR is not an
  SP their role must be assist-only, the RM stays the accountable SP, and every IPR action must be
  audit-logged and attributed separately so the solicitation trail is clean for IRDAI. (4) LOB
  segregation must be visible from day one: DB schema, entity model and config tables carry LOB as
  a first-class dimension from release 1, and product, journey, rules, commission and document
  requirements are all LOB-partitioned from the start. (5) Everything is configuration-driven with
  no exceptions, and this is independent of front-end availability — the configuration layer ships
  now, versioned and seedable in the backend, even if no admin panel is built in R1.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH"
  active_work_item: "none in flight; this instruction is the work"

stage_fit:
  code: SF0
  rationale: >
    Prerequisite, not adjacent. The artefacts corrected here — the domain model, information model,
    R0 solution architecture and security architecture — are the inputs S08 and S11 build from, and
    all four are AI-DRAFTED with human signature outstanding. Nothing is implemented, so this is
    the repair of an unsigned baseline rather than a change to a shipped one. Two of the five
    comments are structural dimensions (LOB, configuration) that are free to carry now and become a
    migration across every table on the sale path once a second line of business, a second insurer
    or a live rule pack exists. Correcting a design document at S08 is exactly the posture RUNBOOK
    section 8.3 prescribes for L4 Foundation: build the floor, and build it right.
  blocks:
    - "GATE-S08 criterion S08-G4 — ArchUnit and static analysis: FF-16 to FF-21 cannot be written against a wrong actor model"
    - "S11 entry — the R0 build order and the service set change"

scope:
  code: SC0
  business_scope: >
    Explicitly in scope. CURRENT-STATE.yaml WS-3 current_scope.in_scope already lists the Lead
    service (context #5), the suitability and consent contexts, the product catalogue and the audit
    store. No new capability is admitted here; the review corrects how the admitted ones are
    modelled.
  serves:
    - "R0 build decisions that are cheap now and expensive later — actors, LOB and configuration"
    - "IRDAI solicitation attribution: one accountable Specified Person per record"
    - "the Health and General onboarding that follows R0 on the same template"
  failure_without_it: >
    An uncertified insurer employee acquires a de-facto solicitation path with no separate
    attribution; LOB becomes a backfill across every table on the sale path plus an audit history
    that cannot be corrected; and every W1 to W4 service is written with hardcoded product and
    insurer branches that are never removed.
  minimal: true
  authority: >
    Board 1 Architecture owns the HLD. Three of the four decisions reach beyond that: ADR-004 is
    A3_JOINT_REVIEW with Deepali and carries a compliance threshold that is Shailja's, ADR-005
    changes the R0 build order and a Product-owned label and needs Rajal, and ADR-007 makes
    configuration an authorization-relevant asset. An AI may draft Mahesh's reasoning; the mandatory
    human T4 Architecture sign-off in 11-REVIEW_GATES.md is NOT satisfied by this record.

necessity:
  verdict: MUST
  evidence_tier: E5
  confidence: C4
  note: >
    The actor and IPR corrections are grounded in the repository's own material: business-problem-
    statement section 6 already names the Insurance Partner Representative as a distinct actor, and
    authentication-authorization README lines 33-34 already state that SP is an attribute and not a
    synonym for RM. The origination correction resolves a live contradiction between
    CURRENT-STATE.yaml in_scope and the deferral in ws3-platform/03 section 3, and the ratified
    state file wins. Confidence is C4 rather than C5 only because the exact assist-only action set
    is a compliance determination that has not been made (OPEN-D9); the gate ships default-deny
    until it is.

overrides_claimed:
  - id: O3
    claim: "Incorrect domain model"
    evidence: >
      ws3-platform/02 section 4.2 recorded lob = TERM, conflating the line of business with the
      product class, against ws3-platform/01 INV-QUO-01 which gates on lob and the S03 acceptance
      criterion AC-LEAD-010-1 which reads 'LOB LIFE'. Also 15 section 4 listed CERTIFIED_SP as an
      actorType and a channel while 15 ID-20 in the same file states that certification is an
      authorization attribute.
    status: >
      Recorded as evidence for necessity and priority, NOT used to interrupt an in-flight item —
      no item was in flight. Nothing is implemented, so the defect is in the model rather than in
      behaviour; stated here so the claim is auditable rather than inflated.

action:
  verdict: ADMIT-BYPASS
  rationale: >
    Implemented in the turn it was raised, which the one rule normally forbids. Recorded as
    ADMIT-BYPASS rather than ADMITTED because it was a direct instruction from the owning human
    authority for the artefact under review, following the precedent set by SUG-20260816-d8v and
    SUG-20260820-n5t. The bypass and its risk are stated: the risk is that four architecture
    decisions with Security, Compliance, Product and Database consequences are recorded without
    their boards having met. Each ADR names the approvals it still requires, and none of them
    becomes binding because this branch merges.
  priority_now: P1
  priority_at_target: P1
  type: ARCH
  risk_tier: T4
  score:
    N: 4
    S: 4
    B: 3
    R: 3
    D: 2
    E: 2
    formula: "2N + 2S + 2B + 2R + D - E"
    total: 28
    band: P1
    pri8_note: "SF0 sets the B floor at 2; B is 3 because S08-G4 is a gate criterion this blocks"

decisions_taken:
  - id: ADR-004
    decision: >
      Two R0 actors. Specified Person is certification state on the BANK_RM principal, evaluated at
      the action and not at login; CERTIFIED_SP is removed as an actor type and as a channel value.
      INSURER_PARTNER_REP is a partner-plane principal, assist-only, with the accountable SP
      immutable and always the originating RM, visibility gated on completed need analysis and
      suitability, insurer scoping applied as a mandatory persistence-layer predicate, out-of-scope
      records absent from result sets rather than refused by identifier, and every partner action
      audited with its acting capacity.
  - id: ADR-005
    decision: >
      The opportunity is the single origination point, creatable only by a BANK_RM. Context #5
      moves from deferred-to-S13 into R0 Wave 1, reconciling ws3-platform/03 with CURRENT-STATE
      in_scope. Every downstream aggregate carries the originating reference. Campaign and bulk
      sales-management breadth stays deferred.
  - id: ADR-006
    decision: >
      lob is mandatory and non-null on every business entity, configuration record, audit event and
      authorization request from the first migration; the vocabulary is frozen at LIFE, HEALTH and
      GENERAL; lob and productClass are separate dimensions. Partitioning is not forking — party,
      opportunity, consent evidence, journey identity, payment, documents, portfolio and audit stay
      shared.
  - id: ADR-007
    decision: >
      The configuration layer ships in R0 as a Wave 0b component — LOB-partitioned, append-only,
      versioned, effective-dated, seeded from source-controlled artefacts, resolved through a port,
      with no compiled-in fallback and no business branch on an insurer, product, LOB or channel
      literal. Explicitly independent of front-end availability; the admin UI stays deferred. This
      withdraws the earlier trade under which a rule-pack change required a deployment.

not_included:
  - "any change to CURRENT-STATE.yaml scope, stage, gate state or standing_constraints. Section 7.5 of the registration document carries the five constraint lines for the orchestrator to transcribe. The ONE edit made to that file is id_allocation.sequential.ADR, advanced from 1 to 8: ADR-001..003 already existed and ADR-004..007 are indexed in the decision register, which FreshnessCheck scans, so leaving the counter behind put the repository into HALT and blocked every agent. It is an ID-allocation correction, not a stage or scope edit"
  - "T4 Architecture sign-off, and the Security, Compliance, Product and Database approvals each ADR names"
  - "the assist-only threshold itself — which assistance actions stop short of solicitation (OPEN-D9, Shailja)"
  - "renaming context #5 from Lead to Opportunity in Product-owned artefacts (OPEN-D10, Rajal)"
  - "physical partitioning of the lob dimension per store (OPEN-I6, Aarti)"
  - "any code, migration or seed artefact — this is design; implementation is S08/S09 work under GATE-S08"
  - "a commission service. Commission is a reserved configuration namespace with no consumer until R1"

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-hr0
  status: ADMIT-BYPASS
  evidence:
    - "docs/platform/ws3-platform/01-domain-model-and-invariants.md — sections 2.4, 2.5, 2.6; INV-ACT-01..04, INV-LED-04..07, INV-CFG-01..03, INV-LOB-01/02; OPEN-D9..D11"
    - "docs/platform/ws3-platform/02-information-model.md — lob corrected to LIFE with productClass separated; opportunity, configuration and audit-attribution sheets"
    - "docs/platform/ws3-platform/03-solution-architecture-r0.md — sections 2.1, 2.2; Wave 0b configuration and Wave 1 opportunity; seams S-20..S-22; FF-16..FF-21"
    - "docs/platform/ws3-platform/04-security-architecture.md — four principal classes; partner gating, scoping and attribution controls"
    - "docs/platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md — SC-W3-8..SC-W3-12 and the section 7.5 transcription block"
    - "docs/platform/architecture-review/08-architecture-decision-log.md — ARCH-023..027 and ADR-004..ADR-007"
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/15-actor-identity-and-authorization.md — ID-15a, ID-15b; CERTIFIED_SP removed from the actorType and channel enumerations"
    - "docs/architecture/r0-reference-architecture.svg — R0 view reconciled (HA-03, HA-06)"
    - "scripts/governance/ci-checks.py — PASSED"
    - "java scripts/governance/FreshnessCheck.java — FRESH"
  closed_reason: null

resumed: null
```

---

### SUG-20260820-al7 · HLD and R0 diagram alignment

```yaml
id: SUG-20260820-al7
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  Verify that the HLD and the R0 diagram are in line — naming convention, nomenclature and the
  layer model already decided in the HLD. The R0 view should be a mirror image of the HLD that
  simply shows what release zero covers, and nothing more. Additionally, because delivery starts
  with the Life module only, the Life-specific modules must be grouped into a distinct colour or
  box so that it is visible which modules belong to a line of business and which are generic or
  shared.

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
  code: SF1
  rationale: >
    Not new architecture. The two diagrams and the authoring protocol that governs them were
    revised four days ago under SUG-20260820-n5t and SUG-20260820-hr0, and the verification
    finds defects introduced by those same two changes: the North Star carries release chips
    that contradict the ratified R0 scope, and the authoring protocol still documents a canvas
    contract for a file whose contents moved. HA-06 requires the R0 view to be reconciled in
    the same change as the target-state view; that reconciliation was partial. Correcting a
    defect in the current stage's own design artefact is on-stage work, not a new increment.

scope:
  code: SC1
  serves: [SUG-20260820-n5t, SUG-20260820-hr0]
  rationale: >
    The R0 reference architecture is an in-scope deliverable and it is unusable in its current
    form for the purpose it exists for. Two diagrams that disagree about which release a
    bounded context belongs to give the reader two answers with no way to tell which is
    current — the exact failure HA-02 and HA-06 exist to prevent. No new capability, service or
    scope is added by this item.

necessity:
  verdict: MUST
  evidence_tier: E2
  confidence: C5
  rationale: >
    The defects are objectively checkable against ratified sources, not matters of taste:
    03-solution-architecture-r0.md section 3 places #5 Opportunity in Wave 1 and #19
    Configuration in Wave 0b of R0, and section 7 defines FF-01..FF-21. The North Star
    contradicts all three.

action: ADMIT
priority:
  now: P2
  at_target: P2
  rationale: >
    Not a P1 override — nothing is broken at runtime and no gate criterion is blocked. P2
    because delivery reads these diagrams when sequencing S08/S09 work, and a wrong release
    chip on #5 and #19 is read as permission to defer two components that R0 depends on.

work_type: ARCH
risk_tier: T2

findings:
  - id: AL-1
    severity: HIGH
    where: "docs/hdl.svg — Boundary 4"
    finding: >
      #5 Lead -> Opportunity carries an R1 release chip. 03-solution-architecture-r0.md section 3
      un-deferred it into Wave 1 of R0 (AC-8, AC-9) because CURRENT-STATE in_scope already lists
      it, and the North Star's own R0 roadmap band lists "Lead (#5)". The diagram contradicts
      both its source and itself.
    resolution: "chip corrected to R0; the R1 text now names only the parts that are R1 — bulk upload, allocation, campaign management"
  - id: AL-2
    severity: HIGH
    where: "docs/hdl.svg — Boundary 10"
    finding: >
      The configuration plane carries an R1 chip and no context number. CF-5 and
      03-solution-architecture-r0.md section 3 ship context #19 as a Wave 0b service in R0 —
      only its admin UI is deferred. Drawn as R1, the layer every other R0 wave resolves its
      rules from appears to be next-release work.
    resolution: "chip corrected to R0, numbered #19, and the R0/R1 split stated on the element: layer in R0, maker-checker governance and admin UI at R1"
  - id: AL-3
    severity: MEDIUM
    where: "docs/hdl.svg — Boundary 10, CI/CD element"
    finding: "asserts 15 fitness functions; the catalogue is FF-01..FF-21 since SUG-20260820-hr0 added FF-16..FF-21"
    resolution: "corrected to 21"
  - id: AL-4
    severity: MEDIUM
    where: "both diagrams"
    finding: >
      Seven contexts are drawn under two different names with no mapping between them:
      #4 Customer / Party-Customer, #7 Suitability / Suitability framework, #8 Product Catalogue /
      Product Governance & Catalogue, #9 Journey Orchestration / Journey Registry, #10 Quotation /
      Life Quote, #11 Proposal & UW / Life Proposal & Case Mgmt, #13 Policy & Issuance / Policy
      Portfolio & Registry. Four of them carry no #n at all on the North Star element, so a reader
      cannot match the box to the register. The target-state names are deliberate — several
      contexts split or widen by RN — but an undeclared rename reads as a different service.
    resolution: >
      Naming rule NC-1 added to the authoring protocol: the #n is the identity and is mandatory on
      every element in both files; the canonical register name is always shown; where the target
      state renames or splits the context, it is rendered as "#n Canonical -> target name" so the
      evolution is explicit. Applied to both diagrams.
  - id: AL-5
    severity: HIGH
    where: "docs/architecture/r0-reference-architecture.svg — whole layout"
    finding: >
      The R0 view is organised by build wave and journey flow; the North Star is organised into
      ten described boundaries. They share a colour vocabulary and a context register but not a
      structure, so the R0 view cannot be read as a release-zero cut of the target picture — which
      is the one job the pair exists to do. Concretely: #10 and #11 sit in a flat row beside #6,
      #12 and #16, giving no hint that the North Star holds Quote and Proposal to be per-LOB and
      that boundary frozen.
    resolution: "R0 view redrawn on the North Star's boundary bands 1-10, each band carrying what R0 contains and an explicit note where a band is thin or empty in R0"
  - id: AL-6
    severity: HIGH
    where: "docs/architecture/r0-reference-architecture.svg"
    finding: >
      Nothing distinguishes LOB-owned execution from shared platform. LB-4 and LB-5 draw the line
      precisely — the rules are partitioned, the evidence is not — and the diagram renders every
      R0 service in wave colour, so a reader planning Health cannot see which boxes get a second
      instance and which never do.
    resolution: >
      Three-class LOB classification rendered: LIFE CELL (LOB-owned execution, #10 and #11, the
      frozen per-LOB boundary), LOB-partitioned shared services (shared code, configuration keyed
      by lob, per CF-2), and LOB-agnostic shared mechanics (single-instance for every LOB, per
      LB-5). New colour token added to the legend in the same edit (HA-08).
  - id: AL-7
    severity: MEDIUM
    where: "docs/context/roles/mahesh-principal-insurance-platform-architect/16-hld-authoring-and-update-protocol.md"
    finding: >
      Root cause of AL-1..AL-6. The authoring protocol still says docs/hdl.svg is horizon H0 —
      R0 as designed — and its canvas contract in section 4 documents the R0 geometry. Since
      SUG-20260820-n5t, hdl.svg holds the North Star and the R0 view lives at
      docs/architecture/r0-reference-architecture.svg. There has been no convention covering two
      files, which is why two files drifted. Its checklist also still ranges seams to S-19 and
      fitness functions to FF-15.
    resolution: >
      Protocol extended to a two-file family with a shared naming rule (NC-1), a shared layer
      model (LY-1: the ten boundaries are the layer vocabulary for every horizon), the LOB
      classification rule (LB-R1), corrected ranges, and a reconciliation checklist that fails
      when the two files disagree about a release chip or a context name.

not_included:
  - "the name of context #5 in Product-owned artefacts — Lead vs Opportunity is OPEN-D10 and Rajal's call. Both diagrams keep the dual form '#5 Opportunity (Lead)' until that decision lands"
  - >
    the contradiction between ARCH-004 database-per-service, which the R0 view asserts, and the
    North Star's Boundary 8 position that physical splitting is scale-driven and R0 may start as
    separate schemas in a shared cluster. Both are drawn as written and the divergence is flagged
    for Mahesh and Aarti; an agent does not pick between a ratified decision and a target-state
    position on a matter with cost, DR and DBA consequences. Raised as OPEN-A1 below
  - "T4 Architecture sign-off. The signature status on both diagrams is unchanged (HA-10)"
  - "any change to CURRENT-STATE.yaml, to scope, stage or gate state"
  - "any code, migration or seed artefact"

open_decisions:
  - id: OPEN-A1
    owner: "Mahesh (Architecture) with Aarti (Database)"
    question: >
      Does R0 start with one Aurora cluster holding a schema per context, or a cluster per
      service? ARCH-004 is Proposed and the R0 view asserts it as decided; the North Star
      asserts the opposite starting point. Until this is settled the two diagrams describe two
      different R0 data topologies.

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-al7
  status: ADMITTED
  evidence:
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/16-hld-authoring-and-update-protocol.md — two-file artefact family, NC-1, LY-1, LB-R1, corrected ranges, reconciliation checklist"
    - "docs/hdl.svg — AL-1, AL-2, AL-3, AL-4 corrected"
    - "docs/architecture/r0-reference-architecture.svg — redrawn on boundary bands; AL-5, AL-6 resolved"
    - "docs/architecture/README.md — the convention and the LOB reading rule stated for readers"
    - "java scripts/governance/FreshnessCheck.java — FRESH"
  closed_reason: null

resumed: null
```

---

### SUG-20260820-dc4 · Data topology and the name of context #5

```yaml
id: SUG-20260820-dc4
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  There is no database per service. As per the North Star's boundary, splitting is scale-driven,
  and R0 may start as a schema in one cluster; afterwards, based on the requirement, we can split
  the clusters for the line of business and the shared resources. Also lead or opportunity — I
  would go with opportunity, because a lead is too thin to identify, whereas an opportunity is
  something which can be converted for a new sale, for a renewal and for a lapse. It has a larger
  scope.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  freshness_check: "exit 0 — FRESH"

stage_fit:
  code: SF1
  rationale: >
    These resolve two open decisions raised against the current stage's own design artefacts —
    OPEN-A1 from SUG-20260820-al7 and OPEN-D10 from SUG-20260820-hr0 — on a design that is not yet
    signed and against which no service, migration or seed exists. Deciding the physical topology
    now is also the cheap moment: it is a documentation change today and a data migration across
    every table once R0 has run.

scope:
  code: SC1
  serves: [SUG-20260820-al7, SUG-20260820-hr0]
  rationale: >
    No capability, service or scope is added. Both items remove a contradiction inside deliverables
    already in scope: two diagrams asserting two different R0 data topologies, and one bounded
    context carrying two names.

necessity:
  verdict: MUST
  evidence_tier: E1
  confidence: C5
  rationale: >
    E1 — a decision by the accountable architect, and the one the repository was already waiting
    for. The topology half was drafted five days ago in 09-target-state-architecture-doctrine.md
    section 5.2 and listed as open item 1 in 10-north-star-capability-model.md; it needed a
    decision, not analysis. The naming half is the open question ADR-005 records as OPEN-D10.

action: ADMIT
priority: {now: P2, at_target: P2}
work_type: ARCH
risk_tier: T3

decisions:
  - id: OPEN-A1
    resolution: >
      ARCH-004 bundled three claims and only two of them are principles. One owner per
      authoritative datum with no cross-service table access, and separate credentials and schema
      ownership per service, remain INVARIANT and enforced. A separate physical cluster per service
      is a DECISION, evidence-led on scale, blast radius, security isolation, RTO/RPO and cost. R0
      starts with one Aurora cluster and a schema per context. The first split, when evidence
      justifies it, follows the LOB-cell / shared-platform seam — not the service boundary.
    recorded_as: ADR-008
    supersedes: "ARCH-004 (physical-topology half only; the ownership half is retained and restated)"
    note: >
      This is the reconciliation Mahesh had already written into
      09-target-state-architecture-doctrine.md section 5.2 and had deliberately not applied
      unilaterally. What the instruction adds beyond that draft is the split AXIS: LOB cell versus
      shared platform, which is what the North Star's boundary 8 already draws and what LB-5 makes
      the natural seam.
  - id: OPEN-D10
    resolution: >
      Context #5 is named Opportunity. The rationale is domain scope, not preference: a lead
      records that someone might buy, and dies at conversion. An opportunity is the durable demand
      object behind a new sale, a renewal, a lapse recovery, a cross-sell and an
      abandoned-journey recovery — which is exactly the R2 rule that a renewal or lapse creates a
      NEW opportunity and a NEW journey rather than reopening an old one. Naming the context Lead
      makes that rule read as a contradiction; naming it Opportunity makes it read as the model.
    recorded_as: "ADR-005, naming_resolution block"

not_included:
  - >
    CURRENT-STATE.yaml current_scope.in_scope line 85 still reads "Lead service (context #5) —
    create, resume, status", and WS-3-PLATFORM-CHARTER.md line 301 mirrors it. Both are
    human-owned scope text and an agent does not edit them (04 section 5). Flagged for Kalpana /
    R12 to transcribe, with Rajal's Product confirmation of the label
  - >
    identifier and register-ID renames. leadId, INV-LED-01..07 and CAP-102 keep their tokens: an ID
    is opaque, and rewriting seven invariant IDs across the corpus is churn that breaks every
    existing citation for no gain. The NAME changes; the IDs do not
  - "Aarti's Database approval of ADR-008 and Rajal's Product confirmation of the #5 label — required, and outstanding"
  - "T4 Architecture sign-off. Signature status on both ADRs and both diagrams is unchanged (HA-10)"
  - "any physical schema, migration or seed artefact — this is design; implementation is S09 work"

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-dc4
  status: ADMITTED
  evidence:
    - "docs/platform/architecture-review/08-architecture-decision-log.md — ADR-008 added; ARCH-004 qualified; ADR-005 naming_resolution; signature block extended"
    - "docs/platform/architecture-review/05-data-architecture.md — governing rule restated as ownership plus an evidence-led topology decision"
    - "docs/platform/ws3-platform/03-solution-architecture-r0.md — database row and build-order row updated"
    - "docs/platform/ws3-platform/04-security-architecture.md — threat I control restated without asserting physical separation"
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md section 5.2 — reconciliation marked applied"
    - "docs/hdl.svg and docs/architecture/r0-reference-architecture.svg — boundary 8 reconciled; OPEN-A1 note removed; #5 renamed"
    - "scripts/governance/ci-checks.py — PASSED · java scripts/governance/FreshnessCheck.java — FRESH"
  closed_reason: null

resumed: null
```

### SUG-20260820-hl1 · R0 stakeholder HLD and AWS LLD pack

```yaml
id: SUG-20260820-hl1
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction — Act as Mahesh; use r0-reference-architecture.svg as the HLD reference; produce a detailed HLD and an LLD for the CTO and AWS platform team"
input: >
  Act as mahesh, use the R0 reference architecture SVG as HLD for R0, and create an HLD
  design document which will have detailed domain, boundary, communication, API details,
  business logic and understanding of the complete R0 and its phases, waves, what to do
  when. Use the HLD for R0 and create the LLD which will have e2e component, services,
  aws component, pvc, external proxy or reverse proxy, db, caching, designed so it is
  easy to give the CTO and AWS platform team the requirement for aws platform and
  services needed.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null
stage_fit:
  code: SF1
  rationale: >
    On-stage for WS-3. GATE-S08 is open and S09 is overlapped; the current deliverable
    includes IaC, environments, secrets and observability. A stakeholder HLD that walks
    the already-ratified R0 picture, and an LLD that is the S09 AWS bill of materials,
    are the artefacts that make that deliverable executable. Not SF3: this is not the
    parked R0→R1→R2 dependency map (SUG-20260820-r1t).
scope:
  code: SC0
  business_scope: "in scope — R0 architecture of the admitted WS-3 slice"
  serves: []
  failure_without_it: >
    S09 cannot be requested from the AWS platform team without a narrowed BOM; the SVG
    alone is not a provisioning contract, and architecture-review/04 still names Kafka,
    ElastiCache and per-service clusters that R0 has explicitly declined.
  minimal: true
  authority: "CURRENT-STATE.yaml WS-3 current_scope; 03-solution-architecture-r0.md; ADR-001; ADR-008"
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  binds_when: "first Terraform apply against a non-dev account"
  evidence_tier: E2
  evidence:
    - "S09-E01 network, compute and data foundation stories"
    - "ADR-001 Terraform / ap-south-1 / Render.com boundary"
    - "ADR-008 one Aurora cluster, schema per context"
  confidence: C4
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true   # CTO + AWS platform team + Shivanshi S09
    X3_cheap_later: false     # provisioning the target-state estate now is the expensive mistake
    X5_stage_necessity: true
    X9_problem_observed: true # 04-aws-infrastructure-architecture.md still reads as the R0 BOM
action: ADMIT-BYPASS
action_rationale: >
  Direct instruction from the Board 1 Architecture authority to produce the artefacts in
  this turn. Bypass records that seven-board review of the *plan* was skipped; the
  documents themselves carry AI-DRAFTED status and name the outstanding human T4
  Architecture, Security, Database and SRE signatures. Risk of the bypass: an AWS LLD
  that will drive S09 provisioning has not yet had Deepali / Aarti / Shivanshi boards.
  No new architectural decision is asserted; Kafka, Redis-for-idempotency, per-service
  clusters and Istio remain out of R0 as already recorded.
duplicate_of: null
conflicts:
  - "architecture-review/04 names MSK, ElastiCache, Istio, per-service RDS — R0-LLD §1.3 lists them DO NOT PROVISION, citing 03 §5.1 and ADR-008"
classification:
  type: ARCH
  also: [DOC, INFRA]
  risk_tier: T4
  security_impact: trust-boundary-realisation   # LLD restates TB-1..TB-6; does not change them
  compliance_impact: residency-and-WORM-restated
  operational_impact: S09-provisioning-input
priority:
  now: P2
  at_target: P1
  factors: "S09 overlapped; GATE-S08 still the in-flight engineering gate"
  caps: []
dependencies:
  - "Authoritative sources already in ws3-platform/ 00–05 and ADR-001…008"
  - "Does not unpark SUG-20260820-r1t"
plan_files:
  - "docs/architecture/R0-HLD.md"
  - "docs/architecture/R0-LLD.md"
bypass:
  authorised_by: "human:Mahesh — direct instruction to act as Board 1 and produce the HLD and LLD"
  skipped: "implementation-plan template; seven-board review of the plan before drafting"
  risk: "AWS LLD may be cited as S09 input before Security, Database and SRE have signed"
  non_negotiable_touched: false   # no secrets, no public contract change, no data-integrity change — presentation of accepted decisions
```

### SUG-20260820-ls1 · R0 LLD SVG rendering

```yaml
id: SUG-20260820-ls1
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "follow-up on SUG-20260820-hl1 — create SVG for the LLD"
input: >
  can you create svg for LLD ?
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: SUG-20260820-hl1
stage_fit:
  code: SF1
  rationale: "Same on-stage S09 input as hl1. The LLD prose exists; this is its rendering (HA-03 source first, diagram second)."
scope:
  code: SC0
  business_scope: "in scope — R0 AWS deployment picture"
  serves: ["SUG-20260820-hl1"]
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
action: ADMIT-BYPASS
action_rationale: >
  Direct follow-up to produce the LLD picture in this turn, same executor lane as hl1.
  No new AWS service is named. Dashed nodes are the existing DO NOT PROVISION list.
duplicate_of: null
continues: SUG-20260820-hl1
classification:
  type: ARCH
  also: [DOC]
  risk_tier: T4
priority:
  now: P2
  at_target: P1
bypass:
  authorised_by: "human:Mahesh — follow-up instruction"
  skipped: "seven-board review of the plan"
  risk: "same as hl1 — SVG may be shown to AWS platform team before Security/Database/SRE sign"
  non_negotiable_touched: false
```

---

### SUG-20260820-pt9 · AWS platform-team application view — AZ, DR and sequence

```yaml
id: SUG-20260820-pt9
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "follow-up on SUG-20260820-hl1 / SUG-20260820-ls1, with a reference deployment diagram attached"
input: >
  Use the architecture diagram, HDLD, LDLD diagram we have for our application, and create a
  similar kind of application diagram for the platform team, so that the AWS platform team can
  know what kind of application we are building, what all services we need, in which availability
  zone we want, what DR services we want, how proxy services are required, and when.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH, 2026-08-20"
  active_work_item: SUG-20260820-ls1
stage_fit:
  code: SF1
  rationale: >
    S09 — Platform & Environment Foundation is the next stage and is already overlapped into the
    current phase. S09-E01-S03 (network foundation across AZs), S09-E01-S05 (data foundation) and
    S09-E06-S03/S04 (backup and proven restore) are exactly the questions this asks. The request
    is the S09 entry artefact, not new scope.
scope:
  code: SC0
  business_scope: "in scope — R0 AWS deployment picture for the platform team"
  serves: ["SUG-20260820-hl1", "SUG-20260820-ls1"]
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
  rationale: >
    S09 entry criterion "cloud account structure and budget approved" cannot be met without a
    request the platform team can price and provision from. R0-LLD.md answers what and how much,
    but three of the six questions asked here are not answered anywhere: per-resource
    availability-zone placement, the DR bill of materials as a resource list, and the order in
    which each resource is needed.
gap_analysis:
  already_answered:
    - "what the application is — R0-HLD.md §1-§3"
    - "service inventory — R0-LLD.md §12, 03-solution-architecture-r0.md §3"
    - "reverse proxy chain — R0-LLD.md §3 (two-hop: API Gateway then internal ALB)"
  not_answered_before_this_item:
    - "availability-zone placement per resource — sources say '3 AZs' and 'min 2 AZ', never which resource sits where"
    - "DR as a bill of materials — R0-LLD.md §11 states the posture, not the ap-south-2 resource list"
    - "when — no mapping from an AWS resource to the S09 story that builds it and the wave that first consumes it"
action: ADMIT-BYPASS
action_rationale: >
  Direct human follow-up in the same executor lane as hl1/ls1. HA-03 is honoured: the three gaps
  are closed in R0-LLD.md first (§2.1, §11.1, §12.1) and only then rendered. No AWS service is
  introduced that §1.1/§1.2 does not already name, and the DO NOT PROVISION list is carried
  through unchanged, so this cannot become scope drift.
duplicate_of: null
continues: SUG-20260820-ls1
classification:
  type: ARCH
  also: [DOC]
  risk_tier: T4
priority:
  now: P2
  at_target: P1
dependencies:
  blocks: ["S09-E01-S03 network foundation", "S09-E01-S05 data foundation", "S09 entry — cloud account structure approved"]
  blocked_by: ["Direct Connect / VPN / bank-proxy decision for CBS and Bank AD — Shivanshi + bank network (R0-LLD §14)"]
bypass:
  authorised_by: "human:Mahesh — direct instruction"
  skipped: "seven-board review of the plan"
  risk: >
    Same as hl1 and ls1 — the view may be handed to the AWS platform team before Deepali
    (Security), Aarti (Database) and Shivanshi (SRE) have signed. The AZ placement and DR resource
    list are architecture constraints, not sizing decisions; every SKU, instance class and
    Aurora-Global-versus-restore choice stays tagged DECIDE WITH.
  non_negotiable_touched: false
notes:
  - "Does not unpark SUG-20260820-r1t (the R0→R1→R2 transition map)"
  - "Does not alter GATE-S08; S08 remains the gate in flight"
```

---

### SUG-20260820-ic3 · Icon notation, generated from code

```yaml
id: SUG-20260820-ic3
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "review of SUG-20260820-pt9's rendering"
input: >
  I'm not looking for all those boxes. I'm looking for the actual images or the logos — when you
  are using a Kubernetes cluster it should show that this is the Kubernetes cluster, there are
  microservices communicating, there is CloudFront, there is an RDS service. Can you think of a
  better approach than SVG, without importing a lot of external images?
context:
  workstream: WS-3
  canonical_stage: "S08 — Engineering Foundation"
  active_work_item: SUG-20260820-pt9
  freshness_check: "exit 0 — FRESH, 2026-08-20"
stage_fit:
  code: SF1
  rationale: "Same S09 artefact as pt9. Notation change, not a content change."
scope:
  code: SC0
  serves: ["SUG-20260820-pt9"]
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
  rationale: >
    The audience reads AWS diagrams daily. Labelled rectangles make them translate before they can
    review, which is friction on the artefact whose whole purpose is to be reviewed by that team.
decision:
  chosen: "mingrammer/diagrams — Python, rendered through Graphviz"
  why: >
    The official AWS, Kubernetes, Argo and Flutter icon sets ship inside the pip wheel, so no image
    is vendored into this repository and nothing is fetched at render time. Being code, the picture
    changes in the same commit as its source and a reviewer sees which sentence changed — which is
    what HA-03 asks for and what a binary canvas file cannot give.
  rejected:
    - "draw.io / Lucid — right icons, but a binary-ish canvas: no useful diff, and the picture drifts from its source"
    - "Mermaid architecture-beta — icon packs resolve over the network at render time"
    - "D2 — icons are external URLs"
    - "hand-authored SVG with embedded base64 icons — vendors the icon set and is slow to change"
  output_format: >
    PNG. Graphviz can emit SVG but references icons by absolute local path, so the SVG is not
    portable. Recorded so nobody 'fixes' the format later.
action: ADMIT-BYPASS
action_rationale: >
  Direct human follow-up in the same executor lane as pt9. No architectural content changes: the
  five diagrams render R0-LLD.md §2.1 / §11.1 / §12.1, which pt9 already added and which remain the
  source of truth (HA-02).
duplicate_of: null
continues: SUG-20260820-pt9
supersedes_artefact: "docs/architecture/r0-platform-topology.svg (deleted — replaced, not kept alongside, to avoid two answers in the repository)"
classification:
  type: DOC
  also: [ARCH]
  risk_tier: T4
priority:
  now: P3
  at_target: P2
new_repository_dependency:
  runtime: "python3 + graphviz (dot) + pip diagrams==0.25.1"
  scope: "documentation build only — not a service dependency, not in any container image"
  recorded_at: "docs/architecture/diagrams/requirements.txt"
defect_found_and_fixed:
  what: "the first render placed an Aurora WRITER in all three AZs"
  cause: "zone test was `\"A\" in zone`, and \"A\" is a substring of \"AVAILABILITY\""
  why_it_matters: "a diagram asserting a Multi-AZ topology the design does not have is worse than no diagram"
bypass:
  authorised_by: "human:Mahesh — direct instruction"
  skipped: "seven-board review of the plan"
  risk: "same as pt9 — the views may be shown to the AWS platform team before Security, Database and SRE sign"
  non_negotiable_touched: false
notes:
  - "HA-10 added to the authoring protocol: notation follows the audience; generate rather than draw"
```

---

### SUG-20260820-lay4 · Deterministic orthogonal layout

```yaml
id: SUG-20260820-lay4
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "review of SUG-20260820-ic3's rendering"
input: >
  The designs look better now. The only problem is that they are not well aligned, not well
  positioned, and not correctly linked. The links move randomly here and there, crossing and
  curving. They should be straight lines, diverted at ninety degrees only, with the blocks and
  logos well balanced on the image.
context:
  workstream: WS-3
  canonical_stage: "S08 — Engineering Foundation"
  active_work_item: SUG-20260820-ic3
  freshness_check: "exit 0 — FRESH, 2026-08-20"
stage_fit: {code: SF1, rationale: "Same S09 artefact. Presentation change, not a content change."}
scope: {code: SC0, serves: ["SUG-20260820-ic3", "SUG-20260820-pt9"]}
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
  rationale: >
    A platform team reads placement in these views as a specification. Curved and crossing
    connectors make the reader re-derive which line goes where, which is the same friction the
    icon change was meant to remove.
decision:
  chosen: "hand-rolled svgcanvas.py — explicit coordinates, axis-aligned connector segments"
  supersedes: "the mingrammer/diagrams + Graphviz layout choice recorded under SUG-20260820-ic3"
  retained_from_ic3: >
    The icon assets. The diagrams wheel is still the dependency, but only as the source of the
    official AWS and Kubernetes art — none of its layout code is used.
  rejected:
    - what: "Graphviz splines=ortho"
      why: >
        TESTED, not assumed. It does emit 90-degree lines, but it detaches edge labels from their
        edges and routes connectors straight through cluster borders, and node positions remain the
        engine's choice rather than a deliberate grid.
    - what: "tuning the Graphviz ranks further"
      why: "a layered layout engine cannot be argued into a fixed grid; it re-ranks on every change"
    - what: "draw.io / Lucid"
      why: "already rejected under ic3 — no useful diff, and the picture drifts from its source"
  output_format: >
    SVG, with the icons embedded as base64 so the file is self-contained, plus a PNG companion for
    tools that will not take an SVG. This reverses ic3's PNG-only decision, which existed only
    because Graphviz's SVG referenced icons by absolute local path.
action: ADMIT-BYPASS
action_rationale: >
  Direct human follow-up in the same executor lane. No architectural content changed: the same five
  views render the same R0-LLD.md sections.
continues: SUG-20260820-ic3
classification: {type: DOC, also: [ARCH], risk_tier: T4}
priority: {now: P3, at_target: P2}
new_repository_dependency:
  runtime: "pip diagrams==0.25.1 (icon assets only) + cairosvg==2.7.1 (optional PNG companion)"
  removed: "graphviz — no longer needed, no layout engine is used"
  scope: "documentation build only — not a service dependency, not in any container image"
defects_found_and_fixed:
  - what: "vertical connectors were drawn straight through their own node's caption"
    cause: "a bottom port started at the icon edge, but the label hangs below the icon"
    fix: "Node.port('B') clears the label block — fixed in the canvas, not per diagram"
  - what: "edge labels rendered as white smears"
    cause: "the white halo relied on the SVG paint-order property"
    why_it_matters: >
      cairosvg and older librsvg ignore paint-order, so the labels would have failed in exactly
      the viewers a platform team is most likely to open the file in. Labels now sit on a real plate.
bypass:
  authorised_by: "human:Mahesh — direct instruction"
  skipped: "seven-board review of the plan"
  risk: "same as pt9 and ic3 — the views may be shown before Security, Database and SRE sign"
  non_negotiable_touched: false
notes:
  - "HA-10 extended: generating a diagram does not mean handing its layout to an engine."
```

### SUG-20260821-jx1 · R0 Journey Execution Specification

```yaml
id: SUG-20260821-jx1
raised_at: "2026-08-21"
raised_by: "human:Mahesh"
source: "direct user instruction — comprehensive end-to-end application document for the dev team"
input: >
  A comprehensive document holding all the use cases of each actor, each request, each
  response, how the request routes from one service to another and under what condition,
  what validation is done at each service layer, the final output, the external API calls
  — everything. Example given: RM login traverses CloudFront, then WAF, then the RM BFF,
  which routes to authentication, which calls the SSO; and inside that, every validation
  and check it performs. The same treatment for the whole application, end to end, with
  the possible outcomes and the algorithm each validation follows, so the dev team can
  build from it.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null
stage_fit:
  code: SF1
  rationale: >
    On-stage, and partly a backfill of stages already passed. The documentation canon
    names three of the requested artefacts as canonical and marks them absent: the S05
    service blueprint and the S05 error / empty / degraded-state catalogue are RED, and
    the S03 requirements traceability matrix is RED
    (05-DOCUMENTATION-CANON.md sections S03, S05). GATE-S08 criteria G6 (test
    infrastructure at every pyramid level), G8 (engineering standards adopted) and G10
    (a new engineer can build, test and ship in a week) all consume a per-request
    specification that does not exist today. It is the named input to S11 slice
    definition. Not SF3: the information exists now and is already ratified — this
    assembles it, it does not invent it.
scope:
  code: SC1
  business_scope: "in scope — the admitted R0 assisted term-sale slice only"
  serves:
    - "R0-ASSISTED-TERM-SALE"
    - "GATE-S08 criteria S08-G6, S08-G8, S08-G10"
    - "S11 vertical-slice definition and its E2E suite"
  failure_without_it: >
    The eight hard gates (C1 suitability, C2 consent, C3 distributorId, C4 payment device,
    C5 no PII in logs, C6 residency, C7 immutable evidence, C8 no inferred sale) are each
    stated in two or three separate authority documents, and no document states at WHICH
    layer each is enforced or with what algorithm. A developer implementing the quote
    endpoint today must infer whether C1 is checked at the BFF PEP, at the Quotation
    service, at the aggregate, or at all three — the HLD says "C1 via S-08", which is a
    seam reference, not an enforcement specification. The observed consequence is either
    a gate enforced only at the BFF (bypassable by any internal caller) or the same rule
    implemented three times with three different expiry semantics. Both are regulatory
    findings, not style problems.
  minimal: true
  scope_split: >
    The request as stated covers "the whole application". Most of the application is
    explicitly out of scope now: DIY / customer journey (R1), hybrid mode switching (R2),
    Group B insurers (R1), ULIP and Savings (R1), Customer BFF (R1), Health / Motor /
    Travel (R2+), renewals and servicing (R2+), admin UI (R1), reporting beyond the pilot
    funnel (R1). Writing their flows would manufacture design decisions Board 1 has not
    made. That half is split out as SUG-20260821-jx2 and parked.
  authority: >
    CURRENT-STATE.yaml WS-3 current_scope and out_of_scope_now;
    R0-HLD.md sections 4.2, 5.1, 5.3, 5.4, 6;
    ws3-platform/01-domain-model-and-invariants.md section 4;
    ws3-platform/03-solution-architecture-r0.md section 5;
    ws3-platform/04-security-architecture.md;
    platform/authentication-authorization/README.md sections 5, 8;
    05-DOCUMENTATION-CANON.md sections S03, S05, S11
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S11 — Vertical Slice (MVP)"
  binds_when: "the first S11 story that implements a hard gate is picked up"
  evidence_tier: E2
  evidence:
    - "05-DOCUMENTATION-CANON.md S05 — service blueprint and degraded-state catalogue both RED"
    - "05-DOCUMENTATION-CANON.md S03 — requirements traceability matrix RED"
    - "05-DOCUMENTATION-CANON.md S11 — slice definition RED, E2E suite RED"
    - "GATE-S08 S08-G6, S08-G8, S08-G10 all OPEN"
  confidence: C4
  assumptions:
    - "ASM-jx1-a: the R0-HLD contract sketches are stable enough to specify against. They are AI-DRAFTED with human T4 Architecture sign-off outstanding, so the specification inherits that status and cannot be cited as approved until R0-HLD is signed."
  anti_over_engineering:
    X1_named_consumer: true   # the dev team building S11, and QA deriving the E2E suite
    X3_cheap_later: false     # the cost lands as rework in code, once each gate is built wrong
    X5_stage_necessity: true
    X9_problem_observed: true # the enforcement layer for C1-C8 is unstated in every current document
action: ADMITTED
action_rationale: >
  Admitted as a document set, not as one file, and pending the author's choice of shape —
  the user asked for a recommendation before development, which is also what Rule
  09-AI_EXECUTION_RULES requires. Nothing is written in the turn the suggestion is raised.
  The proposed pack restates no authoritative content: each flow cites the source that
  owns the fact and adds only the assembly — hop order, enforcement layer, algorithm,
  outcome set. Status on delivery is AI-DRAFTED with human T4 Architecture and Security
  sign-off outstanding, matching R0-HLD.
duplicate_of: null
conflicts:
  - >
    The user's worked example says the BFF routes to an authentication service which calls
    the SSO. In the ratified design there is no separate authentication service: the
    workforce-access-bff owns the login, callback, session and logout endpoints itself
    (authentication-authorization/README.md section 4.1) and calls
    identity-provider-adapter-service, which is a provider-neutral port in front of
    Keycloak, which in turn federates to bank AD. Authorization is a separate hop to
    identity-authorization-service as PDP. The specification must document the ratified
    chain, and the divergence is itself evidence that the document is needed.

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
