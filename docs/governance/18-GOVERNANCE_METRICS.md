# 18 — Governance Metrics & Health

**Layer:** L1 — generic  
**Owner:** Delivery Lead  
**Quality metric steward:** Swapnali — QA Lead  
**Cadence:** snapshot at every stage gate; quality subset also reviewed at release/quality exit

---

## 1. Why measure the process

A governance framework that is not measured becomes ceremony. Metrics answer:

1. **Is the model being used?** — adoption
2. **Is it making good decisions?** — calibration
3. **Is work flowing without governance-induced waste?** — efficiency
4. **Do we have objective evidence that approved work is actually safe/correct to release?** — quality confidence

Prefer metrics derivable from registers, git, CI/test evidence and incident/defect records. Do not create duplicate manual scorebooks when an existing SSOT already contains the source value.

---

## 2. Governance metrics

### Adoption

| Metric | Definition | Target | Source |
|---|---|---:|---|
| Triage coverage | Inputs with a `SUG-####` ÷ all inputs | >95% | Suggestion register vs history |
| Bypass rate | `ADMIT-BYPASS` ÷ admitted items | <10% | Suggestion register |
| Plan coverage | T2+ items with approved plan ÷ T2+ items | 100% | Backlog/plans |
| Unreferenced TODOs | TODOs without work item ID | 0 | Drift check |
| Register freshness | Days since last required register update | <7 | Git |

A rising bypass rate is a process signal, not automatically a discipline problem; fix disproportionate ceremony.

### Decision quality / calibration

| Metric | Definition | Target |
|---|---|---:|
| Admission rate | ADMIT ÷ all triaged | 20–40% |
| Park accuracy | Parked items later admitted at target stage ÷ all unparked | >60% |
| Premature-admission rate | Admitted items later found to be SF3 in hindsight | <5% |
| Rejection reversal rate | Rejections reopened with new evidence | <10% |
| Incident preventability | Incidents traceable to a rejected/parked item | 0 |
| False-P1 rate | P1 overrides claimed without evidence ÷ P1 claims | <5% |
| Recurrence rate | Items independently re-proposed ≥3 times | Falling |

### Flow

| Metric | Target |
|---|---:|
| Triage latency | <1 working day |
| Gate cycle time | <3 working days |
| Rework rounds per plan | <0.5 average |
| Rework escalations reaching round 3 | 0 |
| Blocked ratio | <20% |
| Dependency violations | 0 |
| Queue-order violations | <10% |

### Backlog health

| Metric | Target |
|---|---:|
| Parked items aging >2 gates | 0 |
| Ideas aging >3 gates | Closed as LAPSED |
| Open P0/P1 debt at gate | 0 |
| Debt repayment ÷ debt creation during hardening | ≥1 |
| Epics without completion definition | 0 |
| READY T2+ items without approved plan | 0 |

### Execution fidelity

| Metric | Target | Source |
|---|---:|---|
| Plan accuracy (`files changed ∩ files_expected`) | >0.85 | Git + plan |
| Drift incidents per PR | <0.5 | Drift log |
| Suggestions registered per implementation session | >0 | Register |
| Diff-to-estimate ratio | 0.5–2.0 | Git |
| Scope-creep rejections at Product board | Falling | Verdicts |

---

## 3. Merged quality-health metrics

These metrics are owned semantically by Swapnali but recorded with the governance gate/release evidence so there is one health view.

**Important:** JaCoCo line/branch thresholds are **not duplicated here**. Their canonical values remain in [`../1sb-insurance-integration/service-ssot/COVERAGE.md`](../1sb-insurance-integration/service-ssot/COVERAGE.md).

| Metric | Definition | Target | Source |
|---|---|---:|---|
| Critical-journey evidence coverage | In-scope critical journeys with current passing evidence ÷ in-scope critical journeys | **100%** for Q0/T4-critical paths | QA evidence / test backlog |
| Acceptance traceability | Material acceptance criteria mapped to executed evidence ÷ material AC | **100%** for Q0/Q1; ≥95% overall | Story/plan ↔ tests |
| Coverage-gate pass | Modules subject to canonical coverage gates currently passing | **100%** | CI + `COVERAGE.md` |
| Open Q0 defects/evidence gaps at release | Count | **0** | Defect/QA assessment |
| Critical production escapes | Sev/Q0-equivalent production escapes per release | **0** | Incident/defect log |
| Defect escape ratio | Production defects ÷ all defects for released scope | <5% trend target | Defect log |
| Flaky-test rate | Quarantined/retried flaky tests ÷ active automated tests | <2%; falling | CI/test inventory |
| Expired QA waivers | Waivers past expiry without closure/re-approval | **0** | Waiver/debt register |
| Critical-test bypass count | Non-approved omission of protected-gate testing | **0** | QA verdicts |
| Release evidence freshness | Evidence tied to the release candidate/current commit rather than stale build | **100%** for mandatory evidence | CI/release record |
| Regression effectiveness | Production regressions that should have been in known regression set | 0 critical; falling overall | Incident review |
| Critical idempotency/reconciliation evidence | Applicable critical mutation paths with current retry/reconciliation evidence | **100%** where applicable | QA evidence |

Coverage percentage itself is a floor, not an outcome metric. High coverage with critical escapes is poor quality.

---

## 4. Gate/release scorecard

```yaml
gate_scorecard:
  stage: "..."
  date: 2026-XX-XX

  adoption:
    triage_coverage: 0.00
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

Baseline is established at the first gate after adoption; targets apply from the next meaningful measurement window.

---

## 5. Reading patterns

| Pattern | Likely cause | Response |
|---|---|---|
| High admission + high drift | Gate is nominal | Tighten plan/absorption discipline |
| Low admission + high recurrence | Over-rejecting or weak reasons | Calibration review |
| Growing parked backlog | Scope/stage mismatch | Product scope conversation |
| Rising rework rounds | Plans too early/classification wrong | Increase evidence/spikes |
| High bypass rate | Process too heavy | Lower tiers where justified |
| P1 rate >20% of admissions | Override inflation | Enforce evidence |
| High coverage + critical escapes | Tests measure code execution, not business risk | Rebuild critical scenario/regression strategy |
| Rising flakes + green builds | Signal is being hidden by retries/quarantine | Treat test reliability as debt; fix root causes |
| Repeated expired QA waivers | Temporary risk acceptance has become permanent | Escalate owner; close/reapprove with evidence |
| Critical-journey coverage <100% at release | Core business path lacks current proof | QA `REWORK`/NO-GO unless governed exception is genuinely permitted |
| Incident traceable to missing known regression | Escaped-defect control failure | Add regression + review why control was absent |

---

## 6. Minimum viable measurement

If only four numbers are tracked, track:

1. **Admission rate** — is governance filtering?
2. **Plan accuracy** — did execution follow the decision?
3. **Parked items aging >2 gates** — is parking real?
4. **Critical-journey evidence coverage** — can we prove the important behaviour works?

These catch the dominant failure modes: non-gating governance, execution drift, silent parking/deletion and release without evidence.
