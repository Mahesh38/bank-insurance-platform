# 04 — Risk Taxonomy & Severity Model

## 1. Purpose

This taxonomy separates **risk severity (`R0`–`R3`)** from **AIGEM delivery priority (`P1`–`P5`)**. A low-effort fix can still address a severe risk, and a high-effort fix can still be a low-severity improvement.

## 2. Risk families

Use one or more of these tags:

- `REGULATORY`
- `LEGAL`
- `POLICYHOLDER_CONDUCT`
- `PRIVACY`
- `CYBER_SECURITY`
- `IDENTITY_ACCESS`
- `APPLICATION_SECURITY`
- `DATA_GOVERNANCE`
- `THIRD_PARTY`
- `OPERATIONAL`
- `RESILIENCE_BCP_DR`
- `FINANCIAL_INTEGRITY`
- `FRAUD_AML`
- `MODEL_AI`
- `REPUTATION`
- `AUDIT_ASSURANCE`

## 3. Severity levels

### R0 — Critical / Non-bypassable

Use where the proposed implementation creates or knowingly continues a material condition such as:

- confirmed violation of an applicable law, binding regulation or regulator direction for which no lawful discretionary exception exists;
- known unauthorised disclosure/access/use of customer or highly sensitive information with material impact;
- deliberate circumvention of mandatory authentication, authorization, consent or audit controls;
- a material cyber vulnerability or architecture condition reasonably capable of causing catastrophic compromise and lacking an adequate compensating control;
- release/change reasonably expected to create material systemic, financial, customer-safety or regulatory harm;
- intentional suppression/destruction of required evidence or audit trace;
- an action the organisation is not legally or regulatorily permitted to perform.

**Default outcome:** `BLOCKED_NON_COMPLIANT` or `REJECTED`.

**Risk acceptance:** not available merely to override the obligation. A different compliant solution may be proposed.

### R1 — High / Release-gating unless formally resolved

Examples:

- serious control deficiency with material customer/data/security impact but where lawful mitigation or compensating control may exist;
- significant privileged-access weakness;
- high-impact privacy/security issue with credible short-term mitigation;
- material untested recovery gap for a critical service;
- major vendor/control deficiency affecting a critical journey.

**Default outcome:** `APPROVED_WITH_CONDITIONS`, `RISK_ACCEPTANCE_REQUIRED`, `ESCALATE` or `REJECTED` depending on residual risk.

A temporary exception is exceptional, short-lived and requires senior risk approval plus compensating controls.

### R2 — Medium / Deferrable under controlled exception

Examples:

- non-critical control gap with bounded impact;
- hardening improvement not required for safe initial operation;
- incomplete automation where a reliable manual control exists;
- non-material evidence/process gap that can be remediated without exposing customers to significant risk.

**Default outcome:** may be `TEMPORARY_EXCEPTION_APPROVED` when an authorised human accepts the residual risk and a tracked remediation exists.

### R3 — Low / Improvement

Examples:

- optimisation;
- documentation refinement;
- low-risk standardisation gap;
- minor usability/governance improvement with no meaningful compliance exposure.

**Default outcome:** approve and backlog if useful.

## 4. Impact dimensions

Assess at least:

- regulatory/legal impact;
- customer/policyholder impact;
- personal-data impact;
- confidentiality;
- integrity;
- availability/resilience;
- financial impact;
- fraud potential;
- blast radius;
- exploitability/likelihood;
- detectability;
- recoverability;
- third-party propagation;
- reversibility;
- reputational impact.

## 5. Inherent and residual risk

Always distinguish:

- **Inherent risk:** risk before proposed controls.
- **Control effectiveness:** effectiveness of existing/proposed control.
- **Residual risk:** risk after controls.

A decision is based primarily on **residual risk plus mandatory obligations**, not on inherent risk alone.

## 6. Severity cannot be negotiated by title

Business urgency, sponsor seniority or project importance may affect response time and remediation sequencing, but not the factual severity of the risk.

A human reviewer may provide new facts or approve an exception within their authority; they may not simply relabel R0 as R2 to bypass governance. Delivery urgency may raise AIGEM priority, but it never lowers Shailja risk severity.
