# Stakeholder personas

**Parent:** [`docs/context/README.md`](../README.md)  
**Status:** 🟡 Non-binding context — see the [context folder rules](../README.md#what-this-folder-is)

---

## What these are

Each persona captures how one role thinks — domain focus, vocabulary, priorities, decision posture and the questions the role asks first. Personas are grounding context; they never replace governing SSOT, regulation, enterprise policy or AIGEM review gates.

---

## The panel

| Role | Canonical persona | Optional extension | Domain focus |
|---|---|---|---|
| 📋 **Rajal — Principal Insurance Platform Product Owner** | [Principal Product Owner package](./principal-insurance-platform-product-owner/README.md) | — | Insurance/bancassurance Product authority, journeys, scope, prioritisation, Product acceptance and outcomes |
| 🏛️ **Mahesh — Principal Insurance Platform Architect** | [Stable Mahesh entrypoint](./mahesh-solution-architect.md) + [modular Principal Architect package](./mahesh-principal-insurance-platform-architect/README.md) | [Agentic-AI evolution](./mahesh-solution-architect-agentic-ai-evolution.md) | Architecture ownership, DDD/boundaries, HLD/LLD, integration, distributed systems, insurance architecture, governance |
| ⚙️ **Amit — Technical Head / Principal Engineering function** | [Amit — Technical Head](./amit-technical-head.md) | [Agentic evolution](./amit-technical-head-agentic-ai-evolution.md) | Engineering leadership, implementation standards, AWS EKS, CI/CD, code quality, reliability & SLAs |
| 🗄️ **Aarti — Principal Insurance Data & Database Architect / DBA** | [Aarti DBA package](./principal-insurance-data-database-architect/README.md) | — | Persistence architecture, insurance data modelling, integrity, performance, migrations, backup/recovery, DB operations |
| 🧪 **Swapnali — Principal Insurance Quality Engineering / QA Lead** | [Swapnali QA Lead package](./swapnali-qa-lead/README.md) | [Service QA strategy](../../1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md) | Risk-based test strategy, insurance-critical journeys, release evidence, waivers, regression, quality metrics |
| 🛡️ **Shailja S — Compliance & Risk Head** | [Shailja S package](./shailja-s-compliance-risk-head/README.md) | — | Insurance compliance, privacy, cyber/technology risk, evidence, exceptions |

## 📋 Rajal — Product

Rajal owns **WHAT / WHY / FOR WHOM / Product behaviour / scope / priority / acceptance / outcome**. She is the named reasoning persona for **Board 3 — Product**.

## 🏛️ Mahesh — Architecture

The repository intentionally has one Architecture persona: **Mahesh is the Principal Insurance Platform Architect**. He is the named reasoning persona for **Board 1 — Architecture**.

## ⚙️ Amit — Engineering

Amit is the canonical Technical Head and carries the Principal Engineering function. Engineering owns implementation, developer test implementation, CI/CD, runtime reliability and code quality within approved Product/Architecture/Database/Compliance boundaries.

## 🗄️ Aarti — Database/DBA

Aarti is the specialist persistence authority. She is not an additional AIGEM board; she participates through the applicable Architecture, Technical, Risk/Compliance and Operations boards.

## 🧪 Swapnali — QA Lead

**[Open Swapnali's Principal Insurance Quality Engineering / QA Lead package](./swapnali-qa-lead/README.md).**

Swapnali is the canonical platform QA Lead persona and the named reasoning persona for the existing **Board 5 — QA**. She owns risk-based test strategy, quality evidence sufficiency, critical-journey regression, QA waivers, automation signal quality and quality-exit recommendation.

She does **not** take developer-side tests away from Engineering. Developers/Engineering still implement and maintain unit/component/integration automation as assigned; Swapnali decides what evidence is sufficient and independently reviews it.

The service-specific QA artefacts remain authoritative for concrete thresholds and backlog:

- [`QA-LEAD-TESTING-STRATEGY.md`](../../1sb-insurance-integration/service-ssot/QA-LEAD-TESTING-STRATEGY.md)
- [`TESTING-RULES.md`](../../1sb-insurance-integration/service-ssot/TESTING-RULES.md)
- [`COVERAGE.md`](../../1sb-insurance-integration/service-ssot/COVERAGE.md)
- [`TEST-BACKLOG.md`](../../1sb-insurance-integration/service-ssot/TEST-BACKLOG.md)

Swapnali's local `Q0`–`Q3` labels are quality finding severity and must not replace AIGEM `P1`–`P5`, Rajal Product criticality, Mahesh architecture severity, Aarti database severity or Shailja risk severity.

Recommended loading order:

1. Swapnali package `README.md`;
2. `01-persona.md`;
3. `03-authority-and-decision-rights.md`;
4. `04-risk-based-test-strategy.md`;
5. `05-critical-journeys-and-non-bypassable-gates.md`;
6. `06-release-waiver-and-operating-contract.md`;
7. applicable service QA SSOT;
8. current AIGEM state and review gates;
9. the canonical persona authority matrix.

## 🛡️ Shailja S — Compliance & Risk

Shailja remains independent from QA. Swapnali answers “was the required behaviour tested and what does the evidence prove?” Shailja answers “is the behaviour/control posture permissible and is the issue waivable?”

---

## Shared cross-authority protocols

For consequential ownership, review, approval, block and escalation questions use:

→ [`shared/cross-persona-operating-model.md`](./shared/cross-persona-operating-model.md)

and the canonical authority matrix:

→ [`docs/governance/PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md)

The matrix uses **O/A/R/C/RV/AP/B/I/NA** so agents know not only who performs work, but who owns, reviews, approves, may block, is informed and is explicitly not authorised.

Focused protocols remain available for Product ↔ Architecture ↔ Compliance and Mahesh ↔ Shailja decisions. Neither replaces AIGEM or authoritative policy/regulation.

---

## Shared source

All personas are grounded in the same problem statement:

→ [`../business-problem-statement.md`](../business-problem-statement.md)
