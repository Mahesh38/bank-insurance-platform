# Store ownership — which service uses which database

[← API reference home](../README.md)

**The rule:** one owner per authoritative fact. Each owning service has its **own schema**, its
**own database role**, its **own Flyway history**, and no grant on any other schema. A service that
needs another context's fact **calls that service's API or consumes its event** — it never opens
that schema. There is no cross-schema `FOREIGN KEY`; cross-context links are opaque ULIDs that the
owning service validates.

Decision record: `ADR-008` (ownership is invariant, one Aurora cluster at R0), `DB-DEC-0003`
(store segregation), `DB-DEC-0004` (working payload until payment). Full DBA pack:
[04 store segregation](../../data-architecture/04-store-segregation-and-roadmap.md) ·
[05 ER and field map](../../data-architecture/05-e2e-er-and-field-map.md) ·
[DDL](../../data-architecture/schemas/README.md).

> **Status.** The physical design is `AI-DRAFTED`. Applied today: `identity` (Flyway V1–V2 in
> `identity-authorization-service`) and `bank_persistence` (Flyway V1 in `bank-persistence-service`,
> in the `public` schema). Every other schema is built by its owning service when that service is
> built (waves W0b–W3). Human Aarti / Mahesh / Deepali signatures are outstanding.

---

## 1. Segregation in three layers

```text
  NIP-APP / customer device   never opens a database
            │
  workforce-access-bff        Valkey session keyspace only — no Aurora, no Dynamo
            │  HTTP (API)
  owning domain service       one schema · one app_<schema> role · own Flyway
            │
  Aurora PostgreSQL (ONE cluster, Multi-AZ, ap-south-1; DR ap-south-2)
       identity | bank_persistence | customer | lead_lms | consent | suitability
       catalogue | quotation | proposal | payment | policy | journey | administration
            │  no cross-schema GRANT · no cross-schema FK
  other contexts reached only by: sync API to the owner, or outbox → MSK → consumer
```

| Layer | Separates | Enforced by |
|---|---|---|
| Ownership | One writer per attribute | Service boundary + ArchUnit |
| Credential | One PostgreSQL role per schema | `91-grants.sql` + per-role secret (S09) |
| Engine / cluster | Same cluster at R0; first physical split is LOB-cell vs shared platform | `ADR-008` |

---

## 2. Service → store → role → tables

### 2.1 Services that own an Aurora schema

| Service | Schema | Runtime role / migrator role | Tables | Also opens |
|---|---|---|---|---|
| `identity-authorization-service` | `identity` | `app_identity` / `migrator_identity` | `insurer`, `branch`, `business_user`, `user_branch_assignment`, `organization_relationship`, `certification`, `role`, `permission`, `user_role`, `role_permission`, `entitlement`, `bulk_import`, `bulk_import_row`, `approval_request`, `outbox_event` | — |
| `bank-persistence-service` | `bank_persistence` | `app_bank_persistence` / `migrator_bank_persistence` | `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session` | S3 prefix for raw payload bytes |
| `customer-service` | `customer` | `app_customer` / `migrator_customer` | `customer` (HMAC CIF lookup + encrypted CIF/PII), `customer_snapshot` (journey-scoped frozen copy) | — |
| `lead-service` | `lead_lms` | `app_lead_lms` / `migrator_lead_lms` | `lead`, `lead_assignment`, `lead_meeting`, `lead_follow_up`, `idempotency_record`, `outbox_event` | — |
| `consent-service` | `consent` | `app_consent` / `migrator_consent` | `consent` (write-once evidence), `idempotency_record` | — |
| `suitability-service` | `suitability` | `app_suitability` / `migrator_suitability` | `suitability`, `suitability_answer_set` (encrypted answers), `suitability_mapping_run`, `idempotency_record`, `outbox_event` | — |
| `product-catalogue-service` | `catalogue` | `app_catalogue` / `migrator_catalogue` | `insurer`, `product`, `eligibility_band` | Valkey prefix `catalogue:` |
| `journey-orchestration-service` | `journey` | `app_journey` / `migrator_journey` | `journey`, `journey_ref`, `journey_transition`, `outbox_event`, `idempotency_record` | — |
| `quotation-service` | `quotation` | `app_quotation` / `migrator_quotation` | `quote_working` (encrypted JSON envelope until payment), `quote`, `offer` (structured after `RECONCILED`), `idempotency_record` | — |
| `proposal-service` | `proposal` | `app_proposal` / `migrator_proposal` | `proposal_working`, `proposal`, `uw_requirement`, `uw_document_ref`, `idempotency_record` | — |
| `payment-service` | `payment` | `app_payment` / `migrator_payment` | `payment`, `payment_attempt`, `refund`, `outbox_event`, `idempotency_record` | — |
| `policy-issuance-service` | `policy` | `app_policy` / `migrator_policy` | `policy`, `policy_document_ref`, `idempotency_record` | S3 prefix for policy PDFs |
| `administration-config-service` | `administration` | `app_administration` / `migrator_administration` | `configuration_record` (INSERT-only, versioned, effective-dated) | — |

Two more roles exist on the cluster and belong to no service: `job_retention` (executes purge
procedures, never deletes evidence) and `ro_breakglass` (time-bound human read, IAM-brokered).
**No human holds a standing write on an evidence table.**

### 2.2 Services that must not open Aurora (or DynamoDB)

