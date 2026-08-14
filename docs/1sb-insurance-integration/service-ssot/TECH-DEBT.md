# Tech Debt Log — 1SB Insurance Platform

**Owner:** Tech Lead  
**Branch:** `cursor/phase1-foundations-c259`  
**Last reviewed:** 2026-08-04 (COMP-003 raw payload encryption + coverage/Docker/OpenAPI pass)  
**Source:** Senior engineer review + Phase 1 code review + QA Lead  
**Related:** [phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md](./phase-1/TECH-LEAD-REVIEW-COMMON-PERSISTENCE.md) · [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md)

Severity: **P0** = blocks Phase 2 / multi-service reuse · **P1** = fix this sprint · **P2** = track before prod · **P3** = hygiene

---

## Open items

| ID | Sev | Title | Origin | Status | Owner |
|----|-----|-------|--------|--------|-------|
| TD-001 | P1 | Adopt Lombok; remove hand-rolled builders in shared libs | Senior #1 | **Closed** | Agent 2 |
| TD-002 | P0 | Extract secrets SPI to `bank-common-secrets` reusable lib | Senior #2 | **Closed** | Agent 2 |
| TD-003 | P0 | Split DB ownership into `1sb-persistence-service` | Senior #3–4 | **Closed** | Agent 3 |
| TD-004 | P0 | Integration ↔ persistence communicate via HTTP only | Senior #5 | **Closed** | Agent 3 |
| TD-005 | P2 | Duplicate `ErrorCode` vs `ErrorCodes` in bank-common-error | Phase 1 review | **Closed** | Agent 2 |
| TD-006 | P2 | AWS Secrets Manager provider is stub; prod profile will fail-fast | Phase 1 review | Deferred Phase 2 | Backlog |
| TD-007 | P3 | ArchUnit rules use `allowEmptyShould(true)` — tighten when packages fill | Phase 1 review | Deferred | Phase 2+ |
| TD-008 | P3 | Docs drift: AGENTS.md “placeholder”; libs README Boot 3.3.5 vs 3.3.4 | Phase 1 review | **Closed** | Agent 2 |
| TD-009 | P2 | Missing domain ports vs arch (Proposal/Status/Master/Audit/Idempotency) | Phase 1 review | Deferred Phase 2 | Backlog |
| TD-010 | P2 | No Redis idempotency / cache adapter yet (in-memory OK Phase 2) | Arch gap | Partial — in-memory done P2-B1 | Backlog |
| TD-011 | P1 | Integration service must not own Flyway or JPA after split | Senior #3–4 | **Closed** | Agent 3 |
| TD-012 | P2 | Convention for future JPA entities / API DTOs (Lombok vs records) | Senior #1 + TL | **Closed** | Agent 2 |
| TD-013 | P3 | Stale integration README / STATUS after persistence split | Confirmation | **Closed** | Agent 2 |
| TD-014 | P2 | WireMock / full E2E for integration ↔ persistence HTTP | Confirmation | Deferred Phase 2 | Backlog |
| TD-015 | P2 | Poll-attempt / raw-payload HTTP ports on persistence | Confirmation | Partial — poll-attempt done P2-B2 | Backlog |
| TD-016 | P0 | Rename `1sb-persistence-service` → `bank-persistence-service` | Senior (common persistence) | **Closed** | Agent 3 |
| TD-017 | P0 | Docs/ownership: persistence is platform-common, not 1SB-owned | Senior (common persistence) | **Closed** | Agent 2 |
| TD-018 | P1 | Package `com.bank.persistence` (+ flatten entity/repo) | Senior + TL | **Closed** | Agent 3 |
| TD-019 | P1 | Client config `bank.persistence.base-url` / `BANK_PERSISTENCE_BASE_URL` | Senior + TL | **Closed** | Agent 3 |
| TD-020 | P1 | Multi-consumer contract (integration + audit-consumer + …) | Senior (audit consumer) | **Closed** | Agent 2 |
| TD-021 | P2 | Document audit-consumer → persistence audit API (stub; no full service) | Senior (audit consumer) | **Closed** | Agent 2 |
| TD-022 | P1 | Payment intimation (FUNC-008) not implemented | Phase 4 (FUNC-007) | Deferred P1 | Backlog |
| TD-023 | **P1** | Audit + raw payload coverage incomplete — status/master-data payloads uncaptured, and 5 of 13 declared audit actions never emitted | COMP-003 impl / ASM-008 invalidated 2026-08-14 | **Open — pulled into Phase 4** | Backlog |
| QA-001 | P0 | JaCoCo coverage reports + verification gates | QA Lead / TEST-BACKLOG | **Closed** | Dev |

