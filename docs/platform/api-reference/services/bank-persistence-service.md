# bank-persistence-service — integration job store and audit ingest

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The platform-common database owner for **provider integration state** (async jobs, offers, poll attempts, encrypted raw provider payloads, adapter payment-URL sessions) and **audit event ingestion**. It exists so the 1SB adapter (and future adapters) can be stateless with no datasource. It is **not** a home for business contexts — Lead, Quote, Payment etc. own their own schemas (`ARCH-004`, `DR-OWN-03`) |
| **Module** | `services/bank-persistence-service` · port 8081 · `service-id: persistence` · layer L7 · INTERNAL |
| **Base path** | `/internal/v1` — cluster-private |
| **Callers** | `1sb-integration-service` (all job/payload/session writes) · `audit-compliance-service` (target: audit ingest) · support tooling (reads) |
| **Owns data** | Aurora schema **`bank_persistence`** (runtime today: Flyway `V1__init_schema.sql`, `public` schema) · role `app_bank_persistence` (INSERT/SELECT on `audit_event` and `raw_payload`; no UPDATE/DELETE) · S3 prefix for payload bytes (target) |
| **Code status** | **IMPLEMENTED** — 12 endpoints |

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
| `eventId` | optional — **send it** (consumer dedupe on `eventId`); else a UUID is minted |
| `eventTime` | optional — default now |
| `lob`, `journeyId`, `distributorId`, `agentId`, `traceId` | optional |
| `metadata` | optional JSON string — ids and states only, **no PII** |

INSERT only. The table is the platform's **regulatory evidence of record** for audit, together with
the S3 Object Lock archive (`ADR-012`, `FF-26`); a topic is never the record. Target columns from
`14-audit_event_delta.sql` add reconstruction fields (`actingCapacity`, `actorInsurerId`,
`assistedActorId` — `INV-ACT-04`).

`GET /internal/v1/audit-events?resourceId=` returns all events for that resource ordered by time —
used to reconstruct a quote job or a journey.

## 6. Tables

| Table | Key columns |
|---|---|
| `integration_job` | `job_id`, `job_type`, `lob`, `status`, `failure_reason`, `journey_id`, `application_number`, `policy_number`, `external_req_id`, `external_provider`, `idempotency_key` UNIQUE, `owned_by_instance`, `version` |
| `integration_job_offer` | `offer_id`, `job_id` FK, insurer, product, premium, sum assured, `out_of_bound`, `offer_status`, `error_summary`, `funds_json` |
| `job_poll_attempt` | `attempt_id`, `job_id` FK, `attempt_number`, `http_status`, `is_complete`, `duration_ms`, `error_message` |
| `raw_payload` | `payload_id`, `job_id`, `direction`, `operation`, `lob`, `payload_enc`, `encryption_key_id`, `http_status`, `retain_until` |
| `audit_event` | `event_id`, `event_time`, `action`, `actor_id`, `actor_type`, `resource_type`, `resource_id`, `outcome`, `lob`, `journey_id`, `distributor_id`, `agent_id`, `metadata`, `trace_id` |
| `payment_session` | `session_id`, `job_id`, `application_number`, `lob`, `payment_url`, `redirect_url`, `status`, `external_txn_id`, `expires_at` |

DDL: [`02-bank_persistence.sql`](../../data-architecture/schemas/02-bank_persistence.sql) ·
[`14-audit_event_delta.sql`](../../data-architecture/schemas/14-audit_event_delta.sql).

## 7. Sources

- `services/bank-persistence-service/src/main/java/com/bank/persistence/api/internal/v1/*.java` · `crypto/RawPayloadEncryptionService.java` · `src/main/resources/db/migration/V1__init_schema.sql`
- [bank-persistence-service architecture](../../../1sb-insurance-integration/architecture/bank-persistence-service.md) · [audit consumer](../../../1sb-insurance-integration/architecture/audit-consumer-service.md)
