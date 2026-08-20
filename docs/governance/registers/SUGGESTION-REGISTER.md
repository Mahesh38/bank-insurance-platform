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
