# DB-DEC-0003 — R0 store segregation and service credentials

Draft Aarti decision record. Format:
[`04-operating-and-review-contract.md` §4](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#4-standard-aarti--dba-decision-output).

Companion narrative: [`04-store-segregation-and-roadmap.md`](./04-store-segregation-and-roadmap.md).
Does not replace [`DB-DEC-0001`](./DB-DEC-0001-r0-physical-model.md); it overlays service access and the Dynamo empty-set.

```yaml
database_decision:
  id: DB-DEC-0003
  subject: "R0 e2e store map — Postgres SoR, Dynamo empty, one role per schema"
  stage: "S08 design delta / S09 apply parked"
  accountable_database_authority: "Aarti — Principal Insurance Data & Database Architect / DBA"
  business_owner: "Rajal — Product (logical meaning already in the information model)"
  architecture_owner: "Mahesh — mandatory joint review (ADR-008, strategic store, SoT)"
  engineering_owner: "Amit — Flyway per owning service at S09/S11"
  compliance_reviewer: "Shailja — retention class does not move with engine"
  security_reviewer: "Deepali — per-schema secrets, no Dynamo IAM at R0"
  decision: >
    Every transactional system of record for the R0 assisted Life path lives on one
    Aurora PostgreSQL cluster, schema per bounded context. DynamoDB holds no R0
    SoR and no service is granted Dynamo access. Sessions stay on the ADR-011
    Valkey cache. Audit ingest stays on bank_persistence.audit_event via HTTP;
    there is no second audit database. Each owning service authenticates as its
    own app_<schema> role; replicas share that role; services that own no schema
    have no Aurora login. 1sb-integration-service never opens a datasource.
    The architecture-review/05 Dynamo rows for Journey, Quotation, Audit,
    Notification, Hub routing and Identity sessions are not the R0 apply list.
  status: APPROVED_WITH_OBSERVATIONS
  database_severity: D1
  rationale: >
    DB-DEC-0001 already rejected Dynamo for Journey/Quote/Audit at R0 for lack of
    volume evidence. ADR-011 withdrew Dynamo sessions. ADR-008 made ownership
    plus per-schema credentials the isolation mechanism. The remaining gap was
    operational: engineers still read 05/02 as if R0 services should open Dynamo.
    This decision names the empty Dynamo access list, the service-to-role matrix
    and the evidence-gated revisit so S09 IaC cannot provision a second SoR by
    diagram inertia.
  data_ownership: >
    One writer per attribute. bank_persistence remains job/correlation and audit
    ingest only. Integration Hub and Notification own no Aurora schema at R0.
    Reporting never receives a writer role.
  integrity_controls:
    - "No cross-schema GRANT or FK"
    - "One app_* role and one migrator_* role per schema"
    - "INSERT-only on audit_event and raw_payload"
    - "Idempotency in the owning schema, not cache or Dynamo"
    - "Zero Dynamo IAM principals at R0"
  transaction_consistency: >
    ACID inside one schema. Cross-context eventual via outbox + MSK (ADR-012).
    No 2PC. Payment RECONCILED is the issuance gate.
  performance_implications: >
    R0 bottleneck is provider latency and the customer-device payment path, not
    Aurora. Cap connection pools per role. Do not introduce Dynamo to chase an
    unmeasured access pattern.
  security_lifecycle_implications: >
    Isolation is logical. Deepali owns secret creation, rotation and the
    prohibition on standing human write. Break-glass is time-bound SELECT
    without decrypt. No dynamodb:* on platform task roles at R0.
  availability_recovery_implications: >
    Same design target as DB-DEC-0001: RPO 5 min, RTO 30 min, unproven until
    S09 restore. Dynamo PITR is irrelevant because no Dynamo SoR exists.
  migration_implications: >
    Design only. S09 applies identity and bank_persistence. Each later service
    applies its own schema. No Dynamo table migration to plan.
  analytics_implications: >
    MIS is an isolated reader / S3 extract. Not this cluster's writer roles.
  alternatives_rejected:
    - "Put Journey, Quotation or Audit on DynamoDB at R0 — DB-DEC-0001"
    - "DynamoDB sessions — withdrawn by ADR-011"
    - "One shared Aurora login for all services — breaks ADR-008 ownership"
    - "Give 1sb-integration-service or the BFF a datasource — standing constraint"
    - "Cluster per service at R0 — ADR-008"
    - "Extend bank-persistence-service to business contexts — ARCH-004"
  required_actions:
    - "Human Aarti signs S07-G5 against DB-DEC-0001 plus this overlay"
    - "Mahesh joint-reviews the R0 overlay vs architecture-review/05 Dynamo rows"
    - "Deepali reviews per-schema secrets and the empty Dynamo IAM set"
    - "Shivanshi provisions no Dynamo resource in the S09 inventory"
    - "Amit applies Flyway from owning services only (parked)"
  human_approval_required: true
  revisit_trigger: >
    A named table with measured partition key, item size and QPS shows Aurora
    is the actual bottleneck; or ADR-008 cluster-split triggers fire; or a
    security/regulatory demand requires a different engine. Any Dynamo
    introduction is a source-of-truth change and needs joint Aarti/Mahesh
    review. Audit may not grow a second database.
  evidence:
    - "docs/platform/data-architecture/04-store-segregation-and-roadmap.md"
    - "docs/platform/data-architecture/DB-DEC-0001-r0-physical-model.md"
    - "docs/platform/architecture-review/08-architecture-decision-log.md (ADR-008, ADR-011)"
    - "docs/platform/data-architecture/schemas/91-grants.sql"
    - "docs/platform/ws3-platform/03-solution-architecture-r0.md"
```

### Observations (non-blocking for design review)

1. **Diagram drift.** `05-data-architecture.md` and `02-target-microservices-architecture.md` still name Dynamo for several services. This decision does not edit those files (Mahesh). Until he amends them, this overlay is the R0 apply list.
2. **Lead schema name.** Main still has `opportunity`; `SUG-20261009-lms` / DATA-003 uses `lead_lms`. Roles follow the schema name that is applied.
3. **Restore unproven.** Severity stays `D1` for design; becomes `D2` if S09 starts without a restore drill (same as `DB-DEC-0001`).
