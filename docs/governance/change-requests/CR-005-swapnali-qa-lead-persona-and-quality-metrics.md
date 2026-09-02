# CR-005 — Swapnali QA Lead Persona & Quality Metrics Integration

**Date:** 2026-08-14  
**Type:** GOV  
**Raised by:** Mahesh — repository/architecture owner  
**Branch:** `agent/swapnali-qa-lead-persona`  
**Decision:** APPROVED FOR REVIEW BRANCH; RATIFICATION ON REQUIRED GOVERNANCE APPROVAL/MERGE

## 1. Problem

AIGEM already contains Board 5 — QA and the 1SB service SSOT already contains a QA Lead testing strategy, testing rules, coverage thresholds and a QA backlog. However, the QA Lead is not a named platform persona and quality authority is partially implicit under Engineering.

That creates ambiguity around test strategy ownership, insurance-domain criticality, testing bypasses, release evidence, quality metrics and cross-persona escalation.

## 2. Change

1. Add **Swapnali** as the single canonical Principal Insurance Quality Engineering / QA Lead persona.
2. Map Swapnali to existing **Board 5 — QA**; do not add a new AIGEM board.
3. Preserve developer test implementation and CI mechanics under Engineering while moving platform test strategy/evidence sufficiency to QA authority.
4. Add explicit Q0–Q3 quality finding severity, independent of AIGEM priority and other persona severities.
5. Define protected quality gates for authz, consent, suitability, financial calculations, proposal, payment, issuance, reconciliation, PII and auditability.
6. Define governed testing-waiver and human-risk-acceptance semantics.
7. Extend the cross-persona operating model and authority matrix to include QA.
8. Merge quality-health metrics into `18-GOVERNANCE_METRICS.md` while keeping code coverage thresholds in `COVERAGE.md` as their SSOT.
9. Keep the existing service-level `QA-LEAD-TESTING-STRATEGY.md`, `TESTING-RULES.md`, `COVERAGE.md` and `TEST-BACKLOG.md` as execution SSOT, now under Swapnali's canonical persona authority.

## 3. Non-goals

- no runtime application behaviour changes;
- no eighth AIGEM board;
- no duplicate generic QA Lead persona;
- no weakening of Security or Compliance/Risk vetoes;
- no transfer of developer unit-test responsibility away from Engineering;
- no copying of JaCoCo thresholds into governance metrics.

## 4. Safeguards

- QA cannot redefine Product behaviour or regulatory interpretation.
- QA cannot waive non-waivable Compliance/Security controls.
- QA quality holds must state evidence, severity and closure condition.
- Human risk acceptance does not rewrite the original QA assessment.
- Low-risk testing reduction remains possible when impact is bounded and documented.

## 5. Ratification

```yaml
decision: APPROVED_FOR_REVIEW_BRANCH
conditions:
  - "Preserve the seven-board AIGEM constitution."
  - "Treat Swapnali as the single canonical QA Lead persona."
  - "Keep service coverage thresholds in COVERAGE.md."
  - "Preserve mandatory Security/Compliance and human-signoff semantics."
post_merge:
  - "Use Swapnali for Board 5 reasoning and quality-exit decisions."
  - "Use 18-GOVERNANCE_METRICS.md for merged governance/quality health."
  - "Revalidate the authority matrix when QA or Engineering authority materially changes."
```
