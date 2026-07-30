# Phase 2 — Tech Lead Review (Iteration 1)

**Role:** Tech Lead  
**Branch:** `cursor/phase1-foundations-c259`  
**Date:** 2026-07-30  
**Authority:** [TL-KICKOFF.md](./TL-KICKOFF.md) · [ACTION-PLAN.md](../ACTION-PLAN.md) §Phase 2 · hex boundaries  
**Build:** `./gradlew :services:1sb-integration-service:test` — **GREEN** (incl. ArchUnit + ApplicationContext)

---

## Verdict

**Iteration 1: APPROVED overall.** All seven tasks meet kickoff AC with real automated tests. No architecture boundary violations. No P0 blockers. No mandatory iteration-2 rework required for Phase 2 exit.

Optional P1 hygiene (below) may land in a short iter-2 pass before Phase 3 if capacity allows — **not** required to close Phase 2.

---

## Cross-cutting checks

| Check | Result |
|-------|--------|
| `./gradlew` tests | Green for integration service (PiiMasker, OneSbHttpClient, ErrorNormaliser, audit, Idempotency, JobStore, AsyncJobPoller, Architecture, ApplicationContext) |
| ErrorCodes | `MISSING_IDEMPOTENCY_KEY` added (`ErrorCodes.java:15`); `UPSTREAM_*` + `IDEMPOTENCY_CONFLICT` already present and used |
| ArchUnit | Still sensible: hex layers, `adapter.onesb.client` isolation, no JPA/Flyway in integration, no `SecretProvider` impl in service. `allowEmptyShould(true)` remains (TD-007) — acceptable until packages fill further |
| Secrets not logged | `OneSbHttpClient` logs method+path only; Basic Auth via `SecretProvider` headers; audit metadata uses masked-body hash; startup validator logs credential *names*, not values |
| Commit-per-task | Met — one feat commit per task id; STATUS updates in-task or follow-up docs commits for SHA corrections |
| Hex / topology | 1SB HTTP only under `adapter.onesb.*`; DB only via `HttpJobStoreAdapter` → bank-persistence; no JPA/Flyway in integration |

**Commit map (discipline OK):**

| Task | Commit | Message |
|------|--------|---------|
| P2-B1 | `596a3e0` | `feat(p2-b1/NFR-001): Idempotency-Key filter with in-memory store` |
| P2-A1 | `6f0b7ed` | `feat(p2-a1/COMP-002): PII masking utility` |
| P2-B2 | `aa73b6f` | `feat(p2-b2/TECH-006): job store status transitions + poll-attempt API` |
| P2-A2 | `7e971ff` | `feat(p2-a2/TECH-004): OneSbHttpClient with Basic Auth and WireMock` |
| P2-A3 | `0cd9ed3` | `feat(p2-a3/TECH-005): OneSb error normalisation` |
| P2-B3 | `6b1c294` | `feat(p2-b3/TECH-007): async job poller with backoff` |
| P2-A4 | `4990a36` | `feat(p2-a4/COMP-001): outbound 1SB audit hook` |

Parallel order (B1/A1/B2 then A2/A3/B3/A4) matches kickoff ownership.

**ACTION-PLAN Phase 2 exit criteria:**

1. Probe call via `OneSbHttpClient` — WireMock 200 in `OneSbHttpClientTest` — **met**
2. Poller pending→complete WireMock — `AsyncJobPollerTest` — **met**
3. Masking proves no PAN/mobile/name — `PiiMaskerTest` field/JSON — **met**

---

## Per-task review

### P2-A1 · COMP-002 — PiiMasker — **APPROVE**

| | |
|--|--|
| **AC met?** | Yes (1–4) |
| **Arch** | None — utility in `observability` (kickoff-allowed) |
| **Evidence** | `PiiMasker.java`; `PiiMaskerTest.java` |

