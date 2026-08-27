# DB-DEC-0002 — CR-013 alignment of the DATA-001 physical pack

Draft Aarti decision record. Format:
[`04-operating-and-review-contract.md` §4](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#4-standard-aarti--dba-decision-output).

Full gap list: [`DATA-002-cr013-alignment.md`](./DATA-002-cr013-alignment.md).

```yaml
database_decision:
  id: DB-DEC-0002
  subject: "Is the DATA-001 R0 physical pack aligned with CR-013 / ADR-014 / DEC-20260825-01?"
  stage: "S08 Foundation Recovery — design delta; S09 apply still parked"
  accountable_database_authority: "Aarti — Principal Insurance Data & Database Architect / DBA"
  business_owner: "Rajal — Lead language, sold meaning, R0 admin/MIS surfaces"
  architecture_owner: "Mahesh — mandatory joint review (archive mechanism, isolation, ingest boundary)"
  engineering_owner: "Amit — Flyway apply in owning services at S09; outbox publisher"
  compliance_reviewer: "Shailja — C-RET-1/2, C-ING-1, C-ISO-1, C-ISS-1 (human T4 outstanding)"
  security_reviewer: "Deepali — ingest upload trust boundary; MIS must not read RESTRICTED ciphertext"
  sre_consumer: "Shivanshi — isolated report/ingest capacity; no new S08 isolation service"
  decision: >
    DATA-001 remains the correct R0 topology (one Aurora cluster, schema per context,
    PostgreSQL SoR, no CRUD SPs, lob as index prefix). It is not sufficient for the
    scope CR-013 pulled into R0. Required physical deltas: Lead ARCHIVED + attribution
    retention split; issuance_mode on proposal and policy; policy.source with nullable
    lead_id and Policy-owned ingest; append-only policy state history; isolated
    reporting/MIS read path (not the Lead writer); per-schema transactional outbox
    (ADR-012, already owed). Physical archive mechanism stays undecided (DEC §12) and
    is W1 joint Aarti/Mahesh. No purge until a written job and a restore test.
  status: CHANGES_REQUIRED
  database_severity: D1
  rationale: >
    CR-013 transcribed Lead archive, issuanceMode, off-platform Policy ingest, and
    R0 admin/MIS into current_scope. The DATA-001 DDL was frozen against the
    pre-CR-013 information model: opportunity.state has no ARCHIVED; proposal/policy
    have no issuance_mode; policy.lead_id is NOT NULL with no source; there is no
    state history table; the pack still says Reporting is out of R0; only identity
    has outbox_event. Shipping W1/W3/W4 against that DDL would violate C-RET-1,
    C-ING-1, C-ISO-1 and C-ISS-1. Severity is D1 (design incomplete for admitted
    R0), not D0 (no integrity failure in running systems — those schemas are design
    DDL only). Archive/purge remains D2 until S09 evidence, matching the Aarti card
    on DEC-20260825-01 §11.
  data_ownership: >
    Lead/Opportunity remains the writer for RM origination. Off-platform sales are
    Policy ingest, never lead.create. Reporting/MIS/admin jobs are not writers on
    opportunity. bank_persistence stays job/audit ingest only.
  integrity_controls:
    - "Keep existing PKs, same-schema FKs, lob CHECK, INSERT-only on immutable tables"
    - "Add ARCHIVED to opportunity.state CHECK; archived_at; RET-7Y attribution columns"
    - "issuance_mode NOT NULL CHECK STP|NON_STP|INSTA on proposal and policy"
    - "policy.source ON_PLATFORM|OFF_PLATFORM; lead_id null iff OFF_PLATFORM"
    - "policy_state_history INSERT-only"
    - "outbox_event in every publishing schema, same transaction as the business write"
  transaction_consistency: >
    Unchanged: ACID inside one schema. Cross-context eventual via outbox + MSK (ADR-012).
    No 2PC. Restore does not reconcile payments.
  performance_implications: >
    MIS/admin/reconciliation must not share the Lead writer (C-ISO-1). Isolation is a
    reader/extract/reporting store, not a second cluster and not a new S08 service.
    Do not add speculative reporting indexes on the writer (DR-IDX-01 rewrite).
  security_lifecycle_implications: >
    Ingest is a new trust boundary (Deepali). Maker-checker columns live on Policy.
    Dashboard/replica roles must not SELECT decryptable RESTRICTED columns.
    Archive is not delete (ID-04).
  availability_recovery_implications: >
    Unchanged design target: RPO 5 min / RTO 30 min, not measured. No purge on the
    R0 cluster until the retention job is written and restore is proven.
  migration_implications: >
    Design DDL delta only in DATA-002. Apply remains S09. Rebase onto origin/main
    before editing 02-information-model.md / 03-solution-architecture-r0.md so
    DATA-001 pointers and CR-013 attributes both survive.
  analytics_implications: >
    Reporting/MIS are now in R0. They consume a replica or extract. They do not
    share the opportunity writer. OpenSearch is not the regulatory archive (ADR-013).
  alternatives_rejected:
    - "Treat DATA-001 as sufficient — admitted R0 columns are missing"
    - "Second Aurora cluster for MIS at R0 — ADR-008 / D5"
    - "New isolation microservice in S08 — SUG-20260825-wl1 REJECTED"
    - "Off-platform lead.create — ADR-005 / D3"
    - "Pick partition vs archive table vs dump in this check — DEC §12 undecided"
    - "Implement the DDL in the same turn the gap was raised — 09 triage rule"
  required_actions:
    - "Implement DATA-002 / PLAN-003 after rebase onto origin/main (not this turn)"
    - "W1 joint Aarti/Mahesh: choose archive mechanism; do not purge until restore test"
    - "Rewrite pack sentences that still say Reporting is out of R0"
    - "Human Aarti / Mahesh / Deepali / Shailja sign S07-G5 against the updated pack"
  human_approval_required: true
  revisit_trigger: >
    Human Shailja rejects the shorter working-Lead class; Aarti finds archive unsafe
    on one cluster; measured OLTP contention from a reporting job (D5 reopen);
    an approved CR removes #18/#19 from R0 or puts them on the Lead writer.
  evidence:
    - "docs/platform/data-architecture/DATA-002-cr013-alignment.md"
    - "origin/main:docs/governance/change-requests/CR-013-r0-lead-mis-admin-scope.md"
    - "origin/main:docs/governance/DEC-20260825-01-lead-domain-decisions.md"
    - "origin/main ADR-014"
    - "schemas/04-opportunity.sql, 09-proposal.sql, 11-policy.sql, 01-identity.sql"
```

**Status:** `AI-DRAFTED`. This is not a human Aarti signature and not a `S07-G5` pass.
