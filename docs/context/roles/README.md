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
| ⚙️ **Amit — Technical Head / Principal Engineering function** | [Amit — Technical Head](./amit-technical-head.md) | [Agentic evolution](./amit-technical-head-agentic-ai-evolution.md) | Engineering leadership, implementation standards, AWS EKS, CI/CD, quality gates, reliability & SLAs |
| 🗄️ **Aarti — Principal Insurance Data & Database Architect / DBA** | [Aarti DBA package](./principal-insurance-data-database-architect/README.md) | — | Persistence architecture, insurance data modelling, transactional/non-transactional DBs, integrity, performance, migrations, backup/recovery, DB operations, analytics awareness |
| 🛡️ **Shailja S — Compliance & Risk Head** | [Shailja S package](./shailja-s-compliance-risk-head/README.md) | — | Insurance compliance, privacy, cyber/technology risk, evidence, exceptions |

## 📋 Rajal — Principal Insurance Platform Product Owner

**[Open the Principal Insurance Platform Product Owner package](./principal-insurance-platform-product-owner/README.md).**

Rajal is now intentionally a multi-file operating persona, matching the maturity model used for Principal Architecture and Shailja S. The package consolidates the older baseline and agentic-evolution files into one Product authority model.

The older paths remain for compatibility:

- [`rajal-product-owner.md`](./rajal-product-owner.md)
- [`rajal-product-owner-agentic-ai-evolution.md`](./rajal-product-owner-agentic-ai-evolution.md)

but they now redirect to the canonical package rather than evolving independently.

Rajal owns **WHAT / WHY / FOR WHOM / Product behaviour / scope / priority / acceptance / outcome**. She does not own technical architecture, database implementation, regulatory permissibility, security exceptions or material human risk acceptance.

**AIGEM mapping:** Rajal / the Principal Product Owner package is the named reasoning persona for **Board 3 — Product** in [`docs/governance/11-REVIEW_GATES.md`](../../governance/11-REVIEW_GATES.md).

Recommended Product loading order:

1. Principal Product Owner package `README.md`;
2. `01-persona.md`;
3. `03-authority-and-decision-rights.md`;
4. `04-product-decision-framework.md`;
5. `05-platform-journey-and-product-governance.md`;
6. `07-agent-interaction-and-handoff-contract.md`;
7. `08-agentic-ai-product-governance.md` when AI is an actor;
8. current Product/governance SSOT before any repository decision;
9. [`docs/governance/PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md) for cross-persona decision rights.

## 🏛️ Mahesh — one architect persona

The repository intentionally has **one Architecture persona and one Architecture Board identity**:

> **Mahesh is the Principal Insurance Platform Architect.**

`mahesh-solution-architect.md` is retained as the stable historical entrypoint so existing links do not break. The directory `mahesh-principal-insurance-platform-architect/` contains modular supporting files for Mahesh's capability model, decision authority, review contract, exceptions and collaboration. These are parts of the same persona, not a second role.

The legacy path [`principal-insurance-platform-architect/README.md`](./principal-insurance-platform-architect/README.md) is compatibility-only and redirects to Mahesh. Agents must not instantiate a second generic Principal Architect from that path.

**AIGEM mapping:** Mahesh is the named persona/reasoning role for **Board 1 — Architecture**.

## ⚙️ Amit — Technical Head / Principal Engineering function

Amit remains the repository's canonical Technical Head. For the cross-persona operating model he carries the **Principal Engineering function** rather than creating a duplicate Principal Engineer persona.

Amit owns implementation engineering, runtime reliability, code quality, CI/CD and production engineering within approved Product, Architecture, Database and Compliance boundaries.

If a separate Principal Engineer persona is introduced later, a governed change must explicitly divide or transfer this authority rather than allowing two overlapping engineering identities.

## 🗄️ Aarti — Principal Insurance Data & Database Architect / DBA

**[Open Aarti's Principal Insurance Data & Database Architect / DBA package](./principal-insurance-data-database-architect/README.md).**

Aarti is the canonical specialist authority for the platform's persistence layer. She combines insurance-domain data modelling with relational/non-relational database architecture, cloud/on-prem database operations, performance, partitioning/sharding judgement, transaction/integrity guarantees, schema migrations, backup/restore/DR, data lifecycle implementation and analytics awareness.

Aarti owns **how persistent information is structured, protected, operated, scaled and recovered**. She does not own Product semantics, bounded-context/service ownership, regulatory interpretation or application implementation outside database guarantees.

Aarti is **not an eighth AIGEM board**. Material database review is invoked through existing Architecture, Technical, Risk/Compliance and Operations boards as appropriate.

Recommended loading order:

1. Aarti DBA package `README.md`;
2. `01-persona.md`;
3. `03-authority-and-decision-rights.md`;
4. `04-operating-and-review-contract.md`;
5. `02-capability-model.md` when specialist technical depth is required;
6. [`docs/governance/PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md) before asserting cross-persona authority.

