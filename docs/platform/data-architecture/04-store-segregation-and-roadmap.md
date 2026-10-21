# 04 — E2E store segregation, service access and roadmap

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Work item:** [`DATA-004`](./DATA-004.work-item.yaml) · origin [`SUG-20261012-pdr`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261012-pdr--e2e-postgres-vs-dynamodb-segregation)
**Decision:** [`DB-DEC-0003`](./DB-DEC-0003-store-segregation.md)
**Owner:** Aarti — Principal Insurance Data & Database Architect
**Joint review:** Mahesh (ownership / strategic store) · Deepali (credential / IAM outcome) · Shivanshi (restore, connection limits)
**Status:** `AI-DRAFTED` — does not manufacture `S07-G5`, `ADR-008` or a T4 signature.
**Stage:** S08 design delta. Apply, roles and restore remain S09.

Freshness disclosure: `04-STAGE_GATES.md` is past its 14-day artefact limit (WARN). This pack does not edit stage state.

This document answers, for the R0 assisted Life path and the later evidence-led split:

1. What lives in **Aurora PostgreSQL** and what does **not**.
2. What, if anything, lives in **DynamoDB** — and which services may open it.
3. How **data segregation** is enforced (schema + credential, not engine).
4. **Which service** uses **which store**, and **which database user**.
5. The **roadmap** from this design through S09 apply, S11 vertical slice, and a Dynamo revisit.

ER diagrams, every column and the physical-vs-logical relationship map: [`05-e2e-er-and-field-map.md`](./05-e2e-er-and-field-map.md).

