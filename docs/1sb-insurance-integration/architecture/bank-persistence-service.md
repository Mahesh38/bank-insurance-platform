# Bank Persistence Service — Platform Contract

**Role:** Platform common persistence service (not owned by 1SB integration)  
**Module:** `services/bank-persistence-service`  
**Audience:** Service owners, tech leads, future consumers  
**Status:** Authoritative multi-consumer contract for Phase 1+  
**Related:** [TECH-LEAD-REVIEW-COMMON-PERSISTENCE](../service-ssot/phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) · [audit-consumer-service](./audit-consumer-service.md)

---

## 1. Purpose

`bank-persistence-service` is the **bank platform common DB service**. It owns schema migration (Flyway), JPA entities, and an internal HTTP API for durable insurance/platform tables.

It is **not** a private store of `1sb-integration-service`. Multiple microservices call it over HTTP. Bank apps and public APIs must **never** call it directly.

---

## 2. Multi-consumer topology

```text
                    ┌─────────────────────────────┐
  1sb-integration ──┤                             │
  service (:8080)   │   bank-persistence-service  │── PostgreSQL
                    │   (:8081)                   │   (Flyway ONLY here)
  audit-consumer ───┤   /internal/v1/*            │
  service (future)  │                             │
  (other MS later) ─┤                             │
                    └─────────────────────────────┘
```

| Consumer | Role | Typical resources |
|----------|------|-------------------|
| `1sb-integration-service` | Bank-facing 1SB adapter | jobs, offers, payment-sessions; may append audit |
| `audit-consumer-service` (future) | Async audit sink | **only** `POST`/`GET` `/internal/v1/audit-events` |
| Other platform MS (later) | Shared durable state | Resource groups as needed |

**Rules:**

1. Consumers talk **HTTP only** — no shared DataSource, no consumer-side Flyway.
2. **One** persistence deployable owns **all** Flyway scripts for these shared tables.
3. There is **no second audit database**. Audit-consumer persists via this same service.
4. Public bank APIs never expose or call `/internal/v1`.

---

## 3. Flyway ownership

| Rule | Detail |
|------|--------|
| **Where** | Migrations live **only** under `bank-persistence-service` → `src/main/resources/db/migration/` |
| **Who runs them** | This service on boot (local H2 `MODE=PostgreSQL`; uat/prod PostgreSQL) |
| **Who must not** | `1sb-integration-service`, `audit-consumer-service`, or any other consumer |

Tables (V1): `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session`.

---

## 4. Internal HTTP contract (`/internal/v1`)

Base URL (local): `http://localhost:8081`  
Paths are **stable** — keep `/internal/v1/...` even when the module is renamed.

Auth expectation (Phase 1): **internal network only** (cluster/VPC). Stronger mTLS / service identity may be added later without changing paths.

### 4.1 Resource groups

| Group | Paths | Primary consumers |
|-------|-------|-------------------|
| **Jobs** | `/internal/v1/jobs`, `/jobs/{jobId}`, `/jobs/{jobId}/status`, `/jobs/{jobId}/offers` | `1sb-integration-service` |
| **Audit events** | `/internal/v1/audit-events` | `audit-consumer-service` (required); integration may also append |
| **Payments** | `/internal/v1/payment-sessions`, `/payment-sessions/{sessionId}` | `1sb-integration-service` |

### 4.2 Endpoint list

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/internal/v1/jobs` | Create integration job |
| `GET` | `/internal/v1/jobs/{jobId}` | Get job by id |
| `PATCH` | `/internal/v1/jobs/{jobId}/status` | Update job status |
| `GET` | `/internal/v1/jobs/{jobId}/offers` | List offers for a job |
| `POST` | `/internal/v1/jobs/{jobId}/offers` | Add an offer |
| `POST` | `/internal/v1/payment-sessions` | Create payment session |
| `GET` | `/internal/v1/payment-sessions/{sessionId}` | Get payment session |
| `POST` | `/internal/v1/audit-events` | Append audit event |
| `GET` | `/internal/v1/audit-events?resourceId=` | List audit events by resource id |

Not yet HTTP-exposed (schema/entities only; TD-015): poll-attempt, raw-payload.

Not-found → RFC 7807 problem JSON (`RESOURCE_NOT_FOUND`, HTTP 404) via `bank-common-error`.

Health: `/actuator/health` on port **8081**.

---

## 5. Audit-consumer contract (binding)

`audit-consumer-service` **must**:

- Call `POST /internal/v1/audit-events` to persist audit records
- Call `GET /internal/v1/audit-events?resourceId=` when it needs to list by resource
- Use the same `bank-persistence-service` base URL as other consumers
- **Not** own Flyway, JPA entities for `audit_event`, or a separate audit DB

See [audit-consumer-service.md](./audit-consumer-service.md) for the stub design.

---

## 6. Client configuration

| Key | Env | Default (local) |
|-----|-----|-----------------|
| `bank.persistence.base-url` | `BANK_PERSISTENCE_BASE_URL` | `http://localhost:8081` |

All consumers (integration, audit-consumer, future MS) use these platform keys.

---

## 7. Non-goals

- Bank-facing or public REST on this service
- Moving Flyway into any consumer
- A second persistence deployable for audit only
- Changing `/internal/v1` path prefixes in this loop
