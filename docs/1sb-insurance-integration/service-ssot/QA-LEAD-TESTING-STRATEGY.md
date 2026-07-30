# QA Lead — Testing Strategy

**Role:** QA Lead  
**Branch:** `cursor/phase1-foundations-c259`  
**Date:** 2026-07-30  
**Applies to:** `1sb-integration-service`, `bank-persistence-service`, `libs/bank-common-*`  
**SSOT companions:** [TESTING-RULES.md](./TESTING-RULES.md) · [TEST-BACKLOG.md](./TEST-BACKLOG.md) · architecture §13 (tests)

---

## 1. Honest baseline (as of Phase 2 close)

| Metric | Reality |
|--------|---------|
| Production `.java` files | ~99 |
| `*Test.java` classes | ~18 |
| JaCoCo / coverage gate in CI | **None** — coverage is unmeasured (“zero reported coverage”) |
| Pure single-class unit tests | Sparse — strongest on libs + `PiiMasker` / `OneSbErrorNormaliser` |
| Adapter tests with WireMock / MockRest | Present for HttpClient, poller, JobStore |
| True multi-service integration (Testcontainers) | **Absent** |
| API contract / E2E / performance / security suites | **Absent** |
| Persistence controller coverage | Thin (poll-attempt only) |

**QA verdict:** Phase 2 delivered *some* automated checks, but we do **not** meet a bank-grade quality bar. Unmeasured coverage ≈ unmanaged risk. Phase 3 (quote/proposal) must not land without the rules below.

---

## 2. Testing pyramid (mandatory shape)

```text
                 ┌─────────────┐
                 │  E2E / UAT  │  Few, slow, env-gated
                 ├─────────────┤
                 │ Integration │  Service+DB / dual-service HTTP
                 ├─────────────┤
                 │   Slice     │  @WebMvcTest, @DataJpaTest, WireMock
                 ├─────────────┤
                 │    Unit     │  Majority — pure logic, mappers, policies
                 └─────────────┘
```

| Layer | % of automated effort (target) | Speed | Flakiness budget |
|-------|-------------------------------|-------|------------------|
| Unit | 60–70% | &lt; 1s class | Zero tolerated |
| Slice / component | 15–20% | Seconds | Low |
| Integration | 10–15% | Tens of seconds | Low; quarantine if flaky |
| E2E / sandbox | 5% | Minutes | Nightly / gated OK |

**Anti-pattern:** Replacing unit tests with only Spring context boots. Context tests prove wiring; they do **not** prove business rules.

---

## 3. Test types we require

### 3.1 Unit tests (Developer-owned — **mandatory**)

**What:** Single class / pure function. No Spring context unless the class *is* a Spring bean with heavy DI — prefer constructor + mocks.

**Must cover:**
- Domain policies & state transitions (`JobStatus`, idempotency body-hash rules)
- Mappers / normalisers (`OneSbErrorNormaliser`, future quote/proposal mappers)
- Utilities (`PiiMasker`, hash helpers)
- Shared libs (`ServiceErrorResponse`, `AuditEvent`, secret providers)
- Edge cases: null, blank, boundary, invalid enum, conflict paths

**Tools:** JUnit 5, AssertJ, Mockito. Lombok OK.

**Rule:** Every new non-trivial public method in `application`, `lob`, `adapter.*.error|mapper`, and libs gets a unit test **in the same PR**.

---

### 3.2 Slice / component tests (Developer-owned — **mandatory** for adapters & APIs)

| Slice | Use when |
|-------|----------|
| `@WebMvcTest` | Controllers + filters (idempotency, future `/v1/quotes`) |
| `@DataJpaTest` or MockMvc+H2 Boot | Persistence repositories/controllers |
| WireMock | Outbound 1SB HTTP (`OneSbHttpClient`, poller) |
| `MockRestServiceServer` | Outbound persistence HTTP (`HttpJobStoreAdapter`) |
| ArchUnit | Hex boundaries — already started; tighten as packages fill |

---

### 3.3 Integration tests (Developer-owned primary; QA designs cases)

**Definition:** Multiple real components together **without** mocking the boundary under test.

| Suite | Scope | Tech |
|-------|-------|------|
| **IT-P** Persistence | Flyway + H2/Testcontainers Postgres + HTTP API | `@SpringBootTest` + MockMvc or RestClient |
| **IT-I** Integration service | App + WireMock(1SB) + WireMock/Testcontainers(persistence) | `@SpringBootTest` |
| **IT-D** Dual-service | Real `bank-persistence` + `1sb-integration` processes | Testcontainers / docker-compose CI job |

**Minimum before Phase 3 exit:** IT-P for job create→poll-attempt→complete+offers; IT-I for quote happy path with WireMock 1SB (when FUNC-002 lands).

---

### 3.4 Contract tests (QA + Dev; Automation-friendly)

- OpenAPI for bank `/v1/**` published; consumer-driven checks (Schemathesis / Dredd / Prism) optional later
- Persistence internal API: markdown/OpenAPI kept in sync; smoke contract in CI
- **Pact** only if multiple bank consumers demand it (Phase 4+)

---

### 3.5 End-to-end / sandbox (QA-owned design; Automation / AI execute)