---

## QA-001 — JaCoCo coverage (Closed)

**Problem:** No measured coverage; CI could not enforce strategy §7 floors.

**Done (2026-07-30):**
- JaCoCo on all Java subprojects; `test` → `jacocoTestReport` (HTML + XML); `jacocoTestCoverageVerification` on `check`.
- **Libs:** line ≥ 80%, branch ≥ 70% (strategy §7).
- Excludes: `*Application`, `package-info`, `*Config` / `*Configuration` / `*Properties`.
- Minimal unit tests added for `EnvSecretProvider` and other lib gaps to meet the gate.
- Docs: [COVERAGE.md](./COVERAGE.md), AGENTS.md run command.

**Closed (2026-08-13):** the residual — services on an interim module-wide floor with no
package rules — is retired. `build.gradle.kts` now enforces every strategy §7 package floor as a
JaCoCo `PACKAGE` rule on `check`:

| Package | Line | Branch | Measured at closure |
|---------|------|--------|---------------------|
| `…onesb.application` | 80% | 70% | 92.9 / 75.7 |
| `…onesb.lob`, `…onesb.lob.life.term` | 80% | 70% | 100 / 100 · 94.9 / 74.4 |
| `…onesb.adapter.onesb.*` (7 packages) | 70% | 60% | lowest 88.4 / 71.4 |
| `…onesb.adapter.idempotency` | 80% | 70% | 95.9 / 80.8 |
| `com.bank.persistence.api.internal.v1` | 70% | 60% | 99.6 / 80.0 |

Module floors raised alongside: `bank-persistence-service` 50% line / no branch gate →
**90% line / 70% branch** (measured 99.0 / 81.0). `1sb-integration-service` stays at 90/70.

Four packages were below their branch floor before this change
(`adapter.onesb.client` 50.0%, `adapter.onesb.payment` 56.2%, `adapter.onesb.proposal` 56.0%,
`adapter.idempotency` 65.4%). They were raised by adding tests, not by lowering the floor —
see `OneSbMasterDataAdapterNormaliseTest`, `OneSbHttpClientHashMaskedBodyTest`,
`CachedBodyHttpServletRequestTest`, `OneSbPaymentAdapterPayloadTest`,
`OneSbProposalAdapterResultTest`.

A `packageFloorGuard` task fails the build if a package glob stops matching any package, so a
rename cannot silently delete a gate. Both failure modes were verified by temporarily breaking
them (floor raised to 0.99 → rule violation; glob renamed → guard failure).

**Scope note:** WS-2 services (`identity-provider-adapter-service`,
`identity-authorization-service`, `workforce-access-bff`) remain on the 50% interim line floor.
They are a different workstream at IAM Phase 1; strategy §7 defines no package floors for them,
and their coverage is not what QA-001 or WS-1 gate criterion 4.7 governs. Raising them belongs
to the WS-2 gate, not here.

**Expiry (R7):** discharged — the interim floor was retired before the **2026-08-30** deadline
rather than re-waived. No expiry now applies.

**Status:** **Closed** — pending TL + QA Lead counter-signature per
[15-TECH_DEBT_POLICY §4](../../governance/15-TECH_DEBT_POLICY.md).

---

## QA-002 — Persistence API tests (Done)

**Problem:** Persistence module had only poll-attempt API coverage; jobs/offers/payments/audit/`GlobalExceptionHandler` untested.

**Done (2026-07-30):**
- `JobApiTest`, `PaymentSessionApiTest`, `AuditEventApiTest` (+ extended `JobPollAttemptApiTest`) via `@SpringBootTest` + MockMvc.
- Assert `ErrorCodes.RESOURCE_NOT_FOUND` on NotFound paths (R5).
- Measured persistence **~96% line**; monorepo services interim floor **35% → 50%** (see [COVERAGE.md](./COVERAGE.md)).

