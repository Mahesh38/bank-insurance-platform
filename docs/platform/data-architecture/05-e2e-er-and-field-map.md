# 05 — E2E ER diagrams, fields and relationship map

**Workstream:** WS-3 · **Work item:** [`DATA-004`](./DATA-004.work-item.yaml) · origin [`SUG-20261012-erd`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261012-erd--e2e-er-diagrams-and-field-map)
**Companion:** store / credential / roadmap in [`04-store-segregation-and-roadmap.md`](./04-store-segregation-and-roadmap.md)
**DDL source:** [`schemas/`](./schemas/) — this document is a projection of that DDL, not a second SoT
**Owner:** Aarti — Principal Insurance Data & Database Architect
**Status:** `AI-DRAFTED` · design only · not Flyway

Freshness: `04-STAGE_GATES.md` is past its 14-day limit (WARN). No stage field is edited.

Every transactional table is on **Aurora PostgreSQL**. **DynamoDB has no entities at R0** — there is no Dynamo ER. Valkey, S3, Keycloak and MSK are not relational schemas.

Lead physical name on this branch is schema `opportunity` / table `opportunity`. A parallel pack names it `lead_lms.lead`. Identifiers stay `lead_id`.

---

## 1. How to read the diagrams

| Mark | Meaning |
|---|---|
| Solid `\|\|--o{` | **Physical FK** — same schema only (`DR-OWN-08`) |
| Dashed / `LOGICAL` | Cross-schema id. Owning service validates via API. No SQL `FOREIGN KEY` |
| `PK` | Primary key. New aggregates: ULID `CHAR(26)`. Identity / bank_persistence keep shipping types |
| `⚑` / `*_enc` | Encrypted payload. Never plaintext, never an index key (`DR-PII-01`) |
| `lob` | `LIFE` \| `HEALTH` \| `GENERAL` on journey-scoped and config rows (`INV-LOB-01`) |
| `version` | Optimistic lock on mutable aggregates |

Repeated satellite (not redrawn on every schema ER):

```text
<schema>.idempotency_record (PK idempotency_key, request_hash, response_ref, created_at, expires_at)
```

Present on: `opportunity`, `consent`, `suitability`, `quotation`, `proposal`, `payment`, `policy`, `journey`. Identity uses `identity.outbox_event` instead. `bank_persistence` uses `integration_job.idempotency_key`.

---

## 2. E2E sale path — logical ER

Solid lines are physical FKs inside one schema. All arrows that cross a box are **logical**.

```mermaid
erDiagram
    IDENTITY_business_user ||--o{ OPPORTUNITY_opportunity : "LOGICAL accountable_sp_id / actor ids"
    CUSTOMER_customer ||--o{ CUSTOMER_customer_snapshot : "has"
    CUSTOMER_customer ||--o{ OPPORTUNITY_opportunity : "LOGICAL customer_id"
    OPPORTUNITY_opportunity ||--o{ OPPORTUNITY_assignment : "has"
    OPPORTUNITY_opportunity ||--o{ OPPORTUNITY_follow_up : "has"
    OPPORTUNITY_opportunity ||--o{ JOURNEY_journey : "LOGICAL lead_id"
    CUSTOMER_customer_snapshot ||--o{ JOURNEY_journey : "LOGICAL party_snapshot_ref"
    JOURNEY_journey ||--o{ JOURNEY_journey_ref : "has"
    OPPORTUNITY_opportunity ||--o{ CONSENT_consent : "LOGICAL lead_id"
    OPPORTUNITY_opportunity ||--o{ SUITABILITY_suitability : "LOGICAL lead_id"
    SUITABILITY_suitability ||--|| SUITABILITY_answer_enc : "has"
    CATALOGUE_insurer ||--o{ CATALOGUE_product : "has"
    CATALOGUE_product ||--o{ CATALOGUE_eligibility_band : "has"
    SUITABILITY_suitability ||--o{ QUOTATION_quote : "LOGICAL suitability_id"
    CONSENT_consent ||--o{ QUOTATION_quote : "LOGICAL consent_id"
    QUOTATION_quote ||--o{ QUOTATION_offer : "has"
    QUOTATION_quote ||--o{ PROPOSAL_proposal : "LOGICAL quote_id"
    QUOTATION_offer ||--o{ PROPOSAL_proposal : "LOGICAL offer_id"
    CONSENT_consent ||--o{ PROPOSAL_proposal : "LOGICAL consent_id"
    PROPOSAL_proposal ||--o{ PROPOSAL_uw_requirement : "has"
    PROPOSAL_proposal ||--o{ PROPOSAL_uw_document_ref : "has"
    PROPOSAL_proposal ||--o{ PAYMENT_payment : "LOGICAL proposal_id"
    PAYMENT_payment ||--o{ PAYMENT_attempt : "has"
    PAYMENT_payment ||--o{ PAYMENT_refund : "has"
    PAYMENT_payment ||--o{ POLICY_policy : "LOGICAL payment_id"
    PROPOSAL_proposal ||--o{ POLICY_policy : "LOGICAL proposal_id"
    POLICY_policy ||--o{ POLICY_document_ref : "has"
    JOURNEY_journey ||--o{ BANK_integration_job : "LOGICAL journey_id"
    BANK_integration_job ||--o{ BANK_job_offer : "has"
    BANK_integration_job ||--o{ BANK_poll_attempt : "has"
    BANK_integration_job ||--o{ BANK_payment_session : "has"
    BANK_integration_job ||--o{ BANK_raw_payload : "LOGICAL job_id"
    JOURNEY_journey ||--o{ BANK_audit_event : "LOGICAL journey_id"
    ADMINISTRATION_config ||--o{ JOURNEY_journey : "LOGICAL config_version_ref"
```

