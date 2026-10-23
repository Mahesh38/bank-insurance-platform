# Build playbook — how every service builder works

**Status:** AI-DRAFTED 2026-10-22 under [`SUG-20261022-rdy`](../../governance/registers/SUGGESTION-REGISTER.md) (owner-directed), awaiting Mahesh (architecture) and Amit (engineering) review.

[Contracts index and build waves](./README.md) · [Owner decisions round 2](../architecture-review/DECISIONS-2026-10-22.md) ·
[Round 1](../architecture-review/DECISIONS-2026-10-21.md) · [ADR-024 rules](../architecture-review/ADR-024-configuration-driven-rule-engine.md) ·
[ADR-025 events](../architecture-review/ADR-025-event-specification-and-publisher.md) ·
[ADR-026 audit](../architecture-review/ADR-026-audit-store-and-transport.md) ·
[ADR-027 contracts](../architecture-review/ADR-027-openapi-contract-authority.md)

This page is shared by all 21 builders — human or AI agent — who build the 21 services in
parallel. Read it once, then your service's `BUILD-BRIEF.md`, then its `LLD.md` and
`openapi.yaml`. The goal for each builder is a service at **90–95 % maturity** (§11) **without
inventing behaviour** (§12).

## Contents

1. [Order of authority](#1-order-of-authority)
2. [Ownership — how 21 builders avoid collisions](#2-ownership--how-21-builders-avoid-collisions)
3. [Repository conventions](#3-repository-conventions)
4. [Shared libraries — how to use them](#4-shared-libraries--how-to-use-them)
5. [Contract conformance (ADR-027)](#5-contract-conformance-adr-027)
6. [Configuration, never code (ADR-024, E7)](#6-configuration-never-code-adr-024-e7)
7. [Security, PDP, timeouts, fail closed](#7-security-pdp-timeouts-fail-closed)
8. [Events and audit (ADR-025, ADR-026)](#8-events-and-audit-adr-025-adr-026)
9. [Stubbing a dependency that does not exist yet](#9-stubbing-a-dependency-that-does-not-exist-yet)
10. [Testing and PostgreSQL proof](#10-testing-and-postgresql-proof)
11. [Definition of 90–95 % mature — checklist](#11-definition-of-9095--mature--checklist)
12. [When the contract is ambiguous — stop, record, never invent](#12-when-the-contract-is-ambiguous--stop-record-never-invent)
13. [Compliance posture: STRICT (C1–C8)](#13-compliance-posture-strict-c1c8)
14. [Hand-back report](#14-hand-back-report)

## 1. Order of authority

| Rank | Source | Rule |
|---|---|---|
| 1 | Owner decisions ([round 2](../architecture-review/DECISIONS-2026-10-22.md) E1–E9, C1–C8; [round 1](../architecture-review/DECISIONS-2026-10-21.md) D1–D9) and accepted ADRs (024–027) | A later decision amends an earlier one (E1 amends D8; C1 tightens D5) |
| 2 | `docs/platform/contracts/<service>/openapi.yaml` | **Authoritative on the wire** (ADR-027 §1): paths, headers, schemas, status and error codes. Where code and contract disagree, the code is wrong, or the contract is changed first by review |
| 3 | `docs/platform/contracts/<service>/LLD.md` | Flows, validation, mapping, invariants, events |
| 4 | [Events catalogue](./events/CATALOGUE.md) + `events/schemas/*.json` | Event names, topics, payloads (closed schemas) |
| 5 | [Design DDL](../data-architecture/schemas/README.md) | Table shapes; copied into the service's Flyway migrations |
| 6 | Your `BUILD-BRIEF.md` | Scope, order, stubs, acceptance tests. It summarises 1–5; **it never overrides them** |

If two of rank 1–5 disagree and no decision settles it, that operation is ambiguous: follow §12.
Round-2 work (`SUG-20261022-rdy`) is updating several contracts and LLDs at the same time as these
briefs were written; always re-read the contract when you start, not the brief's summary of it.

## 2. Ownership — how 21 builders avoid collisions

| Asset | Owner (single writer) | How others change it |
|---|---|---|
| `services/<your-service>/**`, `docs/platform/contracts/<your-service>/BUILD-BRIEF.md` (status rows only) | You | — |
| Your `openapi.yaml` / `LLD.md` | Contract owner named in the file's status line | A reviewed contract change **before** code (ADR-027 §4). Never edit another service's contract |
| `libs/bank-common-error` (`ErrorCodes`, `ErrorCatalogue`) | bank-common-error owner (Amit) | Request a new code with: code, HTTP status, category, title, source rule. **Append only**, never rename or reorder. Until it lands, use the nearest registered code and list the gap in your open items |
| `libs/bank-common-events`, `libs/bank-rule-engine`, `libs/bank-common-test`, `-observability`, `-security` | Library owner (Amit; rule engine: Mahesh) | Issue / request; a builder does not patch a shared lib in a service branch |
| `docs/platform/contracts/events/**` (catalogue, schemas) | Events catalogue owner (Amit, Shivanshi for topics) | New event or field: request with a draft JSON-Schema; additive changes keep `schemaVersion` (catalogue §4) |
| Rule-set seeds `libs/bank-rule-engine/src/main/resources/rulesets/*.json` | Rule-engine owner; **content** by the approver in each seed's `governance` block | A seed is a DRAFT only; content needs Shailja / Rajal / BRD owner approval through administration-config (maker-checker) |
| `docs/platform/data-architecture/schemas/*.sql` (design DDL) | DDL owner (Aarti) | Request; your **runtime** Flyway migrations live in your service and copy the design |
| `settings.gradle.kts`, root `build.gradle.kts`, `docs/context/*`, governance registers, stage state | Coordinator only | Never edit |

Rules of thumb: own one module; read every other module, never write it; when you need a change
elsewhere, write it as an open item in your brief with the owner and continue with a stub (§9).

## 3. Repository conventions

### 3.1 Module and packages (hexagonal)

`services/<service>/src/main/java/com/bank/platform/<context>/` (older modules keep their package,
e.g. `com.bank.identity.authz`, `com.bank.workforce.bff`, `com.bank.insurance.onesb`):

| Package | Holds | May import |
|---|---|---|
| `domain` | Aggregates, value objects (Java `record`s), ports (interfaces), domain exceptions | JDK only — **no Spring** (ArchUnit) |
| `application` | Use-case services, transactions, orchestration of ports | `domain`, shared libs; **never `adapter`** (ArchUnit) |
| `adapter.<tech>` | JDBC/JPA, DynamoDB, HTTP clients, Kafka consumers, stubs | `domain`, `application` ports |
| `api` | Controllers, request/response DTOs, bean validation | `application` |
| `config` | Spring wiring, `@ConfigurationProperties` records | everything |

Boilerplate style follows [`libs/README.md`](../../../libs/README.md) (records for domain and
properties; Lombok only for JPA entities). Sparse comments that say **why** and cite the rule id
(`// INV-QUO-01 (D4): …`).

**ArchUnit.** Every service has `architecture/ServiceArchitectureTest` with the two rules (domain
must not import Spring; application must not import adapter). When your packages hold real
classes, **remove `allowEmptyShould(true)`** and add: (a) no class outside `adapter..` imports a
provider / AWS / Kafka type; (b) no import of another service's packages
(`com.bank.platform.<other>..`). Provider adapters additionally keep `INV-ACL-01`.

### 3.2 Errors

- Inject `ServiceErrors` (from `bank-common-error`; configured by `bank.error.service-id`,
  `bank.error.layer`, `bank.error.boundary` in `application.yml`) and raise:
  `errors.error(ErrorCodes.X).component("…").operation("…").reason("…").cause(e).build()`.
- `reason` never contains PII (CIF, PAN, mobile, `q`, OTP) — `INV-LOG-01`.
- Every error code in your contract must exist in `ErrorCodes` / `ErrorCatalogue`
  ([error model](../api-reference/conventions/error-model.md)). Missing → §2 request + open item.
- Responses are `application/problem+json` with the platform envelope; Spring's default 400 without
  the envelope is a defect (several LLDs list it).
- Downstream refusals are propagated with `ErrorPropagation` (same code, same `incidentId`).

### 3.3 Persistence and migrations

- Aurora PostgreSQL, **one schema per service**, role `app_<schema>`, no cross-schema reads or FKs.
- Flyway in `src/main/resources/db/migration` (portable SQL). Postgres-only SQL (partial indexes,
  triggers, `SKIP LOCKED`, `JSONB`, `BYTEA` functions) goes in
  `db/vendor/postgresql/V<n>__….sql` with an H2 counterpart (or a comment-only file) in
  `db/vendor/h2/V<n>__….sql`, and `spring.flyway.locations: classpath:db/migration,classpath:db/vendor/{vendor}`
  — the pattern used by lead-service (`V5__lead_dedupe_open`) and administration-config
  (`V3__rule_set_version_immutability`).
- Copy shapes from the design DDL file named in your brief; record any deviation in your LLD's
  mapping table. JPA `ddl-auto: validate`. INSERT-only tables are enforced by grants **and** (where
  the LLD says so) a trigger in the vendor folder.
- DynamoDB (quotation, proposal only — [`DB-DEC-0005`](../data-architecture/DB-DEC-0005-dynamodb-working-payload.md))
  follows [schemas/20](../data-architecture/schemas/20-dynamodb-working-payload.md).

### 3.4 Build, quality gates, dependency locking

```bash
./gradlew --max-workers=2 :services:<service>:spotlessApply
./gradlew --max-workers=2 :services:<service>:check        # tests + Spotless + Checkstyle + JaCoCo
# after changing dependencies (runtimeClasspath is locked):
./gradlew --max-workers=2 :services:<service>:dependencies --write-locks
```

- Maven Central sometimes answers `429`: retry with backoff, never vendor jars.
- Spotless: google-java-format 1.22.0, ratchet from `origin/main`. Checkstyle: `maxWarnings = 0`
  ([config](../../../config/checkstyle/checkstyle.xml)); do not add suppressions for new code.
- JaCoCo module floors (root [`build.gradle.kts`](../../../build.gradle.kts)): libs 80 % line / 70 %
  branch; phase-1 services 90 / 70; others 50 % line. **The 90–95 % target (§11) asks for ≥ 80 %
  line and ≥ 70 % branch on your module, and 100 % branch on every compliance-gate class**
  (`SUIT-R27`: suitability gate read, quotation C1, proposal C2, payment C4).
- Never add a library a shared lib already provides (Kafka client only via `bank-common-events`).

## 4. Shared libraries — how to use them

| Library | Use it for | Key types |
|---|---|---|
| `bank-common-error` | Every refusal; problem+json; propagation | `ServiceErrors`, `ErrorCodes`, `ServiceException`, `PlatformErrorHandler`, `ErrorPropagation`, `ProblemJsonReader` |
| `bank-common-events` | Outbox write, relay, consumer dedupe, topics | `EventEnvelope.builder()`, `Actor`, `Ulid`, `OutboxWriter.write(connection, event)`, `JdbcOutboxRelay.drain(n)` / `backlog()`, `IdempotentEventHandler`, `JdbcProcessedEventStore`, `TopicResolver`, `InMemoryEventPublisher` (tests), `KafkaEventPublisher` (MSK) , `SpringConnectionProvider` |
| `bank-rule-engine` | Every business decision and policy value | `RuleSetResolver` → `CachingRuleSetResolver(new HttpRuleSetResolver(…, 300 ms), 60 s)`, `RuleSetQuery`, `RuleEngine.evaluate(ruleSet, inputs, evaluatedAt)`, `EvaluationResult.toCanonicalJson()`, `RuleSetUnresolvableException`, `RuleSetSeeds` / `RuleSetParser` (tests) |
| `bank-common-observability` | Correlation, MDC, metrics | `RequestDiagnosticFilter`, `MdcKeys`, `MdcContext`, `TraceHeaders`, `BankMetricNames`, `ErrorMetrics` |
| `bank-common-test` | Test harness | `PostgresTestSupport` (Testcontainers), `WireMockHarness`, `PyramidTags` / `ContractTags` / `E2ETags`, `DomainFixtures` |
| `bank-common-security` | Principal model | `BankPrincipal`, `JwtClaims` |
| `bank-common-audit` | **Legacy** audit model (1SB adapter); new code emits `AuditRecorded` events instead (§8) | `AuditEvent` |

Usage details: events — [catalogue §6–§7](./events/CATALOGUE.md); rules —
[rule-engine LLD §11](./rule-engine/LLD.md#11-using-the-engine-from-a-deciding-service).

**Outbox in a Postgres service.** In the same transaction as the business write:
`outboxWriter.write(connection, EventEnvelope.builder()…build())` (via `SpringConnectionProvider`,
which refuses to write outside a transaction). Schedule `relay.drain(10)` with `@Scheduled` (the
library adds no scheduler). Alert on `backlog()` age and parked rows.

**Consumer.** Wrap the handler: `new IdempotentEventHandler(new JdbcProcessedEventStore(…), "<service>.<purpose>", handler)`;
the claim and your writes share one DB transaction; commit the offset after the DB commit; retry
topic then DLQ (catalogue §3.1). Until MSK exists (S09), quotation/proposal also expose the
`/internal/v1/events/payment-reconciled` HTTP shim already in their contracts — no other service
invents an HTTP event shim.

## 5. Contract conformance (ADR-027)

1. Generate nothing by hand that the contract already fixes: path, method, header names
   (`X-Actor-Id`, `Idempotency-Key`, `X-Correlation-Id` — [headers](../api-reference/conventions/headers-session-and-auth.md)),
   ULID pattern `^[0-9A-HJKMNP-TV-Z]{26}$`, status codes, error codes, required fields.
2. **Conformance test.** ADR-027 §3 names `OpenApiConformance` in `bank-common-test`; it is **not
   built yet** (open item for the bank-common-test owner). Until it lands, each service writes a
   `ContractConformanceTest` (tag `ContractTags`) that boots the context, reads springdoc
   `/v3/api-docs` and asserts every *built* operation's path, method, parameters, required fields
   and response codes match the checked-in YAML. Switch to the shared helper when it exists.
3. **Marking progress.** Each operation in the YAML carries `x-implementation-status`:
   `PLANNED` (designed, not coded) → `BUILT` (coded, conformance test green) →
   `DEPRECATED` (kept for a migration window, e.g. quotation `getSelectedLegacy`). Changing the
   marker is the only edit a builder makes to the contract without a review; do it in the same
   change as the code. The conformance test skips `PLANNED` operations and fails on any `BUILT` one
   that drifts.
4. A behavioural change to the wire (new field, new status) is a contract change first (§2).

## 6. Configuration, never code (ADR-024, E7)

- **Business values** (thresholds, periods, validity days, role lists, allowed outcomes, TAT,
  poll intervals, expiry days) come **only** from rule sets resolved through
  administration-config (`GET /internal/v1/rule-sets:resolve`) and evaluated with
  `bank-rule-engine`. No literal in code, no `application.yml` default for a business value, no
  compiled-in fallback (`INV-CFG-01`).
- **Technical values** (HTTP connect/read timeouts, pool sizes, relay batch) are Spring
  properties overridable by environment variable (E8 pattern).
- **Unresolvable → fail closed.** Resolver unreachable with no cached version →
  `503 UPSTREAM_UNAVAILABLE`; no version in force → `CONFIGURATION_UNRESOLVABLE` (registered as
  422; ADR-024 says 503 — open item for Mahesh, follow the registry). An **unset parameter** fires
  the rule set's `missingConfigOutcome` (e.g. `LEAD.EXCEPTION` → `BLOCK / EH-CONFIG-MISSING`).
- Persist the full `EvaluationResult` (or at least `ruleSetId@version` + `inputsHash`) on the
  business record (`INV-CFG-03`); honour `"pinning": "JOURNEY"`.
- Consume `RuleSetPublished` → `resolver.invalidate(ruleSetId)`.
- A rule set your LLD needs but that has **no seed yet** (e.g. `JOURNEY.POLICY`,
  `CATALOGUE.ELIGIBILITY`, `NOTIFICATION.TEMPLATES`, `AUDIT.SOLD.COMPLETENESS`, `LEAD.POLICY`,
  `CUSTOMER.POLICY`): code against the resolver port, test with a **test-resource** rule set marked
  `"sample": true`, and list the missing seed as an open item. Never ship a seed with invented
  values; unset values stay `null`.

## 7. Security, PDP, timeouts, fail closed

- Actor identity is `X-Actor-Id` set by the BFF from the session; never a body field.
- Regulated or mutating actions re-check the PDP `POST /internal/v1/authorization/decisions`
  (300 ms, no retry). Silence → `403 AUTHORIZATION_UNAVAILABLE`. Partner reps are `ASSIST_ONLY`
  (`INV-ACT-02`, `403 ASSIST_ONLY_ACTOR`); SP certification at the instant of action (`INV-ACT-01`).
- Every outbound call has explicit connect/read timeouts from the seam table
  ([idempotency, timeouts, retries](../api-reference/conventions/idempotency-timeouts-retries.md)).
  No retry on any submit / money / provider-mutating call. Inner budget < outer budget.
- Every mutating endpoint takes `Idempotency-Key` and stores an idempotency record in its own
  schema in the same transaction (`INV-IDM-01`): same key + body → stored response; different body
  → `409 IDEMPOTENCY_CONFLICT`.
- Production guards: any `stub` adapter, zero key, in-memory store or H2 URL is refused at startup
  under the `prod`/`uat` profile (pattern: lead-service `ProductionSafetyGuard`).
- Never log PII; mask per the owning service (customer-service owns masking).

## 8. Events and audit (ADR-025, ADR-026)

- Envelope `event-envelope/v1`, ids and states only, closed `data` schemas, topic
  `{env}.{context}.{aggregate}.v1`, key = `aggregateId` ([catalogue](./events/CATALOGUE.md)).
- An event your LLD names but the catalogue lacks is **not invented on the bus**: write it to your
  outbox only after the catalogue owner adds its schema; until then list it as an open item and
  keep the producer behind a feature property defaulting off.
- **Audit is accepted (E2).** Every regulated action writes an `AuditRecorded` event in the same
  transaction as the business change (`data`: action, resource, outcome, `actingCapacity`,
  prior/new state, rule-set version, no PII; actor per `INV-ACT-04`). audit-compliance-service
  stores it hash-chained. Reads that matter (customer search, evidence view) write one too.
  Database activity audit stays pgaudit.

## 9. Stubbing a dependency that does not exist yet

Every builder starts **now**. A dependency that is not built is replaced by a stub that behaves
**exactly as the dependency's `openapi.yaml` says** — never by guessed behaviour.

1. Put the dependency behind a port in `domain`/`application` (e.g. `ConsentCheckPort`).
2. Two adapters: `adapter.<dep>.http` (real client, budgets from the seam table) and
   `adapter.<dep>.stub` (in-process), selected by `<service>.<dep>.mode=http|stub`.
   The stub is refused under `prod`/`uat` (§7).
3. The stub's responses are fixtures **validated against the dependency's YAML** (response schema,
   status codes, error codes), including its refusals: e.g. a consent stub must be able to answer
   `satisfied=false` with `missing[]`, `503`, and a timeout — so your fail-closed paths are tested.
4. Integration tests use `WireMockHarness` with the same fixtures; a contract change in the
   dependency breaks your fixture validation, not production.
5. When the dependency reaches `BUILT`, switch the default to `http` in non-local profiles and run
   the cross-service test listed in your brief.

Async dependencies: publish/consume through `InMemoryEventPublisher` in tests; real MSK arrives
with S09.

## 10. Testing and PostgreSQL proof

| Layer | What | Tag |
|---|---|---|
| Unit | Aggregates, invariants, mapping, rule-set use with sample rule sets | `PyramidTags.UNIT` |
| Contract conformance | §5 | `ContractTags` |
| Integration (H2, PostgreSQL mode) | Controllers + persistence + outbox + idempotency | `PyramidTags.INTEGRATION` |
| PostgreSQL proof | Migrations (vendor scripts), partial indexes, triggers (immutability), `SKIP LOCKED`, `ON CONFLICT` | `PyramidTags.TESTCONTAINERS` or guarded property |
| Invariant tests | One test per invariant id in your brief, named after it (`INV_QUO_01_…`) | unit / integration |
| Fitness | ArchUnit; `RuleEngineDeterminismTest` pattern where you evaluate | unit |

**PostgreSQL is mandatory proof, not optional.** H2 does not prove Postgres-only SQL.

- With Docker: `PostgresTestSupport.create()` (Testcontainers, `postgres:16-alpine`).
- Without Docker (the shared build container): a local PostgreSQL 16 may be running at
  `-h /tmp -p 55432 -U postgres` (trust). Create **your own** database per run
  (`createdb -h /tmp -p 55432 -U postgres <service>_<run>`), never drop others'. Guard such tests
  with a system property, e.g. `-Dpg.it=true`, so `check` stays green where no Postgres exists.
- Design DDL validation: `scripts/db/validate-postgres.sh` if present (being added by the DDL
  owner; it applies the design schemas and a test-role harness to a scratch database).
- Record in your brief which Postgres-only behaviours were proven and how.

## 11. Definition of 90–95 % mature — checklist

A service is at 90–95 % when **every row below is true** (or its gap is an open item that names a
blocker outside the builder's control). The remaining 5–10 % is what only S09 infrastructure, the
bank, an insurer or a human signature can supply.

| # | Item | Evidence |
|---|---|---|
| M1 | Every contract operation is implemented and marked `BUILT`, or is `PLANNED` with a named external blocker | YAML markers; conformance test green |
| M2 | Request validation matches the LLD validation table field by field, with the contract's error codes | One test per row |
| M3 | Every business value comes from a rule set or configuration; none in code; unresolvable → fail closed | Grep/ArchUnit review; resolver failure tests |
| M4 | Persistence: Flyway migrations equal the design DDL (deviations recorded); INSERT-only/immutability proven; migrations run on PostgreSQL 16 | PostgreSQL proof test or validator output |
| M5 | Idempotency on every mutating endpoint, in the same transaction | Replay + conflicting-body tests |
| M6 | Outbox events written in the business transaction with the catalogue envelope; relay wired; backlog metric | Outbox row assertions; relay test |
| M7 | Consumers idempotent on `eventId`, retry/DLQ wired, offset after commit | Duplicate-delivery test |
| M8 | PDP re-check on regulated/mutating actions; fail closed on silence; `ASSIST_ONLY` honoured | Deny / timeout tests |
| M9 | Explicit timeouts on every outbound hop per seam table; no retry on submits / money; breaker where the LLD says | Timeout tests with WireMock delays |
| M10 | `AuditRecorded` for every regulated action and every gate refusal (ADR-026) | Outbox assertions per action |
| M11 | Observability: correlation id propagated, MDC set, metrics for decisions / refusals / outbox age, no PII in logs | Log-scan test (`INV-LOG-01`) |
| M12 | Every invariant in the brief has a named test | Test names = invariant ids |
| M13 | Every acceptance test in the brief passes (or is blocked by a named external item) | Test report |
| M14 | ArchUnit rules active without `allowEmptyShould`; coverage ≥ 80 % line / 70 % branch; gate classes 100 % branch | `check` green |
| M15 | Stubs only behind properties, refused in `prod`/`uat` | Startup guard test |
| M16 | Docs updated: LLD deviations, YAML markers, brief status and open items | Diff |

## 12. When the contract is ambiguous — stop, record, never invent

1. **Stop that operation** (or that branch of it). Build everything else.
2. Record the ambiguity in your brief's *Open items* with: what is ambiguous, the two readings,
   the sources that disagree (path + section), the owner who decides, and what is blocked.
3. If a safe interim exists that **does not decide the question** — fail closed, refuse with the
   nearest registered error, leave a value unset so the rule set blocks — you may build that and
   say so. Choosing one of the readings is not a safe interim.
4. Never fill a value from memory, a "typical" figure, or another product. A missing threshold
   stays `null`; a missing wording stays unpublished; a missing endpoint stays unbuilt.
5. An approval cannot be manufactured: no agent approves a rule set, a statement, a contract, a
   change request, or GAP-006/007.

## 13. Compliance posture: STRICT (C1–C8)

Round 2 sets the posture to **strict** (E9): where a compliance question is open, the platform
takes the stricter option as **seed configuration**, and relaxing it is a Compliance configuration
change, never code. Builders must make every row below enforceable and testable.

| # | Strict rule the code must enforce | Configured in |
|---|---|---|
| C1 | `CNS-DP` and `CNS-SOL` are required at `BEFORE_SUITABILITY` (before any suitability answers); `BEFORE_MULTI_QUOTE` checks they are still valid and captures `CNS-QSH` | `CONSENT.POLICY` |
| C2 | OTP is entered by the customer on their own device only (`CUSTOMER_DEVICE`); RM relay refused | `CONSENT.POLICY.verificationModes` |
| C3 | `CNS-DP` and `CNS-SOL` are separate affirmative actions | `CONSENT.POLICY.separateActions` |
| C4 | Insurer decision TAT 7 calendar days from proposal receipt | `PROPOSAL.TAT` |
| C5 | Only `SUITABLE` quotes; `SUITABLE_WITH_CAUTION` only with acknowledgement on the customer's device | `SUITABILITY.GATE` |
| C6 | Savings/ULIP rule-pack gaps evaluate to `INSUFFICIENT_DATA` → no quote | Suitability rule sets |
| C7 | A consent purpose whose statement is not `APPROVED` cannot be captured; customer-facing texts are drafts | Statement configuration |
| C8 | GAP-006 / GAP-007 stay **OPEN** until the human Compliance signature ([sign-off packet](../../au-bank-insurance-platform/rule-packs/SIGNOFF-PACKET-GAP-006-007.md)); this blocks go-live (S11), not the build | — |

Full text: [round 2 decisions](../architecture-review/DECISIONS-2026-10-22.md). Where an LLD
still describes the round-1 (looser) position, the strict row wins and the LLD gap is an open item.

## 14. Hand-back report

When you stop, report (and update your brief's status and open items): operations built (with
markers), migrations and what was proven on PostgreSQL, events produced/consumed, error codes
requested, deviations from the LLD and why, acceptance tests passing / blocked, and open questions
with owners. Do not commit, push or change stage state unless the coordinator asked you to.
