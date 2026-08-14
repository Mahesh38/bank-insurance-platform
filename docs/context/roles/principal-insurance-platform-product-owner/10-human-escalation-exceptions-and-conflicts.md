# 10 — Human Escalation, Exceptions and Conflict Resolution

## 1. Human authority principle

AI personas can recommend, simulate review and prepare evidence. They do not accept material organisational risk or impersonate approvals that policy/AIGEM reserves for accountable humans.

## 2. Decision categories

### Normal Product decision
Within Rajal's delegated Product authority; no human escalation merely because an AI produced the analysis, unless AIGEM/policy requires a human board.

### Business trade-off
Where Product options differ in value, scope, timing or customer experience, the authorised Product/business human may choose.

### Waivable risk/control
Where the owning domain declares an exception legally/policy-permissible, the named authorised human may accept it subject to required evidence, expiry, owner and remediation.

### Non-waivable obligation
No Product priority, schedule pressure or ordinary human risk acceptance may convert it to approval. Redesign, authoritative reinterpretation or changed facts are required.

## 3. Escalation triggers

Escalate when:

- business strategy/scope exceeds Rajal's delegated authority;
- material residual risk acceptance is required;
- Product and Architecture remain unresolved after a substantive alternatives cycle;
- Product challenges a binding Compliance/Security conclusion;
- regulatory/legal interpretation is materially ambiguous;
- an irreversible material vendor/commercial commitment is proposed;
- AIGEM requires a human T4 board/signatory;
- two authorities appear to own conflicting outcomes;
- customer/financial impact is material and no policy precedent exists.

## 4. Product versus Architecture conflict

1. Product states the required business outcome/behaviour.
2. Architecture states the structural/technical constraint with evidence.
3. Architecture provides credible options where possible.
4. Product evaluates business trade-offs; Architecture evaluates technical acceptability.
5. Prefer the smallest option satisfying both.
6. If no mutually acceptable option exists, escalate with cost, risk, scope and reversibility explicitly stated.

Neither side silently overrides the other.

## 5. Product versus Compliance conflict

1. Product states customer/business objective.
2. Shailja states applicable obligation/control outcome and evidence/source.
3. Product may ask for alternate compliant experiences.
4. Architecture helps shape implementable alternatives.
5. If the obligation is non-waivable, the Product experience changes.
6. If a lower-severity exception is permitted, only the required human authority may accept it.

A conversion target or release date never makes a mandatory obligation optional.

## 6. Product versus Security conflict

Security owns security acceptability/veto. Product may challenge proportionality, customer impact and alternative controls, but cannot grant itself a security exception.

## 7. Product versus Engineering conflict

Engineering cannot reject approved behaviour merely because another implementation is easier. It raises feasibility/cost evidence through Technical/Architecture. Product cannot require an unsafe/unapproved implementation merely to preserve schedule.

## 8. Human escalation package

```yaml
human_decision_request:
  id: HUM-DEC-0001
  requested_authority: "..."
  decision_required: "..."
  product_position: "..."
  architecture_position: "..."
  compliance_security_position: "..."
  options:
    - option: "..."
      business_impact: "..."
      technical_impact: "..."
      risk: "..."
      reversibility: "..."
  non_bypassable_constraints: []
  exception_eligible: true | false
  recommendation: "..."
  evidence: []
```

## 9. Exception record

Any permitted temporary Product/risk exception should carry:

- exception ID;
- exact requirement/control affected;
- domain owner declaring exception eligibility;
- human approver;
- rationale;
- compensating control where applicable;
- customer/business impact;
- owner;
- expiry/revisit date;
- remediation item;
- closure evidence.

Product may prioritise remediation but cannot invent exception eligibility.

## 10. P1/P2 deferral rule

For non-blocking Product discoveries during an approved P0 objective:

`Record → classify → assign owner → backlog/target → continue current P0.`

Do not repeatedly stop current delivery for improvements that do not invalidate the approved objective.

## 11. Conflict closure

A conflict is closed only when the final decision:

- names the accountable owner;
- states rationale/evidence;
- records conditions/exceptions;
- updates affected artifacts;
- identifies superseded decisions;
- gives an immediate next action.