**Status:** **Done**

---

## TD-016 — Rename to `bank-persistence-service`

**Problem:** Module/folder/JAR/`spring.application.name` still say `1sb-persistence-*`, implying ownership by the 1SB integration estate.

**Fix:** Rename to `bank-persistence-service` everywhere in build + runtime identity. Aligns with `bank-common-*`.

**DoD:** `settings.gradle.kts` includes `:services:bank-persistence-service`; `./gradlew :services:bank-persistence-service:build` works; no code/build refs to old module path.

**Status:** **Closed** (2026-07-30) — Agent 3: folder/JAR/`spring.application.name` → `bank-persistence-service`.

---

## TD-017 — Platform-common ownership framing

**Problem:** Docs describe persistence as the durable store *of* 1SB integration rather than a shared bank DB service.

**Fix:** Rewrite SSOT/AGENTS/READMEs; Flyway header comments; state explicitly that Flyway stays only here and multiple microservices call it over HTTP.

**Status:** **Closed** (2026-07-30) — Agent 2: platform contract doc, README rewrite, AGENTS/SSOT/integration framing, Flyway header.

---

## TD-018 — Package rename

**Problem:** `com.bank.insurance.persistence` + nested `persistence.persistence.entity` couples naming to insurance and is awkward.

**Fix:** `com.bank.persistence` with `entity` / `repo` / `api.internal.v1` / `config`.

**Status:** **Closed** (2026-07-30) — Agent 3: packages moved; entity/repo flattened; `@EntityScan` / `@EnableJpaRepositories` updated.

---

## TD-019 — Client config rename

**Problem:** `insurance.persistence.base-url` is domain-tied; audit-consumer and other MS should use a platform key.

**Fix:** `bank.persistence.base-url` + `BANK_PERSISTENCE_BASE_URL`. Update integration YAML, properties record, tests.

**Status:** **Closed** (2026-07-30) — Agent 3: `PersistenceClientProperties` prefix `bank.persistence`; YAML/env/tests updated; HTTP paths unchanged.

---

## TD-022 — Payment intimation (FUNC-008) not implemented

**Problem:** `POST /v1/payments/{id}/intimation` (FUNC-008, P1) is not built. `OneSbPaymentPort`
gained `sendPaymentIntimation(String sessionId)` as part of FUNC-007's port shape (matching
architecture §5.3), but implementing it fully requires the 1SB payment-intimation payload
(`trackInfo`, `manufacturerQuoteID`, `personalInformation`, `paymentDetails.status`, `paymentMode`,
`txnAmount`/`txnDate` — see `field-guides/payment.md`), none of which the FUNC-007 payment-url flow
collects or persists today.

**Fix (when FUNC-008 is picked up):** implement `OneSbPaymentAdapter.sendPaymentIntimation`
against `POST /v1/payment/url` intimation variant, add a `PaymentIntimationCommand` carrying the
PG receipt fields, wire a `PaymentController` endpoint, and replace the current
`UnsupportedOperationException` stub.

**Status:** Deferred P1 (per `PRODUCT-BACKLOG.md` FUNC-008 priority). Not a P0 blocker.

---

## TD-023 — Raw payload capture gap (status / master-data)

**Problem:** `RawPayloadStorePort` is wired into `OneSbQuoteAdapter` (submit + poll),
`OneSbProposalAdapter` (submit), and `OneSbPaymentAdapter` (payment-url) — all three already carry
a `jobId` in their port signature. `OneSbStatusAdapter.getStatus` and `OneSbMasterDataAdapter` do
not: status is looked up by `applicationNumber` only (no job), and master-data lookups never
create a job. `raw_payload.job_id` is `NOT NULL`, so neither can call the store today without a
schema/port change.

**Fix (when picked up):** either make `job_id` nullable on `raw_payload` (and treat status/master
captures as job-less evidence), or thread a synthetic/job-less capture key through those two
adapters. Low urgency — status/master-data responses are non-mutating and already audit-hashed via
`OneSbHttpClient`'s existing `ONESB_OUTBOUND_CALL` audit event.

