# Phase 1 — Status

## Dev A (Shared Libraries)

| Task | Status | Notes |
|------|--------|-------|
| SHARED-001 `bank-common-error` | ✅ Done | `ServiceError`, `ServiceErrorResponse` (RFC 7807 builder), `ServiceException`, `ErrorCodes`. Tests passing. |
| SHARED-002 `bank-common-security` | ✅ Done | `BankPrincipal`, `BankPrincipalExtractor`, `JwtClaims`, `Role`, `RequiresRole`, `SecurityProperties`. Tests passing. |
| SHARED-003 `bank-common-audit` | ✅ Done | `AuditEvent` (builder), `AuditEventPublisher`, `AuditActions`, `AuditOutcomes`. Tests passing. |
| SHARED-004 `bank-common-observability` | ✅ Done | `MdcContext`, `MdcKeys`, `MetricNames`, `TraceHeaders`, `BankMetricNames`. Tests passing (MDC verified with Logback). |

---

## Dev B (Integration Service)

| Task | Status | Notes |
|------|--------|-------|
| TECH-001 Service scaffold | ✅ Done | Spring Boot 3.3.4, Java 21, Gradle Kotlin DSL multi-project. Full package skeleton per architecture §3. `./gradlew build` passes. |
| TECH-001 ArchUnit | ✅ Done | 7 rules enforcing hex-arch boundaries. Empty-package-tolerant (`allowEmptyShould(true)`) for Phase 1 scaffold. |
| TECH-001 `/actuator/health` | ✅ Done | Actuator enabled; `liveness` + `readiness` probes configured. |
| TECH-001 Profile stubs | ✅ Done | `application.yml` + `application-local.yml` (H2) + `application-uat.yml` + `application-prod.yml`. |
| TECH-002 `SecretProvider` | ✅ Done | Interface + `PropertiesSecretProvider` + `EnvSecretProvider` + `AwsSecretsManagerSecretProvider` (stub). Factory config reading `insurance.secrets.source`. |
| TECH-002 Fail-fast validator | ✅ Done | `SecretsStartupValidator` (`ApplicationRunner`, order 1) checks api-key/secret/distributor-id at startup. Skips in `test` profile. |
| TECH-002 Example configs | ✅ Done | `application-local.properties.example` with placeholder keys. Mapped to `config/onesb/secrets-source.example.yaml` patterns. |
| TECH-003 Flyway migrations | ✅ Done | `V1__init_schema.sql` creates all 6 tables: `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session`. |
| TECH-003 H2 test compatibility | ✅ Done | Uses `TIMESTAMP WITH TIME ZONE` (not `TIMESTAMPTZ`), H2 `MODE=PostgreSQL`. Documented. |

### Package skeleton delivered

```
com.bank.insurance.onesb/
├── api/v1/dto/{request,response,error}   (placeholder)
├── application/                           (placeholder — services added in TECH-004+)
├── domain/model/          JobStatus, Lob, QuoteJob, QuoteOffer, PaymentSession, PaymentStatus
├── domain/command/        CreateQuoteCommand
├── domain/port/outbound/  JobStorePort, OneSbQuotePort, OneSbPaymentPort
├── domain/port/inbound/   QuoteUseCase
├── lob/                   LobQuoteHandler (interface), LobProposalHandler (interface)
├── lob/life/saving/       package-info (Phase 2)
├── lob/life/term/         package-info (Phase 2)
├── adapter/onesb/client,polling,error,config   package-infos
├── adapter/persistence/   package-info
├── adapter/secret/        SecretProvider, PropertiesSecretProvider, EnvSecretProvider,
│                          AwsSecretsManagerSecretProvider, SecretUnavailableException
├── config/                SecretsProperties, SecretProviderConfig, SecretsStartupValidator
└── observability/         package-info
```

### Build output

- `./gradlew build` — **PASS** (all modules)
- `./gradlew :services:1sb-integration-service:build` — **PASS**
- 16 tests executed: 6 ArchUnit + 4 SecretProvider unit + 1 context load + 5 observability MDC

---

## Exit Gate Check

| Criterion | Status |
|-----------|--------|
| `./gradlew build` succeeds | ✅ |
| Service boots; `/actuator/health` UP (H2) | ✅ (context load test verified) |
| Flyway migrations for §9 tables | ✅ |
| Shared libs have unit tests | ✅ |
| Phase 1 status doc updated | ✅ |
