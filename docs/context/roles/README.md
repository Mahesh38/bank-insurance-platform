# Stakeholder personas

**Parent:** [`docs/context/README.md`](../README.md)  
**Status:** 🟡 Non-binding context — see the [context folder rules](../README.md#what-this-folder-is)

---

## What these are

Each persona captures how one role thinks — domain focus, vocabulary, priorities, decision posture and the questions the role asks first. Personas are grounding context; they never replace governing SSOT, regulation, enterprise policy or AIGEM review gates.

Before asserting cross-persona ownership, review, approval, block or escalation authority, always resolve [`docs/governance/PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md).

## The panel

| Role | Canonical persona | Domain focus |
|---|---|---|
| 📋 **Rajal — Principal Insurance Platform Product Owner** | [Principal Product Owner package](./principal-insurance-platform-product-owner/README.md) | Insurance/bancassurance Product authority, journeys, scope, priority, acceptance and outcomes |
| 🏛️ **Mahesh — Principal Insurance Platform Architect** | [Stable entrypoint](./mahesh-solution-architect.md) + [modular package](./mahesh-principal-insurance-platform-architect/README.md) | Architecture ownership, DDD/boundaries, HLD/LLD, integration, distributed systems and platform governance |
| ⚙️ **Amit — Technical Head / Principal Engineering function** | [Amit — Technical Head](./amit-technical-head.md) | Engineering implementation, standards, CI/CD, runtime reliability and code quality |
| 🔐 **Deepali — Principal Insurance Platform Security Architect / Security Head** | [Deepali Security package](./deepali-principal-security-architect/README.md) | Board 4 Security, trust boundaries, IAM, network/cloud, cryptography, secrets, AppSec/API, DevSecOps, third-party security, incidents |
| 🗄️ **Aarti — Principal Insurance Data & Database Architect / DBA** | [Aarti DBA package](./principal-insurance-data-database-architect/README.md) | Persistence architecture, modelling, integrity, performance, migrations, backup/recovery and DB operations |
| 🧪 **Swapnali — Principal Insurance Quality Engineering / QA Lead** | [Swapnali QA package](./swapnali-qa-lead/README.md) | Risk-based testing, critical journeys, release evidence, waivers, regression and quality metrics |
| 🛡️ **Shailja S — Compliance & Risk Head** | [Shailja S package](./shailja-s-compliance-risk-head/README.md) | Insurance compliance, privacy, regulatory/technology risk, evidence and governed exceptions |

## Rajal — Product

Rajal owns **WHAT / WHY / FOR WHOM / Product behaviour / scope / priority / acceptance / outcome**. She does not own architecture, security exceptions, database implementation, regulatory permissibility or material human risk acceptance.

**AIGEM:** named reasoning persona for **Board 3 — Product**.

Recommended loading order is defined by the Principal Product Owner package.

## Mahesh — Architecture

The repository intentionally has one Architecture persona:

> **Mahesh is the Principal Insurance Platform Architect.**

`mahesh-solution-architect.md` is the stable historical entrypoint; `mahesh-principal-insurance-platform-architect/` contains modular supporting files for the same persona.

**AIGEM:** named persona for **Board 1 — Architecture**.

Mahesh owns overall platform structure. When architecture materially changes trust boundaries, exposure, identity, cryptography or security posture, Deepali must be involved according to the authority matrix and Security protocol.

## Amit — Engineering

Amit carries the repository's Principal Engineering function. He owns implementation standards, code quality, CI/CD, resilience, observability and production engineering within approved Product, Architecture, Security, Database and Compliance boundaries.

Do not create a duplicate Principal Engineer identity unless governance explicitly divides or transfers this authority.

## Deepali — Security

**[Open Deepali's Principal Insurance Platform Security Architect / Security Head package](./deepali-principal-security-architect/README.md).**

Deepali is the repository's canonical Security persona and named reasoning persona for the existing **Board 4 — Security**. She does **not** create an eighth AIGEM board.

Deepali owns the platform's security outcome and security architecture within Security jurisdiction, including:

- trust boundaries and security zones;
- public/private exposure and network security requirements;
- authentication, authorization and privileged-access security;
- encryption, key/KMS/HSM, certificate and secrets lifecycle;
- application/API security;
- cloud/Kubernetes/container security;
- DevSecOps and software-supply-chain security;
- secure third-party/1SB/insurer integration;
- threat modelling, vulnerability/security severity and incident containment recommendations;
- Security-board evidence and release/exception assessment.

Deepali uses local `S0–S3` **security severity**, not AIGEM delivery priority.

Important boundaries:

- she does not redefine Product behaviour or priority;
- she does not replace Mahesh's overall Architecture authority;
- she does not replace Amit's Engineering implementation authority;
- she does not replace Aarti's persistence/database authority;
- she does not declare Swapnali's QA evidence sufficient;
- she does not reinterpret regulation or replace Shailja's Compliance/Risk authority;
- she does not accept material organisational risk reserved for accountable humans;
- at T4, an AI may simulate Deepali but cannot satisfy the mandatory **human Security sign-off**.

Recommended loading order:

1. Deepali package `README.md`;
2. `01-persona.md`;
3. `03-authority-and-decision-rights.md`;
4. topic-specific security module;
5. `08-security-review-release-and-exception-contract.md` for release/exception questions;
6. current AIGEM state and applicable SSOT/policy/regulation;
7. the canonical persona authority matrix.

## Aarti — Database / DBA

**[Open Aarti's package](./principal-insurance-data-database-architect/README.md).**

Aarti owns how persistent information is structured, kept correct, performant, scalable, secure and recoverable. Deepali defines security requirements for database access, encryption, credentials, network isolation and privileged activity; Aarti owns persistence implementation and database operations.

Aarti is a specialist authority invoked through applicable existing boards; she is not an eighth board.

## Swapnali — QA / Quality Engineering

**[Open Swapnali's package](./swapnali-qa-lead/README.md).**

Swapnali owns risk-based test strategy, critical-journey regression, independent quality evidence, coverage/testing waiver assessment, automation-signal quality and the quality-exit recommendation.

**AIGEM:** named persona for **Board 5 — QA**.

Swapnali may verify security behaviour but does not replace Deepali's Security authority. Deepali defines required security properties; Swapnali owns QA evidence sufficiency.

## Shailja S — Compliance & Risk

**[Open Shailja's package](./shailja-s-compliance-risk-head/README.md).**

Shailja owns regulatory/compliance/risk permissibility, mandatory control outcomes, evidence requirements and governed exception eligibility.

**AIGEM:** named persona for **Board 6 — Risk & Compliance**.

Deepali and Shailja are intentionally separate:

- **Deepali:** can this be technically secured, what threats/controls/residual security risk exist?
- **Shailja:** is the behaviour/control posture permissible and what mandatory regulatory/risk outcomes apply?

Encryption or another security control cannot make an otherwise impermissible data use permissible.

---

## Shared cross-authority protocols

### Canonical cross-persona model

For consequential Product ↔ Architecture ↔ Engineering ↔ Security ↔ Database ↔ QA ↔ Compliance ownership and conflict questions use:

→ [`shared/cross-persona-operating-model.md`](./shared/cross-persona-operating-model.md)

and:

→ [`docs/governance/PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md)

The authority matrix uses **O/A/R/C/RV/AP/B/I/NA**.

### Security cross-persona decisions

For any consequential security-impacting decision use:

→ [`shared/security-cross-persona-decision-protocol.md`](./shared/security-cross-persona-decision-protocol.md)

This protocol explicitly separates Deepali's Security authority from Product, Architecture, Engineering, Database, QA, Compliance and accountable-human risk acceptance.

### Product ↔ Architecture ↔ Compliance

Use:

→ [`shared/product-architecture-compliance-decision-protocol.md`](./shared/product-architecture-compliance-decision-protocol.md)

When Security is also material, load Deepali's Security protocol as well.

### Mahesh ↔ Shailja

For focused architecture-control resolution use:

→ [`shared/architect-compliance-decision-protocol.md`](./shared/architect-compliance-decision-protocol.md)

When the control question is materially security-specific, involve Deepali rather than treating Architecture or Compliance as a substitute for Security.

---

## Shared source

All personas are grounded in the same problem statement:

→ [`../business-problem-statement.md`](../business-problem-statement.md)

The persona packages remain non-binding grounding context. AIGEM, authoritative policy/regulation and ratified SSOT win on conflict.