# 18 — Governance Metrics & Health

**Layer:** L1 — generic
**Owner:** Delivery Lead
**Cadence:** snapshot at every stage gate; reviewed with the PO and Architect

---

## 1. Why measure the process

A governance framework that is not measured becomes ceremony. These metrics answer three
questions:

1. **Is the model being used?** (adoption)
2. **Is it making good decisions?** (calibration)
3. **Is it worth its cost?** (efficiency)

Every metric below is computable from the registers and git history — no separate tooling and
no manual bookkeeping beyond what the pipeline already produces.

---

## 2. The metrics

### Adoption

| Metric | Definition | Target | Source |
|--------|------------|--------|--------|
| Triage coverage | Inputs with a `SUG-####` ÷ all inputs | > 95% | Suggestion register vs conversation/PR history |
| Bypass rate | `ADMIT-BYPASS` ÷ admitted items | < 10% | Suggestion register |
| Plan coverage | T2+ items with an approved plan ÷ T2+ items | 100% | Backlog |
| Unreferenced TODOs | TODOs without a work item ID | 0 | `grep` (see [17 §5](./17-DRIFT_CONTROL.md#5-pre-pr-drift-check)) |
| Register freshness | Days since the last register update | < 7 | Git |

A **rising bypass rate is a process signal, not a discipline problem**: it means the ceremony
exceeds the value for that class of work. Fix the process (usually by lowering the tier), do
not exhort people.

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

## 3. Gate scorecard

Produced at every stage gate and kept with the gate record:

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

  verdict: "HEALTHY | WATCH | INTERVENE"
  actions: []
```

Baseline is established at the first gate after adoption; targets apply from the second.

---

## 4. Reading the numbers

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

---

## 5. Minimum viable measurement

If only three numbers are ever tracked, track these:

1. **Admission rate** — is the gate filtering at all?
2. **Plan accuracy** — is what we build what we said we'd build?
3. **Parked items aging beyond two gates** — is "parked" a real state, or a bin?

Those three catch the three failure modes that matter: a gate that does not gate, execution
that does not follow the plan, and parking that is really deletion.
