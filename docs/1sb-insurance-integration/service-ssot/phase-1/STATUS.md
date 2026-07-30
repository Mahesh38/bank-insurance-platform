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

---

## Agent 2 — Phase 1 remediation (platform)

| Task | TD | Status | Notes |
|------|----|--------|-------|
| A2-1 Lombok on root subprojects | TD-001 | ✅ Done | `compileOnly` + `annotationProcessor` (+ test) via Spring Boot BOM |
| A2-2 Refactor shared builders | TD-001 | ✅ Done | `AuditEvent`, `ServiceErrorResponse`, `BankPrincipal` → `@Value` + `@Builder`; behaviour/tests preserved |
| A2-3 Remove duplicate `ErrorCode` | TD-005 | ✅ Done | Deleted unused `ErrorCode.java`; kept `ErrorCodes` |
| A2-4 Create `bank-common-secrets` | TD-002 | ✅ Done | Module `:libs:bank-common-secrets`, package `com.bank.common.secrets`, unit tests moved |
| A2-5 Retarget integration secrets | TD-002 | ✅ Done | Service depends on lib; config imports from lib; `adapter/secret` deleted |
| A2-6 Document Lombok vs records | TD-012 | ✅ Done | Convention in `libs/README.md` (+ TECH-DEBT TD-012) |
| A2-7 Fix AGENTS.md / Boot version drift | TD-008 | ✅ Done | AGENTS.md rewritten; libs README Boot **3.3.4** |

### Verify (Agent 2)

```bash
./gradlew :libs:bank-common-error:test :libs:bank-common-security:test \
  :libs:bank-common-audit:test :libs:bank-common-secrets:test
```

---

## Agent 3 — Phase 1 remediation (persistence split)

| Task | TD | Status | Notes |
|------|----|--------|-------|
| A3-1 Scaffold `1sb-persistence-service` | TD-003 | ✅ Done | Boot 3.3.4, port 8081, profiles local/uat/prod/test, actuator |
| A3-2 Move Flyway V1 | TD-011 | ✅ Done | `V1__init_schema.sql` moved; deleted from integration |
| A3-3 JPA entities + repos | TD-003 | ✅ Done | 6 tables; Lombok `@Getter`/`@Setter`/`@NoArgsConstructor` |
| A3-4 Internal REST + README | TD-004 | ✅ Done | `/internal/v1` jobs/offers/payment-sessions/audit-events; 404 problem JSON |
| A3-5 RestClient JobStorePort adapter | TD-004 | ✅ Done | `HttpJobStoreAdapter` + `insurance.persistence.base-url` |
| A3-6 Strip DB from integration | TD-011 | ✅ Done | No data-jpa/Flyway/drivers; empty entity/jpa packages removed |
| A3-7 ArchUnit JPA/Flyway forbid | TD-004 | ✅ Done | `mustNotImportJakartaPersistence`, `mustNotImportFlyway` |
| A3-8 Docs | TD-003/004/011 | ✅ Done | TECH-DEBT closed; this STATUS section |

### Verify (Agent 3)

```bash
./gradlew :services:1sb-persistence-service:test :services:1sb-integration-service:test
```

### Residual gaps

- No WireMock contract test for `HttpJobStoreAdapter` yet (context load + ArchUnit only).
- Poll-attempt / raw-payload HTTP endpoints not exposed (entities/repos only).
- Payment/audit ports on integration not wired (JobStorePort only).
