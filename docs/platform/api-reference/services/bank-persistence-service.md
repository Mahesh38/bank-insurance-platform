# bank-persistence-service — integration job store and audit ingest

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The platform-common database owner for **provider integration state** (async jobs, offers, poll attempts, encrypted raw provider payloads, adapter payment-URL sessions) and **audit event ingestion**. It exists so the 1SB adapter (and future adapters) can be stateless with no datasource. It is **not** a home for business contexts — Lead, Quote, Payment etc. own their own schemas (`ARCH-004`, `DR-OWN-03`) |
| **Module** | `services/bank-persistence-service` · port 8081 · `service-id: persistence` · layer L7 · INTERNAL |
| **Base path** | `/internal/v1` — cluster-private, and every call must carry header **`X-Internal-Service-Key`** (shared internal service key, `persistence.internal-auth.shared-secret`; no default under `uat` / `prod`). Missing or wrong → `401 UNAUTHORIZED` (problem+json) from `InternalServiceAuthFilter`; `/actuator/**` is not guarded. 1SB sends it from `bank.persistence.internal-service-key` (READINESS-REVIEW §3.7: the decrypting raw-payload read had no authorisation) |
| **Callers** | `1sb-integration-service` (all job/payload/session writes) · `audit-compliance-service` (target: audit ingest) · support tooling (reads) |
| **Owns data** | Aurora schema **`bank_persistence`** (runtime today: Flyway `V1`–`V4` in `db/migration` plus `V5` in `db/vendor/{postgresql,h2}`, `public` schema) · role `app_bank_persistence` (INSERT/SELECT on `audit_event` and `raw_payload`; no UPDATE/DELETE) · S3 prefix for payload bytes (target) |
| **Code status** | **IMPLEMENTED** — 12 endpoints |
| **Contract** | **Authoritative (ADR-027):** [`contracts/bank-persistence-service/openapi.yaml`](../../contracts/bank-persistence-service/openapi.yaml) · [LLD](../../contracts/bank-persistence-service/LLD.md). Where this page and the contract differ, the contract wins. Fix-pending: audit append insert-only (duplicate `eventId` → `200` stored row), `X-Internal-Service-Key` |

---

## 1. API index

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/internal/v1/jobs` | Create an integration job (`PENDING`) |
| `GET` | `/internal/v1/jobs/{jobId}` | Read job |
| `PATCH` | `/internal/v1/jobs/{jobId}/status` | Update status / failure reason / external ref / completion / application number |
| `GET` | `/internal/v1/jobs/{jobId}/offers` | List offers of a job |
| `POST` | `/internal/v1/jobs/{jobId}/offers` | Add one offer |
| `POST` | `/internal/v1/jobs/{jobId}/poll-attempts` | Record one poll attempt |
| `GET` | `/internal/v1/jobs/{jobId}/poll-attempts` | List attempts in order |
| `POST` | `/internal/v1/raw-payloads` | Store an encrypted raw payload |
| `GET` | `/internal/v1/raw-payloads/{payloadId}` | Read one payload **decrypted** (compliance tooling only) |
| `GET` | `/internal/v1/raw-payloads?jobId=` | List payload metadata for a job (never plaintext) |
| `POST` | `/internal/v1/payment-sessions` | Store an adapter payment-URL session |
| `GET` | `/internal/v1/payment-sessions/{sessionId}` | Read it |
| `POST` | `/internal/v1/audit-events` | Append an audit event (INSERT only) |
| `GET` | `/internal/v1/audit-events?resourceId=` | Events for a resource, oldest first |

Unknown ids → `404 RESOURCE_NOT_FOUND`. Bean-validation failures → `422 VALIDATION_ERROR`.

---

## 2. Jobs

### 2.1 `POST /internal/v1/jobs`

| Field | Validation |
|---|---|
| `lob` | `@NotBlank` |
| `jobType` | `@NotBlank` — `QUOTE`, `PROPOSAL`, … |
| `journeyId` | optional — bank correlation |
| `idempotencyKey` | optional — **unique** column; a repeat key is a constraint violation |
| `createdByActor` | `@NotBlank` |

Server sets `jobId` (UUID), `status = PENDING`, `externalProvider = ONE_SB`, timestamps,
`version = 0`. Response `201 JobResponse`:

```json
{ "jobId": "3f6c…", "jobType": "QUOTE", "lob": "TERM", "status": "PENDING", "failureReason": null,
  "journeyId": "01JQ…", "applicationNumber": null, "policyNumber": null, "externalReqId": null,
  "externalProvider": "ONE_SB", "idempotencyKey": "k-123", "resultBlobId": null,
  "createdAt": "…", "updatedAt": "…", "completedAt": null, "ownedByInstance": null, "version": 0,
  "createdByActor": "7f0c…" }