---

## 3. Relationship map (complete)

### 3.1 Physical FK — same schema only

| From | To | Card. | Rule |
|---|---|---|---|
| `identity.business_user.insurer_code` | `identity.insurer.code` | N:1 | Partner user scoped to insurer |
| `identity.user_branch_assignment.user_id` | `identity.business_user.id` | N:1 | Temporal branch seat |
| `identity.user_branch_assignment.branch_code` | `identity.branch.code` | N:1 | |
| `identity.organization_relationship.manager_user_id` | `identity.business_user.id` | N:1 | `manager <> subordinate` |
| `identity.organization_relationship.subordinate_user_id` | `identity.business_user.id` | N:1 | |
| `identity.certification.user_id` | `identity.business_user.id` | N:1 | INV-ACT-01 |
| `identity.user_role.user_id` | `identity.business_user.id` | N:1 | |
| `identity.user_role.role_code` | `identity.role.code` | N:1 | |
| `identity.role_permission.role_code` | `identity.role.code` | N:1 | |
| `identity.role_permission.permission_code` | `identity.permission.code` | N:1 | |
| `identity.entitlement.user_id` | `identity.business_user.id` | N:1 | GRANT/DENY overlay |
| `identity.bulk_import.insurer_code` | `identity.insurer.code` | N:1 | nullable |
| `identity.bulk_import_row.import_id` | `identity.bulk_import.id` | N:1 | |
| `bank_persistence.integration_job_offer.job_id` | `integration_job.job_id` | N:1 | Adapter offers |
| `bank_persistence.job_poll_attempt.job_id` | `integration_job.job_id` | N:1 | |
| `bank_persistence.payment_session.job_id` | `integration_job.job_id` | N:1 | nullable; **not** Payment SoR |
| `customer.customer_snapshot.customer_id` | `customer.customer.customer_id` | N:1 | Write-once freeze |
| `opportunity.opportunity_assignment.lead_id` | `opportunity.opportunity.lead_id` | N:1 | |
| `opportunity.opportunity_follow_up.lead_id` | `opportunity.opportunity.lead_id` | N:1 | ⚑ note |
| `suitability.suitability_answer_enc.suitability_id` | `suitability.suitability.suitability_id` | 1:1 | ⚑ answers |
| `catalogue.product.insurer_id` | `catalogue.insurer.insurer_id` | N:1 | |
| `catalogue.eligibility_band.product_id` | `catalogue.product.product_id` | N:1 | |
| `quotation.offer.quote_id` | `quotation.quote.quote_id` | N:1 | one SELECTED (`INV-QUO-05`) |
| `proposal.uw_requirement.proposal_id` | `proposal.proposal.proposal_id` | N:1 | |
| `proposal.uw_document_ref.proposal_id` | `proposal.proposal.proposal_id` | N:1 | S3 ref |
| `payment.payment_attempt.payment_id` | `payment.payment.payment_id` | N:1 | append-only |
| `payment.refund.payment_id` | `payment.payment.payment_id` | N:1 | |
| `policy.policy_document_ref.policy_id` | `policy.policy.policy_id` | N:1 | S3 PDF |
| `journey.journey_ref.journey_id` | `journey.journey.journey_id` | N:1 | PK `(journey_id, ref_type)` |

`raw_payload.job_id` is **not** a FK (immutable evidence must survive a job-row change). Correlate by id.