Cite: [`DB-DEC-0001`](./DB-DEC-0001-r0-physical-model.md) · [`ADR-008`](../architecture-review/08-architecture-decision-log.md#adr-008--data-ownership-is-the-invariant-physical-cluster-topology-is-an-evidence-led-decision) · [`ADR-011`](../architecture-review/08-architecture-decision-log.md) · [`01-physical-design.md` §1](./01-physical-design.md#1-topology) · [`91-grants.sql`](./schemas/91-grants.sql) · [`05-data-architecture.md`](../architecture-review/05-data-architecture.md) · [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) · [`03-solution-architecture-r0.md` §4](../ws3-platform/03-solution-architecture-r0.md).

---

## 1. Aarti verdict (draft)

| | |
|---|---|
| **Decision** | `APPROVED_WITH_OBSERVATIONS` as a draft overlay on `DB-DEC-0001` |
| **Severity** | `D1` — store map is decided; restore, IAM roles and connection-pool caps are unproven until S09 |
| **R0 transactional SoR** | One Aurora PostgreSQL cluster. One schema per bounded context. One application role per schema. No cross-schema `GRANT`. |
| **DynamoDB at R0** | **Amended 2026-10-21 by `DB-DEC-0005` (owner):** pre-payment quote and proposal working state only, two tables, two owning services. Not a system of record for anything sold, paid or evidenced. Journey and Audit in DynamoDB stay **rejected**; sessions are **not** Dynamo (`ADR-011`). |
| **Segregation mechanism** | Ownership + credential, not a second engine. A service that needs another context's fact calls that service's API or consumes its event. It never opens that schema. |
| **Credential rule** | Yes — **each owning service uses a different PostgreSQL role**. Replicas of the same service share that role. Services that own no schema have **no** Aurora login. |

Observations:

1. [`05-data-architecture.md`](../architecture-review/05-data-architecture.md) and [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) still list Dynamo for Journey, Quotation, Audit, Notification, Integration Hub routing and Identity sessions. Those rows are **target-state options**, not the R0 apply list. This pack is the R0 DBA overlay. Mahesh owns any later diagram amendment.
2. Lead schema on this stacked pack is `lead_lms` / table `lead` (`DATA-003` / `SUG-20261009-lms`). Role is `app_lead_lms`. Spoken name stays Lead; identifiers stay `lead_id`.
3. Human Aarti / Mahesh / Deepali signatures remain outstanding. An agent does not close `S07-G5`.

---

## 2. How data is segregated

Segregation is **three layers**. Engine choice is the last of them, not the first.

```text
  Channel (NIP-APP / Figma)     never opens a database
            │
            ▼
  workforce-access-bff          Valkey session only (ADR-011). No Aurora. No Dynamo.
            │  API
            ▼
  owning domain service         one schema · one app_* role · own Flyway
            │
            ▼
  Aurora PostgreSQL (one cluster, ap-south-1)
       identity | bank_persistence | customer | lead_lms
       consent  | suitability      | catalogue| quotation
       proposal | payment          | policy   | journey | administration
            │
            │  no cross-schema GRANT, no cross-schema FK
            ▼
  other contexts reached only by:
       sync API to the owning service
       async event (outbox → MSK → consumer)   (ADR-012)
```

| Layer | What it separates | Enforced by |
|---|---|---|
| **1. Ownership** | One writer per authoritative attribute | Service boundary + ArchUnit (`DR-OWN-01`, `ADR-008` invariant) |
| **2. Credential** | One PostgreSQL role per schema; no cross-schema privilege | [`91-grants.sql`](./schemas/91-grants.sql) + S09 IAM / secret per role |
| **3. Engine / cluster** | Same cluster at R0; first **physical** split is LOB-cell vs shared-platform, not service-per-cluster | `ADR-008` decision half |

What this is **not**:

- Not "Lead in Postgres, Journey in Dynamo, Audit in Dynamo". That forks the SoR and was rejected (`DB-DEC-0001` alternatives).
- Not one shared `bank-persistence-service` for every business table (`ARCH-004` / `DR-OWN-03`).
- Not one Aurora login shared by every pod in the platform.
- Not a SQL `FOREIGN KEY` from `journey.lead_id` to the Lead schema (`DR-OWN-08`). The id is a logical reference; the Lead service validates it.

Cross-context facts travel as **opaque ids** (ULID). Journey holds stage and references only (`INV-JRN-02`). Premium, eligibility, UW decision and payment state are never copied onto `journey`.

---

## 3. Store catalogue — what goes where

### 3.1 Aurora PostgreSQL — every transactional SoR

One cluster, Multi-AZ, PITR, KMS at rest. Design target RPO 5 min / RTO 30 min — **not measured**.

| Schema (main / this branch) | Spoken context | Owning service | Why PostgreSQL |
|---|---|---|---|
| `identity` | Identity & Access (#3) | `identity-authorization-service` | Roles, entitlements, maker-checker, certifications — relational, already shipping Flyway |
| `bank_persistence` | 1SB job store + audit **ingest** | `bank-persistence-service` | Existing job/correlation + `audit_event`. Not the platform business DB |
| `customer` | Customer (#4) | `customer-service` | ETB snapshot, CIF HMAC lookup, journey-scoped freeze |
| `lead_lms` | Lead (#5) | `lead-service` | Working inbox, state machine, assignment history, archive-as-state |
| `consent` | Consent (#6) | `consent-service` | Append-only evidence, OTP-verified grant |
| `suitability` | Suitability (#7) | `suitability-service` | Versioned assessment, encrypted answers, mapping runs, lock |
| `catalogue` | Product Catalogue (#8) | `product-catalogue-service` | Effective-dated products and eligibility |
| `quotation` | Quotation (#10) | `quotation-service` | Working envelope until Payment `RECONCILED`, then structured quote + offer (`DATA-005`, `INV-QUO-05`) |
| `proposal` | Proposal & UW (#11) | `proposal-service` | Working envelope until Payment `RECONCILED`, then structured proposal / UW |
| `payment` | Payment (#12) | `payment-service` | Money path: attempts, one non-terminal payment per proposal, reconciliation |
| `policy` | Policy & Issuance (#13) | `policy-issuance-service` | Unique immutable policy number; PDF bytes stay in S3 |
| `journey` | Journey Orchestration (#9) | `journey-orchestration-service` | Thin stage + refs row — relational, not a document-store problem at R0 load |
| `administration` | Administration & Config (#19) | `administration-config-service` | Versioned INSERT-only configuration |

Audit **events** are rows in `bank_persistence.audit_event` (INSERT-only). `audit-compliance-service` does **not** get its own audit database and does **not** get Dynamo. It writes through `POST /internal/v1/audit-events` on `bank-persistence-service` (`DR-OWN-04`).

Idempotency is a table **inside the owning schema**, written in the same transaction as the business change (`DR-INT-06`). It is not Dynamo and not the `ADR-011` cache.

### 3.2 DynamoDB — R0 access list (amended by DB-DEC-0005)

**2026-10-21 owner decision ([`DB-DEC-0005`](./DB-DEC-0005-dynamodb-working-payload.md), Mahesh, HUMAN):** DynamoDB holds
the **pre-payment working state of quotes and proposals**. Once Payment is `RECONCILED` the owning
service materialises structured rows into Aurora and Aurora is the record. Everything else in the
original ruling stands.

| Candidate | R0 ruling | Store |
|---|---|---|
| Quote working state (request, all offers, selection) before payment | **Admitted — `DB-DEC-0005`** | DynamoDB `{env}-quotation-working`, owner `quotation-service` |
| Proposal working state (form answers, nominee, UW requirements) before payment | **Admitted — `DB-DEC-0005`** | DynamoDB `{env}-proposal-working`, owner `proposal-service` |
| Structured quote / offer / proposal / UW after `RECONCILED` | Unchanged | Aurora `quotation` / `proposal` |
| Journey state machine | **Rejected** — stage + refs, relational | Aurora `journey` |
| Quotation job / poll records | **Rejected** — adapter jobs stay | Aurora `bank_persistence.integration_job` |
| Audit hot store | **Rejected** — no second audit SoR | Aurora `bank_persistence.audit_event` + S3 WORM |
| Identity sessions | **Withdrawn by `ADR-011`** | Valkey |
| Integration Hub routing | **Rejected** — versioned configuration | Aurora `administration` |
| Notification delivery log | Not a table at R0 | Audit event |
| Reporting | Never Dynamo, never this OLTP cluster | S3 + Athena / Redshift |
| Payment, policy, consent, suitability | **Never** — money and evidence | Aurora |

**R0 Dynamo principals:** `quotation-service` (own table only), `proposal-service` (own table only).
**R0 Dynamo tables:** `{env}-quotation-working`, `{env}-proposal-working`.
**Any other `dynamodb:*` grant to a platform service is a defect.** Table design, IAM and write
patterns: [`schemas/20-dynamodb-working-payload.md`](./schemas/20-dynamodb-working-payload.md).

### 3.3 Stores that are not PostgreSQL and not Dynamo

| Store | What lives there | Who may open it | What must never live there |
|---|---|---|---|
| **ElastiCache Valkey** (`ADR-011`) | BFF sessions; catalogue L2 read-through; per-principal rate-limit counters | Owning service's **Valkey ACL user** and key prefix (`FF-24`) | Idempotency, suitability, consent, payment, certification, configuration past TTL |
| **S3** | Encrypted raw 1SB/insurer payloads; policy PDFs; audit **cold** WORM archive (Object Lock, 7-year class) | Owning service IAM role, prefix-scoped | Queryable SoR; card data; a substitute for `audit_event` |
| **Keycloak vendor DB** | IdP realm only | Keycloak operators via the adapter boundary | Business authorization / PDP (`identity` schema is the PDP SoR) |
| **MSK** | Domain events in flight | Producers via outbox; consumers via their service | The only copy of regulatory evidence (`ADR-012` / `FF-26`) |
| **OpenSearch** | Operational search if admitted later | Search readers | Regulatory evidence, gate answers, audit reconstruction (`ADR-013`) |
| **S3 + Athena / Redshift** | MIS / reporting read models | `reporting-mis-service` **reader** path only | Writer credentials of Lead / Payment / Policy (`CR-013` C-ISO-1) |
| **Payment gateway** | Card capture on the **customer device** | Customer device only | Cardholder data in any platform cluster (`DR-PII-06`) |

---

## 4. Service → store → credential

Yes: **each owning service has a different Aurora user**. That user is a PostgreSQL **role**, created by IaC at S09, stored as a secret, rotated. Pods of the same service share the role. They do not share another service's role.

Pattern (already named in [`91-grants.sql`](./schemas/91-grants.sql)):

| Role class | Name pattern | Privilege | Who uses it |
|---|---|---|---|
| Application | `app_<schema>` | Least-privilege DML on **that schema only** | The owning service's runtime |
| Migrator | `migrator_<schema>` | DDL + Flyway history on **that schema only** | CI / startup migration job for that service |
| Retention job | `job_retention` | `EXECUTE` on purge procedures; **no** evidence `DELETE` | Scheduled job, not a business API |
| Break-glass read | `ro_breakglass` | Time-bound `SELECT` on named schemas; no decrypt | Human incident, IAM-brokered, not a standing app secret |

Human standing write on evidence tables is forbidden.

### 4.1 Services that open Aurora

| Deployable | Aurora schema | Runtime role | Migrator role | Also opens |
|---|---|---|---|---|
| `identity-authorization-service` | `identity` | `app_identity` | `migrator_identity` | — |
| `bank-persistence-service` | `bank_persistence` | `app_bank_persistence` | `migrator_bank_persistence` | S3 prefix for `raw_payload` bytes |
| `customer-service` | `customer` | `app_customer` | `migrator_customer` | — |
| `lead-service` | `lead_lms` | `app_lead_lms` | `migrator_lead_lms` | — |
| `consent-service` | `consent` | `app_consent` | `migrator_consent` | — |
| `suitability-service` | `suitability` | `app_suitability` | `migrator_suitability` | — |
| `product-catalogue-service` | `catalogue` | `app_catalogue` | `migrator_catalogue` | Valkey prefix `catalogue:` (read cache) |
| `journey-orchestration-service` | `journey` | `app_journey` | `migrator_journey` | — |
| `quotation-service` | `quotation` | `app_quotation` | `migrator_quotation` | DynamoDB `{env}-quotation-working` (`DB-DEC-0005`) |
| `proposal-service` | `proposal` | `app_proposal` | `migrator_proposal` | DynamoDB `{env}-proposal-working` (`DB-DEC-0005`) |
| `payment-service` | `payment` | `app_payment` | `migrator_payment` | — |
| `policy-issuance-service` | `policy` | `app_policy` | `migrator_policy` | S3 prefix for policy PDFs |
| `administration-config-service` | `administration` | `app_administration` | `migrator_administration` | — |

`app_bank_persistence` may `INSERT`/`SELECT` `audit_event` and `raw_payload`. It may **not** `UPDATE`/`DELETE` those tables.

### 4.2 Services that must not open Aurora or Dynamo

| Deployable | Store it may open | Why no Aurora / Dynamo login |
|---|---|---|
| `1sb-integration-service` | HTTP to `bank-persistence-service` only | Standing constraint: no Flyway, no JPA, no datasource (`DR-OWN-05`) |
| `audit-compliance-service` | HTTP `POST /internal/v1/audit-events`; later reads via the same service | No second audit database (`DR-OWN-04`) |
| `workforce-access-bff` | Valkey session keyspace (`bff:`) | Token-hiding edge. Bank apps never open a database (`DR-OWN-06`) |
| `identity-provider-adapter-service` | Keycloak admin API | IdP isolation; PDP SoR is `identity` |
| `integration-hub-service` | In-process / `administration` **API** for routing policy | No business data. Must not become a silent Dynamo or shared-DB owner |
| `notification-service` | SMS/email/OTP gateway; audit via outbox → audit ingest | R0 is delivery, not an SoR |
| `reporting-mis-service` | Isolated **reader** (replica or extract). Never `app_lead_*` / `app_payment` | `CR-013` C-ISO-1 |
| `direct-insurer-adapter-service` | Out of R0. When admitted: same pattern as 1SB — HTTP to persistence, S3 for raw bytes | Must not skip the Hub |
| NIP-APP / Flutter (not in this repo) | BFF HTTPS only | `DOC-006` / `DR-OWN-06` |
| Customer device | Payment gateway only | Control C4 |

### 4.3 Cache users (not database users)

`ADR-011` / `FF-24`: each deployable that uses Valkey gets its **own ACL user** and key prefix. A BFF must not read `catalogue:*`. Catalogue must not read `bff:session:*`. Cross-prefix read is a failing S09 test.

---

## 5. How a fact moves across the e2e sale

One RM, one ETB customer, one Life policy (Term or Savings/ULIP). Sequential, not a write fan-out.

```text
lead-service        [Aurora lead_lms]               create / assign / convert
        │ lead_id
customer-service    [Aurora customer]               CIF lookup + journey snapshot
        │ customer_id
journey-orch        [Aurora journey]                stage + refs only
        │ journey_id
consent-service     [Aurora consent]                OTP evidence (immutable columns)
suitability-service [Aurora suitability]            answers_enc + mapping (Savings/ULIP)
catalogue-service   [Aurora catalogue] + Valkey     product / eligibility
quotation-service   [Dynamo working → Aurora quotation after RECONCILED]  quote + selected offer
        │  via Integration Hub HTTP
1sb-integration     [no DB] ──HTTP──► bank-persistence [Aurora job + raw_payload + S3]
proposal-service    [Dynamo working → Aurora proposal after RECONCILED]
payment-service     [Aurora payment]                customer device → PG
policy-issuance     [Aurora policy] + S3 PDF
audit-compliance    [no DB] ──HTTP──► bank-persistence.audit_event
reporting-mis       [Athena/S3 — later]             replica/extract, not the writer
```

Every hop that must be atomic **inside one context** is one Aurora transaction (business row + idempotency + outbox). Across contexts: eventual, via outbox + MSK. No 2PC. Payment `RECONCILED` is the issuance gate, not a database restore (`DR-REC-02`).

---

## 6. Roadmap

This is a **technical sequence**, not a calendar. Each row is a gate-shaped increment. Dynamo is a **later revisit**, not a parallel R0 build.

### Phase A — Design close (now · S08)

| Item | Owner | Done when |
|---|---|---|
| This pack + `DB-DEC-0003` | Aarti | Service × store × role matrix is the cited R0 apply list |
| Human Aarti on `S07-G5` / `DB-DEC-0001` + this overlay | Human Aarti | Signature exists (agent does not invent it) |
| Mahesh joint review: ownership + "no Dynamo SoR at R0" | Human Mahesh | Joint note; diagrams in `05` / `02` marked superseded for R0 or amended by Mahesh |
| Deepali: per-schema secret + no standing human write | Deepali | Credential lifecycle accepted |

**Do not:** create Dynamo tables other than the two `DB-DEC-0005` tables, IAM roles for any other service, or a second cluster.

### Phase B — Platform apply (S09 · parked)

| Item | Owner | Done when |
|---|---|---|
| IaC: one Aurora cluster, schemas, `app_*` / `migrator_*` / `job_retention` / `ro_breakglass` | Shivanshi + Aarti | Roles exist; secrets in the bank secret store; FF-09 scan green |
| Flyway apply for **already-shipping** stores: `identity`, `bank_persistence` (+ `14-audit_event_delta.sql`) | Amit | Expand-only; existing V1 histories untouched |
| Connection-pool cap per service | Shivanshi | One context cannot exhaust the cluster (`ADR-008` mitigation) |
| Timed restore drill | Shivanshi consumes Aarti's RPO/RTO | `S09-E06-S04` / `S09-VT-07` measured |
| Valkey ACL users + prefixes | Shivanshi | `FF-24` negative cross-prefix test |
| **Only the two `DB-DEC-0005` Dynamo tables** in the S09 inventory (PITR, CMK, TTL, VPC endpoint policy) | Shivanshi | Inventory diff is the evidence |

New business schemas are **not** applied from a shared admin session (`DR-MIG-04`). Each owning service applies its own history when that service is built.

### Phase C — Configuration then W1–W3 services (W0b → S11)

Wave order from [`03-solution-architecture-r0.md` §3](../ws3-platform/03-solution-architecture-r0.md):

| Wave | Service | Schema applied by that service's Flyway |
|---|---|---|
| W0b | `administration-config-service` | `administration` |
| W1 | `lead-service` | `lead_lms` |
| W1 | `journey-orchestration-service` | `journey` |
| W1 | `customer-service` | `customer` |
| W1 | `product-catalogue-service` | `catalogue` |
| W1 | `integration-hub-service` | none — resolve routing from Administration API |
| W2 | `consent-service` | `consent` |
| W2 | `suitability-service` | `suitability` |
| W2 | `quotation-service` | `quotation` |
| W3 | `proposal-service` | `proposal` |
| W3 | `payment-service` | `payment` |
| W3 | `policy-issuance-service` | `policy` |
| W3 | `audit-compliance-service` | none — ingest via `bank-persistence` |

`1sb-integration-service` stays on HTTP to `bank-persistence-service` through every wave.

### Phase D — W4 edge and MIS

| Item | Store | Rule |
|---|---|---|
| NIP BFF (already in repo) | Valkey session only | No Aurora |
| `notification-service` | Gateway + audit event | No Dynamo delivery table at R0 |
| `reporting-mis-service` | Isolated reader / S3 extract | Never granted `app_*` writer roles |

DATA-002 W4 (MIS reader path, outbox on remaining schemas) stays on DATA-002.

### Phase E — First physical split (not R0)

Trigger from `ADR-008`: a context whose load, connections or locks **demonstrably** degrades another; an RTO/RPO one cluster cannot meet; a security/regulatory demand for physical isolation; or the Health cell arriving.

Split axis: **LOB-cell / shared-platform**, not one cluster per service. Shared stay (party, consent, catalogue, payment, policy portfolio, audit index). Cell-owned journey/quote/proposal schemas become extractable (`life_*`, later `health_*`).

### Phase F — Dynamo revisit (parked · evidence-gated)

Unpark **only** when all of the following exist:

1. A named table and access pattern (partition key, item size, measured QPS, p95).
2. Evidence that Aurora on that pattern is the **actual** bottleneck — not provider latency and not the payment device path ([`01` §7](./01-physical-design.md#7-load-bottleneck-safe-range)).
3. A written SoT statement: what moves, what remains the reconstructable record, how PITR/WORM still hold.
4. Joint Aarti + Mahesh decision; Deepali IAM review; Shailja if evidence class moves.
5. Explicit "not a second audit database" if the candidate is audit.

Until then, any story that "adds Dynamo for Journey/Quote/Audit" is a **duplicate of the rejected alternative** in `DB-DEC-0001` and is not re-admitted as new scope.

---

## 7. What this pack will not do

- Apply Flyway or create AWS Dynamo/Aurora resources.
- Overturn `ADR-008`, `ADR-011`, `ADR-012`, `ADR-013` or `DB-DEC-0001`.
- Give `1sb-integration-service` a datasource.
- Put card data, sessions, or idempotency in the wrong store.
- Edit `CURRENT-STATE.yaml` stage fields.
- Mark `S07-G5` `PASSED`.
- Amend Mahesh's `05-data-architecture.md` diagrams (his lane; this pack names the R0 overlay).

---

## 8. Load, bottleneck, safe range

R0 load is one assisted Life sale at a time ([`BOOT.md`](../../context/BOOT.md) WS-3 objective). Write amplification is sequential (~tens of audit rows per sale). The **actual** R0 bottleneck is provider latency and the customer-device payment path, not Aurora and not the absence of Dynamo.

**Next limit:** connection pools (`maximum-pool-size` × service count). Cap per `app_*` role. Do not add pods to "fix" a database wait (`DR-REC-01`).

**Recovery:** Multi-AZ failover. If the cluster is gone, fail closed. Do not serve stale suitability or certification from Valkey past TTL.
