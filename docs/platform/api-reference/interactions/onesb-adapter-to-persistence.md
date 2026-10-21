# 1SB adapter → bank-persistence-service (job store over HTTP)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | `1sb-integration-service` (`HttpJobStoreAdapter`, `HttpRawPayloadStoreAdapter`, payment-session store, audit publisher) → `bank-persistence-service` `/internal/v1/*` |
| **Why HTTP and not a datasource** | Standing constraint: the adapter owns **no Flyway, no JPA, no datasource** (ArchUnit). Persistence is platform-common so the next adapter reuses it |
| **Base URL** | `BANK_PERSISTENCE_BASE_URL` (default `http://localhost:8081`) |
| **Code status** | **IMPLEMENTED** |

## 1. Port → endpoint map

| Adapter port method | HTTP call | Table |
|---|---|---|
| `JobStorePort.createJob(lob, type, journeyId, key, actor)` | `POST /internal/v1/jobs` | `integration_job` |
| `updateJobPolling(jobId, reqId)` | `PATCH /internal/v1/jobs/{id}/status` `{status: RUNNING, externalReqId}` | `integration_job` |
| `completeJob(jobId, offers[, appNo])` | `PATCH …/status` `{COMPLETED|PARTIAL, completedAt, applicationNumber}` then `POST …/offers` × n | `integration_job`, `integration_job_offer` |
| `failJob(jobId, reason)` | `PATCH …/status` `{FAILED|TIMEOUT, failureReason, completedAt}` | `integration_job` |
| `recordPollAttempt(…)` | `POST /internal/v1/jobs/{id}/poll-attempts` | `job_poll_attempt` |
| `findQuoteJob(jobId)` | `GET /internal/v1/jobs/{id}` + `GET …/offers` | read |
| `RawPayloadStorePort.store(…)` | `POST /internal/v1/raw-payloads` | `raw_payload` (encrypted) |
| `PaymentSessionStorePort` | `POST /internal/v1/payment-sessions` | `payment_session` |
| `AuditEventPublisher.publish` | `POST /internal/v1/audit-events` | `audit_event` |

## 2. Consistency notes

- Status is patched **before** offers are posted on completion. A reader that sees `COMPLETED`
  immediately may briefly see fewer offers than will exist; `GET /v1/quotes/{jobId}` reads both.
- Audit writes and quote / response raw payloads are **best effort** — a failure is logged (no
  body) and the provider call continues. The **request** of a proposal submit or payment-link call
  is required evidence: stored first (one retry), else the call is not made (`503`,
  `SUG-20261021-gap`).
- `integration_job.idempotency_key` is UNIQUE, so a duplicate create cannot produce two jobs even
  across pods (the in-memory idempotency filter is per pod).
- Optimistic locking (`version`) and `owned_by_instance` exist on the table for multi-instance poll
  ownership; using them is Phase 5.4 (parked).

## 3. Sources

- `services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/adapter/persistence/*.java`
- [bank-persistence-service](../services/bank-persistence-service.md) · [bank-persistence architecture](../../../1sb-insurance-integration/architecture/bank-persistence-service.md)
