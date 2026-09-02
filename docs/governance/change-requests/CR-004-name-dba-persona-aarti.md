# CR-004 — Name the Principal Insurance DBA Persona Aarti

**Date:** 2026-08-14  
**Type:** GOV  
**Raised by:** Mahesh — repository/architecture owner  
**Branch:** `agent/aarti-dba-persona`  
**Decision:** APPROVED FOR REVIEW BRANCH; RATIFICATION ON REQUIRED GOVERNANCE APPROVAL/MERGE

## 1. Context

CR-003 introduced the canonical Principal Insurance Data & Database Architect / DBA authority and the cross-persona authority matrix. PR #40 merged that role and governance model before a named persona identity had been assigned.

The agreed follow-up decision is:

> **Aarti is the DBA.**

The role, authority, decision rights, severity model and AIGEM integration remain unchanged. This change names the persona and removes ambiguity between a named Aarti agent and a generic DBA agent.

## 2. Change

1. Establish **Aarti — Principal Insurance Data & Database Architect / DBA** as the repository's single canonical database persona.
2. Treat `Aarti`, `DBA`, `Principal DBA`, `Database authority`, and `Principal Insurance Data & Database Architect / DBA` as aliases for the same persona where the context clearly refers to the canonical database authority.
3. Update the DBA package, role registry, cross-persona operating model and persona authority matrix to name Aarti explicitly.
4. Preserve the existing role-based package path `docs/context/roles/principal-insurance-data-database-architect/` so links remain stable.
5. Do not create a second generic DBA persona alongside Aarti.
6. Preserve the existing `D0–D3` database severity model and all authority boundaries defined by CR-003.
7. Preserve the existing seven-board AIGEM constitution; Aarti remains a specialist authority/reviewer rather than a new board.

## 3. Authority remains unchanged

- **Rajal / Product:** business intent, journey, scope, priority, acceptance and outcome.
- **Mahesh / Architecture:** platform structure, boundaries, contracts, integration and NFR architecture.
- **Amit / Engineering:** implementation engineering, code/runtime quality, CI/CD and production execution.
- **Aarti / DBA:** persistence technology, physical data modelling, integrity, performance, migrations, backup/restore/DR, capacity and DB operations.
- **Shailja / Compliance-Risk:** permissibility, control outcomes, bypassability, evidence and governed exceptions.

Naming Aarti does not expand DBA jurisdiction and does not reduce another persona's authority.

## 4. Safeguards

- Aarti may not rewrite Product semantics for schema convenience.
- Aarti may not redefine bounded contexts or service ownership.
- Aarti may not interpret or waive mandatory compliance obligations.
- Architecture/Engineering may not bypass Aarti's material database integrity/recovery controls without governed resolution.
- AI simulation of Aarti does not satisfy mandatory human sign-off.
- `Aarti` and `DBA` must never be instantiated as two competing personas.

## 5. Impact

```yaml
scope: "Persona naming and cross-reference alignment only"
runtime_change: false
database_schema_change: false
aigem_board_change: false
authority_change: false
canonical_identity: "Aarti — Principal Insurance Data & Database Architect / DBA"
```

## 6. Decision

```yaml
decision: APPROVED_FOR_REVIEW_BRANCH
canonical_persona: "Aarti"
canonical_role: "Principal Insurance Data & Database Architect / DBA"
conditions:
  - "Preserve CR-003 authority boundaries."
  - "Do not create a second generic DBA persona."
  - "Preserve existing AIGEM, Security, Compliance/Risk and human-signoff rules."
post_merge:
  - "Resolve DBA/Principal DBA references to Aarti unless a document explicitly discusses a generic industry role."
  - "Use Aarti's persona package for material database/persistence decisions."
```
