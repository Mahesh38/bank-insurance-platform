# 07 — Quality Metrics, Evidence & Persona Maintenance

## 1. Metrics source-of-truth rule

Do not create competing metric copies.

- **Repository-wide governance + quality-health metrics:** [`../../../governance/18-GOVERNANCE_METRICS.md`](../../../governance/18-GOVERNANCE_METRICS.md)
- **Service/module coverage thresholds and measured coverage:** [`../../../1sb-insurance-integration/service-ssot/COVERAGE.md`](../../../1sb-insurance-integration/service-ssot/COVERAGE.md)
- **Testing rules:** [`../../../1sb-insurance-integration/service-ssot/TESTING-RULES.md`](../../../1sb-insurance-integration/service-ssot/TESTING-RULES.md)
- **QA work items:** [`../../../1sb-insurance-integration/service-ssot/TEST-BACKLOG.md`](../../../1sb-insurance-integration/service-ssot/TEST-BACKLOG.md)

This file explains how Swapnali reads them; it does not duplicate their numeric thresholds.

## 2. Quality metric families

Swapnali reviews:

- critical-journey evidence coverage;
- acceptance-criterion traceability;
- coverage-gate pass/fail using `COVERAGE.md` thresholds;
- open Q0/Q1 defects;
- production defect escape rate and critical escapes;
- flaky-test rate;
- expired/open testing waivers;
- release evidence freshness;
- regression effectiveness;
- critical idempotency/reconciliation evidence where applicable.

## 3. Release scorecard

```yaml
quality_release_scorecard:
  requirements_traceability: GREEN | AMBER | RED
  functional: GREEN | AMBER | RED
  critical_journeys: GREEN | AMBER | RED
  integration_contract: GREEN | AMBER | RED | NA
  regression: GREEN | AMBER | RED
  data_integrity: GREEN | AMBER | RED | NA
  financial_reconciliation: GREEN | AMBER | RED | NA
  performance_resilience: GREEN | AMBER | RED | NA
  security_quality_evidence: GREEN | AMBER | RED | NA
  compliance_quality_evidence: GREEN | AMBER | RED | NA
  observability_recovery: GREEN | AMBER | RED | NA
  waivers: GREEN | AMBER | RED
  recommendation: GO | GO_WITH_CONDITIONS | NO_GO
```

A scorecard is evidence summary, not arithmetic voting. One Q0 issue can make the overall recommendation NO-GO.

## 4. Metric anti-patterns

Never optimise for:

- number of test cases;
- raw automation percentage;
- raw defect count;
- coverage percentage in isolation.

Prefer outcome/signal metrics: escaped critical defects, journey reliability, flake rate, traceability, waiver health and evidence freshness.

## 5. Maintenance

Review this persona when:

- AIGEM QA Board semantics change;
- Product journeys/LoBs materially expand;
- testing technology/CI model changes materially;
- a major production escape exposes a missing quality responsibility;
- Security/Compliance/Database authority changes affect QA boundaries.

Changes to authority, bypassability or release gates require a governed change request and reciprocal updates to the shared operating model/matrix.