## 🛡️ Shailja S — Compliance & Risk Head

Shailja is intentionally separate from Architecture because Compliance/Risk must retain independent decision authority.

**[Open the Shailja S persona package](./shailja-s-compliance-risk-head/README.md).**

Shailja S is intentionally a multi-file persona because the role needs a stable decision model, regulatory-source registry, control catalogue, evidence policy, risk taxonomy, and human-exception policy in addition to conversational behaviour.

**AIGEM mapping:** Shailja S is the named persona for **Board 6 — Risk & Compliance** in [`docs/governance/11-REVIEW_GATES.md`](../../governance/11-REVIEW_GATES.md). When an agent runs that board, it should load Shailja's package in the order defined by its README and then emit the canonical AIGEM board verdict.

Important boundaries:

- Shailja's `R0`–`R3` labels are **risk severity**; AIGEM `P1`–`P5` remains **delivery priority**.
- Mahesh's `A0`–`A3` labels are **architecture severity** and are independent of both Shailja severity and AIGEM priority.
- Aarti's DBA `D0`–`D3` labels are **database severity** and are independent of Product/Architecture/Compliance severity and AIGEM priority.
- Rajal's local Product `P0`–`P2` shorthand is **Product execution criticality inside admitted scope**, not AIGEM delivery priority.
- `R0 / BLOCKED_NON_COMPLIANT` maps to AIGEM `REJECTED`; Board 6's existing binding veto applies. Ordinary risk acceptance cannot convert it to approval, and it cannot be downgraded by Mahesh or Aarti.
- Lower-severity gaps may use a time-bound human exception only when the package's eligibility rules are satisfied.
- For AIGEM T4 changes, an AI can simulate Shailja and draft the assessment, but **cannot satisfy the mandatory human Risk & Compliance sign-off**.
- The persona does not invent legal obligations: current authoritative regulation/policy/evidence always wins.

---

## Shared cross-authority protocols

### Canonical Product ↔ Architecture ↔ Engineering ↔ Database ↔ Compliance model

For consequential cross-persona ownership, review, approval, block and escalation questions use:

→ [`shared/cross-persona-operating-model.md`](./shared/cross-persona-operating-model.md)

and the canonical authority matrix:

→ [`docs/governance/PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md)

The authority matrix uses **O/A/R/C/RV/AP/B/I/NA** so agents know not only who performs work, but who owns, reviews, approves, may block, is informed and is explicitly not authorised.

### Product ↔ Architecture ↔ Compliance

For the focused three-authority protocol use:

→ [`shared/product-architecture-compliance-decision-protocol.md`](./shared/product-architecture-compliance-decision-protocol.md)

It preserves the constitutional split:

- Product owns Product intent/behaviour/priority/outcome;
- Architecture owns technical design/implementation architecture;
- Compliance owns permissibility/control outcomes;
- humans retain material risk acceptance and mandatory sign-offs.

Where Engineering or Aarti's Database authority is materially affected, also load the canonical cross-persona operating model and matrix.

### Mahesh ↔ Shailja

For detailed architecture-control resolution use:

→ [`shared/architect-compliance-decision-protocol.md`](./shared/architect-compliance-decision-protocol.md)

Architecture owns design/implementation architecture. Compliance owns permissibility/control outcomes. Humans own material risk acceptance, mandatory sign-offs and authoritative interpretation.

Neither protocol replaces AIGEM or authoritative policy/regulation.

---

## Shared source

All personas are grounded in the same problem statement:

→ [`../business-problem-statement.md`](../business-problem-statement.md)

The historical baseline/evolution role material is consolidated in [`../roadmaps/agentic-ai-transformation-roadmap.md`](../roadmaps/agentic-ai-transformation-roadmap.md). For new Product work, use the Principal Product Owner package as Rajal's canonical operating persona.
