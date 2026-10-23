# 01 — Physical design: topology, tables, relationships, indexes, routines

**Companion DDL:** [`schemas/`](./schemas/)
**Rules:** [`00-design-rules.md`](./00-design-rules.md)
**Logical source:** [`ws3-platform/02-information-model.md`](../ws3-platform/02-information-model.md)
**States / invariants:** [`ws3-platform/01-domain-model-and-invariants.md`](../ws3-platform/01-domain-model-and-invariants.md)

---

## 1. Topology

```text
                         ap-south-1
            ┌─────────────────────────────────────┐
            │  Aurora PostgreSQL (one cluster)    │
            │  PITR on · Multi-AZ · KMS at rest   │
            │                                     │
            │  identity          customer         │
            │  bank_persistence  lead_lms         │
            │  consent           suitability      │
            │  catalogue         quotation        │
            │  proposal          payment          │
            │  policy            journey          │
            │  administration                     │
            └─────────────────────────────────────┘
                 ▲ one credential per schema
                 │ no cross-schema GRANT
    owning service (Flyway + repository)
```

| Store | Engine | Why this engine at R0 |
|---|---|---|
| Every transactional SoR in the table above | Aurora PostgreSQL | Relational integrity, state CHECKs, unique constraints, same-transaction idempotency, existing two Flyway histories |
| Encrypted raw payloads / policy PDF bytes / audit cold archive | S3 (Object Lock on evidence) | Bytes do not belong in the RDBMS |
| Workforce sessions | `ADR-011` cache — **not** this cluster | TTL key-value; not a system of record |
| Idempotency | Table in the **owning** schema | INV-IDM-01 / `ADR-011` refusal |
| Keycloak | Vendor database | Out of this pack; never the business PDP SoR |
| Reporting | Not provisioned in R0 | Must not share this cluster |

`architecture-review/05` proposed DynamoDB for Journey, Quotation and Audit. **Journey and Audit stay rejected for R0; pre-payment quote/proposal working state is now DynamoDB (`DB-DEC-0005`, 2026-10-21)** — the original R0 ruling read: **Rejected for R0**
in [`DB-DEC-0001`](./DB-DEC-0001-r0-physical-model.md): no volume evidence, would fork the
already-shipping `audit_event` / job store, and Journey is a thin stage+refs row (INV-JRN-02),
not a document store problem. Revisit when a measured access pattern says otherwise.

Service × store × credential matrix and the Dynamo empty-set: [`04-store-segregation-and-roadmap.md`](./04-store-segregation-and-roadmap.md) · [`DB-DEC-0003`](./DB-DEC-0003-store-segregation.md).

Full e2e ER, columns and relationship map: [`05-e2e-er-and-field-map.md`](./05-e2e-er-and-field-map.md).

---

## 2. Relationships (logical, not cross-schema FK)

```text
lead_lms.lead.lead_id
    ├── lead_assignment / lead_meeting / lead_follow_up
    └── journey.lead_id
            ├── customer.customer_snapshot.journey_id
            ├── consent.lead_id / journey_id
            ├── suitability.lead_id / journey_id
            │       ├── suitability_answer_set (⚑ ciphertext)
            │       └── suitability_mapping_run (INSERT-only evidence)
            ├── DynamoDB quotation-working  QUOTE#id/META.journeyId (until RECONCILED, DB-DEC-0005)
            │       └── quotation.quote.journey_id ── offer.quote_id   (materialize)
            ├── DynamoDB proposal-working   PROPOSAL#id/META.journeyId
            │       └── proposal.proposal.quote_id / offer_id / journey_id
            │               └── payment.payment.proposal_id
            │                       └── policy.policy.payment_id / proposal_id
            └── bank_persistence.integration_job.journey_id   (adapter correlation)

identity.business_user.id  ──referenced as actor ids (varchar), never joined
catalogue.product          ──referenced as product_code / insurer_code
administration.config      ──referenced as config version on originating rows
bank_persistence.audit_event.journey_id / resource_id   (append-only evidence)
```

