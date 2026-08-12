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
| SUG-20260812-p1r | 2026-08-12 | human:PO | Process realignment: dual-track recovery for skipped L0–L2 stages; war room; new platform workstream | SF0 | SC4 | MUST | GOV | P1 / P1 | **ESCALATED** → [CR-002](./DECISION-REGISTER.md#3-change-requests) | [§3](#sug-20260812-p1r--process-realignment-dual-track-recovery) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

### SUG-20260812-p1r · Process realignment (dual-track recovery)

```yaml
# schema: triage-record
id: SUG-20260812-p1r
raised_at: "2026-08-12"
raised_by: "human:PO (Rajal, Platform Product Owner)"
source: "PO statement to the delivery team, 2026-08-12"
input: >
  The programme decided the problem statement, skipped BRDs and PRDs, designed screens, chose
  the tech stack, built five services, and only then started writing BRDs. Phase zero should
  have been analysis, guidelines, phase and scope definition, sub-phases and small deliverable
  chunks. Proposal: convene a war room with all stakeholders, identify the missed phases, and
  run a dual-track recovery — some developers on the correct sequence from the start, others
  maturing what already exists in parallel, so integration stays easy and nothing already
  built is lost. Requests design-decision review and sign-off from delivery lead, architect,
  security, QA and all stakeholders.

# ---- STEP 1: CONTEXT RESOLUTION (01) ----
context:
  workstream: "NONE — the input is programme-level; no workstream covers the platform product"
  current_phase: "WS-1 Phase 4 (Hardening) · WS-2 Phase 1 (Foundation)"
  canonical_stage: "WS-1 L7 · WS-2 L4/L6 — the input concerns L0–L2, which no workstream owns"
  current_objective: "WS-1: Term path signed off for UAT · WS-2: provider-neutral workforce identity"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH, run the full pipeline"
  active_work_item: "none — session opened on this input"

# ---- STEP 2: LIFECYCLE VALIDATION (03) ----
stage_fit:
  code: SF0
  rationale: >
    Not premature and not adjacent. WS-1 gate criteria 4.3 (bank caller UAT sign-off) and 4.4
    (compliance review) cannot be satisfied against requirements that carry no acceptance
    criteria and no signature — a caller cannot accept against a specification that does not
    exist. The missing L0-L2 foundation therefore blocks an open gate criterion, which is the
    definition of SF0 (03 section 3).
  target_stage: null
  unpark_trigger: null

# ---- STEP 3: SCOPE VALIDATION (02) ----
scope:
  code: SC4
  business_scope: >
    Changes governance files, adds a workstream, and amends Definition of Ready / Definition of
    Done. 14 section 1 requires a CR for each of these, approved by PO + Architect, with the
    affected boards for any change to stage exit criteria.
  serves: ["GATE-P4 criteria 4.3, 4.4", "GAP-008", "GAP-010", "GAP-018"]
  failure_without_it: >
    No delivered behaviour in five services traces to a signed business requirement. Under
    IRDAI CA0515 the evidence request is requirement -> acceptance criteria -> test, and that
    chain does not exist. UAT and compliance sign-off stall on the same missing artefact.
  minimal: true
  authority: "docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md Wave 0 rule"

# ---- STEP 4: NECESSITY (16) ----
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "immediate"
  binds_when: "before the next sprint commitment"
  evidence_tier: E2
  evidence:
    - "git --follow first-commit dates: services and libs 2026-07-30; charter/BRD/PRD/R0 scope 2026-07-31; problem statement 2026-08-04"
    - "commit cd40460 (2026-08-06) added the workforce auth SSOT and its three services in one commit"
    - "03-PROGRAMME-TODO.md Wave 0: 'No delivery sprint commit until Wave 0 exit criteria met' — Wave 0 exit is open while WS-1 is at Phase 4"
    - "CURRENT-STATE.yaml declares WS-1 and WS-2 only; the Distribution Platform product has no workstream, so 02 section 3 forces its work to SC2/PARK"
    - "GAP-008 'BR templates lack AC' — In progress; no requirement in the repository carries acceptance criteria"
  confidence: C4
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true     # WS-1 gate 4.3/4.4, Compliance, the BA's RTM
    X3_cheap_later: false       # cost rises with every further flow built untraceably
    X5_stage_necessity: true
    X9_problem_observed: true   # the inversion is observed in git history, not predicted

# ---- STEP 5: ACTION MATRIX (00 section 6) ----
action: ESCALATE
action_rationale: >
  SC4 is never an agent decision (02 section 3). The proposal changes governed state — a new
  workstream, DoR/DoD amendments, and the relationship between WS-1/WS-2 and the platform — all
  of which 14 section 1 routes to change control with PO + Architect approval. Rule CC-1: an
  agent may raise a change request and may never approve one. The war room is the approval
  forum. Nothing in the proposal has been implemented.
duplicate_of: null
conflicts:
  - document: "03-PROGRAMME-TODO.md Wave 0 rule vs CURRENT-STATE.yaml WS-1 Phase 4"
    resolution: "Recorded as finding F2; the war room resolves it by approving the realignment or by formally waiving the Wave 0 rule"

# ---- Outcome ----
outcome:
  registered_in: "registers/DECISION-REGISTER.md (CR-002)"
  work_item_id: null
  plan_id: null
  status: ESCALATED
  artefacts:
    - "docs/au-bank-insurance-platform/po-drive/war-room/README.md"
    - "docs/au-bank-insurance-platform/po-drive/war-room/01-PROCESS-GAP-ANALYSIS.md"
    - "docs/au-bank-insurance-platform/po-drive/war-room/02-REALIGNMENT-PROPOSAL.md"
    - "docs/au-bank-insurance-platform/po-drive/war-room/03-DELIVERY-MODEL-AND-FLOW-PLAN.md"
    - "docs/au-bank-insurance-platform/po-drive/war-room/04-WAR-ROOM-RUNSHEET.md"
    - "docs/au-bank-insurance-platform/po-drive/war-room/05-STAKEHOLDER-REVIEW-SHEET.md"
  risks_raised: [RISK-012, RISK-013]

# ---- Return to task ----
resumed: "none — no work item was in flight; awaiting the CR-002 verdict"
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
