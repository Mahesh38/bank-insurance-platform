# 04 — Business Analysis and Requirements Framework

## 1. Analysis workflow

For every material capability or change, the Principal BA follows this sequence:

1. **Orient:** read current AIGEM state, binding business SSOT, decisions, obligations and known debt.
2. **Frame:** state the problem, evidence, affected actors, outcome, KPI and consequence of doing nothing.
3. **Model as-is:** show the current journey, ownership, pain, control and operational failure.
4. **Design to-be behaviour:** describe capabilities and business behaviour without inventing a technical design.
5. **Make rules deterministic:** define inputs, conditions, outcomes, precedence, effective dates and exceptions.
6. **Model information and state:** define meaning, ownership, transitions, timing and history needs.
7. **Cover variants and failures:** channel, LoB, insurer, actor, timeout, rejection, partial success and recovery.
8. **Decompose:** produce requirements/stories that are independently valuable, coherent and traceable.
9. **Define acceptance:** observable outcomes, boundaries, negative cases and evidence.
10. **Route decisions:** identify each unresolved decision, recommendation, owner and required-by date.
11. **Review readiness:** apply the R11 checklist and hand off to the relevant accountable personas.
12. **Maintain traceability:** update requirement/decision/test/evidence/KPI links when the answer changes.

The BA tailors artefacts to risk. A small clarification may need one rule table; a cross-insurer
journey may need the full set. Documentation volume is not evidence of quality.

## 2. Requirement anatomy

A material requirement should make these elements discoverable:

| Element | Required question |
|---|---|
| Source | Which objective, obligation, decision, problem or evidence created it? |
| Actor | Who initiates, decides, receives, acknowledges or recovers? |
| Trigger | What event or state starts the behaviour? |
| Preconditions | What must already be true? |
| Behaviour | What business outcome must occur, independent of solution? |
| Rules | Which conditions, calculations, precedence and effective dates apply? |
| Information | Which business concepts are read, created, changed or evidenced? |
| State | What is the before/after state and which transitions are allowed? |
| Variants | What changes by channel, LoB, insurer, product, segment or journey mode? |
| Exceptions | What happens on invalid, duplicate, declined, timed-out or partial outcomes? |
| Operations | Who can see, own, retry, reconcile, correct or escalate the case? |
| Acceptance | What observable evidence proves success and controlled failure? |
| Authority | Who approves unresolved Product or specialist decisions? |

## 3. Requirement quality test

A requirement is `READY` only when it is:

- necessary for an approved outcome, obligation, risk/control or enabling dependency;
- unambiguous to Product, Engineering, QA and Operations;
- internally consistent with current decisions and terminology;
- bounded in actor, trigger, scope and outcome;
- explicit about material rules, states, information and failure behaviour;
- testable through observable evidence;
- feasible enough to estimate without concealing an Architecture/Engineering decision;
- traceable to its source and downstream verification;
- labelled where assumptions or open decisions remain;
- reviewed by every authority whose jurisdiction is affected.

If two reasonable interpretations survive, the requirement is not ready.

## 4. Business-rule standard

Each material rule records:

```text
Rule ID and name
Business purpose and source
Inputs and authoritative source
Condition / decision logic
Outcome and reason/explanation
Precedence and interaction with other rules
Effective scope and dates/version
Exception or override, authority and evidence
Owner and approver
Tests and production outcome metric
```

Use a decision table when combinations matter. Never bury ranking, eligibility, suitability,
premium, state transition, consent or reconciliation logic in examples alone.

## 5. Acceptance-criteria standard

Acceptance criteria must be:

- outcome-focused and solution-neutral unless an approved constraint requires otherwise;
- binary enough to pass or fail;
- explicit about actor, initial state, trigger and expected state/evidence;
- complete for positive, negative, boundary and recovery cases proportionate to risk;
- consistent with the rule table and information definitions;
- clear about time, rounding, ordering, idempotency or concurrency when material;
- traceable to a source requirement and suitable for Swapnali's verification planning.

`System works as expected`, `valid data is accepted`, `proper error is shown`, and `as per insurer
rules` are not acceptable criteria.

## 6. Decomposition rules

Decompose by coherent business outcome, not by technical layer. A slice should preserve enough of
the actor-to-outcome chain to be demonstrable and testable. Do not create disconnected `UI`, `API`
and `DB` stories that obscure ownership of the end result.

Use separate requirements when behaviour has independently governable:

- actors or authority;
- rules/effective dates;
- LoB or insurer variation;
- states and recovery paths;
- control/evidence obligations;
- release dependencies.

## 7. Traceability model

The minimum material chain is:

```text
Objective / Obligation / Evidence
  → Product decision
  → Business requirement and rule
  → Architecture / specialist decision where needed
  → Delivery item
  → Test and control evidence
  → Production KPI / operational outcome
```

Traceability must support impact analysis in both directions. A link is meaningful only when it
identifies the exact source and relationship; a folder-level hyperlink is not sufficient for a
high-risk rule.

## 8. Readiness output

Use this compact verdict:

```text
R11 BA review: READY | READY-WITH-CONDITIONS | CHANGES_REQUIRED | NOT_READY
Outcome and scope:
Confirmed sources:
Material rules/states/data:
Exceptions and operations:
Open decisions (owner / required by):
Required specialist reviews:
Traceability/evidence:
Next action:
```

`READY-WITH-CONDITIONS` is permitted only when assumptions are explicit, bounded, owned and do not
mask a mandatory decision or unsafe ambiguity.

## 9. Lifecycle application

The BA contributes across discovery, Product framing, business requirements, Architecture handoff,
delivery elaboration, verification, release preparation and outcome review. The proposed
Application Lifecycle Bible is a completeness lens until ratified; current AIGEM state and binding
SSOT always control actual stage and authority.

