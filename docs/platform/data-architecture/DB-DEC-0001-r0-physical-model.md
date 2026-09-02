# DB-DEC-0001 — R0 physical model

Draft Aarti decision record. Format:
[`04-operating-and-review-contract.md` §4](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#4-standard-aarti--dba-decision-output).

```yaml
database_decision:
  id: DB-DEC-0001
  subject: "R0 physical data architecture — one Aurora cluster, schema per context, PostgreSQL SoR"
  stage: "S07-E04 design / S08 Foundation Recovery (S09 apply parked)"
  accountable_database_authority: "Aarti — Principal Insurance Data & Database Architect / DBA"
  business_owner: "Rajal — Product (logical meaning already in the information model)"
  architecture_owner: "Mahesh — mandatory joint review (ADR-008, ARCH-004)"
  engineering_owner: "Amit — Flyway apply in owning services at S09"
  compliance_reviewer: "Shailja — retention classes and PII classification"
  security_reviewer: "Deepali — encryption outcome and key hierarchy"
  decision: >
    R0 persists every transactional system of record on one Aurora PostgreSQL cluster
    with a schema per bounded context, dedicated credentials and no cross-schema grants.
    identity and bank_persistence already exist and are adopted, not redesigned.
    Journey, Quotation and Audit stay on PostgreSQL at R0 (DynamoDB proposal in
    architecture-review/05 is rejected until volume evidence). Stored procedures are
    not the write path. lob is an index prefix, not a partition key, until a second
    LOB or a measured size/IO threshold arrives.
  status: APPROVED_WITH_OBSERVATIONS
  database_severity: D1
  rationale: >
    ADR-008 already separated ownership (invariant) from cluster topology (evidence).
    The information model is relational: state machines, uniqueness, append-only
    evidence, money matching, and reconstruction from audit. R0 load is one assisted
    Term Life path. A second engine for Journey/Quote/Audit would split the SoR,
    contradict shipping Flyway, and fail the anti-over-engineering tests (no measured
    access pattern). Observations: restore and purge are unproven (D2 until S09);
    customer master is LOB-agnostic while INV-LOB-01 is enforced on snapshots and
    journey-scoped rows; OPEN-D7 attribute validation is still Product/BA.
  data_ownership: >
    One writer per attribute as ws3-platform/02 §3. bank_persistence remains the
    integration job/correlation and audit ingestion store only.
  integrity_controls:
    - "Primary keys; same-schema FKs only"
    - "CHECK on lob and state enumerations"
    - "Unique (insurer_code, policy_number); one non-terminal payment per proposal"
    - "INSERT-only grants on immutable tables"
    - "accountable_sp_id immutability trigger"
    - "Idempotency row in the same transaction as the business write"
  transaction_consistency: >
    ACID inside one schema. Cross-context: eventual via outbox + events (ADR-012).
    No 2PC. Payment RECONCILED is the issuance gate, not a restore.
  performance_implications: >
    R0 indexes follow known access paths (id, journey, status+time, audit resource).
    Partitioning deferred. Catalogue read cache is ADR-011 / catalogue service, not
    this cluster's problem.
  security_lifecycle_implications: >
    ⚑ attributes in encrypted payloads or dedicated-CMK columns. CIF lookup via HMAC.
    Deepali owns the CMK hierarchy (S09-E04-S03). This pack specifies where ciphertext
    lives, not the key policy.
  availability_recovery_implications: >
    Design target RPO 5 min (Aurora PITR continuous backup) · RTO 30 min (Multi-AZ).
    Not measured. Shivanshi consumes these numbers; she does not invent them.
  migration_implications: >
    Design DDL only. Apply per owning service Flyway at S09. Existing V1 histories
    stay. Audit column add is expand-only (OPEN-I3 / OPEN-I5).
  analytics_implications: >
    No analytical schema in this cluster. Reporting is out of R0 scope and must not
    share OLTP infrastructure (architecture-review/05).
  alternatives_rejected:
    - "Cluster per service at R0 — ADR-008"
    - "Shared persistence service for business contexts — ARCH-004"
    - "DynamoDB Journey/Quote/Audit at R0 — no volume evidence"
    - "CRUD stored procedures — DR-SP-01"
    - "Declarative lob partitioning at R0 — OPEN-I6 decision"
  required_actions:
    - "Human Aarti signs S07-G5 against this pack + DB-DEC-0001"
    - "Mahesh joint-reviews ownership / no-cross-schema-grant"
    - "Deepali reviews encryption placement vs CMK plan"
    - "Shailja confirms retention classes vs OPEN-I4"
    - "Amit applies Flyway at S09 from the design scripts (parked)"
    - "Shivanshi proves restore (S09-E06-S04) — parked"
  human_approval_required: true
  revisit_trigger: >
    A second LOB is admitted; any table exceeds a measured size or IO threshold;
    restore test misses RTO/RPO; a service needs a cross-schema join to be correct
    (that is an ownership defect, not a grant).
  evidence:
    - "docs/platform/data-architecture/"
    - "docs/platform/ws3-platform/02-information-model.md"
    - "docs/platform/architecture-review/08-architecture-decision-log.md (ADR-008)"
    - "services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql"
    - "services/identity-authorization-service/src/main/resources/db/migration/V1__identity_authorization_schema.sql"
```

### Observations (non-blocking for design review)

1. **Restore unproven.** Severity stays `D1` for design; becomes `D2` if S09 starts without a restore drill.
2. **Customer vs INV-LOB-01.** The living customer snapshot source is CBS and is not a LOB entity. `lob` is mandatory on `customer.customer_snapshot` and on every journey-scoped table. Escalated only if Mahesh requires `lob` on `customer.customer` itself.
3. **Audit columns missing in V1.** `14-audit_event_delta.sql` is the expand migration; not applied here (`OPEN-I3`, `OPEN-I5`).
4. **GAP-016 / OPEN-D7.** Physical types follow the logical sheets. Per-attribute business validation remains Product/BA.
