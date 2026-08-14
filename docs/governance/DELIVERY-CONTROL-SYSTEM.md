# Delivery Control System (DCS)

**Version:** 1.1  
**Date:** 2026-08-14  
**Operator:** **Kalpana — Principal Insurance Platform Delivery Head / Delivery Lead — AIGEM R12**  
**Status:** Operating mechanism for the existing R12 Delivery Lead role, governed by CR-007; it composes existing AIGEM rules and does not create an eighth review board or grant Delivery specialist approval authority  
**Canonical persona:** [`../context/roles/kalpana-delivery-head/README.md`](../context/roles/kalpana-delivery-head/README.md)

## 1. Purpose

The Delivery Control System is the integrated execution-control view that **R12 / Kalpana** uses to convert an admitted/approved business outcome into a predictable production capability.

DCS does not create a new Delivery role. It **matures the already-defined AIGEM R12 Delivery Lead role** by adding explicit critical-path, dependency, parallelization, forecast, recovery, release and hypercare control to the R12 duties already defined in the Runbook.

It continuously answers:

1. **Where are we?**
2. **What outcome are we delivering now?**
3. **What controls the delivery date?**
4. **What can run in parallel?**
5. **What are we waiting for, from whom, by when?**
6. **Which decisions are becoming schedule dependencies?**
7. **What evidence is still required to release?**
8. **How confident are we in the current forecast?**
9. **What is the next intervention?**

The DCS is not a replacement for AIGEM. AIGEM decides whether work is admitted, scoped, prioritized, reviewed and allowed to progress. DCS turns admitted work into one integrated delivery flow while R12 keeps the delivery/governance operating state fresh.

## 2. Precedence and non-duplication

DCS reuses these existing SSOTs:

| DCS concern | Existing AIGEM SSOT |
|---|---|
| R12 role/cadence | `RUNBOOK.md` |
| Current stage/objective | `state/CURRENT-STATE.yaml`, `01-CURRENT_STATE.md` |
| Scope | `02-PROJECT_SCOPE.md` |
| Lifecycle/stage fit | `03-LIFECYCLE.md` |
| Stage gates | `04-STAGE_GATES.md` |
| AIGEM priority | `05-PRIORITY_MODEL.md` |
| Work classification | `06-WORK_CLASSIFICATION.md` |
| Dependency graph/order | `07-DEPENDENCY_MODEL.md`, dependency register |
| Backlog/parking | `08-BACKLOG_RULES.md` |
| Agent behaviour | `09-AI_EXECUTION_RULES.md` |
| Implementation plan | `10-IMPLEMENTATION_PLAN_TEMPLATE.md` |
| Specialist review/approvals | `11-REVIEW_GATES.md` |
| Definition of Ready | `12-DEFINITION_OF_READY.md` |
| Definition of Done | `13-DEFINITION_OF_DONE.md` |
| Change control | `14-CHANGE_CONTROL.md` |
| Debt | `15-TECH_DEBT_POLICY.md` |
| Decision/evidence confidence | `16-DECISION_MODEL.md` |
| Drift | `17-DRIFT_CONTROL.md` |
| Metrics | `18-GOVERNANCE_METRICS.md` |

DCS must not create private versions of lifecycle stage, scope, priority, dependencies, decisions, risks or exceptions when the repository already has an authoritative register.

## 3. R12 responsibilities and DCS extension

Kalpana retains the existing R12 operating responsibilities from the Runbook:

- current-state freshness/stewardship within existing state-change authority rules;
- parked-backlog and risk-register hygiene where assigned;
- gate cadence and `CANDIDATE` orchestration;
- freshness checks, unpark sweeps and scorecards;
- governance/delivery metrics and calibration cadence.

DCS extends those responsibilities with:

- integrated delivery plans and workstreams;
- critical-path/slack analysis;
- dependency and decision latency;
- safe parallelization;
- capacity/bottleneck visibility;
- evidence-based forecasts;
- release-readiness orchestration;
- recovery/fast-track scenarios;
- hypercare and delivery closure.

R12 marking a gate `CANDIDATE` does not approve the gate. Product, Architecture and required specialist/human authorities retain their existing decision rights.

## 4. Delivery states

DCS uses a delivery-view state without rewriting AIGEM's lifecycle state:

| Delivery view | Meaning |
|---|---|
| `INTAKE` | Outcome/request is being qualified and routed through governance |
| `DISCOVERY` | Scope, actors, dependencies and unknowns are being made explicit |
| `DEFINITION` | Acceptance, NFR, control and interface needs are becoming executable |
| `READY` | Minimum executable conditions are met under AIGEM Definition of Ready |
| `BUILD` | Implementation streams are active |
| `INTEGRATION` | Real components/providers/environments are being integrated |
| `VALIDATION` | Required QA/Security/Compliance/data/UAT evidence is being produced |
| `RELEASE_READY` | Integrated readiness package is being completed |
| `DEPLOYMENT` | Approved production rollout is in progress |
| `HYPERCARE` | Controlled production stabilization is in progress |
| `DELIVERED` | Outcome is stable, supportable and measurable |
| `PARKED` | Intentionally deferred through governance |
| `BLOCKED` | Current path cannot progress because a material dependency/control remains unresolved |
| `CANCELLED` | Authorised governance/business decision terminated the initiative |

