# Phase 1 — Status

> **Phase 1 foundations delivered.** Remediations applied per [TECH-LEAD-REVIEW](./TECH-LEAD-REVIEW.md): Lombok on shared builders, secrets extracted to `:libs:bank-common-secrets`, persistence split into `bank-persistence-service` (platform-common Flyway/JPA/H2; not 1SB-owned) with HTTP `JobStorePort` on the integration service. Historical Dev A/B sections below are retained for audit trail; **TECH-002** and **TECH-003** outcomes are **superseded** by the remediation (secrets → lib; Flyway → persistence service).

---

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
| TECH-001 ArchUnit | ✅ Done | Hex-arch boundary rules (+ post-remediation JPA/Flyway forbid, no local `SecretProvider` impl). Empty-package-tolerant (`allowEmptyShould(true)`) where scaffold packages are empty. |
| TECH-001 `/actuator/health` | ✅ Done | Actuator enabled; `liveness` + `readiness` probes configured. No embedded `db` health (no datasource on integration). |
| TECH-001 Profile stubs | ✅ Done | `application.yml` + `application-local.yml` + `application-uat.yml` + `application-prod.yml` (no H2/datasource on integration after remediation). |
| TECH-002 `SecretProvider` | ✅ Done — **superseded** | Originally in `adapter/secret`. **Superseded:** providers live in `:libs:bank-common-secrets`; integration only wires via `config/*`. |
| TECH-002 Fail-fast validator | ✅ Done | `SecretsStartupValidator` (`ApplicationRunner`, order 1) checks api-key/secret/distributor-id at startup. Skips in `test` profile. |
| TECH-002 Example configs | ✅ Done | `application-local.properties.example` with placeholder keys. Mapped to `config/onesb/secrets-source.example.yaml` patterns. |
| TECH-003 Flyway migrations | ✅ Done — **superseded** | Originally on integration. **Superseded:** `V1__init_schema.sql` and schema ownership moved to `1sb-persistence-service`. |
| TECH-003 H2 test compatibility | ✅ Done — **superseded** | H2 `MODE=PostgreSQL` now applies on the **persistence** service only. |

### Package skeleton delivered (post-remediation)

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
├── adapter/persistence/   HttpJobStoreAdapter (+ DTOs) — HTTP client to 1sb-persistence-service
├── config/                SecretsProperties, SecretProviderConfig, SecretsStartupValidator,
│                          PersistenceClientProperties (wires lib SecretProvider + persistence URL)
└── observability/         package-info

Secrets providers (SecretProvider, Properties/Env/Aws implementations):
  → :libs:bank-common-secrets (com.bank.common.secrets) — not under adapter/secret