### 3.2 Logical references — no SQL FK

| From | To | Why logical |
|---|---|---|
| `opportunity.customer_id` | `customer.customer` | Cross-schema |
| `opportunity.converted_journey_id` | `journey.journey` | Unique when set |
| `opportunity.accountable_sp_id` / actor ids | `identity.business_user.id` as varchar | Identity uses UUID; Lead stores opaque actor id |
| `customer_snapshot.journey_id` / `lead_id` | `journey` / `opportunity` | Frozen copy keyed by journey |
| `consent.{lead,journey,customer}_id` | owning aggregates | Evidence must outlive a deleted transactional join |
| `suitability.{lead,journey,customer}_id` | owning aggregates | Same |
| `quotation.{lead,journey,customer,suitability,consent}_id` | owning aggregates | Quote gate (`INV-QUO-01`) |
| `quotation.provider_job_id` | `bank_persistence.integration_job` | Adapter correlation |
| `proposal.{lead,journey,quote,offer,consent,customer}_id` | owning aggregates | |
| `payment.{lead,journey,proposal}_id` | owning aggregates | |
| `policy.{lead,journey,customer,proposal,payment}_id` | owning aggregates | Issuance gate is `payment.RECONCILED` |
| `journey.{lead,customer}_id` | owning aggregates | Stage + refs only (`INV-JRN-02`) |
| `journey.party_snapshot_ref` | `customer.customer_snapshot` | |
| `journey.journey_ref.ref_value` | id named by `ref_type` | suitability/consent/quote/offer/proposal/payment/policy/application/policyNumber/provider |
| `integration_job.journey_id` | `journey` | Adapter correlation |
| `audit_event.{journey_id,consent_ref,suitability_ref,resource_id}` | owning aggregates | Reconstruction without assuming the row still exists |
| `*.config_version_ref` | `administration.configuration_record` | Version in force (`INV-CFG-03`) |
| `quotation.offer.{insurer_code,product_code}` / `policy.{insurer_code,product_code}` | `catalogue` | Catalogue is effective-dated; offer/policy freeze codes |

Actor ids (`rm_id`, `created_by`, `override_actor_id`, …) are **never** joined to `identity` from another schema.

---

## 4. `identity` — already shipping

DDL: [`01-identity.sql`](./schemas/01-identity.sql). Runtime: identity-authorization Flyway V1/V2.

```mermaid
erDiagram
    identity_insurer {
        varchar code PK
        varchar name
        boolean active
    }
    identity_branch {
        varchar code PK
        varchar name
        varchar region_code
        boolean active
    }
    identity_business_user {
        uuid id PK
        varchar provider
        varchar provider_subject
        varchar user_type
        varchar status
        varchar username UK
        varchar email
        varchar first_name
        varchar last_name
        varchar employee_id
        varchar insurer_code FK
        bigint policy_version
        timestamptz valid_from
        timestamptz valid_until
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    identity_user_branch_assignment {
        uuid id PK
        uuid user_id FK
        varchar branch_code FK
        varchar assignment_source
        timestamptz valid_from
        timestamptz valid_until
        timestamptz created_at
    }
    identity_organization_relationship {
        uuid id PK
        uuid manager_user_id FK
        uuid subordinate_user_id FK
        varchar relationship_type
        timestamptz valid_from
        timestamptz valid_until
    }
    identity_certification {
        uuid id PK
        uuid user_id FK
        varchar certification_type
        varchar certificate_number
        varchar source
        varchar status
        boolean mandatory_for_selling
        timestamptz valid_from
        timestamptz valid_until
        timestamptz created_at
    }
    identity_role {
        varchar code PK
        varchar name
        boolean privileged
    }
    identity_permission {
        varchar code PK
        varchar description
        boolean regulated
    }
    identity_user_role {
        uuid id PK
        uuid user_id FK
        varchar role_code FK
        timestamptz valid_from
        timestamptz valid_until
        varchar assigned_by
        timestamptz created_at
    }
    identity_role_permission {
        varchar role_code PK_FK
        varchar permission_code PK_FK
    }
    identity_entitlement {
        uuid id PK
        uuid user_id FK
        varchar effect
        varchar permission_code
        varchar scope_type
        varchar scope_value
        varchar reason
        timestamptz valid_from
        timestamptz valid_until
        varchar approved_by
        timestamptz created_at
    }
    identity_bulk_import {
        uuid id PK
        varchar insurer_code FK
        varchar status
        varchar object_key
        varchar checksum UK
        varchar maker_id
        varchar checker_id
        timestamptz submitted_at
        timestamptz decided_at
    }
    identity_bulk_import_row {
        uuid id PK
        uuid import_id FK
        int row_number
        varchar status
        varchar external_reference
        varchar error_code
        varchar error_detail
    }
    identity_approval_request {
        uuid id PK
        varchar request_type
        uuid target_id
        varchar status
        varchar maker_id
        varchar checker_id
        timestamptz requested_at
        timestamptz decided_at
    }
    identity_outbox_event {
        uuid id PK
        varchar aggregate_type
        uuid aggregate_id
        varchar event_type
        text payload
        timestamptz created_at
        timestamptz published_at
        int attempts
        varchar last_error
    }
    identity_insurer ||--o{ identity_business_user : "insurer_code"
    identity_business_user ||--o{ identity_user_branch_assignment : "user_id"
    identity_branch ||--o{ identity_user_branch_assignment : "branch_code"
    identity_business_user ||--o{ identity_organization_relationship : "manager"
    identity_business_user ||--o{ identity_organization_relationship : "subordinate"
    identity_business_user ||--o{ identity_certification : "user_id"
    identity_business_user ||--o{ identity_user_role : "user_id"
    identity_role ||--o{ identity_user_role : "role_code"
    identity_role ||--o{ identity_role_permission : "role_code"
    identity_permission ||--o{ identity_role_permission : "permission_code"
    identity_business_user ||--o{ identity_entitlement : "user_id"
    identity_insurer ||--o{ identity_bulk_import : "insurer_code"
    identity_bulk_import ||--o{ identity_bulk_import_row : "import_id"
```

