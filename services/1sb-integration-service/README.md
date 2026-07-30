# 1SB Integration Service

Spring Boot 3.3.x microservice that acts as the bank's integration adapter for the 1SilverBullet (1SB) insurance platform. Bank systems call this service; this service calls 1SB. Durable job/offer/payment/audit state is **not** stored here — it is owned by `1sb-persistence-service` over internal HTTP.

---

## Quick Start (Local)

### Prerequisites

- Java 21 (JDK)
- Gradle (uses wrapper — no install needed)
- **`1sb-persistence-service` running on port 8081** when exercising job-store / persistence HTTP calls

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

### 2. Start persistence (required for job store)

```bash
# From workspace root — separate terminal
./gradlew :services:1sb-persistence-service:bootRun --args='--spring.profiles.active=local'
# Listens on http://localhost:8081
```

### 3. Run the integration service

```bash
# From workspace root
./gradlew :services:1sb-integration-service:bootRun --args='--spring.profiles.active=local'
```

Or with the local profile automatically detected:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :services:1sb-integration-service:bootRun
```

The service starts on port **8080** by default. Persistence base URL defaults to `http://localhost:8081` (`insurance.persistence.base-url` / `INSURANCE_PERSISTENCE_BASE_URL`).

### 4. Check health

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP",...} — no embedded datasource / `db` health component
# (this service has no local DB; DB health lives on persistence :8081)
```

Additional actuator endpoints: `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/metrics`, `/actuator/prometheus`.

---

## Running Tests

```bash
# All tests (all modules)
./gradlew test

# Service tests only
./gradlew :services:1sb-integration-service:test

# Secrets provider unit tests (live in the shared lib)
./gradlew :libs:bank-common-secrets:test

# Full build (compile + test + package)
./gradlew :services:1sb-integration-service:build
```

### Test stack

| Layer | What runs |
|-------|-----------|
| ArchUnit | `ArchitectureTest` — hex-arch boundaries + forbid JPA/Flyway + forbid local `SecretProvider` implementations |
| Context load | `ApplicationContextTest` — full Spring Boot context (no datasource; persistence URL stubbed) |
| Secrets unit tests | `PropertiesSecretProviderTest`, `AwsSecretsManagerSecretProviderTest` in `:libs:bank-common-secrets` |

No H2/Flyway/Testcontainers are required for this service's CI unit/context tests. Real HTTP contract tests against persistence (e.g. WireMock) are follow-up work.

---

## Configuration

### Secrets source

Providers live in `:libs:bank-common-secrets` (`com.bank.common.secrets`). The integration service only **wires** them via `config/SecretProviderConfig` and `SecretsStartupValidator`.

Set via `INSURANCE_SECRETS_SOURCE` environment variable or `insurance.secrets.source` property.

| Value | When to use | How |
|-------|-------------|-----|
| `PROPERTIES` | Local dev / CI unit tests | `src/main/resources/application-local.properties` (gitignored) |
| `ENV` | UAT, Docker Compose | `ONESB_API_KEY`, `ONESB_API_SECRET`, `ONESB_DISTRIBUTOR_ID` env vars |
| `AWS_SECRETS_MANAGER` | Production | Phase 2 — stub in Phase 1 |

The service **fails at startup** if any required credential is missing (except in `test` profile).

### Persistence HTTP client

| Property | Default | Description |
|----------|---------|-------------|
| `insurance.persistence.base-url` / `INSURANCE_PERSISTENCE_BASE_URL` | `http://localhost:8081` | Base URL of `1sb-persistence-service` |

`HttpJobStoreAdapter` implements `JobStorePort` and calls the persistence service's `/internal/v1` API.

### Key properties

| Property | Default | Description |
|----------|---------|-------------|
| `INSURANCE_SECRETS_SOURCE` | `PROPERTIES` | Secrets backend to use |
| `INSURANCE_PERSISTENCE_BASE_URL` | `http://localhost:8081` | Persistence service base URL |
| `ONESB_BASE_URL` | `https://demo.api.1silverbullet.tech` | 1SB API base URL |
| `ONESB_CONNECT_TIMEOUT_MS` | `3000` | 1SB HTTP connect timeout |
| `ONESB_READ_TIMEOUT_MS` | `30000` | 1SB HTTP read timeout |

### Spring profiles

| Profile | Secrets source | Persistence |
|---------|----------------|-------------|
| `local` | `PROPERTIES` | HTTP → localhost:8081 |
| `uat` | `ENV` or `AWS_SECRETS_MANAGER` | HTTP → configured base URL |
| `prod` | `AWS_SECRETS_MANAGER` (required) | HTTP → configured base URL |

This service has **no local datasource**, Flyway, or H2. Schema ownership is entirely on `1sb-persistence-service`.

---

## Architecture

The service follows hexagonal architecture (ports & adapters):

```
api/v1 ──► application ──► domain/port/outbound ──► adapter/onesb (1SB HTTP)
                       ──► domain/port/outbound ──► adapter/persistence (HTTP → 1sb-persistence-service)
                       ──► config wires SecretProvider from :libs:bank-common-secrets
```

Key boundaries enforced by ArchUnit:
- `domain.*` must not import Spring annotations
- `application.*` / `api.*` must not import `adapter.*`
- Only `adapter.onesb.*` may import 1SB client types
- LOB handlers must not import `adapter.persistence.*`
- No JPA (`jakarta.persistence`) or Flyway in this service
- No class in this service may implement `com.bank.common.secrets.SecretProvider` (providers live in the lib only)

See [architecture doc](../../docs/1sb-insurance-integration/architecture/1sb-integration-service-architecture.md) for full design.

---

## Database / Flyway

**Not in this service.** Flyway migrations and the PostgreSQL/H2 schema live in [`1sb-persistence-service`](../1sb-persistence-service/README.md) (`src/main/resources/db/migration/`).

---

## Shared Libraries

The service depends on these shared libs (managed in the same Gradle multi-project):

| Module | Contents |
|--------|----------|
| `:libs:bank-common-error` | `ServiceError`, `ServiceErrorResponse`, `ServiceException`, `ErrorCodes` |
| `:libs:bank-common-security` | `BankPrincipal`, `BankPrincipalExtractor`, `Role`, `JwtClaims` |
| `:libs:bank-common-audit` | `AuditEvent`, `AuditEventPublisher`, `AuditActions`, `AuditOutcomes` |
| `:libs:bank-common-observability` | `MdcContext`, `MdcKeys`, `MetricNames`, `TraceHeaders` |
| `:libs:bank-common-secrets` | `SecretProvider`, `PropertiesSecretProvider`, `EnvSecretProvider`, `AwsSecretsManagerSecretProvider` |

---

## Phase Roadmap

- **Phase 1 (current):** Service scaffold, secrets lib wiring, HTTP persistence adapter ✅
- **Phase 1 Tier 1:** Term LOB quote → proposal → payment → status (TECH-004 onward)
- **Phase 2:** Real AWS Secrets Manager, WireMock/Testcontainers contract tests, Health/Motor LOBs
