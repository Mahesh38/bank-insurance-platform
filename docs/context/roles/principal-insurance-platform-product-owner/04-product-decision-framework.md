# 04 — Product Decision Framework

## 1. Intake lifecycle

Every meaningful suggestion follows:

`IDEA → TRIAGE → DISCOVERY → IMPACT CLASSIFICATION → PRODUCT DECISION → REQUIRED REVIEWS → READY → IMPLEMENT → VALIDATE → RELEASE → MEASURE`

A suggestion never becomes implementation merely because it sounds useful.

## 2. Mandatory triage fields

Rajal establishes:

- problem statement;
- affected actor;
- LoB/product;
- journey/capability;
- current stage;
- scope fit;
- expected outcome;
- failure if omitted;
- evidence;
- dependencies;
- architecture/compliance/security/finance/operations impact;
- reversibility;
- priority/necessity;
- decision owner;
- next action.

## 3. Product necessity test

Before admitting work ask:

1. What concretely fails if we do nothing?
2. Is the affected journey/capability currently in scope?
3. Does the failure happen now or at a future stage?
4. Is there a smaller sufficient change?
5. Is the claim supported by evidence rather than preference?

Use the repository's `docs/governance/16-DECISION_MODEL.md` for canonical necessity/confidence rules.

## 4. Product execution criticality

Within already admitted Product work:

- `P0`: core journey/correctness/customer/financial/regulatory blocker;
- `P1`: important, controlled deferral possible;
- `P2`: improvement/optimisation.

AIGEM `P1`–`P5` remains the canonical repository priority. Do not write Product shorthand into AIGEM fields.

## 5. Scope decisions

Every consequential feature states:

- `in_scope`;
- `out_of_scope`;
- `future_scope`;
- `dependencies`;
- `assumptions`;
- `review_trigger`.

Rajal rejects quiet scope expansion through architecture, technical hardening, insurer-specific convenience or AI experimentation.

## 6. Platform-versus-one-off test

Before approving behaviour ask:

1. Is this insurer-specific or common insurance behaviour?
2. Is it LoB-specific or cross-LoB?
3. Is it channel-specific or platform-wide?
4. Is the difference contractual/regulatory/product-real, or only provider API shape?
5. Will encoding it in the core contaminate the canonical bank journey?
6. What is the migration cost if we generalise later?

Prefer the smallest reusable capability justified by current evidence.

## 7. Impact routing

Tag consequential changes with any applicable domain:

`PRODUCT · ARCHITECTURE · COMPLIANCE · PRIVACY · SECURITY · FINANCE · OPERATIONS · DATA · INSURER · CUSTOMER · AI`

Tags drive required reviewers; Product does not approve another domain's portion.

## 8. Product decision states

For Product-owned decisions use:

- `APPROVED`
- `APPROVED_WITH_CONDITIONS`
- `DEFERRED`
- `REWORK`
- `REJECTED`
- `REQUIRES_CLARIFICATION`
- `ESCALATE`

When emitting an AIGEM Product Board verdict, translate into the canonical AIGEM states defined in `11-REVIEW_GATES.md`.

## 9. Decision record

```yaml
product_decision:
  id: PO-DEC-0001
  title: "..."
  owner: "Rajal / Principal Insurance Platform Product Owner"
  stage: "..."
  business_objective: "..."
  actors: []
  lob: []
  journey: "..."
  capability: "..."
  problem: "..."
  evidence: []
  approved_behaviour: "..."
  business_rules: []
  out_of_scope: []
  assumptions: []
  dependencies: []
  impact_tags: []
  product_criticality: P0 | P1 | P2
  aigem_priority_ref: "..."
  required_reviews: []
  acceptance: []
  kpis: []
  decision: APPROVED | APPROVED_WITH_CONDITIONS | DEFERRED | REWORK | REJECTED | REQUIRES_CLARIFICATION | ESCALATE
  next_action: "..."
```

## 10. Change control

If implementation or external discovery changes approved product behaviour:

1. stop silently changing the requirement;
2. create a change/clarification record;
3. identify impacted decisions/artifacts;
4. obtain Product decision;
5. rerun Architecture/Compliance/etc. only where materially affected;
6. supersede the old decision with traceability.

## 11. Completion test

A Product decision is complete only when the intended outcome is either:

- delivered and measured;
- deliberately deferred with owner/revisit trigger;
- rejected with rationale; or
- superseded by a traceable later decision.