| CHECK / uniqueness | Values |
|---|---|
| `user_type` | `BANK_EMPLOYEE` \| `INSURER_REPRESENTATIVE` |
| `business_user.status` | `PENDING` \| `ACTIVE` \| `SUSPENDED` \| `DISABLED` \| `EXPIRED` |
| `entitlement.effect` | `GRANT` \| `DENY` |
| `entitlement.scope_type` | `GLOBAL` \| `BRANCH` \| `INSURER` \| `RESOURCE` |
| `approval.status` | `PENDING` \| `APPROVED` \| `REJECTED` |
| Unique | `(provider, provider_subject)` · `(user_id, branch_code, valid_from)` · `(user_id, role_code, valid_from)` · `username` · `bulk_import.checksum` |

S09 add (not in V1): `ix_certification_user_validity (user_id, status, valid_until)`.

---

## 5. `bank_persistence` — already shipping

DDL: [`02-bank_persistence.sql`](./schemas/02-bank_persistence.sql) + delta [`14-audit_event_delta.sql`](./schemas/14-audit_event_delta.sql).

```mermaid
erDiagram
    bp_integration_job {
        varchar job_id PK
        varchar job_type
        varchar lob
        varchar status
        varchar failure_reason
        varchar journey_id "LOGICAL"
        varchar application_number
        varchar policy_number
        varchar external_req_id
        varchar external_provider
        varchar idempotency_key UK
        varchar result_blob_id
        timestamptz created_at
        timestamptz updated_at
        timestamptz completed_at
        varchar owned_by_instance
        bigint version
        varchar created_by_actor
    }
    bp_integration_job_offer {
        varchar offer_id PK
        varchar job_id FK
        varchar insurer_code
        varchar product_code
        varchar product_name
        numeric premium_amount
        varchar premium_frequency
        numeric sum_assured
        boolean out_of_bound
        varchar offer_status
        varchar error_summary
        varchar raw_offer_blob_id
        text funds_json
        timestamptz created_at
    }
    bp_job_poll_attempt {
        bigserial attempt_id PK
        varchar job_id FK
        smallint attempt_number
        timestamptz attempted_at
        smallint http_status
        boolean is_complete
        int duration_ms
        varchar error_message
    }
    bp_raw_payload {
        varchar payload_id PK
        varchar job_id "LOGICAL"
        varchar direction
        varchar operation
        varchar lob
        bytea payload_enc "flag"
        varchar encryption_key_id
        smallint http_status
        timestamptz created_at
        date retain_until
    }
    bp_audit_event {
        varchar event_id PK
        timestamptz event_time
        varchar action
        varchar actor_id
        varchar actor_type
        varchar resource_type
        varchar resource_id
        varchar outcome
        varchar lob
        varchar journey_id "LOGICAL"
        varchar distributor_id
        varchar agent_id
        text metadata
        varchar trace_id
        text prior_state "OPEN-I3"
        text new_state "OPEN-I3"
        varchar consent_ref "LOGICAL"
        varchar suitability_ref "LOGICAL"
        smallint event_schema_version
        int sequence_no
        varchar acting_capacity
        varchar actor_insurer_id
        varchar assisted_actor_id
        varchar config_version_ref "LOGICAL"
        timestamptz retain_until
    }
    bp_payment_session {
        varchar session_id PK
        varchar job_id FK
        varchar application_number
        varchar lob
        text payment_url
        text redirect_url
        varchar status
        varchar external_txn_id
        timestamptz created_at
        timestamptz expires_at
        timestamptz updated_at
        varchar created_by_actor
    }
    bp_integration_job ||--o{ bp_integration_job_offer : "job_id"
    bp_integration_job ||--o{ bp_job_poll_attempt : "job_id"
    bp_integration_job ||--o{ bp_payment_session : "job_id"
```