Lead + Suitability e2e ER and column sheets: [`03-lead-suitability-e2e.md`](./03-lead-suitability-e2e.md).
Journey orchestrator + JSON-until-payment: [`06-journey-orchestrator-and-working-payload.md`](./06-journey-orchestrator-and-working-payload.md).

**Physical FK** exists only inside one schema (offer→quote, attempt→payment, assignment/meeting/follow-up→lead, answer-set/mapping-run→suitability).
A `journey_id CHAR(26)` on another schema is a **logical** reference. The owning service validates
it exists via API before insert.

Partner visibility (`AC-4` / `AC-5`) is a **predicate** on the owning table
(`insurer_id IS NOT NULL AND partner_visible_from IS NOT NULL AND need_analysis_state = 'COMPLETED'`
on lead_lms.lead; journey materialises a copy for query). It is not a grant to the partner schema.

---

## 3. Table catalogue

Column-level types for new tables follow the logical sheets. ⚑ fields are `*_enc BYTEA` +
`encryption_key_id`. Existing tables are cited, not re-specified.

### 3.1 `identity` — already implemented

Source:
[`V1__identity_authorization_schema.sql`](../../../services/identity-authorization-service/src/main/resources/db/migration/V1__identity_authorization_schema.sql),
[`V2__seed_role_permission_catalog.sql`](../../../services/identity-authorization-service/src/main/resources/db/migration/V2__seed_role_permission_catalog.sql).

| Table | Kind | Keys / uniqueness |
|---|---|---|
| `insurer` | reference | `code` PK |
| `branch` | reference | `code` PK |
| `business_user` | transactional | `id` PK; `username` unique; `(provider, provider_subject)` unique |
| `user_branch_assignment` | temporal | `(user_id, branch_code, valid_from)` unique |
| `organization_relationship` | temporal | `id` PK; `manager <> subordinate` |
| `certification` | temporal | `id` PK; queried by `user_id` + validity (INV-ACT-01) |
| `role` / `permission` / `role_permission` | reference | PK / composite PK |
| `user_role` | temporal | `(user_id, role_code, valid_from)` unique |
| `entitlement` | temporal | GRANT/DENY + scope CHECK |
| `bulk_import` / `bulk_import_row` | maker-checker | `checksum` unique |
| `approval_request` | maker-checker | status CHECK |
| `outbox_event` | reliability | unpublished partial index |

Recommended additions at S09 (not applied): index `certification (user_id, status, valid_until)`
and `entitlement (user_id, valid_until)` for the PDP hot path. See [`01-identity.sql`](./schemas/01-identity.sql).

### 3.2 `bank_persistence` — already implemented

Source: [`V1__init_schema.sql`](../../../services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql).

| Table | Kind | Notes |
|---|---|---|
| `integration_job` | transactional | `idempotency_key` unique; `version` optimistic lock |
| `integration_job_offer` | child | FK → job; maps 1:1 to the logical Offer sheet |
| `job_poll_attempt` | operational | FK → job |
| `raw_payload` | immutable evidence | `payload_enc` BYTEA; `retain_until`; 7-year |
| `audit_event` | immutable evidence | INSERT-only intent; **column gaps** in §3.2.1 |
| `payment_session` | transactional | adapter payment-link session; **not** the Payment SoR |

`payment_session` is the 1SB adapter's link-session record. The platform Payment aggregate lives
in schema `payment`. They share `application_number` / `job_id` as correlation, not a FK.

#### 3.2.1 Audit columns still required (`OPEN-I3`, `OPEN-I5`)

