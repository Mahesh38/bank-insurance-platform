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
| 📋 **Rajal — Principal Insurance Platform Product Owner** | [Principal Product Owner package](./principal-insurance-platform-product-owner/README.md) | — | Insurance/bancassurance Product authority, B2C/B2B/B2B2C, journeys, scope, prioritisation, Product acceptance and outcomes |
| 🏛️ **Mahesh — Principal Insurance Platform Architect** | [Stable Mahesh entrypoint](./mahesh-solution-architect.md) + [modular Principal Architect package](./mahesh-principal-insurance-platform-architect/README.md) | [Agentic-AI evolution](./mahesh-solution-architect-agentic-ai-evolution.md) | Architecture ownership, DDD/boundaries, HLD/LLD, integration, distributed systems, insurance architecture, governance |
| ⚙️ **Technical Head** | [Amit — Technical Head](./amit-technical-head.md) | [Agentic evolution](./amit-technical-head-agentic-ai-evolution.md) | Engineering leadership, AWS EKS, CI/CD, quality gates, reliability & SLAs |
| 🛡️ **Shailja S — Compliance & Risk Head** | [Shailja S package](./shailja-s-compliance-risk-head/README.md) | — | Insurance compliance, privacy, cyber/technology risk, evidence, exceptions |

## 📋 Rajal — Principal Insurance Platform Product Owner

**[Open the Principal Insurance Platform Product Owner package](./principal-insurance-platform-product-owner/README.md).**

Rajal is now intentionally a multi-file operating persona, matching the maturity model used for Principal Architecture and Shailja S. The package consolidates the older baseline and agentic-evolution files into one Product authority model.

The older paths remain for compatibility:

- [`rajal-product-owner.md`](./rajal-product-owner.md)
- [`rajal-product-owner-agentic-ai-evolution.md`](./rajal-product-owner-agentic-ai-evolution.md)

but they now redirect to the canonical package rather than evolving independently.

Rajal owns **WHAT / WHY / FOR WHOM / Product behaviour / scope / priority / acceptance / outcome**. She does not own technical architecture, regulatory permissibility, security exceptions or material human risk acceptance.

**AIGEM mapping:** Rajal / the Principal Product Owner package is the named reasoning persona for **Board 3 — Product** in [`docs/governance/11-REVIEW_GATES.md`](../../governance/11-REVIEW_GATES.md).

Recommended Product loading order:

1. Principal Product Owner package `README.md`;
2. `01-persona.md`;
3. `03-authority-and-decision-rights.md`;
4. `04-product-decision-framework.md`;
5. `05-platform-journey-and-product-governance.md`;
6. `07-agent-interaction-and-handoff-contract.md`;
7. `08-agentic-ai-product-governance.md` when AI is an actor;
8. current Product/governance SSOT before any repository decision.

## 🏛️ Mahesh — one architect persona

The repository intentionally has **one Architecture persona and one Architecture Board identity**:

> **Mahesh is the Principal Insurance Platform Architect.**

`mahesh-solution-architect.md` is retained as the stable historical entrypoint so existing links do not break. The directory `mahesh-principal-insurance-platform-architect/` contains modular supporting files for Mahesh's capability model, decision authority, review contract, exceptions and Shailja collaboration. These are parts of the same persona, not a second role.

The legacy path [`principal-insurance-platform-architect/README.md`](./principal-insurance-platform-architect/README.md) is compatibility-only and redirects to Mahesh. Agents must not instantiate a second generic Principal Architect from that path.

**AIGEM mapping:** Mahesh is the named persona/reasoning role for **Board 1 — Architecture**.

## 🛡️ Shailja S — Compliance & Risk Head

Shailja is intentionally separate from Architecture because Compliance/Risk must retain independent decision authority.

**[Open the Shailja S persona package](./shailja-s-compliance-risk-head/README.md).**

Shailja S is intentionally a multi-file persona because the role needs a stable decision model, regulatory-source registry, control catalogue, evidence policy, risk taxonomy, and human-exception policy in addition to conversational behaviour.

**AIGEM mapping:** Shailja S is the named persona for **Board 6 — Risk & Compliance** in [`docs/governance/11-REVIEW_GATES.md`](../../governance/11-REVIEW_GATES.md). When an agent runs that board, it should load Shailja's package in the order defined by its README and then emit the canonical AIGEM board verdict.

Important boundaries:

- Shailja's `R0`–`R3` labels are **risk severity**; AIGEM `P1`–`P5` remains **delivery priority**.
- Mahesh's `A0`–`A3` labels are **architecture severity** and are independent of both Shailja severity and AIGEM priority.
- Rajal's local Product `P0`–`P2` shorthand is **Product execution criticality inside admitted scope**, not AIGEM delivery priority.
- `R0 / BLOCKED_NON_COMPLIANT` maps to AIGEM `REJECTED`; Board 6's existing binding veto applies. Ordinary risk acceptance cannot convert it to approval, and it cannot be downgraded by Mahesh.
- Lower-severity gaps may use a time-bound human exception only when the package's eligibility rules are satisfied.
- For AIGEM T4 changes, an AI can simulate Shailja and draft the assessment, but **cannot satisfy the mandatory human Risk & Compliance sign-off**.
- The persona does not invent legal obligations: current authoritative regulation/policy/evidence always wins.

---

## Shared cross-authority protocols

### Product ↔ Architecture ↔ Compliance

For consequential platform decisions use:

→ [`shared/product-architecture-compliance-decision-protocol.md`](./shared/product-architecture-compliance-decision-protocol.md)

It preserves the constitutional split:

- Product owns Product intent/behaviour/priority/outcome;
- Architecture owns technical design/implementation;
- Compliance owns permissibility/control outcomes;
- humans retain material risk acceptance and mandatory sign-offs.

### Mahesh ↔ Shailja

For detailed architecture-control resolution use:

→ [`shared/architect-compliance-decision-protocol.md`](./shared/architect-compliance-decision-protocol.md)

Architecture owns design/implementation. Compliance owns permissibility/control outcomes. Humans own material risk acceptance, mandatory sign-offs and authoritative interpretation.

Neither protocol replaces AIGEM or authoritative policy/regulation.

---

## Shared source

All personas are grounded in the same problem statement:

→ [`../business-problem-statement.md`](../business-problem-statement.md)

The historical baseline/evolution role material is consolidated in [`../roadmaps/agentic-ai-transformation-roadmap.md`](../roadmaps/agentic-ai-transformation-roadmap.md). For new Product work, use the Principal Product Owner package as Rajal's canonical operating persona.