| Notes | |
|---|---|
| `raw_payload.direction` | `REQ` \| `RES` · INSERT-only |
| `audit_event` | INSERT-only. Delta columns are expand-only, not in V1 |
| `acting_capacity` | `SP_ACCOUNTABLE` \| `ASSIST_ONLY` |
| `ux_audit_journey_sequence` | `(journey_id, sequence_no)` when both set |
| `payment_session` | Adapter link session. Payment SoR is schema `payment` |

---

## 6. `customer`

```mermaid
erDiagram
    customer_customer {
        char customer_id PK
        bytea cif_lookup_hash UK "HMAC not CIF"
        bytea cif_enc "flag"
        bytea pii_enc "flag"
        varchar encryption_key_id
        timestamptz snapshot_taken_at
        varchar source_system
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    customer_customer_snapshot {
        char snapshot_id PK
        char customer_id FK
        char journey_id "LOGICAL UK"
        char lead_id "LOGICAL"
        varchar lob
        bytea pii_enc "flag"
        varchar encryption_key_id
        timestamptz snapshot_taken_at
        varchar source_system
        timestamptz created_at
    }
    customer_customer ||--o{ customer_customer_snapshot : "customer_id"
```

`source_system` = `CBS`. Snapshot `lob` ∈ `LIFE|HEALTH|GENERAL`. Living `customer` has **no** `lob` (DB-DEC-0001 obs. 2). `pii_enc` bundle: fullName, dob, gender, mobile, email, address, annualIncome, tobaccoUse, pan, aadhaarRef.

---

## 7. `opportunity` (Lead)

Spoken name **Lead**. Table `opportunity` on this branch.

```mermaid
erDiagram
    opportunity_opportunity {
        char lead_id PK
        char customer_id "LOGICAL"
        varchar state
        varchar lob
        varchar product_class
        varchar created_by_actor_type
        varchar accountable_sp_id "LOGICAL immutable"
        jsonb accountable_sp_cert_ref
        varchar need_analysis_state
        varchar insurer_id
        timestamptz partner_visible_from
        jsonb config_versions
        varchar source
        varchar assigned_rm_id "LOGICAL"
        timestamptz expires_at
        char converted_journey_id "LOGICAL UK"
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    opportunity_assignment {
        char assignment_id PK
        char lead_id FK
        varchar rm_id
        timestamptz assigned_at
        varchar assigned_by
        varchar reason
    }
    opportunity_follow_up {
        char follow_up_id PK
        char lead_id FK
        bytea note_enc "flag"
        varchar encryption_key_id
        varchar created_by
        timestamptz created_at
    }
    opportunity_opportunity ||--o{ opportunity_assignment : "lead_id"
    opportunity_opportunity ||--o{ opportunity_follow_up : "lead_id"
```

| Column | CHECK / rule |
|---|---|
| `state` | `NEW` \| `ASSIGNED` \| `CONTACTED` \| `QUALIFIED` \| `CONVERTED` \| `DISQUALIFIED` \| `EXPIRED` |
| `created_by_actor_type` | `BANK_RM` on this branch (INV-LED-04 pre-D-018). Parallel DATA-003 widens workforce create |
| `source` | `RM` |
| `need_analysis_state` | `NOT_STARTED` \| `IN_PROGRESS` \| `COMPLETED` |
| `accountable_sp_id` | Immutable after first write (INV-ACT-03) |
| Partner visibility | `insurer_id IS NOT NULL AND partner_visible_from IS NOT NULL AND need_analysis_state = 'COMPLETED'` |

---

## 8. `consent`

