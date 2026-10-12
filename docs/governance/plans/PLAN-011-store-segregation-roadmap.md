# PLAN-011 — E2E Postgres vs DynamoDB segregation and roadmap

```yaml
# schema: implementation-plan
id: PLAN-011
work_item: DATA-004
origin: SUG-20261012-pdr
workstream: WS-3
risk_tier: T3
author: "agent:cursor (persona: Aarti)"
date: "2026-10-12"

objective: >
  After this change engineers have one R0 apply list: every transactional SoR
  on Aurora PostgreSQL, DynamoDB empty, one database role per owning schema,
  and a staged roadmap with a measured Dynamo revisit — without applying
  Flyway or provisioning a second engine.

problem: >
  DATA-001 / DB-DEC-0001 rejected Dynamo for Journey, Quotation and Audit at
  R0, and ADR-011 withdrew Dynamo sessions. architecture-review/05 and
  02-target-microservices-architecture.md still list those Dynamo rows.
  There is no service × store × credential matrix, so S09 IaC and S11
  services can still invent a second SoR by diagram inertia.

proposed_solution: >
  Admit DATA-004 on the Aarti design lane. Publish
  04-store-segregation-and-roadmap.md and DB-DEC-0003 as an overlay on
  DB-DEC-0001: Aurora for every transactional SoR; Dynamo access list empty;
  Valkey/S3/Keycloak/MSK/MIS named as non-SoR stores; one app_* role per
  schema; explicit no-login for 1sb-integration, BFF, audit-compliance,
  Hub, Notification, Reporting. Roadmap Phases A–F. Do not apply Flyway.
  Do not create Dynamo resources. Do not edit CURRENT-STATE stage fields.

alternatives:
  - option: "Put Journey, Quotation and Audit on DynamoDB now"
    rejected_because: "DB-DEC-0001; no volume evidence; forks the shipping audit_event store"
  - option: "One shared Aurora login for all services"
    rejected_because: "Breaks the ADR-008 ownership invariant that credentials enforce"
  - option: "Wait until S09 IaC and let platform invent roles"
    rejected_because: "S09 would provision from the still-published Dynamo rows in 05/02"
  - option: "Amend architecture-review/05 in this turn"
    rejected_because: "Mahesh owns those diagrams; this pack is the DBA overlay"

affected_components:
  - docs/platform/data-architecture
  - docs/governance/plans
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/context/DOC-MAP.yaml

files_expected:
  - docs/platform/data-architecture/04-store-segregation-and-roadmap.md
  - docs/platform/data-architecture/DB-DEC-0003-store-segregation.md
  - docs/platform/data-architecture/DATA-004.work-item.yaml
  - docs/platform/data-architecture/01-physical-design.md
  - docs/platform/data-architecture/README.md
  - docs/governance/plans/PLAN-011-store-segregation-roadmap.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/context/DOC-MAP.yaml

data_changes: none
api_changes: none
security_impact: "documents existing per-schema credential pattern; does not create, rotate or widen secrets (G3 considered, does not fire)"
compliance_impact: none
backward_compatibility: "compatible — design overlay; runtime Flyway unchanged"
performance_impact: none
operational_impact: "S09 inventory must contain no Dynamo resource — absence is the evidence"

testing:
  unit: []
  integration: []
  other:
    - "Cross-doc: DB-DEC-0001 Dynamo rejection ↔ 04 empty access list ↔ 91-grants role names"
    - "Every services/ deployable appears in section 4.1 or 4.2"
    - "python3 scripts/context/build-doc-map.py after new docs"
    - "python3 scripts/context/context-load.py validate"
    - "No CURRENT-STATE stage field edit"

rollback: >
  Revert the documentation commit. No runtime or data impact.

dependencies:
  - DATA-001
  - DB-DEC-0001
  - ADR-008
  - ADR-011
assumptions: []
risks:
  - risk: "Readers keep treating architecture-review/05 Dynamo rows as R0 apply"
    mitigation: "DB-DEC-0003 names those rows as target-state options; Mahesh owns diagram amendment"
  - risk: "G3 (secrets lifecycle) misread as T4"
    mitigation: "RG-5 — documentation of the existing 91-grants pattern; no secret is created"

acceptance_criteria:
  - "AC-1 Aurora SoR + empty Dynamo access list"
  - "AC-2 every deployable mapped"
  - "AC-3 one app_* role per owning schema"
  - "AC-4 S09 inventory has no Dynamo"
  - "AC-5 no stage-field edit"

out_of_scope:
  - "Runtime service or Flyway apply"
  - "AWS resource creation"
  - "Editing architecture-review/05 or 02"
  - "Human T4 / T3 signatures"

estimate: M

reviews: []
variance_log: []
```
