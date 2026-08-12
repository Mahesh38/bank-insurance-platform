# Decision Register

Index of decisions that constrain future work: ADRs, change requests, stage transitions, and
board escalations. **A decision recorded here may not be re-litigated without new evidence**
([14 §6](../14-CHANGE_CONTROL.md#6-reversing-a-rejection)).

**Owner:** Architect
**Upstream logs:** [architecture-review/08-architecture-decision-log.md](../../platform/architecture-review/08-architecture-decision-log.md) ·
[au-bank-insurance-platform/DECISION-LOG.md](../../au-bank-insurance-platform/DECISION-LOG.md)

---

## 1. Architecture decisions (ADR index)

Full ADRs live in the architecture decision log. This index exists so triage can cite them
without a search.

| ID | Decision | Status | Constrains |
|----|----------|--------|------------|
| — | Bank apps never call 1SB or the DB directly; integration service is the only path | Accepted | All WS-1 topology suggestions |
| — | Replaceable middleware: 1SB behind ports/adapters (Case 2) | Accepted | Any proposal to call 1SB from orchestration |
| — | Persistence is platform-common (`bank-persistence-service`), reached over HTTP | Accepted | Any proposal to embed a DB in a consumer |
| — | Integration service owns no Flyway/JPA | Accepted | Any persistence change in the integration service |
| — | Orchestration first, LOB handler second (`QuoteService` → `TermQuoteHandler`) | Accepted | LOB expansion design |
| — | Flutter communicates only with the workforce BFF | Accepted | Any direct-IdP proposal |
| — | Flutter never receives OAuth tokens (token-hiding session) | Accepted | Session design |
| — | Keycloak behind `identity-provider-adapter-service`; provider-neutral | Accepted | Provider-specific code placement |
| — | `identity-authorization-service` is the business source of truth for authorization | Accepted | Any proposal to use IdP roles as business roles |
| — | Authorization is default-deny, RBAC + ABAC + resource relationships | Accepted | Policy design |
| — | Maker-checker for bulk and privileged identity changes | Accepted | Admin flows |
| — | Production IdP decision deferred behind the adapter | Accepted (deferral) | Do not re-open before WS-2 Phase 2 |

> ADR IDs are assigned by the architecture decision log. New architectural decisions arising
> from AIGEM triage are raised there and indexed here.

## 2. Governance decisions

| ID | Date | Decision | Rationale | Decided by |
|----|------|----------|-----------|------------|
| GOV-001 | 2026-08-07 | Adopt AIGEM 1.0 as the governance model for this repository | Prevent AI scope drift; schedule suggestions rather than implementing or losing them | Pending ratification |
| GOV-002 | 2026-08-07 | Route admitted work to existing backlogs; AIGEM keeps no parallel backlog | Two sources of truth both rot | Pending ratification |
| GOV-003 | 2026-08-07 | Seed the parked backlog from deferred `TECH-DEBT.md` rows; no retrospective triage | Backfilling costs days and teaches nothing ([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)) | Pending ratification |
| GOV-004 | 2026-08-10 | **Ratify the current-state snapshot**: WS-1 at Phase 4 (Hardening), WS-2 at IAM Phase 1, with the scope and standing constraints as recorded | Reconstructed from `ACTION-PLAN.md`, phase `STATUS.md` files, `TECH-DEBT.md` and git history; reviewed and accepted | **Mahesh (Solution Architect), 2026-08-10** — PO counter-signature outstanding |

## 3. Change requests

| ID | Date | Type | Summary | Decision | Approvers |
|----|------|------|---------|----------|-----------|
| CR-001 | 2026-08-10 | STAGE | Add exit criterion **4.7** (coverage gates green; QA-001 closed or waived with expiry) to the WS-1 Phase 4 gate | **APPROVED** 2026-08-10 | Mahesh (Solution Architect) — PO + QA Lead counter-signature outstanding |
| CR-002 | 2026-08-12 | GOV | Process realignment: add workstream **WS-0 — Distribution Platform**, adopt the dual-track recovery model and its seam rules, amend DoR/DoD for traceability | **PENDING** — war room | PO + Architect (required); QA, Security, Compliance, Delivery Lead, Tech Head, Tech Lead, BA verdicts recorded |

### CR-001 — add Phase 4 exit criterion 4.7

```text
current_position:  ACTION-PLAN.md Phase 4 defines exit criteria 4.1-4.6. Coverage is not
                   among them. QA-001 is tracked as P0 tech debt with "Partial" status.
proposed_change:   Add 4.7 to 04-STAGE_GATES.md as a binding criterion.
driver:            QA-001 is P0 debt; 15-TECH_DEBT_POLICY forbids a P0 debt item crossing a
                   gate. Either 4.7 is a criterion, or QA-001 is not P0. Today the two
                   documents disagree.
raised_because:    The criterion was added during framework authoring WITHOUT a CR - a
                   governance violation caught in review. It is demoted to PROPOSED until
                   ratified, rather than quietly kept.
impact:            If approved, Phase 4 cannot pass with the service coverage floor still
                   "interim". If rejected, QA-001 must be re-severitised below P0.
alternatives:      (a) approve as written  (b) reject and downgrade QA-001 to P1
                   (c) approve with a dated waiver for the interim service floor
decision:          APPROVED (2026-08-10, Mahesh / Solution Architect)
chosen_option:     (a) approve as written
consequence:       Phase 4 cannot pass with the service coverage floor still "interim".
                   QA-001 must close, or carry a dated waiver co-approved by TL + QA Lead
                   per 15-TECH_DEBT_POLICY section 4.
outstanding:       PO and QA Lead counter-signature. The criterion is binding now; the
                   counter-signature is recorded when they next review the gate.
```

### CR-002 — process realignment (dual-track recovery)

Full proposal: [`po-drive/war-room/`](../../au-bank-insurance-platform/po-drive/war-room/README.md) ·
triage record: [`SUG-20260812-p1r`](./SUGGESTION-REGISTER.md#sug-20260812-p1r--process-realignment-dual-track-recovery)

```text
raised_by:         human:PO (Rajal) — pack prepared by agent:claude
type:              GOV  (also SCOPE and STAGE in effect — see impact)

current_position:  CURRENT-STATE.yaml declares two workstreams, WS-1 (1SB adapter) and WS-2
                   (workforce auth), both engineering. The AU Bank Insurance Distribution
                   Platform itself has no workstream, no stage, no objective and no gate.
                   03-PROGRAMME-TODO.md Wave 0 states "No delivery sprint commit until Wave 0
                   exit criteria met"; Wave 0 exit is open while WS-1 is at Phase 4 Hardening.
                   No delivered behaviour in the five built services cites a business
                   requirement ID; GAP-008 records that BR templates still lack acceptance
                   criteria.

proposed_change:   (1) Add workstream WS-0 "AU Bank Insurance Distribution Platform" to
                       state/CURRENT-STATE.yaml and 01-CURRENT_STATE.md, at canonical stage L1,
                       with WS-1 and WS-2 as modules within it; add its stage map to 03 and its
                       gates to 04.
                   (2) Adopt the dual-track operating model: Track A (foundation-first, WS-0)
                       and Track B (harden and hold, WS-1/WS-2), converging when both existing
                       gates pass.
                   (3) Adopt seam rules S-1 (Track B may only close work an existing gate
                       criterion, debt ID or defect already names; anything new needs a Track A
                       requirement ID), S-2 (canonical contracts owned by Track A), S-3
                       (traceability in Definition of Done), S-4 (single escalation path).
                   (4) Amend 12-DEFINITION_OF_READY (work item cites a signed requirement ID or
                       a gate/debt ID) and 13-DEFINITION_OF_DONE (RTM row complete).
                   (5) Add WS-0 routing entries so platform FUNC/DOC/COMP work does not route
                       into the 1SB module backlog.

driver:            New evidence — validated assumption failure. The repository's own history
                   shows L3-L5 executed before L0-L1, and the programme's own Wave 0 rule is
                   being violated. Not a preference: gate criteria 4.3 and 4.4 cannot be
                   satisfied against unsigned, untestable requirements.

evidence:
  - "git --follow: services + libs 2026-07-30; charter/BRD/PRD/R0-SCOPE 2026-07-31; consolidated problem statement 2026-08-04"
  - "commit cd40460 (2026-08-06): workforce auth SSOT (342 lines) and three implementing services in a single commit — no design review point existed"
  - "03-PROGRAMME-TODO.md Wave 0 rule vs CURRENT-STATE.yaml WS-1 Phase 4"
  - "CURRENT-STATE.yaml workstreams[] — no platform workstream; 02 section 3 therefore forces platform work to SC2/PARK"
  - "GAP-008 In progress: no requirement carries acceptance criteria; GAP-010 sponsor unnamed; GAP-018 team boundary open"

impact:
  scope:         "No change to WHAT is being built. Changes the ORDER and adds a traceability obligation. All five services stay in scope; none is parked."
  stage:         "No stage is advanced or reverted by this CR. Adds a new workstream at L1. Track B's gates (GATE-P4, GATE-IAM-P1) are unchanged; the DoR/DoD amendments do change what 'done' means for items inside them, which is why the affected boards must record verdicts."
  dependencies:  "Unblocks nothing mechanically; makes platform requirement work admittable for the first time. DEP-002 (external bank caller) remains the constraint on gate 4.3."
  parked_items:  "None currently parked become eligible; the parked backlog is unaffected."
  effort:        "M — 8 governance/document changes, then ~40% of engineering capacity redirected for approximately 3 sprints."
  risk_if_rejected: "RISK-012 and RISK-013 stay open and grow with each further flow built without traceability. UAT (4.3) and compliance (4.4) sign-off remain blocked on artefacts that do not exist."

alternatives_considered:
  - option: "A — do nothing, carry on building"
    consequence: "Fastest short-term velocity; requires a FORMAL WAIVER of the Wave 0 rule and acceptance of the CA0515 evidence exposure by name. Product converges on the aggregator's model over time."
  - option: "B — stop all build, complete discovery first"
    consequence: "Cleanest process; 6-10 weeks with no demonstrable delivery; wastes the maturity of the 1SB adapter; unlikely to survive steering."
  - option: "C — dual track with the seam rule (PROPOSED)"
    consequence: "Delivery stays visible, foundation is repaired, convergence in 4-6 weeks; costs ~40% capacity for 3 sprints and requires Rule S-1 discipline."
  - option: "D — dual track without the seam rule"
    consequence: "The status quo with a name. The built track keeps running ahead of the signed track. Not recommended."

decision:          PENDING
approvers:         []
decided_on:
conditions:        []

note:              Raised by an agent on the PO's instruction. Rule CC-1 — an agent may raise a
                   change request and may never approve one, not even its own. No governance
                   state, stage field, gate or Definition of Done has been edited; the pack, this
                   CR, the triage record and RISK-012/013 are the complete set of changes made.
```

## 4. Stage transitions

| Date | Workstream | From | To | Criteria met | Waivers | Approvers |
|------|------------|------|----|--------------|---------|-----------|
| — | WS-1 | Phase 3 | Phase 4 | Term vertical slice delivered (FUNC-001…007, FUNC-009) | — | Recorded retrospectively from `phase-4/STATUS.md`; not gate-reviewed under AIGEM |
| 2026-08-10 | Both | *(provisional)* | *(ratified)* | State snapshot accepted as the governing context — see GOV-004 | — | Mahesh (Solution Architect) |

## 5. Board escalations

Conflicts between boards that required an Architect + PO resolution
([11 §12](../11-REVIEW_GATES.md#12-aggregation)).

| ID | Date | Plan | Conflict | Resolution | Recorded as |
|----|------|------|----------|------------|-------------|
| — | — | — | *none* | — | — |

## 6. Rejections of note

Rejected proposals worth remembering, so they are not re-argued from scratch.

| ID | Proposal | Rejected because | Reopen if |
|----|----------|------------------|-----------|
| — | — | *none yet* | — |