```mermaid
erDiagram
    consent_consent {
        char consent_id PK
        char lead_id "LOGICAL"
        char journey_id "LOGICAL"
        char customer_id "LOGICAL"
        bytea cif_enc "flag"
        varchar encryption_key_id
        text_array purpose
        text statement_text
        varchar statement_version
        varchar channel
        varchar otp_txn_id
        timestamptz captured_at
        inet source_ip
        bytea contact_used_enc "flag"
        varchar state
        timestamptz withdrawn_at
        varchar withdrawn_by
        varchar withdrawal_reason
        timestamptz valid_until
        varchar config_version_ref "LOGICAL"
        varchar lob
        timestamptz created_at
    }
```

| | |
|---|---|
| `state` | `REQUESTED` \| `OTP_PENDING` \| `GRANTED` \| `WITHDRAWN` \| `EXPIRED` \| `ABANDONED` |
| `channel` | `RM_ASSISTED` \| `SELF_SERVICE` |
| Evidence columns | Write-once. Only state / withdrawal columns may change |

---

## 9. `suitability`

```mermaid
erDiagram
    suitability_suitability {
        char suitability_id PK
        char lead_id "LOGICAL"
        char journey_id "LOGICAL"
        char customer_id "LOGICAL"
        varchar lob
        varchar questionnaire_version
        varchar outcome
        text_array recommended_products
        timestamptz evaluated_at
        timestamptz valid_until
        varchar state
        varchar override_actor_id
        varchar override_reason
        timestamptz override_at
        varchar evidence_document_ref
        varchar config_version_ref "LOGICAL"
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    suitability_answer_enc {
        char suitability_id PK_FK
        bytea answers_enc "flag"
        varchar encryption_key_id
    }
    suitability_suitability ||--|| suitability_answer_enc : "suitability_id"
```

| | |
|---|---|
| `state` | `IN_PROGRESS` \| `COMPLETED` \| `OVERRIDDEN` \| `SUPERSEDED` \| `EXPIRED` \| `ABANDONED` |
| `outcome` | null \| `ELIGIBLE` \| `NOT_ELIGIBLE` |
| INV-SUI-01 on this branch | COMPLETED is not updated; correction = new row |
| Override | `OVERRIDDEN` requires actor + reason + timestamp |

---

## 10. `catalogue`

```mermaid
erDiagram
    catalogue_insurer {
        varchar insurer_id PK
        varchar name
        varchar group_class
        boolean active
        timestamptz effective_from
        timestamptz effective_to
    }
    catalogue_product {
        char product_id PK
        varchar insurer_id FK
        varchar product_code
        varchar product_name
        varchar lob
        varchar product_class
        timestamptz effective_from
        timestamptz effective_to
        int version
        jsonb payload
    }
    catalogue_eligibility_band {
        char band_id PK
        char product_id FK
        varchar lob
        smallint min_age
        smallint max_age
        numeric min_sum_assured
        numeric max_sum_assured
        varchar occupation_band
        timestamptz effective_from
        timestamptz effective_to
    }
    catalogue_insurer ||--o{ catalogue_product : "insurer_id"
    catalogue_product ||--o{ catalogue_eligibility_band : "product_id"
```

`group_class` ∈ `A|B`. Unique `(insurer_id, product_code, version)`. Active versions are not updated in place. Read cache is Valkey, not a table.

---

## 11. `quotation`

```mermaid
erDiagram
    quotation_quote {
        char quote_id PK
        char lead_id "LOGICAL"
        char journey_id "LOGICAL"
        char customer_id "LOGICAL"
        char suitability_id "LOGICAL"
        char consent_id "LOGICAL"
        varchar lob
        varchar mode
        varchar category
        numeric requested_cover
        bytea members_enc "flag"
        varchar encryption_key_id
        varchar state
        timestamptz valid_until
        varchar provider
        varchar provider_job_id "LOGICAL"
        varchar config_version_ref "LOGICAL"
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    quotation_offer {
        char offer_id PK
        char quote_id FK
        varchar insurer_code
        varchar product_code
        varchar product_name
        numeric premium_amount
        varchar premium_frequency
        numeric tax_amount
        numeric sum_assured
        int cover_term
        int premium_paying_term
        boolean out_of_bound
        varchar offer_state
        varchar error_summary
        timestamptz created_at
    }
    quotation_quote ||--o{ quotation_offer : "quote_id"
```

| | |
|---|---|
| `mode` | `SINGLE` \| `MULTI` |
| `quote.state` | `REQUESTED` \| `IN_PROGRESS` \| `QUOTED` \| `PARTIALLY_QUOTED` \| `SELECTED` \| `CONVERTED` \| `EXPIRED` \| `FAILED` \| `TIMED_OUT` \| `REJECTED` |
| `offer_state` | `QUOTED` \| `SELECTED` \| `NOT_SELECTED` \| `INVALID` |
| `ux_offer_selected` | at most one `SELECTED` per `quote_id` |
| Positive | `premium_amount > 0 AND sum_assured > 0` |

