# CR-003 — Principal Insurance DBA & Cross-Persona Authority Matrix

**Date:** 2026-08-14  
**Type:** GOV  
**Raised by:** Mahesh — repository/architecture owner  
**Branch:** `agent/principal-insurance-dba-authority-matrix`  
**Decision:** APPROVED FOR REVIEW BRANCH; RATIFICATION ON REQUIRED GOVERNANCE APPROVAL/MERGE

## 1. Current position

The repository already has mature canonical personas for:

- Rajal — Principal Insurance Platform Product Owner;
- Mahesh — Principal Insurance Platform Architect;
- Amit — Technical Head / engineering leadership;
- Shailja S — Compliance & Risk Head.

AIGEM defines board ownership and several bilateral/three-way protocols, but the repository does not yet have:

1. a canonical Principal database/data authority persona; or
2. one explicit matrix showing ownership, accountability, responsibility, consultation, formal review, approval, blocking authority, informed status and not-authorised boundaries across the core personas.

That gap becomes material as persistence, data lifecycle, analytics and database-operational decisions increase.

## 2. Proposed change

1. Add a canonical **Principal Insurance Data & Database Architect / DBA** persona package.
2. Define the DBA as specialist authority for persistence technology, physical data modelling, integrity, performance, migrations, database reliability, backup/restore/DR, capacity and database-side lifecycle/security implementation.
3. Add a canonical **Cross-Persona Operating Model** covering Product ↔ Architecture ↔ Engineering ↔ Database ↔ Compliance/Risk.
4. Add `docs/governance/PERSONA-AUTHORITY-MATRIX.md` as the explicit segregation-of-duties matrix.
5. Extend traditional RACI with `O/A/R/C/RV/AP/B/I/NA` so agents know not only who does work but who reviews, approves, may block and is not authorised.
6. Treat **Amit — Technical Head** as the repository's existing Principal Engineering function for this model instead of introducing a duplicate Principal Engineer persona.
7. Add reciprocal links from Product, Architecture, Engineering and Compliance persona entrypoints to the new operating model and authority matrix.
8. Preserve the existing seven AIGEM review boards; **do not create an eighth DBA board**.
9. Invoke the DBA as a mandatory specialist reviewer/authority through the existing Architecture, Technical, Risk/Compliance and Operations boards when database impact is material.
10. Preserve all existing human-sign-off, Security-veto and Risk/Compliance-veto semantics.

## 3. Driver

A standard RACI is insufficient for an AI multi-agent system. AI agents need explicit answers to:

- who owns the domain;
- who is accountable;
- who implements;
- who must be consulted;
- who formally reviews;
- who approves;
- who can block and for what severity;
- who is only informed;
- where the agent is **not authorised** to make a unilateral decision.

The new model reduces persona competition and makes database governance explicit without changing AIGEM's constitutional board count.

## 4. Key separation of duties

- **Product / Rajal:** WHAT / WHY / FOR WHOM / business behaviour / scope / priority / acceptance / outcome.
- **Architecture / Mahesh:** platform structure, boundaries, contracts, integration and NFR architecture.
- **Engineering / Amit:** production engineering, implementation standards, code/runtime quality, CI/CD, reliability execution.
- **Database / Principal DBA:** persistence model, database technology, integrity, performance, recoverability and DB operations.
- **Compliance/Risk / Shailja:** permissibility, obligation/control outcomes, bypassability, evidence and governed exceptions.
- **Humans:** material risk acceptance, mandatory sign-offs, governance exceptions and authoritative interpretation where required.

## 5. AIGEM integration

No new board is added.

DBA specialist review is invoked through existing boards when applicable:

- Board 1 — Architecture: persistence architecture, data ownership, CDC, shared DBs, cross-service DB access, sharding/multi-region.
- Board 2 — Technical: transactions, ORM/SQL, migrations, connection management and DB-facing implementation.
- Board 6 — Risk & Compliance: PII storage, retention/deletion implementation, DB controls, backup/archive protection and auditability.
- Board 7 — Operations: backup/restore, DR, capacity, monitoring, failover and DB production readiness.

The board owner retains the AIGEM board verdict; the DBA supplies the specialist database verdict within the board's evidence.

## 6. Database severity

The DBA persona may use `D0–D3` strictly as database finding severity:

- `D0` critical/non-bypassable database integrity, loss, recovery or sensitive-data risk;
- `D1` major database risk, normally requiring rework;
- `D2` manageable database debt;
- `D3` non-blocking improvement.

This does not replace AIGEM `P1–P5`, Architecture `A0–A3`, Shailja `R0–R3` or Product local criticality.

## 7. Alternatives considered

### A — Keep database authority implicit under Architecture/Technical

Rejected. It leaves physical integrity, migration, backup/recovery and database-operational authority ambiguous.

### B — Create an eighth AIGEM Database Board

Rejected for this change. It would alter AIGEM's constitutional review structure and is unnecessary because database expertise can be invoked through existing boards.

### C — Create a new separate Principal Engineer persona

Rejected for now. Amit already owns the repository's engineering leadership concerns. A second overlapping engineering identity would recreate the duplication problem previously removed from Architecture.

### D — Add specialist DBA persona + common authority matrix while preserving existing boards

**Recommended and accepted for the review branch.**

## 8. Impact

```yaml
scope: "Governance/persona grounding only; no runtime product behavior changes"
stage: "No lifecycle-stage change"
dependencies:
  - "AIGEM existing seven-board model"
  - "Rajal Product persona"
  - "Mahesh Architecture persona"
  - "Amit Technical Head persona"
  - "Shailja Compliance/Risk persona"
risk_if_rejected: >
  Database ownership remains implicit and AI personas can issue overlapping or contradictory
  decisions about persistence, data integrity, migrations, data lifecycle and production DB operation.
```

## 9. Safeguards

- No persona receives unlimited authority.
- `Not Authorised` boundaries are explicit.
- Blocking authority is jurisdiction-specific and evidence-based.
- DBA cannot rewrite Product semantics or Architecture boundaries for schema convenience.
- Architecture/Engineering cannot remove database integrity/recovery guarantees without DBA resolution.
- Compliance defines required control outcomes but does not dictate technology by preference.
- AI personas do not impersonate mandatory human approvals.
- Existing Security and Risk/Compliance veto semantics remain unchanged.

## 10. Decision and ratification

```yaml
decision: APPROVED_FOR_REVIEW_BRANCH
requested_by: "Mahesh — 2026-08-14"
conditions:
  - "Merge/ratification must follow repository governance and protected-branch rules."
  - "Do not create an eighth AIGEM board as part of this CR."
  - "Do not instantiate a duplicate Principal Engineer while Amit retains engineering authority."
  - "Preserve existing Security, Compliance/Risk and T4 human-signoff rules."
post_merge:
  - "Treat PERSONA-AUTHORITY-MATRIX.md as the canonical cross-persona segregation reference."
  - "Use the Principal DBA package for material persistence/database decisions."
  - "Revalidate this matrix whenever a persona's canonical authority materially changes."
```
