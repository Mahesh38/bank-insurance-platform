# 02 — Operations: migration, encryption, backup, retention, troubleshooting

**Design only.** Restore, Object Lock and residency attestation are proven at S09
([`S09-E06`](../../application-lifecycle-bible/stages/S09-platform-foundation.md#s09-e06--data-protection-backup-and-retention--aarti--shivanshi)).
This document is what Shivanshi may rely on for **targets**, not for a signed Board 7 pass.

---

## 1. Migration strategy (`S07-E04-S03`)

| Topic | Rule |
|---|---|
| Tool | Flyway, one history **per owning service** |
| Location | `src/main/resources/db/migration/` of that service only |
| Local / CI | `identity` and `bank_persistence`: H2 `MODE=PostgreSQL`. All other schemas: PostgreSQL (Testcontainers or a local Aurora-compatible instance at S09) |
| UAT / prod | Aurora PostgreSQL, same scripts |
| Order | Expand → dual-write/read if needed → contract. Never rewrite `audit_event` or `raw_payload` |
| Rollback | Prefer roll-forward. Destructive down-scripts are forbidden on immutable tables |
| Mixed versions | Additive columns are nullable or have defaults so old writers survive a rolling deploy (`S09-E03-S04`) |
| Design → apply | Copy the relevant [`schemas/*.sql`](./schemas/) into a versioned Flyway file in the owning module. Do **not** run the design folder against production with a superuser |

Existing histories stay at V1/V2. Next additive work: `14-audit_event_delta.sql` as
`V3__audit_event_reconstruction_columns.sql` on `bank-persistence-service` (parked apply).

---

## 2. Encryption and access (`S07-E04-S04`)

Aarti specifies **where** ciphertext lives. Deepali owns the security outcome and the CMK
hierarchy (`S09-E04-S03`).

| Class | At rest | In transit | DB role |
|---|---|---|---|
| `PUBLIC` / `INTERNAL` | Cluster CMK (Aurora default) | TLS 1.3 to the cluster; mTLS service-to-service | Owner `SELECT/INSERT/UPDATE/DELETE` as the table allows |
| `CONFIDENTIAL` | Cluster CMK; ⚑ columns additionally application-encrypted | Same | Owner only; no reporting replica until a classified replica design exists (not R0) |
| `RESTRICTED` | Dedicated CMK (`payment`, `consent`, payload store) | Same | Owner + INSERT-only on evidence tables |

Access model:

```text
app_<schema>     — the service. Least privilege per 91-grants.sql
migrator_<schema>— Flyway at deploy. DDL + DML for seed only
ro_breakglass    — time-bound, ticketed, logged. No ⚑ decrypt.
job_retention    — S09 purge routines only
```

No human standing write on `audit_event` or `raw_payload`. Immutability is a **privilege**,
not the absence of a delete button (INV-AUD-01).

IAM/network isolation of the cluster is Shivanshi + Deepali (S09). This pack requires: private
subnets, no public hostname, one login per schema, `rds_iam` or Secrets Manager rotation.

---

## 3. Backup, PITR, recovery (`S07-E04-S05`)

| Store | RPO target | RTO target | Mechanism |
|---|---|---|---|
| Aurora cluster (all schemas) | **5 minutes** | **30 minutes** | Continuous PITR + Multi-AZ failover |
| S3 evidence (payloads, policy PDF, audit archive) | **0** (write acknowledged) | **60 minutes** to restore a prefix | Object Lock compliance mode; versioning |
| `ADR-011` cache | n/a | n/a | Reconstruct by re-auth / cache miss |
| MSK | n/a | n/a | Replay from the **outbox** in Aurora (`ADR-012`) |

These numbers are **design targets** for S09-E06. They are not measured. Shivanshi consumes
them; she does not invent a tighter RTO by adding pods.

**What restore is for:** cluster loss, schema-wide corruption, ransomware, regional DR
(`ap-south-2` warm standby — not designed here).

**What restore is not for:** a single payment in `UNCERTAIN`, a missing audit event, a
reconciliation break. Those are application/reconciliation procedures. Restoring the database
to "undo" a capture can create a second truth against the PG
([`03 §8`](../ws3-platform/03-solution-architecture-r0.md)).

DR note: cache, broker and search are deliberately **not** replicated
([`03` robustness](../ws3-platform/03-solution-architecture-r0.md)). Outbox lives in Aurora
and is therefore already in the PITR window.

---

## 4. Retention and purge (`S07-E04-S06`)

| Class | Implementation |
|---|---|
| `RET-7Y-IMMUTABLE` | `retain_until = created_at::date + 7 years`; INSERT-only role; S3 Object Lock for archives |
| `RET-7Y` | `retain_until` on close + 7 years; mutable in life |
| `RET-POLICY+7Y` | `retain_until` on policy terminal + 7 years |
| `RET-OPERATIONAL` | 90 days — poll attempts, unpublished-outbox tombstones after success |
| `RET-TRANSIENT` | `idempotency_record.expires_at` ≤ 24h |

`OPEN-I4` (exact IRDAI/DPDP horizon) can move the interval; it must not remove `retain_until`.

Purge (`sp_retention_sweep`, `sp_purge_operational`) is **S09-E06-S06**. The procedure only
returns candidates or deletes operational rows in its own schema. Disposal of evidence writes
an audit record of the disposal (PII-06) from the owning service, after Object Lock allows it
(which, for 7-year evidence, is after seven years).

---

## 5. Troubleshooting plan

Incident order is fixed
([operating contract §10](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#10-incident-operating-mode)):

1. Protect integrity  
2. Stop further corruption/loss  
3. Stabilise the database  
4. Restore availability **safely**  
5. Verify consistency / reconciliation  
6. Recover missing state if required  
7. Root cause  
8. Preventive control  

**Aarti** owns database technical authority. **Amit** owns connections, transactions, retries.
**Mahesh** owns cross-system recovery. **Rajal** owns customer impact. **Shailja** owns
reportability. Fast traffic restoration must not knowingly corrupt financial or policy data.

### 5.1 Foundation (today — H2 / local PostgreSQL / Flyway)

| Symptom | Likely cause | Do | Do not |
|---|---|---|---|
| Integration job-store 404 / connection refused on 8081 | Persistence not running | Start `bank-persistence-service` with `local` profile; `BANK_PERSISTENCE_BASE_URL` | Point integration at a database JDBC URL |
| Flyway checksum mismatch | A V1 file was edited | Treat as an incident; repair only with a new version in non-prod; never `flyway repair` in prod to hide an edit | Rewrite history |
| Flyway "validation failed" after pull | Two histories diverged on a branch | Rebase; one linear sequence per module | Copy SQL into the other service |
| H2 type error (`JSONB`, `TIMESTAMPTZ`) | New SQL used PG-only types in the two CI schemas | Keep `identity` / `bank_persistence` on the H2-safe subset **or** move that module's tests to PostgreSQL | Sprinkle `JSONB` into V1 |
| "Table already exists" on boot | Two services sharing one H2 file / URL | Each service has its own JDBC URL | One datasource for both |
| Lock timeout on `integration_job` | Long transaction + poller | Check `version` usage; shorten transaction to one aggregate | Raise lock timeout and continue |
| Duplicate idempotency key | Retry succeeded twice | Unique constraint is working. Return stored result | Delete the unique index |
| Audit row updated | Grant too wide or missing trigger | Revoke `UPDATE`/`DELETE`; add blocker trigger | Fix "in the app" only |

### 5.2 S09-ready (Aurora)

| Symptom | Likely cause | Do | Do not |
|---|---|---|---|
| Failover / brief connection errors | Multi-AZ failover | Use the cluster **writer** endpoint; retry idempotent writes; fail closed on money path if unsure | Spray new payment attempts (`INV-PAY-04`) |
| `too many connections` | Pool × replicas | Lower `maximum-pool-size`; one pool per service; name the **actual** bottleneck | Add pods |
| CPU / IO high on `audit_event` | Missing `journey_id+sequence` or seq scan | Check `14` indexes; archive cold rows to S3 (design), do not delete | Drop INSERT-only to "clean up" |
| Replication lag (if a reader is added later) | Read replica used for SoR | R0 has **no** SoR read replica. Writers use the writer | Read-your-writes on a replica |
| PITR restore requested | Data loss / corruption | Restore to a **new** cluster; reconcile payments against the PG; cut over only after INV-PAY-05 style matching | Restore in place over a cluster that has newer captures |
| Missing `sequence_no` gap | Lost audit event | Replay from outbox; do not mint a fake gap-fill row without the original payload | Mark journey `SOLD` (INV-JRN-05) |
| IPR sees another insurer's row | Predicate missing | Treat as `D0` / security incident; revoke the query path; Deepali + Aarti | "Filter in the BFF" |
| Migration blocking deploy | Expand not used | Abort; ship additive-only; roll forward | Accept a rewrite of `audit_event` |
| Credential leak | Secret in logs / image | Rotate (`S09-E04`); revoke; Deepali | Continue with the same password |

### 5.3 Severity (database only)

| Sev | Meaning | Example |
|---|---|---|
| `D0` | Integrity or isolation broken now | Cross-insurer read; UPDATE on audit; restore that duplicates a capture |
| `D1` | Design or control missing that will break a gate or a sale | Missing `sequence_no`; no INSERT-only grant |
| `D2` | Operability / unproven recovery | Backup never restored |
| `D3` | Hygiene | Missing recommended PDP index |

A `D0` stays with Aarti inside DB jurisdiction and is resolved through the cross-persona
protocol when it blocks an architecture option
([card](../../context/personas/aarti-database.card.md)).

### 5.4 Evidence to collect before changing data

- Writer endpoint, engine version, `pg_stat_activity` (query text **redacted** of PII)
- Flyway `flyway_schema_history` rows (version, checksum, success)
- For money: `payment.state`, `pg_txn_id`, PG settlement file — **not** a snapshot restore
- For audit: `journey_id`, `sequence_no` min/max/count
- Trace id (already on `audit_event`)

Never paste ⚑ values into the ticket.

---

## 6. Monitoring signals (for Shivanshi — not built here)

Parked production dashboards stay parked
([`PARKED-BACKLOG.md` §2](../../governance/registers/PARKED-BACKLOG.md#2-parked--stage-deferred-by-nature)).
The **signals** this design needs, when those dashboards unpark:

- Aurora: CPU, FreeableMemory, DatabaseConnections vs `max_connections`, CommitLatency, Deadlocks
- ReplicaLag (when a replica exists)
- Flyway migrate duration and fail count (pipeline)
- Idempotency unique-violation rate (expected on retry; spike = client bug)
- Audit `sequence_no` gap job (INV-AUD-02 / reconstruction test)

---

## 7. Roles during an incident

| Step | Aarti | Amit | Shivanshi | Others |
|---|---|---|---|---|
| Integrity hold | Freeze writes / revoke role | Stop publishers | Isolate network | — |
| Stabilise | Failover / PITR decision | Recycle pools | Execute IaC / restore cluster | — |
| Reconcile | Confirm store consistency | Replay outbox; payment match | — | Rajal impact; Shailja reportability |
| Close | Preventive DDL / grant | Test + deploy | Alert / runbook | Swapnali evidence |