| Column | Purpose |
|---|---|
| `prior_state` / `new_state` | Reconstruction (information model §5) |
| `consent_ref` / `suitability_ref` | Regulator join without assuming the transactional row still exists |
| `event_schema_version` | Records outlive code |
| `sequence_no` | Gap detection per `journey_id` |
| `acting_capacity` | `SP_ACCOUNTABLE` \| `ASSIST_ONLY` (INV-ACT-04) |
| `actor_insurer_id` | Partner principal's insurer |
| `assisted_actor_id` | Accountable RM alongside an assist |
| `config_version_ref` | Rule version in force (INV-CFG-03) |

DDL: [`14-audit_event_delta.sql`](./schemas/14-audit_event_delta.sql). Apply as the next
`bank-persistence-service` Flyway version at S08/S09 — **not in this change**.

### 3.3 New R0 schemas (design)

| Schema | Tables | Immutable? |
|---|---|---|
| `customer` | `customer`, `customer_snapshot`, `idempotency_record`, `outbox_event` | `customer_id` ULID, CIF encrypted + HMAC UNIQUE (`D-023`); snapshot write-once except one-time `journey_id` bind; `fresh_until` freshness |
| `lead_lms` | `lead`, `lead_assignment`, `lead_meeting`, `lead_follow_up`, `lead_exception_evaluation`, `lead_exception_decision` (D2), `idempotency_record`, `outbox_event` | First write of `accountable_sp_id` then immutable; assignment/meeting/follow-up append-only; `ARCHIVED` is a state not a delete. Column sheet: [`03-lead-suitability-e2e.md`](./03-lead-suitability-e2e.md) |
| `consent` | `consent_record`, `idempotency_record`, `outbox_event` | Append-only: state change = new row via `supersedes_consent_id` (`D-024`); `CNS-QSH` purpose (D5) |
| `suitability` | `suitability`, `suitability_answer_set`, `suitability_mapping_run`, `evaluation`, `evaluation_outcome`, `evaluation_status_event`, `caution_ack`, `suitability_usage`, `idempotency_record`, `outbox_event` | Header mutable until `LOCKED`; answer sets and the D4 evaluation record INSERT-only; mapping `run_state` may move CURRENT→INVALID. Every class, Term included, has a row (`D-022`) |
| `catalogue` | `insurer`, `product`, `eligibility_band`, `outbox_event` | Effective-dated; no in-place replace of an active version |
| `quotation` | `quote`, `offer`, `idempotency_record` | Pre-payment state is the DynamoDB item (`DB-DEC-0005`). Structured `quote` (`CONVERTED`, `selected_offer_id`) + every `offer` incl. `INVALID` INSERT after Payment RECONCILED (`D-025`); `idempotency_record` dedupes materialize events |
| `proposal` | `proposal`, `proposal_answer`, `proposal_status_history`, `uw_requirement`, `uw_document_ref`, `idempotency_record` | Pre-payment state in DynamoDB (`DB-DEC-0005`); at materialize every question/answer → `proposal_answer` (encrypted), later changes → append-only `proposal_status_history` (`D-025`) |
| `payment` | `payment`, `payment_attempt`, `refund`, `outbox_event`, `idempotency_record` | Always structured (money path); carries `quote_id` + `proposal_id` (`D-025`). On the journey as `paymentId` + stages `PAYMENT_*`; independent schema — Journey never writes it (`DATA-005` §6) |
| `policy` | `policy`, `policy_status_history`, `policy_document_ref`, `idempotency_record`, `processed_event`, `outbox_event` | `policy_number` unique+immutable once issued (INV-POL-02); issuance status append-only; insurer-decision TAT columns (`D-025`) |
| `journey` | `journey`, `journey_ref`, `journey_transition`, `outbox_event`, `idempotency_record` | Stage + refs only (`INV-JRN-02`). `retain_until` / `archived_at`: unconverted (no `paymentId`) purge after operational window; paid path is `RET-7Y` |
| `administration` | `configuration_record`, `rule_set_version`, `idempotency_record`, `outbox_event` | Versioned INSERT-only (INV-CFG-02); published rule sets immutable, maker ≠ checker (ADR-024) |
| `audit` — **PROPOSED** | `audit_event`, `archive_export` | Only if ADR-026 is accepted: INSERT-only, hash chain, UNIQUE `event_id`, UNIQUE `(journey_id, sequence_no)` ([`15-audit.sql`](./schemas/15-audit.sql)) |