- PAN → `*****1234F`, no contiguous PAN pattern (`PiiMaskerTest:18–24`)
- Mobile last-4 only (`:27–33`)
- Name / email / DOB originals absent via dedicated + JSON/map paths (`:36–41`, `:56–89`)
- Null/blank safe (`:44–53`)

**Note (non-blocking):** `maskText` pattern scrub covers PAN/mobile/email/DOB, not free-form personal names. Field/JSON masking covers name — sufficient for A1 AC and audit hash path.

---

### P2-A2 · TECH-004 — OneSbHttpClient — **APPROVE**

| | |
|--|--|
| **AC met?** | Yes (1–5) |
| **Arch** | Client confined to `adapter.onesb.client`; config in `adapter.onesb.config`; Basic Auth from `SecretProvider`; ArchUnit `onlyOneSbAdapterMayImportOneSbClientPackage` green |
| **Evidence** | `OneSbHttpClient.java`; `OneSbClientConfig.java`; `OneSbClientProperties.java`; `OneSbHttpClientTest.java` |

- WireMock 200 + Basic Auth header + deserialize (`OneSbHttpClientTest:79–90`)
- 401 → `UPSTREAM_AUTH_FAILURE`, exactly one request (`:92–104`)
- Timeouts default 3s/30s from `onesb.client.*` (`OneSbClientProperties:18–23`; test `:119–126`)
- No credentials in log statements (`OneSbHttpClient:76`)

---

### P2-A3 · TECH-005 — ErrorNormaliser — **APPROVE**

| | |
|--|--|
| **AC met?** | Yes (1–4) |
| **Arch** | Lives in `adapter.onesb.error`; wired into client so callers get bank `ServiceException` only |
| **Evidence** | `OneSbErrorNormaliser.java`; `OneSbErrorNormaliserTest.java` |

- `errors[]` → bank `ServiceError` list; top-level code `UPSTREAM_BUSINESS_ERROR`, raw code in `upstreamCode` only (`OneSbErrorNormaliserTest:28–51`)
- 5xx → `UPSTREAM_UNAVAILABLE` retryable (`:53–60`)
- 401 → `UPSTREAM_AUTH_FAILURE` not retryable (`:19–26`)
- Three status families covered

---

### P2-A4 · COMP-001 — Outbound audit hook — **APPROVE**

| | |
|--|--|
| **AC met?** | Yes (1–4) |
| **Arch** | Hook inside `OneSbHttpClient`; publisher SPI + logging impl; no raw PII in metadata |
| **Evidence** | `OneSbHttpClient.publishAudit` / `hashMaskedBody`; `LoggingAuditEventPublisher.java`; `AuditActions.ONESB_OUTBOUND_CALL`; tests in `OneSbHttpClientTest` |

- SUCCESS + latency ≥ 0 + status 200 (`OneSbHttpClientTest:128–141`)
- 401 → FAILURE audit still emitted (`:143–151`)
- `requestHash` = SHA-256 of PiiMasker-masked body (`OneSbHttpClient:142–158`; test `:153–165`)
- Publisher invoked via recording mock

**P1 (optional):** Add WireMock **5xx** FAILURE audit assertion to fully mirror AC2 wording (impl already emits via `finally`; only 401 covered today).

---

### P2-B1 · NFR-001 — Idempotency filter — **APPROVE**

| | |
|--|--|
| **AC met?** | Yes (1–4) |
| **Arch** | Port + in-memory adapter; filter on mutating `/v1/**`; Redis swap deferred (TD-010) |
| **Evidence** | `IdempotencyPort`, `InMemoryIdempotencyStore`, `IdempotencyFilter`, `IdempotencyFilterTest` |

- Missing key → 400 + `MISSING_IDEMPOTENCY_KEY` (`IdempotencyFilterTest:59–67`)
- Replay same key+body without re-invoking controller (counter stays 1) (`:69–90`)
- Same key, different body → 409 + `IDEMPOTENCY_CONFLICT` (`:92–108`)
- GET exempt (`:110–115`)
- Probe controller is **test-only** (`@Import` / `@TestConfiguration`) — not production surface

---