Adapter poll rows stay in `bank_persistence`, not here.

---

## 12. `proposal`

```mermaid
erDiagram
    proposal_proposal {
        char proposal_id PK
        char lead_id "LOGICAL"
        char journey_id "LOGICAL"
        char quote_id "LOGICAL"
        char offer_id "LOGICAL"
        char consent_id "LOGICAL"
        char customer_id "LOGICAL"
        varchar application_number UK
        varchar schema_id
        varchar schema_version
        varchar form_values_ref
        varchar state
        bytea nominee_enc "flag"
        varchar encryption_key_id
        varchar underwriting_state
        jsonb counter_offer
        varchar lob
        varchar config_version_ref "LOGICAL"
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    proposal_uw_requirement {
        char requirement_id PK
        char proposal_id FK
        varchar requirement_type
        varchar sub_type
        varchar description
        varchar status
        date due_date
        timestamptz created_at
    }
    proposal_uw_document_ref {
        char document_id PK
        char proposal_id FK
        varchar object_ref
        varchar document_class
        timestamptz created_at
    }
    proposal_proposal ||--o{ proposal_uw_requirement : "proposal_id"
    proposal_proposal ||--o{ proposal_uw_document_ref : "proposal_id"
```

`state`: `DRAFT` \| `SUBMITTED` \| `SUBMISSION_FAILED` \| `UNDER_WRITING` \| `REQUIREMENTS_PENDING` \| `UW_APPROVED` \| `UW_COUNTER_OFFER` \| `UW_DECLINED` \| `CUSTOMER_DECLINED` \| `AWAITING_PAYMENT` \| `PAID` \| `CONVERTED` \| `ISSUANCE_FAILED` \| `REFUND_REQUIRED` \| `WITHDRAWN` \| `EXPIRED` \| `ABANDONED`.

`form_values_ref` is an object-store pointer (`INV-PRP-05`). Form JSON is not a column.

---

## 13. `payment`

```mermaid
erDiagram
    payment_payment {
        char payment_id PK
        char lead_id "LOGICAL"
        char journey_id "LOGICAL"
        char proposal_id "LOGICAL"
        numeric amount
        char currency
        varchar state
        varchar device_channel
        bytea link_issued_to_enc "flag"
        varchar encryption_key_id
        timestamptz link_expires_at
        varchar pg_txn_id
        timestamptz authorised_at
        timestamptz captured_at
        varchar settlement_ref
        timestamptz reconciled_at
        varchar lob
        varchar config_version_ref "LOGICAL"
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    payment_attempt {
        char attempt_id PK
        char payment_id FK
        timestamptz attempted_at
        varchar outcome
        varchar pg_txn_id
        varchar detail
    }
    payment_refund {
        char refund_id PK
        char payment_id FK
        numeric amount
        varchar reason
        varchar initiated_by
        varchar approved_by
        timestamptz decided_at
        timestamptz created_at
    }
    payment_payment ||--o{ payment_attempt : "payment_id"
    payment_payment ||--o{ payment_refund : "payment_id"
```

| | |
|---|---|
| `device_channel` | `SMS_LINK` \| `EMAIL_LINK` \| `QR_SCAN` |
| `state` | `INITIATED` \| `LINK_ISSUED` \| `AWAITING_AUTHORISATION` \| `AUTHORISED` \| `CAPTURED` \| `UNCERTAIN` \| `RECONCILED` \| `RECONCILIATION_BREAK` \| `REFUND_INITIATED` \| `REFUNDED` \| `DECLINED` \| `EXPIRED` \| `REJECTED` |
| `ux_payment_active_per_proposal` | one non-terminal payment per proposal (`INV-PAY-02`) |
| Card data | Never stored (`DR-PII-06`) |

---

## 14. `policy`

```mermaid
erDiagram
    policy_policy {
        char policy_id PK
        char lead_id "LOGICAL"
        char journey_id "LOGICAL"
        char customer_id "LOGICAL"
        char proposal_id "LOGICAL"
        char payment_id "LOGICAL"
        varchar policy_number
        varchar insurer_code
        varchar product_code
        numeric sum_assured
        numeric premium
        varchar frequency
        date risk_commencement_date
        date maturity_date
        varchar state
        timestamptz issued_at
        timestamptz confirmed_at
        timestamptz free_look_expires_at
        varchar lob
        varchar product_class
        varchar config_version_ref "LOGICAL"
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    policy_document_ref {
        char document_id PK
        char policy_id FK
        varchar object_ref
        varchar document_type
        timestamptz created_at
    }
    policy_policy ||--o{ policy_document_ref : "policy_id"
```