Idempotency is a small table **inside** each mutating schema (`key`, `request_hash`, `response_ref`,
`created_at`, `expires_at`) so INV-IDM-01 is mechanically true.

---

## 4. Index design

| Schema | Index | Why |
|---|---|---|
| identity | `ux_business_user_provider_subject` | Existing IdP subject |
| identity | `ix_certification_user_validity` **(add)** | INV-ACT-01 at action time |
| identity | `ix_outbox_unpublished` | Existing publisher |
| bank_persistence | `idx_job_journey`, `idx_job_status`, `idx_job_application` | Existing |
| bank_persistence | `idx_offer_job`, `idx_poll_job`, `idx_payload_job` | Existing |
| bank_persistence | `idx_audit_resource`, `idx_audit_actor`, `idx_audit_journey` | Existing |
| bank_persistence | `ux_audit_journey_sequence` **(add with delta)** | Gap detection |
| customer | `ux_customer_cif_hash` | ETB lookup without indexing CIF |
| customer | `ix_snapshot_journey` | Frozen profile by journey |
| lead_lms | `ix_lead_customer_lob`, `ix_lead_state_expires`, `ix_lead_insurer_visible`, `ix_lead_inbox_sp`, `ix_lead_inbox_ipr`, `ux_lead_dedupe_open` | Book, ageing, IPR predicate, working inbox, creator+customer+productClass dedupe |
| consent | `ix_consent_customer_state`, `ix_consent_journey` | INV-PRP-01 lookup |
| suitability | `ix_suit_customer_lob_state`, `ux_suit_lead_current`, `ux_suit_map_current` | INV-QUO-01 lookup; one current assessment per lead; one CURRENT mapping |
| catalogue | `ix_product_effective`, `ix_elig_product` | Read path + cache fill |
| quotation | `ix_qwork_journey`, `ix_qwork_state`, `ux_qwork_open_journey`, `ix_quote_journey`, `ix_offer_quote_state` | Working envelope by journey/state; structured selection / conversion |
| proposal | `ix_pwork_journey`, `ux_pwork_open_journey`, `ux_pwork_application`, `ix_proposal_journey`, `ux_proposal_application` | Working envelope by journey; structured status + insurer number |
| payment | `ux_payment_active_per_proposal` (partial) | INV-PAY-02 |
| payment | `ix_payment_pg_txn`, `ix_payment_journey`, `ix_pay_outbox_unpublished` | Reconciliation; saga lookup; ADR-012 |
| policy | `ux_policy_insurer_number` | INV-POL-02 |
| journey | `ix_journey_lead`, `ix_journey_stage_due`, `ix_journey_archive_due`, `ix_jrn_transition`, `ix_jrn_outbox_unpublished` | Saga + inactivity (INV-JRN-03); unconverted archive; reconstruction; ADR-012 |
| administration | `ux_config_version`, `ix_config_resolve` | Seed idempotence + effective-dated get |
| all mutating | `ux_idempotency_key` | INV-IDM-01 |

No index on ⚑ plaintext — there is no ⚑ plaintext column.

---

## 5. OPEN-I6 — LOB partitioning

**Decision for R0:** `lob` is a `NOT NULL` column + `CHECK` + **leading prefix** on list indexes
that are already LOB-scoped. It is **not** a PostgreSQL declarative partition key.

**Why not partition now**

- R0 traffic is `LIFE` / `TERM` only. A `PARTITION BY LIST (lob)` on a 100% `LIFE` table never
  prunes and complicates unique constraints (`policy_number` would need `lob` in every unique
  index).
