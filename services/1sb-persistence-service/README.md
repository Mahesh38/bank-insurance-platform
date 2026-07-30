# 1SB Persistence Service

Internal Spring Boot service that owns the insurance integration PostgreSQL schema (Flyway + JPA).
Bank apps and the public API must **not** call this service — only `1sb-integration-service` (and other platform services) via the internal HTTP API.

- Port: **8081**
- Profiles: `local` (H2 MODE=PostgreSQL), `uat`, `prod`, `test`

## Quick start

```bash
./gradlew :services:1sb-persistence-service:bootRun --args='--spring.profiles.active=local'
curl http://localhost:8081/actuator/health
```

## Internal API (`/internal/v1`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/internal/v1/jobs` | Create integration job |
| `GET` | `/internal/v1/jobs/{jobId}` | Get job by id |
| `PATCH` | `/internal/v1/jobs/{jobId}/status` | Update job status (optional `failureReason`, `externalReqId`, `completedAt`) |
| `GET` | `/internal/v1/jobs/{jobId}/offers` | List offers for a job |
| `POST` | `/internal/v1/jobs/{jobId}/offers` | Add an offer to a job |
| `POST` | `/internal/v1/payment-sessions` | Create payment session |
| `GET` | `/internal/v1/payment-sessions/{sessionId}` | Get payment session by id |
| `POST` | `/internal/v1/audit-events` | Append audit event |
| `GET` | `/internal/v1/audit-events?resourceId=` | List audit events by resource id |

Not-found responses use RFC 7807 problem JSON via `bank-common-error` (`RESOURCE_NOT_FOUND`, HTTP 404).

Health: `/actuator/health` (liveness/readiness probes enabled).

## Schema ownership

Flyway migrations live only in this service: `src/main/resources/db/migration/`.
Tables: `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session`.