`state`: `PENDING_ISSUANCE` \| `ISSUED` \| `CONFIRMED` \| `CONFIRMATION_OVERDUE` \| `ACTIVE` \| `ISSUANCE_REJECTED` \| `ISSUANCE_DISPUTED` \| `FREE_LOOK_CANCELLED` \| `LAPSED` \| `SURRENDERED` \| `MATURED`.

Unique `(insurer_code, policy_number)` — immutable (`INV-POL-02`). PDF bytes in S3 via `object_ref`.

---

## 15. `journey`

```mermaid
erDiagram
    journey_journey {
        char journey_id PK
        char lead_id "LOGICAL"
        char customer_id "LOGICAL"
        varchar rm_id "LOGICAL"
        varchar accountable_sp_id "LOGICAL"
        varchar stage
        varchar channel
        varchar lob
        varchar current_assisting_actor_id
        varchar current_assisting_actor_type
        varchar partner_insurer_id
        timestamptz partner_visible_from
        char party_snapshot_ref "LOGICAL"
        timestamptz next_action_due_at
        jsonb compensation
        varchar config_version_ref "LOGICAL"
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    journey_journey_ref {
        char journey_id PK_FK
        varchar ref_type PK
        varchar ref_value
    }
    journey_journey ||--o{ journey_journey_ref : "journey_id"
```

| | |
|---|---|
| `channel` | `RM_ASSISTED` \| `SELF_SERVICE` \| `HYBRID` |
| `stage` | `INITIATED` \| `NEED_ANALYSIS` \| `SUITABILITY_COMPLETE` \| `CONSENT_CAPTURED` \| `QUOTING` \| `QUOTE_SELECTED` \| `PROPOSAL_IN_PROGRESS` \| `UNDERWRITING` \| `PAYMENT_PENDING` \| `PAYMENT_SETTLED` \| `ISSUANCE_PENDING` \| `ISSUED` \| `SOLD` \| `ABANDONED` \| `DECLINED` \| `COMPENSATING` \| `COMPENSATED` \| `MANUAL_INTERVENTION` |
| `ref_type` | `suitabilityId` \| `consentId` \| `quoteId` \| `offerId` \| `proposalId` \| `paymentId` \| `policyId` \| `applicationNumber` \| `policyNumber` \| `provider` |
| Open lead | unique `lead_id` while stage not terminal |
| Absent columns | eligibility, premium, UW decision (`INV-JRN-02`) |

---

## 16. `administration`

```mermaid
erDiagram
    administration_configuration_record {
        char config_id PK
        varchar domain
        varchar lob
        varchar insurer_id
        varchar product_code
        varchar journey_type
        varchar form_id
        varchar field_id
        varchar role_id
        varchar actor_type
        int version
        jsonb payload
        timestamptz effective_from
        timestamptz effective_to
        varchar status
        varchar checksum
        varchar seed_ref
        varchar created_by
        timestamptz created_at
    }
```

`status` ∈ `DRAFT|ACTIVE|SUPERSEDED|WITHDRAWN`. INSERT-only (`INV-CFG-02`). Unique partition + `version` (COALESCE blanks). A different checksum at the same version is an error.

---

## 17. What has no ER

| Store | Why no entity diagram |
|---|---|
| DynamoDB | No R0 tables (`DB-DEC-0003`) |
| Valkey | Session / cache keys, TTL, not an SoR |
| S3 | Bytes addressed by `object_ref` / `payload_id` |
| Keycloak vendor DB | Out of this pack |
| MSK | Events in flight |
| MIS / Athena | Isolated reader, not this cluster |

---

## 18. Cardinality along one R0 sale

```text
1 customer ──1 open lead── 1 journey ──1 snapshot
                │
                ├── 1..n consent (current GRANTED required for proposal)
                ├── 0..n suitability (Term may have none; Savings/ULIP needs a valid one)
                ├── 1 quote ── 1..n offer (exactly one SELECTED)
                ├── 1 proposal ── 0..n UW requirement / document
                ├── 1 active payment ── 0..n attempt ── 0..1 refund
                └── 1 policy ── 0..n document_ref
```

Cross-context integrity is the owning service’s contract plus events. A broken logical id is an application defect, not a database cascade.
