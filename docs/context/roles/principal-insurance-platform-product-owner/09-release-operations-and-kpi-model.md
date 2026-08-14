# 09 — Release, Operations and KPI Model

## 1. Product release principle

`Code Complete` is not `Product Ready`.

A release is Product-ready only when the approved behaviour can be safely operated, evidenced and measured.

## 2. Product readiness checklist

As applicable, Rajal verifies:

- approved Product scope matches delivered scope;
- all P0 Product blockers are closed;
- acceptance criteria pass;
- material negative/exception flows are demonstrated;
- Architecture conditions are resolved;
- Compliance/Security conditions are resolved;
- required human approvals exist;
- analytics/events/KPIs are observable;
- Operations has failure/recovery procedures;
- Finance/reconciliation path is ready where money is involved;
- known limitations are documented;
- support/escalation ownership is known;
- rollback/customer-remediation implications are understood.

## 3. Operational readiness

For each material journey Product ensures Operations can answer:

- how a failed/stuck case is identified;
- who owns intervention;
- what the SLA/TAT is;
- what retry/resume/remediation is allowed;
- how insurer/aggregator follow-up occurs;
- how customer complaints are handled;
- how payment/issuance mismatches are reconciled;
- what dashboard/report exposes unresolved cases;
- what evidence proves closure.

Operations owns procedures; Product owns the required business-operational capability.

## 4. Core KPI families

### Acquisition / Need
Lead volume · need-analysis completion · suitability completion · eligibility failure.

### Quote
Quote-request success · insurer response coverage · quote latency · quote-to-selection conversion · quote expiry/failure.

### Proposal
Proposal start/completion · field/question failure · abandonment · resume rate.

### Underwriting
STP rate · referral/medical rate · rejection · underwriting TAT · additional-information loops.

### Payment
Payment initiation/success/failure · duplicate/timeout · refund/reversal · unreconciled payment.

### Issuance
Issuance success · issuance TAT · payment-to-issuance mismatch · proposal-to-policy conversion.

### Business
Policies sold under the governed definition · premium/business volume · LoB/product/insurer/channel mix · branch/RM performance.

### Operations
Manual intervention · stuck cases · SLA breach · reconciliation exception · reopen/rework.

### Customer Experience
Journey completion time · drop-off by step · repeat data entry · failure/retry · complaint/support contact · assisted-to-self-service transition where applicable.

## 5. KPI contract

Each KPI should define:

```yaml
kpi:
  id: KPI-0001
  name: "..."
  business_meaning: "..."
  journey: "..."
  formula: "..."
  numerator: "..."
  denominator: "..."
  source: "..."
  owner: "..."
  frequency: "..."
  target_or_threshold: "..."
  segmentation: []
  action_when_bad: "..."
```

A metric without agreed meaning/source/action is not a useful Product KPI.

## 6. Release decision record

```yaml
product_release_readiness:
  release: "..."
  product_scope_match: PASS | FAIL
  p0_open: []
  acceptance: PASS | FAIL
  architecture_gate: "..."
  compliance_gate: "..."
  security_gate: "..."
  operations_ready: true | false
  finance_ready: true | false | not_applicable
  analytics_ready: true | false
  known_limitations: []
  accepted_exceptions: []
  product_decision: READY | NOT_READY | READY_WITH_CONDITIONS
  next_action: "..."
```

This Product decision supplements, not replaces, AIGEM/release-management gates.

## 7. Post-release ownership

Rajal reviews whether the intended Product outcome materialised. Significant divergence triggers:

- hypothesis review;
- requirement/journey correction;
- backlog reprioritisation;
- assumption invalidation;
- Architecture/Compliance re-review if the corrective change affects them.

A delivered feature with no measurable benefit should not remain protected merely because it was once approved.
