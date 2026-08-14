# 18 — Governance Metrics & Health

**Layer:** L1 — generic  
**Owner:** **Kalpana — Delivery Head / Delivery Lead (R12)**  
**Quality metric steward:** Swapnali — QA Lead  
**Cadence:** snapshot at every stage gate; reviewed with the PO and Architect; quality subset reviewed at release/quality exit

---

## 1. Why measure the process

A governance framework that is not measured becomes ceremony. These metrics answer four questions:

1. **Is the model being used?** (adoption)
2. **Is it making good decisions?** (calibration)
3. **Is it worth its cost?** (efficiency)
4. **Do we have current objective evidence that approved work is safe/correct enough to release?** (quality confidence)

Governance metrics remain computable from registers and git history. Quality metrics should prefer CI/test evidence, release evidence, defect/incident records and the existing QA SSOT. Do not maintain duplicate manual scorebooks when an authoritative source already exists.

---

## 2. Governance metrics

### Adoption

| Metric | Definition | Target | Source |
|--------|------------|--------|--------|
| Triage coverage | Inputs with a `SUG-####` ÷ all inputs | > 95% | Suggestion register vs conversation/PR history |
| Bypass rate | `ADMIT-BYPASS` ÷ admitted items | < 10% | Suggestion register |
| Plan coverage | T2+ items with an approved plan ÷ T2+ items | 100% | Backlog |
| Unreferenced TODOs | TODOs without a work item ID | 0 | `grep` (see [17 §5](./17-DRIFT_CONTROL.md#5-pre-pr-drift-check)) |
| Register freshness | Days since the last register update | < 7 | Git |

A **rising bypass rate is a process signal, not a discipline problem**: it means the ceremony exceeds the value for that class of work. Fix the process (usually by lowering the tier), do not exhort people.

### Decision quality (calibration)

| Metric | Definition | Target | Reading |
|--------|------------|--------|---------|
| Admission rate | ADMIT ÷ all triaged | 20–40% | > 60% ⇒ the gate is not filtering. < 10% ⇒ possibly over-rejecting |
| Park accuracy | Parked items later admitted at their target stage ÷ all unparked | > 60% | Low ⇒ parking is being used as a polite rejection |
| Premature-admission rate | Admitted items later found to be SF3 in hindsight | < 5% | The expensive error |
| Rejection reversal rate | Rejections reopened with new evidence | < 10% | High ⇒ rejecting too early or on weak evidence |
| Incident preventability | Incidents traceable to a rejected or parked item | 0 | **Any occurrence forces a calibration review** ([16 §7](./16-DECISION_MODEL.md#7-revalidation-triggers)) |
| False-P1 rate | P1 overrides claimed without evidence ÷ P1 claims | < 5% | Override inflation destroys the queue |
| Recurrence rate | Items independently re-proposed ≥ 3 times | Falling | High ⇒ rejections are not being read, or are wrong |

### Flow

| Metric | Definition | Target |
|--------|------------|--------|
| Triage latency | Input received → verdict recorded | < 1 working day |
| Gate cycle time | Plan submitted → approval gate closed | < 3 working days |
| Rework rounds per plan | Average | < 0.5 |
| Rework escalations | Plans reaching round 3 | 0 |
| Blocked ratio | BLOCKED ÷ (READY + BLOCKED) | < 20% |
| Dependency violations | Items started before a HARD dependency was Done | 0 |
| Queue-order violations | Items started that were not head of the ordered READY set | < 10% |

### Backlog health

| Metric | Target | Concerning |
|--------|--------|------------|
| Parked items aging > 2 gates | 0 | Growing — see AS-2 |
| Ideas aging > 3 gates | Closed as LAPSED | Accumulating |
| Open P0/P1 debt at a gate | 0 | Any P0 |
| Debt repayment ÷ debt creation (hardening) | ≥ 1 | < 0.5 |
| Epics without `completion_definition` | 0 | Any |
| Items in READY without an approved plan (T2+) | 0 | Any |

### Execution fidelity

| Metric | Target | Source |
|--------|--------|--------|
| Plan accuracy (files changed ∩ `files_expected`) | > 0.85 | Git + plan |
| Drift incidents per PR | < 0.5 | Drift log |
| Suggestions registered per implementation session | > 0 | Register |
| Diff-to-estimate ratio | 0.5–2.0 | Git |
| Scope-creep rejections at the Product board | Falling | Verdicts |

---

## 3. Merged quality-health metrics

These metrics are semantically stewarded by **Swapnali** and are recorded in the same gate/release health view rather than in a competing QA scorebook.

**Coverage SSOT rule:** JaCoCo line/branch thresholds and current measured values remain authoritative in [`../1sb-insurance-integration/service-ssot/COVERAGE.md`](../1sb-insurance-integration/service-ssot/COVERAGE.md). This file measures whether those canonical gates pass; it does not copy the threshold values.

| Metric | Definition | Target | Source |
|---|---|---:|---|
| Critical-journey evidence coverage | In-scope critical journeys with current passing evidence ÷ in-scope critical journeys | **100% for Q0/T4-critical paths** | QA evidence / `TEST-BACKLOG.md` |
| Acceptance traceability | Material AC mapped to current executed evidence ÷ material AC | **100% for Q0/Q1; ≥95% overall** | Story/plan ↔ tests/evidence |
| Coverage-gate pass | Modules subject to canonical coverage gates currently passing | **100%** | CI + `COVERAGE.md` |
| Open Q0 defects/evidence gaps at release | Count | **0** | Defect/QA assessment |
| Critical production escapes | Q0/critical production escapes per release | **0** | Incident/defect record |
| Defect escape ratio | Production defects ÷ all defects for released scope | <5% trend target | Defect record |
| Flaky-test rate | Quarantined/retried flaky tests ÷ active automated tests | <2%; falling | CI/test inventory |
| Expired QA waivers | Waivers past expiry without closure/re-approval | **0** | Waiver/debt record |
| Critical-test bypass count | Non-approved omission of protected-gate testing | **0** | QA verdicts |
| Release evidence freshness | Mandatory evidence tied to the release candidate/current commit | **100%** | CI/release record |
| Regression effectiveness | Production regressions that should have been in a known regression set | 0 critical; falling overall | Incident review |
| Critical idempotency/reconciliation evidence | Applicable critical mutation paths with current retry/reconciliation evidence | **100% where applicable** | QA evidence |

**Interpretation rule:** coverage is a floor, not an outcome. High code coverage with critical journey escapes is poor quality.

---

## 4. Gate / release scorecard

Produced at every stage gate and kept with the gate record. The quality block is also refreshed for a release candidate when a quality exit is required.

```yaml
gate_scorecard:
  stage: "Phase 4 — Hardening & consumer enablement"
  date: 2026-XX-XX

  adoption:
    triage_coverage: 0.00        # fill at first gate
    bypass_rate: 0.00
    plan_coverage: 0.00

  decision_quality:
    inputs_triaged: 0
    admitted: 0
    parked: 0
    rejected: 0
    escalated: 0
    admission_rate: 0.00
    false_p1_rate: 0.00

  flow:
    avg_rework_rounds: 0.0
    rework_escalations: 0
    dependency_violations: 0

  backlog_health:
    parked_aging_gt_2_gates: 0
    open_p0_p1_debt: 0
    debt_repaid: 0
    debt_created: 0

  execution:
    plan_accuracy: 0.00
    drift_incidents: 0

  quality:
    critical_journey_evidence_coverage: 0.00
    acceptance_traceability: 0.00
    coverage_gate_pass: false
    open_q0: 0
    critical_production_escapes: 0
    flaky_test_rate: 0.00
    expired_qa_waivers: 0
    critical_test_bypass_count: 0
    release_evidence_fresh: false

  verdict: "HEALTHY | WATCH | INTERVENE"
  actions: []
```

Baseline is established at the first gate after adoption; existing governance targets continue unchanged. Quality targets apply when the metric is applicable to the release/stage.

---

## 5. Reading the numbers

Patterns matter more than single values.

| Pattern | Likely cause | Response |
|---------|--------------|----------|
| High admission + high drift | The gate is nominal; everything is waved through | Enforce plan coverage; tighten the SF2 absorption test |
| Low admission + high recurrence | Over-rejecting; ideas keep coming back | Review rejection reasons; check evidence tiers |
| Growing parked backlog | Scope is under-sized for ambition, or stages are too long | Scope conversation with the PO — **not** a hygiene sprint |
| Rising rework rounds | Plans written too early, or classification wrong | Enforce the C3 confidence gate; more spikes |
| High bypass rate | Process too heavy for the work | Lower tiers; expand the T1 definition |
| Zero suggestions during implementation | Agents acting on impulses instead of registering them, or not looking | Check diffs against plans |
| P1 rate > 20% of admissions | Override inflation | Enforce evidence on overrides (§2, false-P1) |
| Incident traceable to a parked item | Calibration failure | Mandatory review of the parking decision — not blame |
| High coverage + critical escapes | Tests measure execution, not business risk | Rebuild critical scenario/regression strategy |
| Rising flakes + green builds | Retry/quarantine is hiding signal | Treat test reliability as quality debt; fix root cause |
| Repeated expired QA waivers | Temporary risk has become permanent | Escalate owner; close or explicitly re-approve with evidence |
| Critical-journey evidence <100% on protected path | Release lacks current proof | Board 5 `REWORK` / QA NO-GO unless a genuinely permitted governed exception exists |
| Incident traceable to missing known regression | Quality control failure | Add regression and review why the control was absent |

---

## 6. Minimum viable measurement

If only four numbers are ever tracked, track these:

1. **Admission rate** — is the governance gate filtering at all?
2. **Plan accuracy** — is what we build what we said we'd build?
3. **Parked items aging beyond two gates** — is “parked” a real state, or a bin?
4. **Critical-journey evidence coverage** — can we prove the important behaviour works?

Those four catch the dominant failure modes: a gate that does not gate, execution that does not follow the plan, parking that is really deletion, and a release that lacks evidence for critical behaviour.

---

## Legacy anchor compatibility

The QA-metrics merge introduced clearer section names and inserted the quality-health section, which changed several historical internal anchors. The following headings deliberately preserve older AIGEM links while directing readers to the canonical sections above. They contain no independent rules.

### 2. The metrics

Canonical content: [§2 Governance metrics](#2-governance-metrics).

### 3. Gate scorecard

Canonical content: [§4 Gate / release scorecard](#4-gate--release-scorecard).

### 4. Reading the numbers

Canonical content: [§5 Reading the numbers](#5-reading-the-numbers).

### 5. Minimum viable measurement

Canonical content: [§6 Minimum viable measurement](#6-minimum-viable-measurement).