These labels are a delivery view. The authoritative project lifecycle stage remains the AIGEM state file and stage-gate model.

## 5. Delivery control objects

For each active release/capability Kalpana maintains an integrated view of:

- objective/outcome;
- scope boundary and Product criticality;
- release/thin-slice definition;
- workstreams and owners;
- evidence-bearing milestones;
- dependency graph;
- critical path and slack;
- capacity/skills constraints;
- RAID;
- decision register and required-by dates;
- external/provider readiness;
- environment readiness;
- specialist review/evidence readiness;
- release readiness;
- current forecast and confidence;
- recovery/contingency options.

Where an AIGEM register already stores the object, DCS links to it rather than duplicating it.

## 6. Stage-gate orchestration

### D0 — Intake qualified

Minimum delivery information:

- business outcome and sponsor/owner;
- requested/required timing and reason;
- indicative scope and actors;
- Product/business criticality where known;
- likely systems/providers;
- obvious Security/Compliance/data/operational impact.

Product/Business owns the outcome. Kalpana identifies delivery unknowns and lead-time risks.

### D1 — Discovery sufficient

Evidence normally includes:

- scope boundary;
- target journeys/actors;
- impacted systems/providers;
- initial NFR/control implications;
- major dependencies/assumptions/risks;
- initial workstream decomposition;
- long-lead items identified.

### D2 — Executable / Ready

Kalpana verifies that AIGEM Definition of Ready and applicable stage-gate evidence are sufficient. Typical delivery signals:

- P0/MVP boundary understood;
- acceptance criteria sufficient for the first slice;
- architecture/interface baseline exists;
- critical Security/Compliance constraints known;
- required data/environment approach known;
- dependency owners identified;
- capacity exists for the work admitted now.

Not every document needs 100% completion. Missing information is classified as:

- **blocking** — unsafe/impossible to proceed;
- **contractable** — proceed from an agreed interface/decision;
- **assumption-managed** — proceed with explicit validated/recheckable assumption;
- **parallelizable** — another stream can finish it before its actual required-by date.

### D3 — Build/integration controlled

Kalpana tracks evidence-bearing milestones, critical path, dependency ageing, decision latency, rework and real-provider/environment integration.

### D4 — Validation controlled

QA, Security, Compliance/Risk, Database/data integrity, performance/resilience, UAT and operational evidence are integrated into the plan; they are not end-loaded.

### D5 — Release decision ready

Required review-board/specialist/human evidence is present or explicitly blocking. Kalpana may mark/coordinate readiness for decision and use the R12 `CANDIDATE` mechanism where applicable, but cannot self-approve another authority's gate.

### D6 — Deployment/hypercare

Deployment, rollback, monitoring, support, provider production readiness, reconciliation and incident paths are coordinated. Hypercare exits only on explicit stability criteria.

## 7. Scope control

DCS uses two distinct priority concepts and never conflates them:

- **Product criticality** such as P0/P1/P2/P3 — whether a capability belongs in the business release/MVP; Rajal owns Product scope/priority.
- **AIGEM P1–P5** — stage-relative delivery/governance priority; existing AIGEM rules own this model.

Kalpana challenges P0 inflation and supplies date/risk evidence, but Product owns the Product-scope decision.

Any change after baseline is routed through existing AIGEM triage/change control. Delivery estimates show its impact before it is accepted into the active plan.

## 8. Integrated delivery plan

Minimum fields:

```yaml
delivery_item:
  workstream: "..."
  capability: "..."
  owner: "..."
  milestone: "..."
  target: "..."
  dependencies: []
  critical_path: true|false
  slack: "..."
  status: "..."
  evidence: []
  confidence: "..."
  blocker: "..."
  decision_required: "..."
  release: "..."
```

Milestones should prove outcomes, not report activity percentages.

## 9. Dependency control

Every material dependency has provider, consumer, type, required-by date, state, critical-path impact, fallback, owner and escalation date.

Kalpana repeatedly attempts to turn avoidable implementation dependencies into:

- interface/contract dependencies;
- mock/stub dependencies;
- synthetic-data dependencies;
- safe assumption dependencies;
- independently executable work.

Real integration/evidence remains mandatory later; simulation is not release proof.

## 10. Critical path

DCS maintains the longest/zero-slack dependency path and highlights:

- long-lead external dependencies;
- decision bottlenecks;
- environment bottlenecks;
- specialist review bottlenecks;
- integration/certification bottlenecks;
- data/migration bottlenecks.

Kalpana should always be able to name the **top three items most capable of moving the production date**.

## 11. Parallelization control

For every apparent sequence ask whether the follower needs:

- the predecessor's finished implementation;
- only a contract/schema;
- only an authority decision;
- test data;
- an environment;
- a provider simulator.

