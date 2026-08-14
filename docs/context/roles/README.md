# Stakeholder personas

**Parent:** [`docs/context/README.md`](../README.md)
**Status:** 🟡 Non-binding context — see the [context folder rules](../README.md#what-this-folder-is)

---

## What these are

Each persona captures **how one role thinks** — domain focus, vocabulary, priorities, decision posture, and the questions the role asks first. They are written as RAG/grounding context so that a discussion (or an AI assistant) can adopt a specific viewpoint deliberately rather than blending all perspectives into one voice.

They are **not** job descriptions, and they are **not** the authoritative stakeholder catalogue — that is [`knowledge-base/06-stakeholders.md`](../../au-bank-insurance-platform/knowledge-base/06-stakeholders.md).

For delivery decisions, the persona never replaces the governing SSOT, regulation, enterprise policy, or AIGEM review gate. It shapes the reviewer's reasoning; the authoritative source still determines the outcome.

---

## The panel

The Product Owner, Solution Architect, and Technical Head each have a **baseline** persona and an **agentic-AI evolution** file. Read the baseline first; the evolution file is a delta on top of it.

| Role | Baseline persona | Agentic-AI evolution | Domain focus |
|------|-----------------|---------------------|--------------|
| 📋 **Product Owner** | [rajal-product-owner.md](./rajal-product-owner.md) | [→ agentic evolution](./rajal-product-owner-agentic-ai-evolution.md) | Bancassurance vision, IRDAI CA0515, BRD/user stories, customer & RM journeys |
| 🏛️ **Solution Architect** | [mahesh-solution-architect.md](./mahesh-solution-architect.md) | [→ agentic evolution](./mahesh-solution-architect-agentic-ai-evolution.md) | Banking/insurance architecture, microservices, security, aggregator abstraction |
| ⚙️ **Technical Head** | [amit-technical-head.md](./amit-technical-head.md) | [→ agentic evolution](./amit-technical-head-agentic-ai-evolution.md) | Engineering leadership, AWS EKS, CI/CD, quality gates, reliability & SLAs |

### 🛡️ Shailja S — Compliance & Risk Head

**[Open the Shailja S persona package](./shailja-s-compliance-risk-head/README.md).**

Shailja S is intentionally a multi-file persona rather than a baseline/evolution pair because the role needs a stable decision model, regulatory-source registry, control catalogue, evidence policy, risk taxonomy, and human-exception policy in addition to conversational behaviour.

**AIGEM mapping:** Shailja S is the named persona for **Board 6 — Risk & Compliance** in [`docs/governance/11-REVIEW_GATES.md`](../../governance/11-REVIEW_GATES.md). When an agent runs that board, it should load Shailja's package in the order defined by its README and then emit the canonical AIGEM board verdict.

Important boundaries:

- Shailja's `R0`–`R3` labels are **risk severity**; AIGEM `P1`–`P5` remains **delivery priority**.
- `R0 / BLOCKED_NON_COMPLIANT` maps to AIGEM `REJECTED`; Board 6's existing binding veto applies. Ordinary risk acceptance cannot convert it to approval.
- Lower-severity gaps may use a time-bound human exception only when the package's eligibility rules are satisfied.
- For AIGEM T4 changes, an AI can simulate Shailja and draft the assessment, but **cannot satisfy the mandatory human Risk & Compliance sign-off**.
- The persona does not invent legal obligations: current authoritative regulation/policy/evidence always wins.

---

## Shared source

All personas are grounded in the same problem statement:

→ [`../business-problem-statement.md`](../business-problem-statement.md)

The three baseline/evolution role pairs are consolidated into a single panel view in [`../roadmaps/agentic-ai-transformation-roadmap.md`](../roadmaps/agentic-ai-transformation-roadmap.md). Shailja S is additionally anchored to AIGEM Board 6 because the role participates in formal risk/compliance gating.
