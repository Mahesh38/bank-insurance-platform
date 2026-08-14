# 07 — Mahesh Architecture Review and Evidence Contract

## 1. Purpose

This contract standardises how humans/agents request a review from **Mahesh — Principal Insurance Platform Architect** and how the architecture verdict returns to AIGEM Board 1.

## 2. Recommended request envelope

```yaml
architecture_review_request:
  request_id: ARCH-REQ-0001
  work_item: "FUNC/NFR/TD/..."
  plan_id: "PLAN-..."
  project_stage: "..."
  business_objective: "..."
  current_architecture: "..."
  proposed_change: "..."
  affected_domains: []
  affected_components: []
  data_changes: []
  api_changes: []
  event_changes: []
  infrastructure_changes: []
  security_impact: none | low | material
  compliance_impact: none | low | material
  operational_impact: none | low | material
  constraints: []
  accepted_adrs: []
  alternatives_considered: []
  requested_decision: "..."
```

Material unknowns become assumptions/clarification items rather than invented facts.

## 3. Canonical architecture response

```yaml
architecture_review:
  decision_id: ARCH-DEC-0001
  reviewer_persona: "Mahesh — Principal Insurance Platform Architect"
  decision: APPROVED | APPROVED_WITH_CONDITIONS | REWORK | REJECTED | ESCALATE
  architecture_severity: A0 | A1 | A2 | A3 | NONE
  authority_class: A1_AUTONOMOUS | A2_NOTIFY | A3_JOINT_REVIEW | A4_HUMAN_REQUIRED
  confidence: HIGH | MEDIUM | LOW
  summary: "..."
  findings: []
  alternatives: []
  recommendation:
    option: "..."
    rationale: "..."
  required_board_reviews:
    product: true | false
    technical: true | false
    security: true | false
    qa: true | false
    risk_compliance: true | false
    operations: true | false
  adr:
    required: true | false
    reason: "..."
  debt_or_exception:
    required: true | false
    type: DEBT | EXCEPTION | NONE
    reason: "..."
  revisit_trigger: "..."
  next_action: "..."
```

## 4. Evidence requirements

Consequential verdicts should cite accepted requirements/criteria, current-state/scope constraints, accepted ADRs/baselines, ownership definitions, API/schema/sequence evidence, measured latency/volume/capacity evidence, integration contracts, Security/Compliance findings, failing tests/incidents or approved enterprise standards. `This is best practice` alone is not blocking evidence.

## 5. AIGEM Board 1 adapter

| Mahesh decision | AIGEM Architecture verdict |
|---|---|
| `APPROVED` | `APPROVED` |
| `APPROVED_WITH_CONDITIONS` | `APPROVED_WITH_CONDITIONS` |
| `REWORK` | `REWORK` |
| `REJECTED` | `REJECTED` |
| `ESCALATE` | Gate remains unapproved until required human/change-control decision |

Every Board 1 verdict must still satisfy `docs/governance/11-REVIEW_GATES.md`, including human requirements by tier.

## 6. Review scope

Mahesh reviews Architecture questions and routes concerns owned by Product, Security, Compliance, QA or Operations rather than impersonating those boards.

## 7. Architecture resolution

```yaml
architecture_resolution:
  decision_id: ARCH-DEC-0001
  findings:
    - id: A-F01
      status: RESOLVED | DEFERRED_WITH_APPROVAL | NOT_RESOLVED
      change: "..."
      evidence: "..."
  plan_status: READY_FOR_REVIEW | HUMAN_DECISION_REQUIRED | REDESIGN_REQUIRED
```

Re-review only affected findings unless the revised design materially changes the plan.

## 8. Definition of a good architecture review

A good review makes the next action obvious and distinguishes blocker vs debt vs optimization, Architecture vs another board's concern, required change vs style preference, target architecture vs temporary exception, and evidence vs opinion.
