# Tech Debt Log — 1SB Insurance Platform

**Owner:** Tech Lead  
**Branch:** `cursor/phase1-foundations-c259`  
**Last reviewed:** 2026-07-30  
**Source:** Senior engineer review + Phase 1 code review

Severity: **P0** = blocks Phase 2 / multi-service reuse · **P1** = fix this sprint · **P2** = track before prod · **P3** = hygiene

---

## Open items

| ID | Sev | Title | Origin | Status | Owner |
|----|-----|-------|--------|--------|-------|
| TD-001 | P1 | Adopt Lombok; remove hand-rolled builders in shared libs | Senior #1 | Open → assigned | Agent 2 |
| TD-002 | P0 | Extract secrets SPI to `bank-common-secrets` reusable lib | Senior #2 | Open → assigned | Agent 2 |
| TD-003 | P0 | Split DB ownership into `1sb-persistence-service` | Senior #3–4 | Open → assigned | Agent 3 |
| TD-004 | P0 | Integration ↔ persistence communicate via HTTP only | Senior #5 | Open → assigned | Agent 3 (after TD-003) |
| TD-005 | P2 | Duplicate `ErrorCode` vs `ErrorCodes` in bank-common-error | Phase 1 review | Open | Agent 2 (with TD-001) |
| TD-006 | P2 | AWS Secrets Manager provider is stub; prod profile will fail-fast | Phase 1 review | Deferred Phase 2 | Backlog |
| TD-007 | P3 | ArchUnit rules use `allowEmptyShould(true)` — tighten when packages fill | Phase 1 review | Deferred | Phase 2+ |
| TD-008 | P3 | Docs drift: AGENTS.md “placeholder”; libs README Boot 3.3.5 vs 3.3.4 | Phase 1 review | Open | Agent 2 |
| TD-009 | P2 | Missing domain ports vs arch (Proposal/Status/Master/Audit/Idempotency) | Phase 1 review | Deferred Phase 2 | Backlog |
| TD-010 | P2 | No Redis / idempotency / cache adapter yet | Arch gap | Deferred Phase 2 | Backlog |
| TD-011 | P1 | Integration service must not own Flyway or JPA after split | Senior #3–4 | Open → assigned | Agent 3 |
| TD-012 | P2 | Convention for future JPA entities / API DTOs (Lombok vs records) | Senior #1 + TL | Open → assigned | Tech Lead (doc) + Agent 2 |

---

## TD-001 — Lombok adoption

**Problem:** Hand-rolled immutable builders inflate shared libs (`AuditEvent` ~119 LOC, `ServiceErrorResponse` ~157 LOC, `BankPrincipal` ~92 LOC). Domain already uses Java records correctly; future JPA/DTOs will amplify boilerplate without a convention.

**Fix:**
1. Add Lombok to root `subprojects` (`compileOnly` + `annotationProcessor`; test same).
2. Refactor the three builder classes to `@Value` / `@Builder` (or `@Getter` + `@Builder` with required-args validation preserved).
3. Keep **Java records** for domain models and config properties (KISS — do not Lombok records).
4. Document convention in `libs/README.md` and TD-012.

**DoD:** Build green; existing unit tests pass without API breakage for callers; LOC of builders clearly reduced.

---

## TD-002 — `bank-common-secrets` lib

**Problem:** Secrets SPI lives under `services/1sb-integration-service/.../adapter/secret` — not reusable by other microservices (e.g. persistence, future BFFs).

**Fix:**
1. New module `:libs:bank-common-secrets` with package `com.bank.common.secrets`.
2. Move: `SecretProvider`, `PropertiesSecretProvider`, `EnvSecretProvider`, `AwsSecretsManagerSecretProvider` (stub), `SecretUnavailableException`.
3. Optional thin Spring `@Configuration` / properties record can stay in the consuming service **or** live as optional auto-config in the lib (`spring.factories` / `AutoConfiguration.imports`) — prefer **SPI in lib + Spring wiring in each service** for KISS (no magic auto-config unless needed).
4. Delete secrets classes from integration service `adapter/secret`; wire via dependency on the lib.
5. Update ArchUnit: secrets may live in lib; service must not re-implement providers.

**DoD:** Lib has unit tests; integration service boots with same PROPERTIES/ENV behaviour; no duplicate provider code in service.

---

## TD-003 / TD-011 — Persistence service + Flyway ownership

**Problem:** Senior requires DB operations in a dedicated service; Flyway must live with that service. Phase 1 put Flyway + JPA starter on the integration service (entities empty).

**Target topology:**

```text
Bank → 1sb-integration-service ──HTTP──► 1sb-persistence-service ──► PostgreSQL
              │                                    │
              │ (1SB HTTP later)                   └── Flyway + JPA only here
              └── no Flyway, no DataSource for domain tables
```

**Fix:**
1. Create `:services:1sb-persistence-service` (Spring Boot 3.3.4, Java 21).
2. Move `V1__init_schema.sql` to persistence service `src/main/resources/db/migration/`.
3. Scaffold internal HTTP API (v1) for job / offer / poll-attempt / raw-payload / audit-event / payment-session CRUD (minimal stubs OK for Phase 1 refactor — real implementations fill in Phase 2).
4. JPA entities + repositories under persistence service only.
5. Remove Flyway + JPA + DB drivers from integration service; remove empty `adapter/persistence` entity/jpa packages (replace with HTTP adapter).
6. Profiles: local H2 / uat+prod PostgreSQL on persistence service only.

**DoD:** Persistence service migrates schema on boot; integration service has **zero** Flyway migrations and **zero** `spring-boot-starter-data-jpa`.

---

## TD-004 — HTTP between services

**Problem:** After split, modules must not share a DB; communicate via HTTP.

**Fix:**
1. Persistence service exposes REST under `/internal/v1/...` (not public bank API).
2. Integration service: `RestClient` (or WebClient) adapter implementing `JobStorePort` (and later audit/payment ports) calling persistence.
3. Config: `insurance.persistence.base-url` (local default `http://localhost:8081`).
4. ArchUnit: integration service must not import `jakarta.persistence` / Flyway.
5. Contract stub OpenAPI or markdown API list in persistence service README.

**DoD:** Context-load / ArchUnit prove no JPA in integration; HTTP client bean present; WireMock or stub test for JobStorePort adapter preferred (minimum: compile + ArchUnit).

---

## TD-005 — ErrorCode duplication

Delete unused `ErrorCode` if present; keep single `ErrorCodes` constant class.

---

## TD-006…TD-010 — Deferred notes

Track only; do not block this refactor PR. TD-006 remains Phase 2 (real AWS SM). TD-009/010 stay Phase 2 backlog.

---

## TD-012 — Boilerplate convention (binding)

| Layer | Preference |
|-------|------------|
| Domain models / commands | Java `record` |
| Config properties | Java `record` + `@ConfigurationProperties` |
| Shared immutable events / problem JSON / principal | Lombok `@Value` + `@Builder` |
| JPA entities (persistence service) | Lombok `@Getter` `@Setter` `@NoArgsConstructor` `@AllArgsConstructor` (or `@Builder` where useful) |
| Public bank API DTOs | Prefer `record`; Lombok only if Jackson/validation needs mutability |

---

## Closed items

_None yet — update after Agent 2 / Agent 3 land fixes._

---

## Change log

| Date | Change |
|------|--------|
| 2026-07-30 | Initial log from senior review + Phase 1 TL review |
