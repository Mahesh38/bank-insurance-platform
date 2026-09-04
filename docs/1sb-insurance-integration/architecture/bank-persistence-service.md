# Bank Persistence Service — Integration Ops / Evidence contract

**Role:** Integration Ops / Evidence bounded context — **not** a platform-wide DB gateway  
**Module:** `services/bank-persistence-service`  
**Audience:** Service owners, tech leads, future context owners  
**Status:** Authoritative contract for this service. Scope constrained by [`ADR-019`](../../platform/architecture-review/08-architecture-decision-log.md#adr-019--persistence-ownership-is-per-bounded-context-bank-persistence-service-is-not-a-platform-wide-gateway) · [`ARCH-004`](../../platform/architecture-review/08-architecture-decision-log.md) · [`R0-LLD` §5.1](../../architecture/R0-LLD.md)  
**Related:** [TECH-LEAD-REVIEW-COMMON-PERSISTENCE](../service-ssot/phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) (Phase 1 split — **§2 "other MS later" superseded**) · [audit-consumer-service](./audit-consumer-service.md) · [`CR-015`](../../governance/change-requests/CR-015-persistence-ownership-per-context.md)

---

## 1. Purpose

`bank-persistence-service` owns schema migration (Flyway), JPA entities, and an internal HTTP API
for the **1SB adapter job/correlation store and audit ingest**.

It is **not** a private store of `1sb-integration-service` (that Phase 1 split stands).
It is **not** the write path for Customer, Lead, Consent, Suitability, Catalogue, Quotation,
Proposal, business Payment, Policy, Journey or Identity (`ADR-019`).

Bank apps and public APIs must **never** call it directly.

---

## 2. Consumers (closed set)

```text
                    ┌─────────────────────────────┐
  1sb-integration ──┤                             │
  service (:8080)   │   bank-persistence-service  │── Aurora / PostgreSQL
                    │   (:8081)                   │   schema: onesb + audit ingest
  audit-consumer ───┤   /internal/v1/*            │   Flyway for THESE tables only
  service (future)  │                             │
                    └─────────────────────────────┘

  Other bounded contexts ── own Flyway + own schema on the same R0 cluster
  (identity already does; Lead/Consent/Payment-context/Policy must too)
```

| Consumer | Role | Typical resources |
|----------|------|-------------------|
| `1sb-integration-service` | Bank-facing 1SB adapter | jobs, offers, payment-sessions (adapter link/URL); may append audit |
| `audit-consumer-service` (future) | Async audit sink | **only** `POST`/`GET` `/internal/v1/audit-events` |

There is **no** "other MS later" row. A new business service is not a consumer of this HTTP API.

**Rules:**

1. These two consumers talk **HTTP only** — no shared DataSource, no consumer-side Flyway **for these tables**.
2. This deployable owns Flyway **only** for the V1 tables listed in §3.
3. **No second audit database.** Audit-consumer persists via this same service.
4. Public bank APIs never expose or call `/internal/v1`.
5. Identity, Lead, Consent, Payment-context, Policy and every other R0 context own Flyway
   **in that service**, on the same Aurora cluster, with no cross-schema grants (`ADR-008`).

---

## 3. Flyway ownership

| Rule | Detail |
|------|--------|
| **Where (this context)** | `bank-persistence-service` → `src/main/resources/db/migration/` |
| **Who runs them** | This service on boot (local H2 `MODE=PostgreSQL`; uat/prod PostgreSQL) |
| **Who must not (these tables)** | `1sb-integration-service`, `audit-consumer-service` |
| **Who owns other schemas** | The bounded-context service that writes them — see `identity-authorization-service` |

Tables (V1): `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session`.

`payment_session` is the **1SB adapter's** payment-link session record. It is **not** the
Payment bounded context (`CAP-301`). Do not add Opportunity / Consent / Policy tables here.

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
| **Payments** | `/internal/v1/payment-sessions`, `/payment-sessions/{sessionId}` | `1sb-integration-service` (adapter session, not CAP-301) |

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
- Use the same `bank-persistence-service` base URL as the 1SB adapter
- **Not** own Flyway, JPA entities for `audit_event`, or a separate audit DB

See [audit-consumer-service.md](./audit-consumer-service.md) for the stub design.

---

## 6. Client configuration

| Key | Env | Default (local) |
|-----|-----|-----------------|
| `bank.persistence.base-url` | `BANK_PERSISTENCE_BASE_URL` | `http://localhost:8081` |

Consumers of **this** store (integration, audit-consumer) use these keys. A Lead or Consent
service does **not** become a client of this URL.

---

## 7. Non-goals

- Bank-facing or public REST on this service
- Moving Flyway for these tables into any consumer
- A second persistence deployable for audit only
- A Flyway-only mega-migrator holding every context's scripts (`ADR-019`)
- Adding Lead, Consent, Suitability, Catalogue, business Payment, Policy, Journey or Identity
  schemas to this service
- Changing `/internal/v1` path prefixes in this loop
