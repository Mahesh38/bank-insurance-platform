# 07 — Architecture Review and Evidence Contract

## 1. Purpose

This contract standardises how humans/agents request a Principal Architect review and how the architecture verdict is returned into AIGEM Board 1.

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

Missing fields may be inferred only when evidence is available. Material unknowns should become assumptions or clarification items rather than invented facts.

## 3. Canonical architecture response

```yaml
architecture_review:
  decision_id: ARCH-DEC-0001
  decision: APPROVED | APPROVED_WITH_CONDITIONS | REWORK | REJECTED | ESCALATE
  architecture_severity: A0 | A1 | A2 | A3 | NONE
  authority_class: A1_AUTONOMOUS | A2_NOTIFY | A3_JOINT_REVIEW | A4_HUMAN_REQUIRED
  confidence: HIGH | MEDIUM | LOW
  summary: "..."

  boundary_assessment:
    ownership: "..."
    coupling: "..."
    consistency: "..."
    deployment: "..."

  findings:
    - id: A-F01
      severity: A0 | A1 | A2 | A3
      finding: "..."
      evidence: "..."
      disposition: MUST_FIX | CONDITION | BACKLOG | RECOMMENDATION

  alternatives:
    - option: "..."
      benefits: []
      costs: []
      reversibility: HIGH | MEDIUM | LOW

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

A consequential architecture verdict should cite evidence from the plan/repository/accepted decisions. Valid evidence includes:

- accepted requirement/acceptance criteria;
- current-state or project-scope constraint;
- accepted ADR/architecture baseline;
- service/data ownership definition;
- API/schema/sequence diagram;
- measured latency/volume/error/capacity evidence;
- known integration contract;
- Security/Compliance control finding;
- failing test/fitness function/operational incident;
- approved enterprise standard.

`This is best practice` is not sufficient evidence for a blocking finding.

## 5. AIGEM Board 1 adapter

Translate the persona decision into the canonical Board 1 verdict:

| Persona decision | AIGEM Architecture verdict |
|---|---|
| `APPROVED` | `APPROVED` |
| `APPROVED_WITH_CONDITIONS` | `APPROVED_WITH_CONDITIONS` |
| `REWORK` | `REWORK` |
| `REJECTED` | `REJECTED` |
| `ESCALATE` | Gate remains unapproved until required human/change-control decision |

Every Board 1 verdict must still satisfy `docs/governance/11-REVIEW_GATES.md`, including human requirements by tier.

## 6. Review scope

The Architect reviews Architecture questions and should not duplicate other boards.

Architecture may identify that a concern belongs to Product, Security, Compliance, QA or Operations and route it, but must avoid pretending to grant that board's verdict.

## 7. Architecture control-resolution

When a plan is returned for rework/conditions, the author should respond per finding:

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

Architecture re-reviews only affected findings unless the revised design materially changes the plan.

## 8. Definition of a good architecture review

A good review makes the next action obvious. It distinguishes:

- blocker versus debt versus optimization;
- architecture concern versus another board's concern;
- required change versus preferred style;
- target architecture versus temporary exception;
- evidence versus opinion.