**Status:** Deferred, not a P0/P1 blocker.

---

## TD-020 — Multi-consumer contract

**Problem:** No written contract that non-1SB services (esp. audit-consumer) share this persistence API.

**Fix:** Architecture/contract doc listing consumers, auth expectations (internal network), and resource groups (jobs vs audit-events).

**Status:** **Closed** (2026-07-30) — Agent 2: `architecture/bank-persistence-service.md` multi-consumer contract.

---

## TD-021 — Audit-consumer usage (doc)

**Problem:** Senior requires audit-consumer to store audit via the same persistence service; no guidance doc yet.

**Fix:** Short design note: audit-consumer calls `POST/GET /internal/v1/audit-events`; does **not** own Flyway or a second `audit_event` store. Full consumer service scaffold is Phase 2+ / separate story.

**Status:** **Closed** (2026-07-30) — Agent 2: `architecture/audit-consumer-service.md` stub (doc-only; no Boot app).

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

## TD-013 — Stale integration README / STATUS after split

**Problem:** After TD-003/004/011, `services/1sb-integration-service/README.md` and parts of `phase-1/STATUS.md` still describe Flyway/H2 ownership, `adapter/secret`, and pre-split test stacks.

**Owner:** Agent 2 (docs rewrite). Agent 3 confirmation pass does **not** own the integration README rewrite.

**Status:** **Closed** — Agent 2 rewrote integration README + STATUS banner/skeleton/exit gate (commit on this branch). Residual service gaps tracked as TD-014 / TD-015 (payment/audit HTTP on integration remains TD-009).

---

## TD-014 — WireMock / full E2E (Deferred Phase 2)

**Problem:** End-to-end contract test with WireMock (or Testcontainers dual-service) for integration → persistence HTTP is not yet in CI.

**Mitigation (Phase 1):** `HttpJobStoreAdapterTest` uses `MockRestServiceServer` for createJob + findQuoteJob. Full WireMock E2E deferred to Phase 2.

---

## TD-015 — Poll-attempt / raw-payload HTTP (Deferred Phase 2)

**Problem:** Flyway + JPA cover `job_poll_attempt` and `raw_payload`, but persistence does not yet expose `/internal/v1` CRUD for them.

**Status:** Deferred Phase 2. Jobs/offers/payment-sessions/audit-events HTTP is sufficient for Phase 1 JobStorePort wiring.

**Related:** Payment/audit **ports on integration** (not just persistence HTTP) remain **TD-009**.

---

## Closed items

| ID | Closed | Notes |
|----|--------|-------|
| TD-001 | 2026-07-30 | Lombok on root subprojects; `AuditEvent`, `ServiceErrorResponse`, `BankPrincipal` → `@Value` + `@Builder` |
| TD-002 | 2026-07-30 | `:libs:bank-common-secrets`; integration service wires SPI from lib; old `adapter/secret` deleted |
| TD-003 | 2026-07-30 | `:services:1sb-persistence-service` owns Flyway V1 + JPA entities/repos; internal REST stubs |
| TD-004 | 2026-07-30 | Persistence `/internal/v1` API; integration `HttpJobStoreAdapter` via RestClient + `insurance.persistence.base-url` |
| TD-005 | 2026-07-30 | Removed unused duplicate `ErrorCode`; kept `ErrorCodes` |
| TD-008 | 2026-07-30 | AGENTS.md rewritten for real platform; libs README Boot version → 3.3.4 |
| TD-011 | 2026-07-30 | Integration stripped of data-jpa / Flyway / drivers / `db/migration`; ArchUnit forbids JPA+Flyway imports |
| TD-012 | 2026-07-30 | Convention documented in `libs/README.md` and this log |
| TD-013 | 2026-07-30 | Agent 2 aligned integration README + STATUS with post-split reality |
| TD-017 | 2026-07-30 | Platform-common framing: contract doc, READMEs, AGENTS, Flyway header |
| TD-016 | 2026-07-30 | Module/JAR/app name → `bank-persistence-service` |
| TD-018 | 2026-07-30 | Packages → `com.bank.persistence`; entity/repo flattened |
| TD-019 | 2026-07-30 | Client config → `bank.persistence.base-url` / `BANK_PERSISTENCE_BASE_URL` |
| TD-020 | 2026-07-30 | Multi-consumer contract in `architecture/bank-persistence-service.md` |
| TD-021 | 2026-07-30 | Audit-consumer stub doc; persists via `/internal/v1/audit-events` only |

