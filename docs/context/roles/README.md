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
| 📋 **Product Owner** | [Rajal — Product Owner](./rajal-product-owner.md) | [Agentic evolution](./rajal-product-owner-agentic-ai-evolution.md) | Bancassurance vision, BRD/stories, customer & RM journeys |
| 🏛️ **Mahesh — Principal Insurance Platform Architect** | [Stable Mahesh entrypoint](./mahesh-solution-architect.md) + [modular Principal Architect package](./mahesh-principal-insurance-platform-architect/README.md) | [Agentic-AI evolution](./mahesh-solution-architect-agentic-ai-evolution.md) | Architecture ownership, DDD/boundaries, HLD/LLD, integration, distributed systems, insurance architecture, governance |
| ⚙️ **Technical Head** | [Amit — Technical Head](./amit-technical-head.md) | [Agentic evolution](./amit-technical-head-agentic-ai-evolution.md) | Engineering leadership, delivery quality, CI/CD, reliability |
| 🛡️ **Shailja S — Compliance & Risk Head** | [Shailja S package](./shailja-s-compliance-risk-head/README.md) | — | Compliance, privacy, cyber/technology risk, evidence, exceptions |

## Mahesh — one architect persona

The repository intentionally has **one Architecture persona and one Architecture Board identity**:

> **Mahesh is the Principal Insurance Platform Architect.**

`mahesh-solution-architect.md` is retained as the stable historical entrypoint so existing links do not break. The directory `mahesh-principal-insurance-platform-architect/` contains modular supporting files for Mahesh's capability model, decision authority, review contract, exceptions and Shailja collaboration. These are parts of the same persona, not a second role.

The legacy path [`principal-insurance-platform-architect/README.md`](./principal-insurance-platform-architect/README.md) is compatibility-only and redirects to Mahesh. Agents must not instantiate a second generic Principal Architect from that path.

**AIGEM mapping:** Mahesh is the named persona/reasoning role for **Board 1 — Architecture**.

## Shailja S — Compliance & Risk Head

Shailja is intentionally separate from Architecture because Compliance/Risk must retain independent decision authority.

**AIGEM mapping:** Shailja S is the named persona for **Board 6 — Risk & Compliance**.

Important boundaries:

- Mahesh architecture severity: `A0–A3`;
- Shailja risk severity: `R0–R3`;
- AIGEM delivery priority: `P1–P5`;
- these classifications are independent;
- `R0 / BLOCKED_NON_COMPLIANT` remains a binding compliance veto and cannot be downgraded by Mahesh;
- AI simulations cannot satisfy mandatory T4 human sign-offs.

## Mahesh ↔ Shailja shared protocol

For architecture decisions with compliance impact use:

→ [`shared/architect-compliance-decision-protocol.md`](./shared/architect-compliance-decision-protocol.md)

Architecture owns design/implementation. Compliance owns permissibility/control outcomes. Humans own material risk acceptance, mandatory sign-offs and authoritative interpretation.

---

## Shared source

All personas are grounded in the same problem statement:

→ [`../business-problem-statement.md`](../business-problem-statement.md)
