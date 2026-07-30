# 1SB Integration Service

Spring Boot 3.3.x microservice that acts as the bank's integration adapter for the 1SilverBullet (1SB) insurance platform. Bank systems call this service; this service calls 1SB.

---

## Quick Start (Local)

### Prerequisites

- Java 21 (JDK)
- Gradle (uses wrapper — no install needed)

### 1. Copy and fill local secrets

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
# Edit and set:
#   onesb.api-key=<your sandbox API key>
#   onesb.api-secret=<your sandbox API secret>
#   onesb.distributor-id=<your distributor ID>
```

`application-local.properties` is **gitignored** — never commit real credentials.

### 2. Run the service

```bash
# From workspace root
./gradlew :services:1sb-integration-service:bootRun --args='--spring.profiles.active=local'
```

Or with the local profile automatically detected:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :services:1sb-integration-service:bootRun
```

The service starts on port **8080** by default.

### 3. Check health

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},...}}
```

Additional actuator endpoints: `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/metrics`, `/actuator/prometheus`.

---

## Running Tests

```bash
# All tests (all modules)
./gradlew test

# Service tests only
./gradlew :services:1sb-integration-service:test

# Full build (compile + test + package)
./gradlew :services:1sb-integration-service:build
```

### Test stack

| Layer | What runs |
|-------|-----------|
| Unit | `PropertiesSecretProviderTest`, `AwsSecretsManagerSecretProviderTest` |
| ArchUnit | `ArchitectureTest` — 7 hex-arch boundary rules |
| Context load | `ApplicationContextTest` — full Spring Boot context with H2 in-memory DB |

Tests use H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`). No external DB or Redis needed for CI. Real Testcontainers-based integration tests are targeted for Phase 2.

---

## Configuration

### Secrets source

Set via `INSURANCE_SECRETS_SOURCE` environment variable or `insurance.secrets.source` property.

| Value | When to use | How |
|-------|-------------|-----|
| `PROPERTIES` | Local dev / CI unit tests | `src/main/resources/application-local.properties` (gitignored) |
| `ENV` | UAT, Docker Compose | `ONESB_API_KEY`, `ONESB_API_SECRET`, `ONESB_DISTRIBUTOR_ID` env vars |
| `AWS_SECRETS_MANAGER` | Production | Phase 2 — stub in Phase 1 |

The service **fails at startup** if any required credential is missing (except in `test` profile).

### Key properties

| Property | Default | Description |
|----------|---------|-------------|
| `INSURANCE_SECRETS_SOURCE` | `PROPERTIES` | Secrets backend to use |
| `ONESB_BASE_URL` | `https://demo.api.1silverbullet.tech` | 1SB API base URL |
| `ONESB_CONNECT_TIMEOUT_MS` | `3000` | 1SB HTTP connect timeout |
| `ONESB_READ_TIMEOUT_MS` | `30000` | 1SB HTTP read timeout |
| `DATASOURCE_URL` | H2 in-memory | PostgreSQL JDBC URL for UAT/prod |

### Spring profiles

| Profile | Database | Secrets source |
|---------|----------|----------------|
| `local` | H2 in-memory (PostgreSQL mode) | `PROPERTIES` |
| `uat` | PostgreSQL (via `DATASOURCE_URL`) | `ENV` or `AWS_SECRETS_MANAGER` |
| `prod` | PostgreSQL | `AWS_SECRETS_MANAGER` (required) |

---

## Architecture

The service follows hexagonal architecture (ports & adapters):

```
api/v1 ──► application ──► domain/port/outbound ──► adapter/onesb (1SB HTTP)
                       ──► domain/port/outbound ──► adapter/persistence (PostgreSQL)
                       ──► adapter/secret (credentials)
```

Key boundaries enforced by ArchUnit:
- `domain.*` must not import Spring annotations
- `application.*` must not import `adapter.*`
- `api.*` must not import `adapter.*` 
- Only `adapter.onesb.*` may import 1SB client types

See [architecture doc](../../docs/1sb-insurance-integration/architecture/1sb-integration-service-architecture.md) for full design.

---

## Database Migrations (Flyway)

Migrations live in `src/main/resources/db/migration/`.

| Version | Description |
|---------|-------------|
| V1 | Initial schema: `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session` |

For CI, H2 in `MODE=PostgreSQL` is used. For UAT/prod, PostgreSQL 16. Use `TIMESTAMP WITH TIME ZONE` (not `TIMESTAMPTZ`) for H2 compatibility.

---

## Shared Libraries

The service depends on these shared libs (managed in the same Gradle multi-project):

| Module | Contents |
|--------|----------|
| `:libs:bank-common-error` | `ServiceError`, `ServiceErrorResponse`, `ServiceException`, `ErrorCodes` |
| `:libs:bank-common-security` | `BankPrincipal`, `BankPrincipalExtractor`, `Role`, `JwtClaims` |
| `:libs:bank-common-audit` | `AuditEvent`, `AuditEventPublisher`, `AuditActions`, `AuditOutcomes` |
| `:libs:bank-common-observability` | `MdcContext`, `MdcKeys`, `MetricNames`, `TraceHeaders` |

---

## Phase Roadmap

- **Phase 1 (current):** Service scaffold, secrets wiring, Flyway schema ✅
- **Phase 1 Tier 1:** Term LOB quote → proposal → payment → status (TECH-004 onward)
- **Phase 2:** Real AWS Secrets Manager, Testcontainers integration tests, Health/Motor LOBs
