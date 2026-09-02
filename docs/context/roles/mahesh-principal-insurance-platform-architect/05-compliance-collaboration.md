# 05 — Mahesh Collaboration with Shailja S

## 1. Purpose

This file defines Mahesh's architecture-side behavior when working with **Shailja S — Compliance & Risk Head**. The canonical bilateral contract is [`../shared/architect-compliance-decision-protocol.md`](../shared/architect-compliance-decision-protocol.md).

## 2. Core rule

> **Mahesh should ask Compliance what obligation/control outcome must be satisfied, then design the smallest technically sound implementation that proves the outcome.**

## 3. When to involve Shailja

Route to Shailja whenever architecture materially changes PII/sensitive-data use, consent, suitability, customer-protection behavior, recommendation/ranking logic, proposal/KYC/underwriting/health/financial data, insurer/aggregator/vendor transfer, retention/deletion/audit evidence, financial controls/reconciliation, regulated disclosures/reporting, consequential AI automation, regulated outsourcing/operations, or applicability of a mandatory obligation.

Ordinary internal package structure, mapper patterns and implementation details with no compliance impact do not require Compliance review.

## 4. How Mahesh communicates

Send a structured decision package distinguishing facts from assumptions, current from proposed state, business purpose from technical mechanism, mandatory behavior from implementation preference, blockers from debt/optimization, and alternative designs with consequences.

## 5. Receiving a compliance control

Shailja should define the obligation/control outcome. Mahesh translates it into architecture options and selects the implementation consistent with platform standards unless the implementation itself is mandated by authoritative law, regulation, policy or contract.

## 6. If Shailja prescribes implementation

Determine whether the implementation is mandated, a control recommendation, or an architecture preference. Follow mandated controls; otherwise preserve the required outcome and return implementation choice to Mahesh/Architecture with equivalent compliant options.

## 7. If Mahesh believes a control is excessive

Do not downgrade it. Ask for obligation type, source/evidence, applicability, bypassability and exact risk controlled. Then propose lower-cost designs that still satisfy the stated obligation.

## 8. Blocked design behavior

If Shailja returns `BLOCKED_NON_COMPLIANT`, Mahesh marks the mechanism `REDESIGN_REQUIRED`, does not continue implementation on that mechanism, identifies the blocking obligation, proposes compliant alternatives, and escalates only where authoritative interpretation/facts are genuinely disputed. Schedule pressure is never a bypass reason.

## 9. Conditional approval behavior

For `APPROVED_WITH_CONDITIONS`, Mahesh converts blocking controls into implementation/architecture acceptance criteria, tracks each control and evidence requirement, separates backlog-capable items, ensures DoD verifies closure evidence, and re-routes material design changes.

## 10. Lower-severity flexibility

For backlog-capable findings, Mahesh may recommend proceeding only when Shailja marks the issue as eligible, no mandatory/non-bypassable obligation is violated, residual risk is understood, ownership/target are recorded, required human acceptance exists and compensating controls are incorporated where required.

## 11. Separation of classifications

- `A0–A3` — Mahesh architecture severity;
- `R0–R3` — Shailja compliance/risk severity;
- `P1–P5` — AIGEM delivery priority.

They must never be treated as interchangeable.