```

### 2.2 `PATCH /internal/v1/jobs/{jobId}/status`

Body: `status` (`@NotBlank`), optional `failureReason`, `externalReqId`, `completedAt`,
`applicationNumber`. Only non-null optional fields overwrite. `updatedAt` refreshed.
Used by the adapter for `RUNNING` (+ `reqId`), `COMPLETED` / `PARTIAL` (+ `completedAt`,
`applicationNumber`), `FAILED` / `TIMEOUT` (+ reason).

### 2.3 Offers and poll attempts

`POST /jobs/{id}/offers` body: `offerId` (optional, else UUID), `insurerCode`, `productCode`,
`productName`, `premiumAmount`, `premiumFrequency`, `sumAssured`, `outOfBound`, `offerStatus`,
`errorSummary`, `rawOfferBlobId`, `fundsJson`. `POST /jobs/{id}/poll-attempts` body:
`attemptNumber` (`@NotNull`), `attemptedAt`, `httpStatus`, `isComplete`, `durationMs`,
`errorMessage`.

---

## 3. Raw payloads

`POST /internal/v1/raw-payloads`:

| Field | Validation |
|---|---|
| `jobId` | `@NotBlank` |
| `direction` | `@NotBlank`, `REQ` \| `RES` |
| `operation` | `@NotBlank` (provider path) |
| `lob` | `@NotBlank` |
| `payload` | `@NotBlank` — plaintext JSON; **encrypted before it touches the database; never logged** |
| `httpStatus` | optional |
| `retainUntil` | optional — default today + `RAW_PAYLOAD_RETENTION_YEARS` (7) |

Encryption: AES-256-GCM, random 96-bit IV per row, stored as IV ‖ ciphertext ‖ tag; key from
`RAW_PAYLOAD_ENCRYPTION_KEY` (must decode to 32 bytes — the service **refuses to start** otherwise);
`encryption_key_id = raw-payload-key-v1` for rotation. Response `201` is metadata only.
More: [payload storage and transactions](../data/payload-storage-and-transactions.md#2-raw-provider-payloads-implemented).

---

## 4. Payment sessions (adapter)

`POST /internal/v1/payment-sessions`: `applicationNumber`, `lob`, `paymentUrl`, `redirectUrl`,
`status`, `createdByActor` (`@NotBlank`); optional `sessionId`, `jobId`, `externalTxnId`,
`expiresAt`. This is the **1SB link session**, correlated by `application_number` — not the
Payment bounded context's money record (`payment.payment`).

---

## 5. Audit events

`POST /internal/v1/audit-events`:

| Field | Validation |
|---|---|
| `action`, `actorId`, `actorType`, `resourceType`, `resourceId`, `outcome` | `@NotBlank` |
| `eventId` | optional, ≤ 36 — **send it** (idempotency key, see below); else a UUID is minted |
| `eventTime` | optional — default now; stored at microsecond precision |
| `lob`, `journeyId`, `distributorId`, `agentId`, `traceId` | optional (column lengths enforced → `400`) |
| `metadata` | optional JSON string — ids and states only, **no PII** |
| `priorState`, `newState` | optional JSON text (TEXT columns while tests run on H2) |
| `consentRef`, `suitabilityRef`, `configVersionRef`, `actorInsurerId`, `assistedActorId` | optional |
| `eventSchemaVersion` | optional, ≥ 1 — default `1` |
| `sequenceNo` | optional, ≥ 0 — with `journeyId`, unique per journey |
| `actingCapacity` | optional, `SP_ACCOUNTABLE` \| `ASSIST_ONLY` (`INV-ACT-04`) → else `400 VALIDATION_ERROR` |
| `retainUntil` | optional instant |

The response echoes every field above (V3 reconstruction columns included).

**Insert-only and idempotent on `eventId`** (READINESS-REVIEW §3.6 — a replay used to become an
UPDATE of an evidence row):

| Request | Result |
|---|---|
| new `eventId` | INSERT → `201` |
| known `eventId`, same content | stored row → `200`, nothing written |
| known `eventId`, different content | `409 IDEMPOTENCY_CONFLICT`, stored row unchanged |
| `(journeyId, sequenceNo)` already used by another event | `409 CONFLICT` |

"Same content" compares every field the caller supplied; `eventTime` and `eventSchemaVersion` are
server-defaulted, so leaving them out of a replay matches whatever was stored. The entity is a
`Persistable` that is always new, so the repository can only `persist` (INSERT), never `merge`.

The table is the platform's **regulatory evidence of record** for audit, together with
the S3 Object Lock archive (`ADR-012`, `FF-26`); a topic is never the record. ADR-026 (PROPOSED,
D7) would move business audit to its own store; until then this table stays current.

Database guards (Flyway): `V4` adds `ck_audit_capacity`; `V5` (PostgreSQL) adds partial unique
`ux_audit_journey_sequence (journey_id, sequence_no) WHERE both NOT NULL` and trigger
`trg_audit_event_immutable` → `fn_prevent_update_delete()` (refuses UPDATE / DELETE,
`restrict_violation`), per [`14-audit_event_delta.sql`](../../data-architecture/schemas/14-audit_event_delta.sql)
and [`90-routines.sql`](../../data-architecture/schemas/90-routines.sql). `V5` on H2 is a plain
unique index over the two columns (H2 treats NULLs as distinct — same guarantee) and **omits the
trigger** (H2 triggers are Java classes); the insert-only controller is the local guard. Neither
is proven on PostgreSQL until Testcontainers runs in CI (no Docker here).

`GET /internal/v1/audit-events?resourceId=` returns all events for that resource ordered by time —
used to reconstruct a quote job or a journey.

## 6. Tables

| Table | Key columns |
|---|---|
| `integration_job` | `job_id`, `job_type`, `lob`, `status`, `failure_reason`, `journey_id`, `application_number`, `policy_number`, `external_req_id`, `external_provider`, `idempotency_key` UNIQUE, `owned_by_instance`, `version` |
| `integration_job_offer` | `offer_id`, `job_id` FK, insurer, product, premium, sum assured, `out_of_bound`, `offer_status`, `error_summary`, `funds_json` |
| `job_poll_attempt` | `attempt_id`, `job_id` FK, `attempt_number`, `http_status`, `is_complete`, `duration_ms`, `error_message` |
| `raw_payload` | `payload_id`, `job_id`, `direction`, `operation`, `lob`, `payload_enc`, `encryption_key_id`, `http_status`, `retain_until` |
| `audit_event` | `event_id`, `event_time`, `action`, `actor_id`, `actor_type`, `resource_type`, `resource_id`, `outcome`, `lob`, `journey_id`, `distributor_id`, `agent_id`, `metadata`, `trace_id`; V3: `prior_state`, `new_state`, `consent_ref`, `suitability_ref`, `event_schema_version`, `retain_until`, `sequence_no`, `acting_capacity` (CHECK, V4), `actor_insurer_id`, `assisted_actor_id`, `config_version_ref`; unique `(journey_id, sequence_no)` and immutability trigger (V5) |
| `payment_session` | `session_id`, `job_id`, `application_number`, `lob`, `payment_url`, `redirect_url`, `status`, `external_txn_id`, `expires_at` |

DDL: [`02-bank_persistence.sql`](../../data-architecture/schemas/02-bank_persistence.sql) ·
[`14-audit_event_delta.sql`](../../data-architecture/schemas/14-audit_event_delta.sql).

## 7. Sources

- `services/bank-persistence-service/src/main/java/com/bank/persistence/api/internal/v1/*.java` · `security/InternalServiceAuthFilter.java` · `crypto/RawPayloadEncryptionService.java` · `src/main/resources/db/migration/V1__init_schema.sql` … `V4__audit_event_capacity_check.sql` · `src/main/resources/db/vendor/{postgresql,h2}/V5__audit_event_sequence_and_immutability.sql`
- [bank-persistence-service architecture](../../../1sb-insurance-integration/architecture/bank-persistence-service.md) · [audit consumer](../../../1sb-insurance-integration/architecture/audit-consumer-service.md)