### P2-B2 · TECH-006 — Job store — **APPROVE**

| | |
|--|--|
| **AC met?** | Yes (1–5) |
| **Arch** | HTTP adapter only; poll-attempt on `bank-persistence-service`; no JPA in integration |
| **Evidence** | `JobStorePort`, `JobStatus`, `HttpJobStoreAdapter`, `HttpJobStoreAdapterTest`; persistence `JobController` poll-attempts |

- `createJob` → jobId; persistence creates `PENDING` (`JobController:54`)
- `updateJobPolling` → RUNNING + `externalReqId` (test `:75–86`)
- `completeJob` → COMPLETED + offers POST; `findQuoteJob` retrieves COMPLETED+offers (`:88–111`, `:154–203`)
- `failJob(POLL_TIMEOUT)` → `TIMEOUT` (documented choice; test `:113–124`)
- Invalid transitions: best-effort documented on `JobStorePort` javadoc (no overbuilt FSM)

**P1 (optional):** Persistence `POST/GET .../poll-attempts` has **no** dedicated test (only MockRest on adapter). Add a thin `@WebMvcTest`/`@DataJpaTest` on `JobController.addPollAttempt` before Phase 3 load.

**P1 (optional):** Annotate `HttpJobStoreAdapter(RestClient)` with `@Qualifier("persistenceRestClient")` — second RestClient (`oneSbRestClient`) now exists; name-based wiring works (`-parameters`) but qualifier hardens the boundary.

---

### P2-B3 · TECH-007 — AsyncJobPoller — **APPROVE**

| | |
|--|--|
| **AC met?** | Yes (1–4) |
| **Arch** | `adapter.onesb.polling` + `OneSbPollPort`; uses `OneSbHttpClient` only via poll adapter inside `adapter.onesb.*`; dedicated `pollingExecutor` |
| **Evidence** | `AsyncJobPoller`, `PollingProperties`/`PollingConfig`, `OneSbHttpClientPollAdapter`, `AsyncJobPollerTest` |

- WireMock incomplete→complete → COMPLETED, attempts ≥ 2 (`AsyncJobPollerTest:60–86`)
- Max incomplete → `TIMEOUT` + `POLL_TIMEOUT` (`:88–104`); retryable semantics noted in class javadoc
- `pollUntilDone` testable; `schedulePoll` returns immediately (`:106–132`)
- Sleep only on poller worker / test sleeper — not Tomcat request thread

---

## Assignment — Iteration 2 (optional hygiene only)

**No P0. No task reopened as CHANGES_REQUESTED.**  
If doing a short iter-2 polish before Phase 3:

### Dev A (optional)

1. **P1** — `OneSbHttpClientTest`: assert FAILURE audit on WireMock **5xx** (close A4 AC2 coverage parity with 401).
2. **P1** — Strengthen `requestHash` test: assert `hash(masked) != hash(plaintext)` (today hash≠PAN is weak for hex digests).

### Dev B (optional)

1. **P1** — `@Qualifier("persistenceRestClient")` on `HttpJobStoreAdapter` constructor.
2. **P1** — Persistence-side test for `POST /internal/v1/jobs/{id}/poll-attempts` (entity save + 404 on missing job).

Do **not** start Phase 3 LOB controllers in this polish pass.

---

## Phase 2 exit checklist (post-iter1)

- [x] All P2-A* and P2-B* tasks on branch with feat commits
- [x] Integration service tests green (WireMock + MockMvc + MockRest + ArchUnit)
- [x] ACTION-PLAN Phase 2 exit criteria satisfied (WireMock)
- [x] TL iteration-1 approval recorded (this file)
- [ ] Optional: final `TL-REVIEW.md` / STATUS closeout after any iter-2 polish (or promote this approval as final if polish skipped)

---

## Sign-off

**Outcome:** APPROVE iteration 1 — proceed to Phase 3 when PO/TL schedule allows.  
**Iteration-2:** optional P1 hygiene only; not required for Phase 2 exit.