- Journey: masters → quote → proposal → payment → status against **1SB sandbox**
- Gated: secrets required; nightly or manual promote — not every PR
- Evidence: jobId, audit events present, no PII in collected logs

---

### 3.6 Non-functional & compliance (split ownership)

| Type | Owner | Automation |
|------|-------|------------|
| PII / log masking regression | Dev (unit) + QA sample review | AI can generate payload corpora |
| Performance smoke (p95 quote) | Eng + QA | k6/Gatling scripts; AI drafts scenarios |
| Security (authn/z, no secret leak) | Security + QA | Semgrep/gitleaks in CI; AI assist review |
| Chaos / upstream 5xx / timeout | QA designs | WireMock fault injection in IT-I |
| Accessibility | N/A (API-only) | — |

---

## 4. Ownership matrix

| Activity | Developer | QA Lead / QA | Automation / AI agents |
|----------|-----------|--------------|------------------------|
| Unit tests for own code | **R/A** | C (reviews gaps) | Assist: generate cases from AC; **never** replace review |
| Slice tests (MVC/JPA/WireMock) | **R/A** | C | Assist scaffolding |
| Integration tests (IT-P/IT-I) | **R** | **A** for scenarios | Scaffold + maintain WireMock stubs |
| Dual-service IT-D / sandbox E2E | C | **R/A** | **R** execute in CI/nightly |
| Coverage gate / JaCoCo | **R** wire in Gradle | **A** thresholds | Report in PR |
| Test data & PII corpora | C | **A** | **R** generate masked fixtures |
| Flaky triage | **R** | **A** quarantine policy | Detect flakes across runs |
| Exploratory / UAT sign-off | — | **R/A** | Suggest charters |
| ArchUnit / static rules | **R** | C | Propose new rules from architecture |

R = Responsible · A = Accountable · C = Consulted

---

## 5. What developers must do (DoD for every story)

See [TESTING-RULES.md](./TESTING-RULES.md). Summary:

1. **Unit first** for logic; PR blocked without tests for new `application`/`lob`/mapper code.
2. **WireMock** for any new 1SB call path.
3. **MockMvc** for any new bank-facing endpoint + idempotency behaviour.
4. **No secrets** in test resources committed as real values — placeholders only.
5. **Green** `./gradlew test` locally before push.
6. After JaCoCo lands: meet module thresholds or justify waiver in TECH-DEBT.

---

## 6. What Automation / AI agents should do

**Allowed / encouraged**
- Generate unit test skeletons from public methods + backlog AC
- Expand WireMock stub libraries from `api-catalog`
- Propose edge cases (partial offers, 401, timeout, idempotency conflict)
- Maintain fixtures under `src/test/resources/fixtures/` (already masked)
- Run regression packs and summarize failures
- Draft k6 scripts from NFR table
- Flag missing tests via PR checklist bot

**Forbidden**
- Approving their own PRs without human QA/TL review
- Weak tests that only call constructors / assertNotNull without behaviour
- Hitting real 1SB sandbox from every PR (cost + flake + credential risk)
- Committing production credentials or live PII

---

## 7. Coverage targets (once JaCoCo enabled)

| Module / package | Line | Branch | When enforced |
|------------------|------|--------|---------------|
| `libs/*` | 80% | 70% | Immediate after JaCoCo PR |
| `…onesb.application.*` / `…lob.*` | 80% | 70% | From first Phase 3 story |
| `…adapter.onesb.*` | 70% | 60% | Phase 2 follow-up |
| `…adapter.idempotency.*` | 80% | 70% | Phase 2 follow-up |
| `com.bank.persistence.api.*` | 70% | 60% | Before Phase 3 exit |
| Config / Spring Boot main | Excluded | — | Always |

**Principle:** Coverage is a **floor**, not a goal. Mutation testing (PIT) optional Phase 4 for mappers.

---

## 8. Environments & data

| Env | Tests allowed |
|-----|----------------|
| Local / CI | Unit, slice, IT with H2 or Testcontainers; WireMock |
| UAT | E2E sandbox; exploratory |
| Prod | Synthetic health probes only — **no** functional test suites |

Test data: synthetic customers only; PAN/mobile must be obviously fake; masking tests use dedicated fixtures.

---

## 9. Traceability

Every P0 backlog AC (PRODUCT-BACKLOG) maps to ≥1 automated test id:

```text
FUNC-002 AC "missing fields → 422, no 1SB call"
  → QuoteControllerSliceTest.missingFields_noUpstreamCall
  → tagged @Tag("FUNC-002")
```

QA maintains the map in [TEST-BACKLOG.md](./TEST-BACKLOG.md) as stories close.

---

## 10. Immediate QA directives (next engineering work)

1. **QA-001** Wire JaCoCo + publish HTML/XML; fail build under thresholds (libs first).
2. **QA-002** Fill persistence API tests (jobs, offers, payment, audit, exception handler).
3. **QA-003** Add IT-I template: integration service + WireMock 1SB + WireMock persistence.
4. **QA-004** Phase 3 definition of done includes unit+slice+IT for quote path.
5. **QA-005** CI badge/report for coverage on PR.

Owned as backlog items in [TEST-BACKLOG.md](./TEST-BACKLOG.md); tech-debt cross-links in TECH-DEBT.md.