| Service | What it uses instead | Why |
|---|---|---|
| `1sb-integration-service` | HTTP to `bank-persistence-service` `/internal/v1/*` | Standing constraint: no Flyway, no JPA, no datasource; ArchUnit enforced |
| `audit-compliance-service` | HTTP `POST /internal/v1/audit-events` on `bank-persistence-service` | No second audit database (`DR-OWN-04`) |
| `workforce-access-bff` | Valkey session vault | Token-hiding edge; bank apps never open a database |
| `identity-provider-adapter-service` | Keycloak admin API; bank AD-verify API | IdP isolation; PDP truth is `identity` |
| `integration-hub-service` | Routing policy from `administration-config-service` API | No business data |
| `notification-service` | SMS / email / OTP gateway; audit via outbox | Delivery, not a system of record at R0 |
| `reporting-mis-service` | Isolated reader / S3 + Athena extract | Never the Lead or Payment writer (`C-ISO-1`) |
| `direct-insurer-adapter-service` | Out of R0; when admitted, same pattern as 1SB | Must sit behind the Hub |

### 2.3 DynamoDB — the R0 list is empty

Older diagrams (HLD Boundary 8, `architecture-review/05`) put journey state, quote jobs and audit
events in DynamoDB. **The R0 DBA ruling rejects all of them** ([04 §3.2](../../data-architecture/04-store-segregation-and-roadmap.md#32-dynamodb--r0-access-list-is-empty)):
no Dynamo tables, no Dynamo IAM grants, no principals. Treat any `dynamodb:*` grant to a platform
service as a defect. A later introduction is a source-of-truth change needing a joint Aarti +
Mahesh decision on measured evidence.

---

## 3. Stores that are not PostgreSQL

| Store | What lives there | Who may open it | Never there |
|---|---|---|---|
| **ElastiCache for Valkey** (`ADR-011`) | BFF sessions (pending logins, sessions, native completion codes), catalogue L2 read-through, per-principal rate-limit counters | Each deployable through its **own ACL user and key prefix** (`FF-24`) | Idempotency, suitability, consent, payment, certification, configuration past its TTL — anything that must survive eviction |
| **S3 (+ Object Lock for evidence)** | Encrypted raw 1SB payloads (if offloaded from the table), policy PDFs, audit cold WORM archive (7-year class) | Owning service IAM role, prefix-scoped | Card data, a queryable system of record |
| **Keycloak's own DB** | Keycloak realm (partner users, OIDC clients) | Keycloak, via the IdP adapter boundary | Business authorization |
| **Amazon MSK** (`ADR-012`) | Domain events in flight | Producers via their outbox; consumers via their service | The only copy of regulatory evidence |
| **OpenSearch** (`ADR-013`) | Operational logs (30 d hot, deleted at 90 d) | Log readers | Evidence; gate or audit answers |
| **S3 + Athena / Redshift** | MIS read models | `reporting-mis-service` reader path | Writer credentials of any OLTP schema |
| **AU Bank PG** | Card capture on the customer's device | Customer device only | Cardholder data in any platform store |

### 3.1 Valkey keys used by the BFF today

| Key | Value | TTL | Read pattern |
|---|---|---|---|
| `workforce:pending:{state}` | AES-256-GCM ciphertext of `PendingLogin` (state, nonce, PKCE verifier, client type, identity source, return URI) | `pending-login-ttl` 5 min | `GETDEL` (single use) on callback |
| `workforce:session:{sessionId}` | AES-256-GCM ciphertext of `WorkforceSession` (business user id, type, status, policy version, insurer code, provider subject, username, provider tokens, expiry, claims) | `session-ttl` 8 h | `GET` on every request; `DEL` on logout |
| `workforce:completion:{code}` | AES-256-GCM ciphertext of `NativeCompletion` (session id) | `completion-code-ttl` 1 min | `GETDEL` (single use) |

The DBA pack names the target prefix `bff:` with a dedicated ACL user. The code uses
`workforce:`; the ACL rule (no other service can read it) is what matters, and it is an S09 item.

---

## 4. How one sale moves across stores

```text
lead-service           [lead_lms]          create / assign / BI / convert
customer-service       [customer]          CIF lookup + journey snapshot
journey-orchestration  [journey]           stage + refs only
consent-service        [consent]           OTP evidence, write-once
suitability-service    [suitability]       encrypted answers + mapping run
product-catalogue      [catalogue]+Valkey  products, eligibility bands
quotation-service      [quotation]         quote_working (encrypted JSON) → quote/offer after RECONCILED
   └─ via integration-hub (no DB) → 1sb-integration-service (no DB) → bank-persistence [bank_persistence + S3]
proposal-service       [proposal]          proposal_working → proposal/UW after RECONCILED
payment-service        [payment]           always structured; RECONCILED is the issuance gate
policy-issuance        [policy] + S3 PDF   created only after RECONCILED
audit-compliance       (no DB) → bank-persistence.audit_event (INSERT-only) → S3 WORM
reporting-mis          [S3/Athena, later]  replica / extract, never the writer
```

Inside one context: business row + idempotency row + outbox row in **one** local transaction.
Across contexts: eventual, via outbox → MSK. **No 2PC anywhere.** Details:
[payload storage and transactions](./payload-storage-and-transactions.md).

## 5. Sources

- [04 store segregation and roadmap](../../data-architecture/04-store-segregation-and-roadmap.md) · [06 journey and working payload](../../data-architecture/06-journey-orchestrator-and-working-payload.md) · [DDL](../../data-architecture/schemas/README.md)
- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/session/RedisSessionStore.java`
- `services/identity-authorization-service/src/main/resources/db/migration/V1__identity_authorization_schema.sql`