---

## Change log

| Date | Change |
|------|--------|
| 2026-07-30 | Initial log from senior review + Phase 1 TL review |
| 2026-07-30 | Agent 2 closed TD-001, TD-002, TD-005, TD-008, TD-012 |
| 2026-07-30 | Agent 3 closed TD-003, TD-004, TD-011 (persistence service + HTTP split) |
| 2026-07-30 | Confirmation circle #2: docs/ArchUnit hygiene; HttpJobStoreAdapterTest; TL pass closed vs senior #1–#5 |
| 2026-07-30 | Agent 3 confirmation: TD-013 Closed (Agent 2 docs); TD-014/015 Deferred Phase 2; MockRestServiceServer JobStore test |
| 2026-07-30 | Senior: persistence is platform-common (not 1SB-owned); audit-consumer shares it — opened TD-016…021 |
| 2026-07-30 | Phase 2 complete (TECH-004..007, COMP-001..002, NFR-001); TD-015 poll-attempt done, raw-payload HTTP still open |
| 2026-07-30 | QA Lead: testing strategy/rules/backlog; opened QA-001..003 (coverage + persistence tests + IT template) |
| 2026-07-30 | Agent 2 closed TD-017, TD-020, TD-021 (platform contract + audit-consumer stub + framing) |
| 2026-07-30 | Agent 3 closed TD-016, TD-018, TD-019 (module rename + packages + client config) |
| 2026-07-30 | Confirmation circle #2 closed docs hygiene for common-persistence (STATUS banner + TL §7 exit criteria) |
| 2026-07-30 | QA-001 Partial: JaCoCo reports + libs 80%/70% gates; services interim 35% line (see COVERAGE.md) |
| 2026-07-30 | QA-002 Done: persistence API tests (jobs/offers/payments/audit); services interim 35%→50% line |
| 2026-08-04 | Phase 4: FUNC-007 (payment) + FUNC-009 (status) implemented; TD-022 opened (FUNC-008 intimation deferred P1) |
| 2026-08-13 | **QA-001 Closed** — strategy §7 package floors enforced as JaCoCo `PACKAGE` rules (`…onesb.application`, `…onesb.lob*` 80/70; `…adapter.onesb.*` 70/60; `…adapter.idempotency` 80/70; `com.bank.persistence.api.internal.v1` 70/60), plus `packageFloorGuard` so a renamed package fails the build instead of silently dropping its gate. Four packages were below their branch floor and were raised with tests, not by lowering the floor. `bank-persistence-service` module floor 50% line/no branch → 90/70. WS-2 services stay on the 50% interim floor (different workstream, IAM Phase 1). Interim floor retired ahead of its 2026-08-30 expiry; closes WS-1 gate criterion **4.7**. |
| 2026-08-04 | COMP-003 (raw payload encryption at rest) implemented: `RawPayloadEncryptionService` (AES-256-GCM) + `/internal/v1/raw-payloads` on `bank-persistence-service`; `RawPayloadStorePort` + `HttpRawPayloadStoreAdapter` wired into quote/proposal/payment submit+response capture on `1sb-integration-service` (best-effort, never fails the caller). Status/master-data capture not wired (no `jobId` in those port signatures) — tracked as **TD-023**. `1sb-integration-service` JaCoCo floor raised **50% → 90% line / 70% branch** (measured ~90.7%/~71.4%) — see COVERAGE.md. Dockerfiles added for both services + root `docker-compose.yml`; springdoc-openapi (`/swagger-ui.html`, `/v3/api-docs`) added to both — both services already run on embedded Tomcat independently (`spring-boot-starter-web`, no exclusions), no change needed there. |
