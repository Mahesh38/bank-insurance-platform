# Bank Persistence Service

Platform **common** Spring Boot persistence service for the bank insurance estate.
It owns the shared PostgreSQL schema (**Flyway + JPA only here**) and exposes an
internal HTTP API. It is **not** owned by `1sb-integration-service`.

**Consumers (HTTP only):**

| Consumer | Status |
|----------|--------|
| `1sb-integration-service` | Current — jobs, offers, payments; may append audit |
| `audit-consumer-service` | Future — **must** use `POST`/`GET` `/internal/v1/audit-events` on this service |
| Other platform MS | Later |

Bank apps and public APIs must **not** call this service.

- Gradle module: `:services:bank-persistence-service`
- `spring.application.name`: `bank-persistence-service`
- Default port: **8081**
- Profiles: `local` (H2 `MODE=PostgreSQL`), `uat`, `prod`, `test`

## Quick start

```bash
./gradlew :services:bank-persistence-service:bootRun --args='--spring.profiles.active=local'
curl http://localhost:8081/actuator/health
```

## Internal API (`/internal/v1`)

### Jobs

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/internal/v1/jobs` | Create integration job |
| `GET` | `/internal/v1/jobs/{jobId}` | Get job by id |
| `PATCH` | `/internal/v1/jobs/{jobId}/status` | Update job status (optional `failureReason`, `externalReqId`, `completedAt`) |
| `GET` | `/internal/v1/jobs/{jobId}/offers` | List offers for a job |
| `POST` | `/internal/v1/jobs/{jobId}/offers` | Add an offer to a job |

### Payments

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/internal/v1/payment-sessions` | Create payment session |
| `GET` | `/internal/v1/payment-sessions/{sessionId}` | Get payment session by id |

### Audit events

Used by **audit-consumer** (required) and optionally by integration. **No second audit DB.**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/internal/v1/audit-events` | Append audit event |
| `GET` | `/internal/v1/audit-events?resourceId=` | List audit events by resource id |

Not yet HTTP-exposed (entities/repos + Flyway tables only; see TD-015): poll-attempt, raw-payload.

Not-found responses use RFC 7807 problem JSON via `bank-common-error` (`RESOURCE_NOT_FOUND`, HTTP 404).

Health: `/actuator/health` (liveness/readiness probes enabled) on port **8081**.

## Schema ownership (Flyway)

Flyway migrations live **only** in this service: `src/main/resources/db/migration/`.
Platform/common schema for shared tables — not “1SB integration’s private DB”.

Tables: `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session`.

Consumers (`1sb-integration-service`, future `audit-consumer-service`, …) must not ship Flyway scripts or a DataSource for these tables.

## Client config

| Key | Env | Local default |
|-----|-----|---------------|
| `bank.persistence.base-url` | `BANK_PERSISTENCE_BASE_URL` | `http://localhost:8081` |

## Further reading

- [Platform contract](../../docs/1sb-insurance-integration/architecture/bank-persistence-service.md)
- [Audit-consumer stub](../../docs/1sb-insurance-integration/architecture/audit-consumer-service.md)
