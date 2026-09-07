# Shared Libraries — `libs/`

These are Java 21 / Spring Boot **3.5.x** compatible shared libraries for the bank insurance platform.
They are **not Spring Boot applications** — they are plain JARs consumed by services via
`implementation(project(":libs:..."))`.

| Module | Owns |
|--------|------|
| `bank-common-domain` | Bank-owned domain models (`Lob`, jobs, payments, status) — **not** 1SB wire types |
| `bank-common-error` | Error catalogue, problem+JSON, shared handler |
| `bank-common-security` | Security helpers |
| `bank-common-audit` | Audit event model + publisher SPI |
| `bank-common-observability` | MDC / metrics helpers |
| `bank-common-secrets` | SecretProvider SPI |

1SB provider DTOs and HTTP clients stay in `services/1sb-integration-service/.../adapter.onesb.*`.

---

## Boilerplate convention (TD-012)

| Layer | Preference |
|-------|------------|
| Domain models / commands | Java `record` |
| Config properties | Java `record` + `@ConfigurationProperties` |
| Shared immutable events / problem JSON / principal | Lombok `@Value` + `@Builder` |
| JPA entities (persistence service) | Lombok `@Getter` `@Setter` `@NoArgsConstructor` `@AllArgsConstructor` (or `@Builder` where useful) |
| Public bank API DTOs | Prefer `record`; Lombok only if Jackson/validation needs mutability |

**Do not** Lombok domain records. Root `build.gradle.kts` supplies Lombok (`compileOnly` + `annotationProcessor`, including test) to all subprojects via the Spring Boot BOM.

---

## `bank-common-error` — SHARED-001

**Package:** `com.bank.common.error`

RFC7807-style error model shared by all bank platform services.

| Class / Type             | Description |
|--------------------------|-------------|
| `ErrorCodes`             | String constants: `VALIDATION_ERROR`, `UPSTREAM_BUSINESS_ERROR`, `UPSTREAM_AUTH_FAILURE`, `UPSTREAM_UNAVAILABLE`, etc. |
| `ServiceError`           | A single field/code/message error entry (used in `errors[]` array). |
| `ServiceErrorResponse`   | Full RFC7807 response envelope: `type`, `title`, `status`, `detail`, `code`, `retryable`, `upstreamCode?`, `errors[]`. Has factory methods for common cases. Lombok `@Value` + `@Builder`. |
| `ServiceException`       | Unchecked exception wrapping a `ServiceErrorResponse`. Factory methods: `validation()`, `upstreamBusiness()`, `upstreamAuth()`, `upstreamUnavailable()`, `forbidden()`, `internal()`. |

---

## `bank-common-security` — SHARED-002

**Package:** `com.bank.common.security`

JWT principal model and security configuration properties.  
**Does not** implement JWT validation — services wire this into Spring Security OAuth2 Resource Server.

| Class / Type             | Description |
|--------------------------|-------------|
| `BankPrincipal`          | Immutable authenticated actor: `actorId`, `employeeId`, `branchCode`, `roles`, `customerId?`. Lombok `@Value` + `@Builder`. `toString()` emits only `actorId`. |
| `Role`                   | Enum: `RM`, `BRANCH_MANAGER`, `OPS`, `AUDITOR`, `SERVICE_ACCOUNT`. |
| `BankPrincipalExtractor` | Stateless helper: `fromClaims(Map<String,Object>)` builds a `BankPrincipal` from validated JWT claims. |
| `JwtClaims`              | JWT claim name constants: `actor_id`, `emp_id`, `branch_code`, `roles`, `cust_id`. |
| `SecurityProperties`     | Interface for `bank.security.jwt.*` config properties: `issuer`, `audience`, `jwksUri`. Implement with `@ConfigurationProperties`. |
| `@RequiresRole`          | Method/type annotation declaring required roles. Services enforce via AOP or Spring Security. |

---

## `bank-common-audit` — SHARED-003

**Package:** `com.bank.common.audit`

Audit event model and publisher contract matching architecture §8.3.

| Class / Type           | Description |
|------------------------|-------------|
| `AuditEvent`           | Immutable audit record: `eventId`, `timestamp`, `actorId`, `actorType`, `action`, `resourceType`, `resourceId`, `outcome`, `lob`, `journeyId`, `distributorId`, `agentId`, `traceId`, `metadata`. Lombok `@Value` + `@Builder`. Auto-generates `eventId`/`timestamp` if not provided. |
| `AuditEventPublisher`  | Interface: `void publish(AuditEvent)`. Implement in service (DB, Kafka, or both). |
| `AuditActions`         | String constants: `QUOTE_JOB_CREATED`, `QUOTE_COMPLETED`, `PROPOSAL_SUBMITTED`, `PAYMENT_SESSION_CREATED`, `APPLICATION_STATUS_CHECKED`, etc. |
| `AuditOutcomes`        | Constants: `SUCCESS`, `FAILURE`, `REJECTED`, `TIMEOUT`, `PENDING`. |

Audit records are **append-only** — never mutate a published event.

---

## `bank-common-observability` — SHARED-004

**Package:** `com.bank.common.observability`

MDC keys, metric names, and trace header constants for the bank observability platform.

| Class / Type    | Description |
|-----------------|-------------|
| `MdcKeys`       | SLF4J MDC key constants: `jobId`, `lob`, `actorId`, `traceId`, `spanId`, `journeyId`, `correlationId`. |
| `MetricNames`   | Micrometer metric name constants: `bank.onesb.job.created`, `bank.onesb.http.request.duration`, etc. |
| `TraceHeaders`  | HTTP header name constants: `traceparent`, `X-Correlation-Id`, `X-Request-Id`. |
| `MdcContext`    | Helper for safe MDC management: `put()`, `clear()`, `with(Map, Runnable)`, `supply(Map, Supplier)` — always restores previous MDC state. |

---

## `bank-common-secrets` — TD-002

**Package:** `com.bank.common.secrets`

Reusable secrets SPI for 1SB (and future) credentials. Spring wiring stays in each consuming service.

| Class / Type                     | Description |
|----------------------------------|-------------|
| `SecretProvider`                 | Interface: `getApiKey()`, `getApiSecret()`, `getDistributorId()`. |
| `PropertiesSecretProvider`       | Resolves from Spring `Environment` properties (`onesb.api-key`, etc.). |
| `EnvSecretProvider`              | Resolves from `ONESB_API_KEY` / `ONESB_API_SECRET` / `ONESB_DISTRIBUTOR_ID`. |
| `AwsSecretsManagerSecretProvider`| Phase 1 stub — throws `UnsupportedOperationException` (real AWS SM = Phase 2). |
| `SecretUnavailableException`     | Thrown when a required secret cannot be resolved. |

Services depend on this lib and keep `@Configuration` / `@ConfigurationProperties` / startup validators locally.

---

## Gradle coordinates

```kotlin
// In your service build.gradle.kts:
dependencies {
    implementation(project(":libs:bank-common-error"))
    implementation(project(":libs:bank-common-security"))
    implementation(project(":libs:bank-common-audit"))
    implementation(project(":libs:bank-common-observability"))
    implementation(project(":libs:bank-common-secrets"))
}
```

The root `build.gradle.kts` applies the Spring Boot **3.3.4** BOM to all subprojects, so no
explicit Spring/SLF4J/Lombok versions are needed in individual modules.
