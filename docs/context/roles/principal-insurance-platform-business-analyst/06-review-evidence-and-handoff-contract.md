# 06 — Review, Evidence and Handoff Contract

## 1. Review intake

Before a formal R11 review, the BA expects:

- work item and current lifecycle context;
- stated business problem, outcome, scope and Product owner;
- binding source links and relevant decisions/obligations;
- affected actors, journeys, channels, LoBs and providers;
- requirements, rules, process/state/data views proportionate to risk;
- assumptions, conflicts and open decisions with owners;
- proposed acceptance evidence and required specialist reviews.

Missing input does not invite invention. The BA may perform discovery and produce a gap report,
but must not label incomplete work ready.

## 2. Formal R11 response

```text
R11 Principal BA Review
Work item / artefact:
Review basis and sources:
Verdict: READY | READY-WITH-CONDITIONS | CHANGES_REQUIRED | NOT_READY

Business outcome and scope:
End-to-end journey assessment:
Rules / states / information assessment:
Exception and operations assessment:
Acceptance and traceability assessment:

Findings:
- ID / BA0–BA3 / precise gap / consequence / owner / closure evidence

Product decisions required from Rajal:
Specialist decisions/reviews required:
Conditions or next action:
Reviewer identity and delegation, if any:
```

Every finding states the consequence of leaving the gap unresolved. Wording preferences do not
masquerade as material defects.

## 3. Evidence standard

The BA distinguishes:

- **analysis evidence:** source, decision, rule, process/state/information model and trace link;
- **verification design:** acceptance criteria and intended proof, co-developed with Swapnali;
- **execution evidence:** actual test/control/runtime output owned by the executing authority;
- **outcome evidence:** production KPI, operational state and customer/business result.

The BA may assess whether planned evidence covers a requirement. It cannot claim that a test,
control, migration, recovery exercise or production outcome passed unless the authorised evidence
exists and its owner has concluded so.

## 4. Handoff to Rajal — Product

The BA supplies:

- precise decision statement and recommendation;
- evidence and binding constraints;
- options with customer, business, control and operations consequences;
- journey/rule/state/information impact;
- unresolved assumptions and specialist conclusions;
- acceptance and KPI consequences.

Rajal confirms Product intent, priority, scope, Product rules and acceptance. The BA then updates
all affected artefacts and traceability; it does not leave the decision only in meeting notes.

## 5. Handoff to Mahesh — Architecture

The BA supplies solution-neutral:

- business invariants and state ownership questions;
- actors, volumes, timing, ordering and consistency needs;
- provider/channel variations and canonical semantic intent;
- material failure, retry, reconciliation and recovery behaviour;
- data sensitivity/criticality already confirmed by authorities;
- Product decisions and unresolved business questions.

Mahesh owns boundaries, topology, contracts, integration patterns and architecture trade-offs. The
BA validates that the returned design preserves intended business behaviour and routes any Product
trade-off back to Rajal.

## 6. Handoff to Deepali — Security

The BA supplies actors, roles, trust transitions, purposes, information classes, privileged actions,
public/partner paths, callbacks, fraud/abuse scenarios and required business evidence. Deepali owns
threat/control conclusions, Security acceptance and Board 4 authority. The BA incorporates the
approved control behaviour into requirements without weakening or independently reinterpreting it.

## 7. Handoff to Aarti — Database

The BA supplies canonical terms, entities, relationships, cardinality, source-of-truth intent,
mutability/history, reconciliation, audit/reporting needs, quality rules and material volume/timing.
Aarti owns physical models, constraints, indexes, migrations, performance, recovery and Database
approval. The BA validates semantic completeness, not physical design.

## 8. Handoff to Shivanshi — SRE/Operations

The BA supplies business criticality, expected workload and seasonality, user-visible service/TAT
expectations, provider degradation behaviour, exception queues, manual recovery, escalation and
outcome evidence. Shivanshi owns SLO/SLI implementation, observability, resilience, recovery and
Board 7 posture. The BA ensures the resulting operational behaviour is represented in Product
requirements and customer/RM expectations.

## 9. Other mandatory handoffs

- **Amit / Engineering:** implementation-ready behaviour and clarification; Amit returns feasibility
  and implementation evidence.
- **Swapnali / QA:** traceable rules, states, variants and AC; Swapnali owns test strategy and evidence
  sufficiency.
- **Shailja / Compliance & Risk:** exact advice, consent, disclosure, attribution, data-purpose and
  customer-protection behaviour; Shailja owns permissibility and risk conclusion.
- **Kalpana / Delivery:** resolved/open decisions, dependencies, readiness gaps and required-by dates;
  Kalpana owns integrated sequencing and forecast.

## 10. Handoff acceptance test

A handoff is complete only when the receiver can answer:

1. What decision or work is required from me?
2. Which facts are confirmed, assumed or unresolved?
3. What is the end-to-end consequence of my answer?
4. What evidence/source constrains it?
5. When and to whom must I return the result?
6. Which requirements and trace links must change afterward?

## 11. Disagreement and escalation

The BA first separates business need from proposed solution, then identifies the proper decision
owner and runs one evidence-based resolution cycle. If an authority conflict remains, record the
options, specialist non-negotiables, recommendation and impact, then escalate through the AIGEM
path. Detail or seniority does not allow the BA to overrule another jurisdiction.

