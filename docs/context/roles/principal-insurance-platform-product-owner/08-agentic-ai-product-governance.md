# 08 — Agentic AI Product Governance

## 1. Principle

AI changes the actor model, not the underlying accountability model.

> **An agent may assist, explain, draft, recommend, classify or trigger permitted tools, but it does not become a loophole around deterministic business rules, customer rights, compliance controls or mandatory human authority.**

## 2. Agent-as-actor specification

Every agentic Product requirement identifies:

- agent role/persona;
- user/customer/RM beneficiary;
- allowed inputs;
- allowed knowledge sources;
- allowed tools/actions;
- prohibited actions;
- deterministic hard gates;
- human confirmation point;
- fallback/escalation;
- audit/attribution requirement;
- success and harm metrics.

## 3. Hard-gate preservation

Agentic UX must not bypass required deterministic or governed checks such as, where applicable:

- suitability/eligibility;
- consent/disclosure;
- authentication/authorisation;
- payment/customer-device restrictions;
- underwriting/insurer decision;
- policy issuance confirmation;
- financial reconciliation;
- mandatory compliance/security controls.

The exact current hard gates are retrieved from authoritative Product/Compliance/Architecture sources.

## 4. Agentic acceptance criteria

Each material agentic story should test both **allowed success** and **forbidden action**.

Example pattern:

```gherkin
Scenario: Agent performs a permitted assisted action
  Given the user has satisfied all required prerequisites
  When the agent prepares the requested action
  Then the output is grounded in approved sources
  And the required human/system confirmation occurs before a consequential write
  And the action is attributable in the audit evidence

Scenario: Agent attempts an action before a mandatory gate
  Given the mandatory prerequisite is incomplete
  When the agent attempts the protected action
  Then the platform blocks the action deterministically
  And records the attempted action and block reason
  And offers the permitted next step
```

## 5. Grounding

Product facts about coverage, price, exclusion, eligibility, policy terms, insurer/product features or regulated disclosures must come from governed authoritative sources appropriate to the use case.

Rajal treats materially ungrounded customer-facing insurance claims as Product/safety defects, not merely conversational-quality issues.

## 6. Human-in-the-loop

Human confirmation is required wherever Product/Compliance/Security/enterprise policy says accountability cannot be delegated.

The requirement must state **who** confirms **what**, not merely `human in the loop`.

Examples:

- RM confirms an agent-drafted proposal field set;
- customer provides consent directly;
- authorised Operations user accepts a remediation action;
- human board owner signs a T4 gate.

## 7. Agent autonomy classification

For Product purposes classify agent actions as:

- `A0_READ_EXPLAIN` — retrieve/summarise/explain;
- `A1_DRAFT` — prepare a draft for human confirmation;
- `A2_RECOMMEND` — rank/recommend within governed rules, with evidence;
- `A3_EXECUTE_REVERSIBLE` — perform a permitted reversible action under explicit controls;
- `A4_CONSEQUENTIAL` — customer/financial/regulatory/irreversible action; requires explicit governance and normally stronger deterministic/human controls.

This Product classification does not replace AIGEM risk tiers or Architecture/Compliance severity models.

## 8. Product KPIs for agentic features

Prefer metrics such as:

- groundedness/source coverage;
- task completion time;
- RM/customer correction or override rate;
- guardrail-block rate and reasons;
- escalation/fallback rate;
- business journey completion;
- complaint/error rate;
- false recommendation/action rate;
- human time saved;
- audit completeness.

Do not use raw `automation %` as the headline success metric if it incentivises bypassing appropriate human checkpoints.

## 9. Agentic Product failure modes

Rajal explicitly reviews:

- hallucinated product facts;
- stale catalogue/regulation context;
- incorrect tool choice;
- action beyond authority;
- failure to escalate uncertainty;
- hidden prompt/context causing unfair/inappropriate recommendation;
- excessive collection/exposure of customer data;
- inconsistent answer for a deterministic business rule;
- agent action without attribution;
- fallback that silently changes the customer journey.

## 10. Policy Sold in the agentic era

An agent may identify anomalies, explain status or propose remediation. It may not redefine or independently assert a `Sold` outcome contrary to the deterministic/business/financial evidence required by the governing Product definition.

## 11. Agent change control

Prompt/model/tool/retrieval changes that can materially alter customer or regulated behaviour are Product changes and may also require Architecture, Security and Shailja review. They are not treated as invisible implementation-only tuning.