```

### Build output

- `./gradlew build` — **PASS** (all modules)
- `./gradlew :services:1sb-integration-service:build` — **PASS**
- ArchUnit + context load on integration; secrets unit tests on `:libs:bank-common-secrets`; persistence service has its own tests

---

## Exit Gate Check

| Criterion | Status |
|-----------|--------|
| `./gradlew build` succeeds | ✅ |
| Service boots; `/actuator/health` UP | ✅ (context load test verified; no embedded `db` on integration) |
| Flyway migrations for §9 tables | ✅ (owned by `1sb-persistence-service`; met via persistence split) |
| Shared libs have unit tests | ✅ (includes `bank-common-secrets`) |
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

> **Rename note:** Module later renamed to `bank-persistence-service` (platform-common); historical A3 names below retained as audit trail.

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

- WireMock / full E2E deferred → **TD-014** (MockRestServiceServer covers create + find).
- Poll-attempt / raw-payload HTTP → **TD-015**.
- Payment/audit ports on integration not wired (JobStorePort only) → **TD-009**.

---

## Agent 3 — confirmation pass

Second confirmation circle (senior #1–#5 already PASS in code). Hygiene + residual tracking only.

| Check | Result | Notes |
|-------|--------|-------|
| No `db/migration` under integration | ✅ | Flyway only under `1sb-persistence-service` |
| No empty `adapter/persistence/entity` or `jpa` | ✅ | HTTP adapter + `dto` only |
| Empty `adapter/secret` dirs | ✅ | Removed (`rmdir`); no `.gitkeep` |
| `HttpJobStoreAdapter` unit test | ✅ | `MockRestServiceServer`: `createJob` + `findQuoteJob` |
| Persistence README endpoints / Flyway / :8081 | ✅ | All `/internal/v1` routes listed; Flyway ownership noted |
| TD-013 / TD-014 / TD-015 | ✅ | TD-013 Closed (Agent 2 docs); TD-014/015 Deferred Phase 2 |

### Verify (confirmation)

```bash
./gradlew :services:1sb-persistence-service:test :services:1sb-integration-service:test
# Prefer: ./gradlew build
```

---

## Agent 2 — Common persistence docs/contract (B2)

Authority: [TECH-LEAD-REVIEW-COMMON-PERSISTENCE](./TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) · [REFACTOR-COMMON-PERSISTENCE](./REFACTOR-COMMON-PERSISTENCE.md)

| Task | TD | Status | Notes |
|------|----|--------|-------|
| B2-2 Platform contract doc | TD-017, TD-020 | ✅ Done | `architecture/bank-persistence-service.md` — multi-consumer, Flyway-only, resource groups, audit-events binding |
| B2-3 Persistence README rewrite | TD-017 | ✅ Done | Title **Bank Persistence Service**; jobs / payments / audit-events sections |
| B2-4 SSOT / AGENTS / integration README | TD-017 | ✅ Done | Common-service language; `bank.persistence.base-url`; audit-consumer as future consumer |
| B2-5 Flyway V1 header | TD-017 | ✅ Done | Platform/common schema owned by bank-persistence-service |
| B2-6 Audit-consumer stub | TD-021 | ✅ Done | `architecture/audit-consumer-service.md` — HTTP only; no second audit DB |
| B2-7 STATUS + TECH-DEBT | — | ✅ Done | TD-017, TD-020, TD-021 **Closed**; living-docs links in SSOT README |

Physical module rename / packages / client keys → **Agent 3** (TD-016, TD-018, TD-019) — **done** (see Agent 3 B3 section below).

---

## Agent 3 — Common persistence rename (B3)

Authority: [TECH-LEAD-REVIEW-COMMON-PERSISTENCE](./TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) · [REFACTOR-COMMON-PERSISTENCE](./REFACTOR-COMMON-PERSISTENCE.md)

| Task | TD | Status | Notes |
|------|----|--------|-------|
| B3-1 Module rename | TD-016 | ✅ Done | `services/bank-persistence-service`; JAR + `spring.application.name` |
| B3-2 Package move + flatten | TD-018 | ✅ Done | `com.bank.persistence` + `entity` / `repo`; `@EntityScan` / `@EnableJpaRepositories` |
| B3-3 Client config | TD-019 | ✅ Done | `bank.persistence.base-url` / `BANK_PERSISTENCE_BASE_URL`; YAML + tests |
| B3-4 Grep cleanup | TD-016/019 | ✅ Done | No code/build refs to `1sb-persistence-service` or `insurance.persistence.base-url` |
| B3-5 `./gradlew build` | — | ✅ Done | BUILD SUCCESSFUL; ArchUnit still forbids JPA/Flyway in integration |
| B3-6 TECH-DEBT + STATUS | — | ✅ Done | TD-016, TD-018, TD-019 **Closed** |

### Verify (Agent 3 B3)

```bash
./gradlew build
# or targeted:
./gradlew :services:bank-persistence-service:test :services:1sb-integration-service:test
```

### Residual gaps

- Historical docs (prior TL review / confirmation) still mention old module name — intentional audit trail.
- WireMock E2E / poll-attempt HTTP unchanged → **TD-014**, **TD-015**.
- Full audit-consumer Boot app not scaffolded (doc-only per TD-021) — intentional.

---

## Agent 3 — confirmation pass (common persistence)

Confirmation circle #2 — senior comment **PASS** in code. Hygiene only; Agent 2 banner/SSOT edits retained.

| Check | Result | Notes |
|-------|--------|-------|
| No `1sb-persistence-service` / `insurance.persistence` in `services/`, `libs/`, `settings.gradle.kts`, `AGENTS.md`, active service READMEs | ✅ | Historical phase-1 TL docs retained as audit trail |
| Persistence README **Consumers** | ✅ | `1sb-integration-service` (jobs/payments); future `audit-consumer-service` (audit-events only); both HTTP; no Flyway in consumers |
| Module / package / client key | ✅ | `bank-persistence-service`, `com.bank.persistence`, `bank.persistence.base-url` |

### Verify (confirmation — common persistence)

```bash
./gradlew :services:bank-persistence-service:test :services:1sb-integration-service:test
```