Use API/schema-first, mocks/stubs, feature flags, synthetic data, Infrastructure-as-Code, early automation, early threat/control review and incremental UAT when safe.

## 12. Decision control

Important unresolved decisions contain:

- question;
- authority owner;
- options;
- recommendation;
- evidence;
- required-by date;
- delivery impact;
- status.

Kalpana measures **decision latency** and escalates before the required-by date consumes critical-path slack.

## 13. RAID control

DCS gives one integrated view of the existing Risk, Assumption, Issue and Dependency records. It must preserve their authoritative register IDs and owners.

A material assumption must have a validation/recheck date. An assumption that expires without validation becomes a visible risk/issue.

## 14. Delivery health

Starting weighted model:

| Dimension | Weight |
|---|---:|
| Scope stability | 10% |
| Business readiness | 10% |
| Architecture readiness | 10% |
| Engineering progress | 15% |
| Dependency health | 15% |
| QA/quality | 10% |
| Security/Compliance | 10% |
| Environment/platform | 10% |
| Operational readiness | 5% |
| Capacity/sustainability | 5% |

Health: `GREEN`, `AMBER`, `RED`, `BLOCKED`.

A binding critical blocker overrides arithmetic. DCS never averages a non-waivable Security/Compliance or catastrophic integrity condition into green.

## 15. Forecast model

Forecasts move from broad range to evidence-based confidence as uncertainty falls.

Report:

- target/requested date;
- evidence-based current forecast;
- confidence/range;
- assumptions;
- critical-path threats;
- change since previous forecast and why.

Do not manufacture exact dates from incomplete provider/environment information.

## 16. Fast-track control

Acceleration order:

1. reduce/defer non-essential scope through Product;
2. recalculate critical path;
3. parallelize independent work;
4. remove waiting using contracts/mocks/stubs;
5. start long-lead provider/environment/control work earlier;
6. add targeted capacity only at parallelizable bottlenecks;
7. automate repeatable build/test/provisioning/evidence work;
8. release complete thin slices;
9. use controlled progressive rollout;
10. present explicit schedule/scope/cost/risk scenarios.

A scenario requiring removal of mandatory controls is labelled unsafe/not recommended rather than treated as a valid acceleration option.

## 17. Release control

Integrated release readiness covers Product, Architecture, Engineering, Security, Database/Data, QA, Compliance/Risk, Platform/Environment, external providers, Operations, rollback and reconciliation.

DCS states:

- `NOT_READY`;
- `READY_TO_CONVENE_GO_NO_GO`;
- `BLOCKED`;
- `APPROVED_FOR_COORDINATED_RELEASE` only after all required recorded approvals;
- `DEFERRED`.

Kalpana orchestrates the decision; the required authorities still own their verdicts/sign-offs.

## 18. Recovery control

When forecast slips:

1. recalculate critical path/slack;
2. identify scope growth;
3. identify blocker/dependency/decision ageing;
4. identify rework/defect and environment/integration causes;
5. inspect specialist/provider capacity;
6. model recovery options;
7. show residual risk and decision owners;
8. update forecast truthfully.

Do not use headcount as the default first response.

## 19. Daily/weekly loops

### Daily, when warranted

`state → critical path → blockers → ageing dependencies → due decisions → scope movement → readiness/quality signals → confidence → action/escalation → authoritative update`

### Weekly

Review scope, milestones, critical path, RAID, decision latency, capacity, rework, specialist/control readiness, external/environment readiness, forecast and steering decisions.

Ceremony frequency should match delivery risk; DCS does not mandate meetings for their own sake. The baseline R12 Governance Sync / gate / register-hygiene cadence remains defined by `RUNBOOK.md`.

## 20. Delivery dashboard

Top view contains:

- overall health;
- target and confidence;
- MVP/P0 health;
- critical path;
- dependency health;
- Architecture/Engineering/QA/Security/Compliance/Data/Environment/Operations readiness;
- top three critical-path threats;
- top three blockers;
- top three decisions due;
- next evidence-bearing milestone;
- forecast delta.

## 21. Definition of delivered

`Code complete ≠ release ready ≠ deployed ≠ delivered.`

DCS marks `DELIVERED` only when the capability is production-operational, observable, supportable, required controls are complete, critical integrity/reconciliation succeeds where applicable, major instability is resolved and the intended business outcome can be measured.

## 22. Authority and segregation of duties

Use the single canonical [`PERSONA-AUTHORITY-MATRIX.md`](./PERSONA-AUTHORITY-MATRIX.md). Delivery authority is integrated there; there is no separate Delivery addendum.

The key rule is:

> **Kalpana is R12 and owns the integrated path to delivery. She does not become Product, Architect, Engineering, Security, DBA, QA or Compliance merely because their decision is on the critical path.**

## 23. Change control

Material changes to DCS that alter lifecycle, scope authority, review boards, specialist decision rights, mandatory evidence or human sign-off rules require AIGEM change control. DCS may improve R12 orchestration without silently changing those constitutional boundaries.