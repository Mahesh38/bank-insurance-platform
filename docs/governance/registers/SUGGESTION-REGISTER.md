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
| SUG-20260811-r7k | 2026-08-11 | human:Mahesh (Solution Architect) | Split the monorepo into one repo per microservice, with a parent repo holding common documentation + governance that every service repo and every agent reads first | SF3 | SC2 | NOT-NOW | ARCH | P5 / P2 | ESCALATED | [CR-002](./DECISION-REGISTER.md#3-change-requests) · [plan](../../platform/repository-topology/README.md) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

<!-- ### SUG-0001 · <title>  ... full triage record ... -->

### SUG-20260811-r7k · Split the monorepo; parent repo for shared docs + governance

```yaml
# schema: triage-record
id: SUG-20260811-r7k
raised_at: "2026-08-11"
raised_by: "human:Mahesh (Solution Architect)"
source: "direct request to the cloud agent"
input: >
  We need to start with migration plan, we need to create new repository for each
  individual micro services. Also find way that all repo will refer to one parent
  repository for the documentation and the governance which are common accross
  services and each micro services repo will be able to access the documentation
  and even agents will refer to this documentation first then will work on the service.

context:
  workstream: "platform — spans WS-1 and WS-2; neither owns repository topology"
  current_phase: "WS-1 Phase 4 (Hardening) · WS-2 IAM Phase 1 (Foundation)"
  canonical_stage: "WS-1 L7 Hardening · WS-2 L4/L6 Foundation"
  current_objective: "WS-1: Term path signed off for UAT by at least one bank caller"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null

stage_fit:
  code: SF3
  rationale: >
    L7 Hardening's posture is "prove it, don't extend it", and RUNBOOK 8.3 lists new
    infrastructure as reject-on-sight at this stage. Three unmet GATE-P4 criteria run
    through the build a split would rewrite: 4.1 sandbox E2E in CI, 4.2 OpenAPI
    publication, 4.7 coverage gates (CR-001). WS-2 is at L4, where the posture is
    "build the floor, thinly" — not "re-plumb the repository estate". The need is real
    but belongs to L8 Expansion, when the service and team count grow toward the
    ~16-service target in ARCH-003.
  target_stage: "L8 — Expansion (WS-1 Phase 5+), after GATE-P4 has passed"
  unpark_trigger: >
    GATE-P4 PASSED, or the arrival of a second independent delivery team, or the
    creation of the 8th platform service — whichever occurs first

scope:
  code: SC2
  business_scope: >
    Repository topology appears in no authority document's in-scope list. WS-1's scope
    is the Term vertical slice and its hardening; WS-2's is workforce identity. No
    ARCH-xxx decision has ever ratified the repository topology, so there is no
    approved position to derive from either.
  serves: []
  failure_without_it: >
    Nothing in flight fails today. No in-scope deliverable is incorrect, unsafe,
    non-compliant or unusable because the code lives in one repository.
  minimal: false
  authority: "02-PROJECT_SCOPE.md sections 6-7; docs/README.md (context/ is non-binding)"

necessity:
  now: NOT-NOW
  future_necessity: SHOULD
  target_stage: "L8 — Expansion"
  binds_when: >
    A second delivery team needs independent release cadence, or the service count
    passes roughly eight and one settings.gradle.kts stops being a sane unit of review
  evidence_tier: E5
  evidence:
    - "ARCH-003 — target platform of ~16 services + 2 BFFs + routing layer, phased"
    - "19-PORTING_GUIDE section 3 — multi-team platforms keep one shared registers folder"
    - "CURRENT-STATE.yaml id_allocation — SUG/DEP already collision-resistant for parallel minting"
    - "No measured or reported monorepo pain exists in TECH-DEBT.md or the phase STATUS files"
  confidence: C4
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: false
    X2_two_implementations: null
    X3_cheap_later: false
    X4_reversibility: true
    X5_stage_necessity: false
    X6_simplest_sufficient: false
    X7_runtime_cost: true
    X8_cognitive_cost: true
    X9_problem_observed: false
    X10_do_nothing: true

action: ESCALATE
action_rationale: >
  The triage verdict on its own is PARK (SF3 + SC2 + NOT-NOW). It is escalated rather
  than parked for three reasons. (1) 14-CHANGE_CONTROL section 1 requires a CR to add
  something to project scope and to change any docs/governance/** file; this proposal
  does both, and moving the governance model to another repository is the largest GOV
  change the model can undergo. (2) SC2 forces a park regardless of necessity, and
  02-PROJECT_SCOPE section 8 states that scope changes happen only through change
  control — so the CR is the only legitimate route to admit it at all. (3) The request
  comes from the Architect, and an agent silently parking an Architect's direction
  would be substituting its own judgment for a human's on a scope question that belongs
  to the PO and the Architect jointly. CR-002 is raised with the full impact analysis
  required by 14 section 3 step 2; the agent does not approve it (Rule CC-1).
duplicate_of: null
conflicts:
  - >
    docs/context/business-problem-statement.md names the Gradle monorepo among the
    technical architecture decisions to uphold. Resolved: docs/README.md classifies
    context/ as non-binding, and no ARCH-xxx decision ratifies repository topology, so
    there is no approved decision to reverse — ARCH-023 would be the first.
  - >
    19-PORTING_GUIDE section 2 prescribes copying the framework into a target
    repository. Resolved: that is written for installing AIGEM into an unrelated repo.
    Section 3 already prescribes shared registers for a multi-team platform. If CR-002
    is approved, 19 needs a federated-consumption section — itself a GOV change.

classification:
  type: ARCH
  also: [MIGRATION, GOV, INFRA]
  breakdown: EPIC
  epic: null
  risk_tier: T4
  rationale: >
    Classified by the strictest review requirement (06 section 3): it changes topology
    and the governance model, adds a supply-chain component (artifact registry), and is
    hard to reverse after Wave 3. T4 mandates Architecture, Technical, Ops and Security
    boards.
  destination: "docs/platform/repository-topology/ — impact analysis for CR-002"

priority:
  now: P5
  at_target: P2
  factors: { N: 0, S: 0, B: 0, R: 1, D: 1, E: 3 }
  score: 0
  matrix_default: P5
  consistency: OK
  overrides_applied: []
  caps_applied: [PRI-2]
  rationale: >
    SCORE = 2(0) + 2(0) + 2(0) + 2(1) + 1 - 3 = 0, band P5, matching the SF3 x NOT-NOW
    matrix default exactly. R=1 (contained: cost grows with service count but nothing
    fails today). D=1 (moderate rework later — each new service built in the monorepo
    is one more to extract, though Wave 4 removes that decay entirely). E=3 (XL,
    multi-component). At L8 Expansion with a second team, N rises to SHOULD and S to
    SF1, giving P2.

dependencies:
  edges:
    - type: HARD
      target: "GATE-P4 (WS-1 Phase 4 exit criteria 4.1, 4.2, 4.7)"
      relation: blocked_by
      state: OPEN
    - type: TECHNICAL
      target: "TD-014 — E2E integration <-> persistence contract proof"
      relation: requires
      state: PARKED
    - type: EXTERNAL
      target: "Binary artifact registry decision (GitHub Packages vs AWS CodeArtifact)"
      relation: requires
      state: MISSING
      owner: "Tech Lead"
      follow_up: "2026-09-09"
    - type: DECISION
      target: "CR-002 / ARCH-023"
      relation: decision_dependency
      state: OPEN
  state: BLOCKED
  enablement_count: 0
  earliest_start: "After GATE-P4 for WS-1 services; Wave 0 needs only CR-002"
  parked_because: "Escalated pending CR-002; SF3 park stands if the CR is rejected or deferred"
  cycles: none

breakdown:
  children: []
  completion_definition: >
    Not broken down. Breakdown into work items happens only if CR-002 is approved —
    06 section 5 breakdown of an unapproved change is speculative work.
  not_included:
    - "Creating any repository"
    - "Moving any code or documentation"
    - "Choosing the artifact registry"
    - "Any change to service boundaries, contracts, or standing constraints"

outcome:
  registered_in: "docs/governance/registers/DECISION-REGISTER.md (CR-002)"
  work_item_id: null
  plan_id: null
  status: ESCALATED
  closed_reason: null

resumed: >
  No work item was in flight — this was the session's opening input. Nothing is picked
  up: WS-1's ordered READY queue is headed by the GATE-P4 criteria, and CR-002 must be
  decided before any part of this proposal becomes work.
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
