# 00 — Aarti persistence design rules (R0)

**Authority:** Aarti — Principal Insurance Data & Database Architect
**Binding on:** every schema, migration and repository that writes platform state
**Does not bind:** Product behaviour, service boundaries, Security *outcomes*, QA evidence
**Sources:** [`BOOT.md` standing constraints](../../context/BOOT.md),
[`aarti-database.card.md`](../../context/personas/aarti-database.card.md),
[`ADR-008`](../architecture-review/08-architecture-decision-log.md#adr-008--data-ownership-is-the-invariant-physical-cluster-topology-is-an-evidence-led-decision),
[`02-information-model.md`](../ws3-platform/02-information-model.md),
[`01-domain-model-and-invariants.md`](../ws3-platform/01-domain-model-and-invariants.md)

A rule that would change Product behaviour or a service boundary is escalated, not implemented
in the store for convenience.

---

## 1. Ownership and topology

| ID | Rule | Enforced by |
|---|---|---|
| DR-OWN-01 | Exactly one context writes each authoritative attribute | Schema privilege + service API; no cross-schema `SELECT`/`INSERT`/`UPDATE`/`DELETE` grants ([`ADR-008`](../architecture-review/08-architecture-decision-log.md#adr-008--data-ownership-is-the-invariant-physical-cluster-topology-is-an-evidence-led-decision)) |
| DR-OWN-02 | Cross-context facts travel as **references** (ULID / opaque id), never as copied decision columns | Absence of `eligibility`, `premium`, `uw_decision` on `journey.journey` (INV-JRN-02) |
| DR-OWN-03 | `bank-persistence-service` stays scoped to integration jobs, raw payloads and audit **ingestion**. It is not the platform database | [`bank-persistence-service.md`](../../1sb-insurance-integration/architecture/bank-persistence-service.md) · `ARCH-004` |
| DR-OWN-04 | There is no second audit database. Audit-consumer writes `POST /internal/v1/audit-events` on the same store | Standing constraint |
| DR-OWN-05 | `1sb-integration-service` owns no Flyway and no JPA | ArchUnit + module layout |
| DR-OWN-06 | Bank apps never open a database connection | Standing constraint |
| DR-OWN-07 | R0 is **one Aurora cluster, schema per context**, own credential, own migration history. The first physical split follows the LOB-cell / shared-platform seam, not the service boundary | `ADR-008` |
| DR-OWN-08 | Logical foreign keys **do not** become cross-schema SQL `FOREIGN KEY`. Integrity across contexts is the owning service's contract plus events | `ADR-008` invariant half |

## 2. Integrity

| ID | Rule | Enforced by |
|---|---|---|
| DR-INT-01 | Every table has a primary key. New business aggregates use ULID `CHAR(26)`. Existing stores keep their current id types | DDL |
| DR-INT-02 | `lob` is `NOT NULL` and `CHECK (lob IN ('LIFE','HEALTH','GENERAL'))` on every business, configuration and audit row (INV-LOB-01). `product_class` is a **different** column (INV-LOB-02) | Column + CHECK |
| DR-INT-03 | State columns use a closed `CHECK` matching the domain state machine. Illegal transitions are rejected by the aggregate; the CHECK stops garbage states, not the transition table | DDL + domain |
| DR-INT-04 | Uniqueness that the invariant names is a unique constraint, not an application `SELECT` | `policy (insurer_code, policy_number)`; one non-terminal payment per proposal; idempotency key |
| DR-INT-05 | Optimistic concurrency uses `version BIGINT` on mutable aggregates | JPA `@Version` / `UPDATE … WHERE version = :expected` |
| DR-INT-06 | Idempotency records are written in the **same transaction** as the business change (INV-IDM-01). They are not stored in the `ADR-011` cache | Table in the owning schema |
| DR-INT-07 | `accountable_sp_id` is written once. Subsequent `UPDATE` of that column is rejected at the store (INV-ACT-03) | Trigger `trg_accountable_sp_immutable` |
| DR-INT-08 | Append-only tables (`consent` evidence columns, `audit_event`, `raw_payload`, payment attempts, configuration versions) grant `INSERT` (and `SELECT` to the owner). `UPDATE`/`DELETE` are revoked | [`91-grants.sql`](./schemas/91-grants.sql) + blockers in [`90-routines.sql`](./schemas/90-routines.sql) |

## 3. Classification, PII and indexing

| ID | Rule | Enforced by |
|---|---|---|
| DR-PII-01 | An attribute marked ⚑ in the information model is never a queryable plaintext column and never an index key (PII-01) | Encrypted payload / dedicated CMK column |
| DR-PII-02 | Lookup of a confidential identifier (CIF) uses an HMAC lookup hash, not the raw value in a unique index | `customer.cif_lookup_hash` |
| DR-PII-03 | No ⚑ value appears in a log, `error_message`, or `metadata` that is not itself classified | Application + S08-G7 test. The database does not log row values |
| DR-PII-04 | Personal data is not copied between schemas. Exceptions already decided: consent `contact_used` and the customer snapshot | Information model §3 / PII-04 |
| DR-PII-05 | An `INSURER_PARTNER_REP` read is constrained in the **owning** schema by `insurer_id` **and** the `AC-4` predicate. A miss is absence, not a redacted row (PII-07) | Mandatory query predicate `fn_ipr_visible` — documentation in routines; repositories must call it |
| DR-PII-06 | Cardholder data never enters this cluster. Payment stores a link target and a PG reference | Information model §4.7 |

## 4. Access patterns and indexes

| ID | Rule |
|---|---|
| DR-IDX-01 | Index the access path the service actually uses: by id, by journey, by status+time, by resource for audit. Do not index "in case reporting needs it" — Reporting is out of R0 scope |
| DR-IDX-02 | Every SQL `FOREIGN KEY` (same schema only) has an index on the referencing column |
| DR-IDX-03 | `lob` is a **leading index prefix** on list/search paths that are already filtered by LOB. It is **not** a declarative partition key at R0 — see `OPEN-I6` in [`01-physical-design.md`](./01-physical-design.md#5-open-i6--lob-partitioning) |
| DR-IDX-04 | Partial indexes are preferred over partitioning when the hot set is a state (`status = 'IN_PROGRESS'`) |
| DR-IDX-05 | Unique constraints that include `NULL` use a partial unique index where PostgreSQL NULL-semantics would otherwise allow duplicates |

## 5. Stored procedures

| ID | Rule |
|---|---|
| DR-SP-01 | Business transactions are **not** stored procedures. The owning service's repository and aggregate are the write path. A second write path in PL/pgSQL would hide invariants from tests |
| DR-SP-02 | The database **may** hold: immutability triggers, sequence allocators, visibility helpers, and (from S09) retention/purge routines that run as a controlled job role |
| DR-SP-03 | A routine never reaches another schema. Purge that needs object-store deletes is orchestrated by the owning service, not by a cross-store procedure |
| DR-SP-04 | Required routines are listed in [`01-physical-design.md` §6](./01-physical-design.md#6-required-routines) and created in [`90-routines.sql`](./schemas/90-routines.sql) |

## 6. Migration

| ID | Rule |
|---|---|
| DR-MIG-01 | The owning service owns its Flyway history. One linear version sequence per schema |
| DR-MIG-02 | Expand/contract: additive first; drop only after readers are gone. No rewrite of an immutable table |
| DR-MIG-03 | Checksums are immutable. A changed file at the same version is an incident, not a fix |
| DR-MIG-04 | Design DDL in this folder is the **target**. It is applied by S09 Flyway in the owning module, not by running these scripts against a shared admin session in production |
| DR-MIG-05 | Existing H2 `MODE=PostgreSQL` compatibility remains only for `identity` and `bank_persistence` CI. New schemas are PostgreSQL-only |

## 7. Recovery and retention

| ID | Rule |
|---|---|
| DR-REC-01 | Application scaling never overrides a database limit. Name the bottleneck before adding pods ([card](../../context/personas/aarti-database.card.md)) |
| DR-REC-02 | Money-path recovery is **reconciliation**, not restore. Restoring Aurora does not decide whether the PG captured a payment ([`03 §8`](../ws3-platform/03-solution-architecture-r0.md)) |
| DR-REC-03 | Fast traffic restoration must not knowingly corrupt financial or policy data ([operating contract §10](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#10-incident-operating-mode)) |
| DR-REC-04 | Backup, PITR and Object Lock are designed here and **proven** at S09 (`S09-E06-S04`, `S09-VT-07`). A backup that has never been restored is a hypothesis |
| DR-REC-05 | Retention classes from the information model §2.2 are columns (`retain_until`) or archive policy, not comments |

## 8. What Aarti will not decide alone

Recorded so this pack cannot be misread as a signature on someone else's domain
([`AUTHORITY-QUICK-CARD.md`](../../context/personas/AUTHORITY-QUICK-CARD.md)):

- Accepting residual PII or encryption risk — Deepali / Shailja.
- Changing who may create an opportunity, override suitability, or issue a payment link — Rajal / Shailja.
- Merging or splitting services because a join would be easier — Mahesh.
- Claiming the design is QA-verified — Swapnali.
- Declaring `S07-G5` `PASSED` — human Aarti.
