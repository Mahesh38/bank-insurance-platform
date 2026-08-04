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

### Raw payloads (COMP-003 — encrypted at rest)

| Method | Path | Description |
|--------|------|--------------|
| `POST` | `/internal/v1/raw-payloads` | Encrypt (AES-256-GCM) + store a raw 1SB request/response body; returns metadata only |
| `GET` | `/internal/v1/raw-payloads/{payloadId}` | Decrypt + return one payload (dispute/audit use) |
| `GET` | `/internal/v1/raw-payloads?jobId=` | List a job's captured payloads — metadata only, never plaintext |

Encryption key: `raw-payload.encryption.key` / `RAW_PAYLOAD_ENCRYPTION_KEY` (base64, 32 bytes).
No default in uat/prod — missing/invalid key fails service startup
(`RawPayloadEncryptionService`). Default retention `raw-payload.retention.years` /
`RAW_PAYLOAD_RETENTION_YEARS` (default 7).

Not yet HTTP-exposed (entity/repo + Flyway table only; see TD-015): poll-attempt.

Not-found responses use RFC 7807 problem JSON via `bank-common-error` (`RESOURCE_NOT_FOUND`, HTTP 404).

Health: `/actuator/health` (liveness/readiness probes enabled) on port **8081**.

API docs (Swagger / OpenAPI): `http://localhost:8081/swagger-ui.html`, `http://localhost:8081/v3/api-docs`.

## Schema ownership (Flyway)

Flyway migrations live **only** in this service: `src/main/resources/db/migration/`.
Platform/common schema for shared tables — not “1SB integration’s private DB”.

Tables: `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session`.

Consumers (`1sb-integration-service`, future `audit-consumer-service`, …) must not ship Flyway scripts or a DataSource for these tables.

## Client config

| Key | Env | Local default |
|-----|-----|---------------|
| `bank.persistence.base-url` | `BANK_PERSISTENCE_BASE_URL` | `http://localhost:8081` |

## Docker

```bash
# From the repo root — Dockerfile expects the multi-module build context
docker build -f services/bank-persistence-service/Dockerfile -t bank-persistence-service .
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=uat \
  -e DATASOURCE_URL=jdbc:postgresql://host:5432/bank_persistence \
  -e DATASOURCE_USERNAME=... -e DATASOURCE_PASSWORD=... \
  -e RAW_PAYLOAD_ENCRYPTION_KEY=$(openssl rand -base64 32) \
  bank-persistence-service
```

Or `docker compose up --build` from the repo root to run this service + Postgres +
`1sb-integration-service` together (see root `docker-compose.yml` / `.env.example`).

Runs on embedded Tomcat (`spring-boot-starter-web`, no exclusions) — no external servlet
container needed either way.

## Further reading

- [Platform contract](../../docs/1sb-insurance-integration/architecture/bank-persistence-service.md)
- [Audit-consumer stub](../../docs/1sb-insurance-integration/architecture/audit-consumer-service.md)