- The **first physical split** is already decided at cluster level: LOB-cell / shared-platform
  seam (`ADR-008`). That is a cutover of schemas, not a `ATTACH PARTITION`.
- Unique constraints that must stay global (`policy (insurer_code, policy_number)`) fight list
  partitions unless the partition key is in the unique key.

**Revisit trigger:** a second LOB is admitted **or** any table shows measured sequential-scan
or bloat attributable to mixed-LOB access. Then consider `PARTITION BY LIST (lob)` **per store**,
not as a platform default. Payment and audit may stay unpartitioned longer because
reconstruction and reconciliation are global.

---

## 6. Required routines

CRUD stored procedures are **refused** (`DR-SP-01`).

| Routine | Type | When applied | Purpose |
|---|---|---|---|
| `fn_prevent_update_delete` | trigger fn | With each immutable table | Reject `UPDATE`/`DELETE` |
| `consent.fn_prevent_update_delete` | trigger fn | `consent.consent_record` | Whole row immutable — withdrawal is a new row (`D-024`; replaces `fn_protect_consent_evidence`) |
| `administration.fn_protect_rule_set_version` | trigger fn | `administration.rule_set_version` | Published rule-set document immutable (ADR-024 §5.1) |
| `audit.fn_chain_hash` | trigger fn | `audit.audit_event` — **PROPOSED** (ADR-026) | Hash chain `row_hash = sha256(prev_hash ‖ row)` |
| `fn_accountable_sp_immutable` | trigger fn | `lead_lms.lead` (and copies) | INV-ACT-03 — first NULL→value allowed |
| `fn_lead_origination_immutable` | trigger fn | `lead_lms.lead` | `lead_id` / `lob` / `product_class` / creator / `source` frozen |
| `fn_protect_answer_set` | trigger fn | Suitability | Answer ciphertext INSERT-only |
| `fn_protect_mapping_run` | trigger fn | Suitability | Mapping evidence frozen; `run_state` may change |
| `fn_next_audit_sequence` | function | With audit delta | Allocate `sequence_no` per `journey_id` |
| `fn_ipr_visible` | function | `lead_lms` / journey | Documents the `AC-4` SQL predicate |
| ~~`fn_freeze_working_payload`~~ | removed | — | Replaced by DynamoDB `ConditionExpression attribute_not_exists(materializedAt)` (`DB-DEC-0005`) |
| `sp_retention_sweep` | procedure | **S09** | Selects rows past `retain_until` for the owning job; does not cross schemas |
| `sp_purge_operational` | procedure | **S09** | Deletes `RET-OPERATIONAL` / expired idempotency keys / aged working envelopes |

Bodies: [`90-routines.sql`](./schemas/90-routines.sql).

---

## 7. Load, bottleneck, safe range

R0 business load: one RM, one ETB customer, one Life policy (Term or Savings/ULIP per CR-015),
one Group A insurer ([`BOOT.md` WS-3 objective](../../context/BOOT.md)).

| Amplification | Path |
|---|---|
| ~1 lead → 1 journey → 1 suitability → 1 consent → 1 quote → N offers (N = in-scope insurers) → 1 proposal → 1 payment → 1 policy | Sequential, not fan-out at write |
| Poll attempts | Adapter `job_poll_attempt` — already bounded by poll budget |
| Audit | One event per aggregate transition (INV-AUD-02) — tens of rows per sale, not millions |

**Actual bottleneck at R0** is not this cluster. It is provider latency and the payment device
path. Aurora on the smallest Multi-AZ class is inside a wide safe range.

**Next downstream limit:** connection pool per service (default too high × many services starves
Aurora). Cap `maximum-pool-size` per service; do not "fix" latency by adding pods
(`DR-REC-01`).

**Recovery behaviour:** Multi-AZ failover (seconds to low minutes). If the cluster is gone,
fail closed. Do not serve stale suitability or certification from cache past TTL
([`03` degraded inventory](../ws3-platform/03-solution-architecture-r0.md)).
