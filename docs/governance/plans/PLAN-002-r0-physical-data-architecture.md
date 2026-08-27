# PLAN-002 — R0 physical data architecture pack

```yaml
# schema: implementation-plan
id: PLAN-002
work_item: DATA-001
origin: SUG-20260825-db1
workstream: WS-3
risk_tier: T3
author: "agent:cursor-grok (persona: Aarti)"
date: "2026-08-25"

objective: >
  Close OPEN-I1 / OPEN-I6 as an AI-drafted physical design: rules, topology, per-context
  schema, indexes, required routines, operations and troubleshooting, plus PostgreSQL
  design DDL for every R0 schema — ready for human Aarti / Mahesh / Deepali / Shailja
  review. Do not apply Flyway to unbuilt services and do not mark S07-G5 PASSED.

problem: >
  The logical information model (ws3-platform/02) is explicit that it is not a physical
  schema. S07-E04-S01..S06 and OPEN-I1 / OPEN-I6 assign that work to Aarti. S07-G5 is
  still OPEN. Two Flyway histories exist (bank-persistence, identity-authorization) and
  are undocumented as a DBA pack. Without this pack, S09 data foundation and every new
  domain service will invent tables.

proposed_solution: >
  Publish docs/platform/data-architecture/ as the Aarti pack: binding rules, physical
  design (including OPEN-I6: lob is an index prefix, not a partition key at R0),
  operations/troubleshooting, a draft database_decision, and one SQL script per schema.
  Existing Flyway remains the runtime truth for the two implemented stores. New schemas
  are design DDL only. Stored procedures are refused for CRUD; triggers and S09 purge
  routines are specified.

alternatives:
  - option: "Adopt architecture-review/05 DynamoDB for Journey, Quotation and Audit in R0"
    rejected_because: >
      No R0 volume evidence; contradicts the already-shipping PostgreSQL job and audit
      store; ADR-008 already chose one Aurora cluster; ADR-011 already refused cache-backed
      idempotency. Revisit with measured access patterns.
  - option: "Extend bank-persistence-service /internal/v1 to every R0 context"
    rejected_because: "ARCH-004 / ADR-008 — a shared persistence service becomes the platform coupling point."
  - option: "Put business writes in stored procedures"
    rejected_because: "Hides invariants from service tests; a second write path. DR-SP-01."
  - option: "Apply Flyway now for unbuilt contexts"
    rejected_because: "Implementation is S09 (OPEN-I1). No owning service exists to run it."

affected_components:
  - "docs/platform/data-architecture/"
  - "docs/context/AGENT-CONTEXT-INDEX.yaml"
  - "docs/governance/registers/SUGGESTION-REGISTER.md"
  - "docs/governance/registers/PARKED-BACKLOG.md"
  - "docs/platform/README.md"
  - "docs/application-lifecycle-bible/evidence/S07-solution-architecture-evidence.md"

files_expected:
  - docs/platform/data-architecture/README.md
  - docs/platform/data-architecture/00-design-rules.md
  - docs/platform/data-architecture/01-physical-design.md
  - docs/platform/data-architecture/02-operations-and-troubleshooting.md
  - docs/platform/data-architecture/DB-DEC-0001-r0-physical-model.md
  - docs/platform/data-architecture/DATA-001.work-item.yaml
  - docs/platform/data-architecture/schemas/README.md
  - docs/platform/data-architecture/schemas/00-cluster-bootstrap.sql
  - docs/platform/data-architecture/schemas/01-identity.sql
  - docs/platform/data-architecture/schemas/02-bank_persistence.sql
  - docs/platform/data-architecture/schemas/03-customer.sql
  - docs/platform/data-architecture/schemas/04-opportunity.sql
  - docs/platform/data-architecture/schemas/05-consent.sql
  - docs/platform/data-architecture/schemas/06-suitability.sql
  - docs/platform/data-architecture/schemas/07-catalogue.sql
  - docs/platform/data-architecture/schemas/08-quotation.sql
  - docs/platform/data-architecture/schemas/09-proposal.sql
  - docs/platform/data-architecture/schemas/10-payment.sql
  - docs/platform/data-architecture/schemas/11-policy.sql
  - docs/platform/data-architecture/schemas/12-journey.sql
  - docs/platform/data-architecture/schemas/13-administration.sql
  - docs/platform/data-architecture/schemas/14-audit_event_delta.sql
  - docs/platform/data-architecture/schemas/90-routines.sql
  - docs/platform/data-architecture/schemas/91-grants.sql
  - docs/governance/plans/PLAN-002-r0-physical-data-architecture.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/governance/registers/PARKED-BACKLOG.md
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/platform/README.md
  - docs/application-lifecycle-bible/evidence/S07-solution-architecture-evidence.md

data_changes: "none — design DDL only; no Flyway applied"
api_changes: none
security_impact: "PII — design of encryption/access and column placement; no runtime change"
compliance_impact: "retention — design of retain_until and INSERT-only grants; no runtime change"
backward_compatibility: compatible
performance_impact: none
operational_impact: "S09 consumes this pack; foundation troubleshooting is documentary"

testing:
  unit: []
  integration: []
  other:
    - "python3 scripts/context/context-load.py validate"
    - "python3 scripts/context/build-doc-map.py --check after regenerate"
    - "java scripts/governance/FreshnessCheck.java (exit 0 or 1 warn only)"
    - "python3 scripts/governance/ci-checks.py"

rollback: >
  Revert the documentation commit. No database has been migrated by this change.

dependencies:
  - "docs/platform/ws3-platform/02-information-model.md"
  - "docs/platform/ws3-platform/01-domain-model-and-invariants.md"
  - "ADR-008"

acceptance_criteria:
  - "AC-1 Every R0 context in the information model ownership matrix has a named schema and a SQL script"
  - "AC-2 Existing two Flyway histories are documented, not rewritten"
  - "AC-3 OPEN-I6 is decided for R0 (index prefix, not partition key) with a named revisit trigger"
  - "AC-4 Required routines are listed; CRUD stored procedures are explicitly refused"
  - "AC-5 Troubleshooting plan exists for foundation and S09-ready incidents"
  - "AC-6 S07-G5 remains OPEN; no stage field edited"

out_of_scope:
  - "Applying new Flyway in any service"
  - "Proven restore, Object Lock deletion-refusal, residency attestation"
  - "Notification, Reporting/MIS, Integration Hub routing, Keycloak vendor schema"
  - "Tokenisation service (OPEN-I2)"
  - "Human signatures"
  - "Changing CURRENT-STATE.yaml stage fields"

assumptions:
  - "R0 volume is the assisted Term Life pilot — not a partitioning driver"
  - "OPEN-D7 residual: Product/BA have not ratified every attribute validation rule; physical types follow the logical sheets"

risks:
  - risk: "Readers treat design SQL as applied migrations"
    mitigation: "Every script header states DESIGN ONLY; DR-MIG-04; parked S09 apply row"
  - risk: "INV-LOB-01 vs LOB-agnostic customer master"
    mitigation: "lob sits on customer_snapshot and journey-scoped rows; recorded as D2 observation"

estimate: L
reviews: []
```
