# PLAN-010 — Lead + Suitability e2e physical schema

```yaml
# schema: implementation-plan
id: PLAN-010
work_item: DATA-003
origin: SUG-20261009-lss
workstream: WS-3
risk_tier: T3
author: "agent:cursor (persona: Aarti)"
date: "2026-10-09"

objective: >
  After this change engineers have an ER diagram and design DDL for the R0
  Lead → Suitability e2e path, traced from the module BRDs and EPIC-005 HLD,
  without applying Flyway or inventing Term / campaign / MIS tables.

problem: >
  PLAN-007 left physical schema out of scope. DATA-001 opportunity/suitability
  DDL is pre-D-018 and pre-Suitability-BRD: BANK_RM-only create, mandatory
  accountable_sp_id at insert, no ARCHIVED, no meeting, no versioned answers
  or mapping runs. S11 cannot implement Lead Screens 1–7 or Savings/ULIP
  suitability against that pack.

proposed_solution: >
  Admit DATA-003 on the Aarti design lane. Extend 04-lead_lms.sql and
  06-suitability.sql; publish 03-lead-suitability-e2e.md (ER + column sheets);
  update physical-design catalogue, routines and grants. Absorb DATA-002 W1
  archive columns. Leave W3/W4 and other-schema outboxes on DATA-002.
  Record OPEN-SUI-IMMUTABLE rather than silently choosing INV-SUI-01 vs BRD.

alternatives:
  - option: "Wait for S11 and let the Lead service invent tables"
    rejected_because: "AP-5 is contract-first; incorrect domain model at S11 is a hard P1 class."
  - option: "Implement the full DATA-002 pack (issuance, ingest, MIS) in this turn"
    rejected_because: "Requested slice is Lead+Suitability e2e; extra schemas would expand PLAN-010."
  - option: "Keep schema name opportunity (ADR-014 D1 default)"
    rejected_because: "Owner direction SUG-20261009-lms: schema lead_lms, table lead."
  - option: "Apply Flyway now"
    rejected_because: "DR-MIG-04; apply is S09; no owning Suitability service yet."

affected_components:
  - docs/platform/data-architecture
  - docs/governance/plans
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/context/DOC-MAP.yaml

files_expected:
  - docs/platform/data-architecture/03-lead-suitability-e2e.md
  - docs/platform/data-architecture/DATA-003.work-item.yaml
  - docs/platform/data-architecture/schemas/04-lead_lms.sql
  - docs/platform/data-architecture/schemas/06-suitability.sql
  - docs/platform/data-architecture/schemas/90-routines.sql
  - docs/platform/data-architecture/schemas/91-grants.sql
  - docs/platform/data-architecture/01-physical-design.md
  - docs/platform/data-architecture/README.md
  - docs/governance/plans/PLAN-010-lead-suitability-e2e-schema.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/context/DOC-MAP.yaml

data_changes: "design DDL only — not applied"
api_changes: none
security_impact: "additional ⚑ ciphertext columns (meeting link, closure remarks, versioned answers); no new plaintext PII"
compliance_impact: "C-RET-1 attribution columns named; OPEN-SUI-IMMUTABLE flagged for Board 6"
backward_compatibility: "compatible — design artefacts; runtime Flyway unchanged"
performance_impact: none
operational_impact: none

testing:
  unit: []
  integration: []
  other:
    - "PostgreSQL parse of 04-lead_lms.sql and 06-suitability.sql"
    - "Cross-doc: HLD R0 cut ↔ ER tables ↔ DDL CHECKs"
    - "python3 scripts/context/build-doc-map.py after new docs"
    - "python3 scripts/context/context-load.py validate"
    - "No CURRENT-STATE stage field edit"

rollback: >
  Revert the documentation commit. No runtime or data impact.

dependencies:
  - DATA-001
  - EPIC-005
  - DATA-002 (W1 absorbed; W3/W4 remain)
  - ADR-014
  - ADR-021
assumptions: []
risks:
  - risk: "INV-SUI-01 vs Suitability BRD same-id edit"
    mitigation: "Versioned children + OPEN-SUI-IMMUTABLE; do not claim the Product outcome"
  - risk: "Information model §4.2 still says BANK_RM-only create"
    mitigation: "DDL follows D-018/INV-LED-04; drift named in 03 §4.1"

acceptance_criteria:
  - "AC-1 Lead BRD create/assign/meeting/exception/reporting columns"
  - "AC-2 open-lead dedupe unique index"
  - "AC-3 ARCHIVED + archived_at + RET-7Y attribution"
  - "AC-4 Suitability versioned answers, mapping, lock"
  - "AC-5 ER diagram for the e2e path"
  - "AC-6 no stage-field edit"

out_of_scope:
  - "Runtime Lead / Suitability / BFF code"
  - "Flyway apply"
  - "DATA-002 W3/W4 remainder"
  - "Human T4 / T3 signatures"

estimate: M

reviews: []
variance_log:
  - date: "2026-10-09"
    change: "Physical schema renamed opportunity → lead_lms; aggregate table renamed to lead"
    reason: "Owner direction SUG-20261009-lms. ADR-014 D1 allowed either name; this pack takes lead_lms. Identifiers stay lead_id."
    re_review: "none — design DDL only; no T4 claim"
```
