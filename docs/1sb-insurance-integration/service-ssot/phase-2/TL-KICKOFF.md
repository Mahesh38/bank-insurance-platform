# Phase 2 — Tech Lead Kickoff & Task Breakdown

**Role:** Tech Lead (20+ years)  
**Branch:** `cursor/phase1-foundations-c259`  
**Date:** 2026-07-30  
**SSOT:** [ACTION-PLAN.md](../ACTION-PLAN.md) §Phase 2 · [PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md) · [architecture](../../architecture/1sb-integration-service-architecture.md)

---

## 1. Phase 2 goal

Deliver **connectivity + async infra** so Phase 3 LOB vertical slices can call 1SB safely:

| Backlog ID | Title |
|------------|-------|
| TECH-004 | OneSbHttpClient |
| TECH-005 | Error normalisation |
| COMP-001 | Outbound call audit hook |
| COMP-002 | PII masking in logs |
| TECH-006 | Job store port + impl (complete vs Phase 1 stub) |
| TECH-007 | Async poller |
| NFR-001 | Idempotency-Key filter |

**Exit criteria (ACTION-PLAN):**
1. Sandbox master/probe call succeeds via `OneSbHttpClient` (WireMock acceptable in CI; live sandbox optional).
2. Poller unit-tested pending→complete (WireMock).
3. Masking tests prove no PAN/mobile/name in logs.

**Topology reminder (do not violate):**
- 1SB HTTP **only** in `adapter.onesb.*`
- DB **only** via `bank-persistence-service` HTTP (`JobStorePort` / future ports)
- No Flyway/JPA in integration service
- Secrets from `bank-common-secrets`
- SOLID + DRY + KISS; Lombok/records per TD-012

---

## 2. Scope boundaries

### In scope
- Integration service adapters + thin application scaffolding needed to prove client/poller/idempotency
- Persistence HTTP additions required for poll-attempt / job status completeness (TD-015 subset)
- Unit + WireMock tests; ArchUnit still green

### Out of scope (Phase 3+)
- Full Term/Saving quote/proposal controllers (FUNC-002+)
- Real AWS Secrets Manager (TD-006)
- Redis-backed idempotency (in-memory OK for Phase 2 with interface for Redis later)
- Full audit-consumer Boot app
- COMP-003 raw payload encryption at rest

---

## 3. Small tasks with detailed AC

### P2-A1 · COMP-002 — PII masking utility
**Owner:** Dev A · **Independence:** Fully independent  

**Deliverables:**
- `com.bank.insurance.onesb.observability.PiiMasker` (or `adapter`/`observability`) with mask rules for: name, mobile, email, PAN/ID, DOB
- Unit tests with sample JSON/maps proving plaintext never returned

**AC:**
1. Given a string/JSON containing PAN `ABCDE1234F`, When masked, Then output contains no contiguous PAN pattern (masked as `XXXXXX1234F` or `***`).
2. Given mobile `9876543210`, When masked, Then last 4 digits only visible max.
3. Given full name / email / DOB ISO date, When masked, Then originals absent from result.
4. Null/blank inputs safe (no NPE).

---

### P2-A2 · TECH-004 — OneSbHttpClient
**Owner:** Dev A · **Depends on:** — (config already in `onesb.client.*`)

**Deliverables:**
- `adapter.onesb.client.OneSbHttpClient` using Spring `RestClient` (or WebClient) 
- Basic Auth from `SecretProvider` (apiKey:apiSecret)
- Connect/read timeouts from config (defaults 3s/30s)
- Method(s): `exchange(method, path, body, responseType)` or typed helpers
- **No retry on 401**; map to `UPSTREAM_AUTH_FAILURE` via exception
- WireMock tests for 200 + 401
- Config class under `adapter.onesb.config`

**AC:**
1. Given WireMock 200 JSON, When GET probe path, Then Basic Auth header present and body deserialized.
2. Given WireMock 401, When called, Then `ServiceException` with `UPSTREAM_AUTH_FAILURE`, **single** request (no retry).
3. Connect/read timeouts bound from `onesb.client.*`.
4. ArchUnit: only `adapter.onesb..` may use the client package types.
5. No credentials logged.

---

### P2-A3 · TECH-005 — Error normalisation
**Owner:** Dev A · **Depends on:** P2-A2 (client error path)

**Deliverables:**
- `adapter.onesb.error.OneSbErrorNormaliser`
- Maps 1SB `errors[]` / HTTP status → `ServiceError` / `ServiceErrorResponse` / `ServiceException`
- Controllers (when present) never see raw 1SB JSON — enforce via client wrapper returning bank errors only

**AC:**
1. Given 1SB body with `errors[{field, code, message}]`, When normalised, Then bank `ServiceError` list populated; `code` is bank code (e.g. `UPSTREAM_BUSINESS_ERROR`), not raw passthrough as sole response.
2. Given 5xx, Then `UPSTREAM_UNAVAILABLE` retryable.
3. Given 401, Then `UPSTREAM_AUTH_FAILURE` not retryable.
4. Unit tests cover at least 3 status families (4xx business, 401, 5xx).

---

### P2-A4 · COMP-001 — Outbound audit hook
**Owner:** Dev A · **Depends on:** P2-A2, P2-A1 (mask request hash input)

**Deliverables:**
- Hook in `OneSbHttpClient` (or interceptor) emitting `AuditEvent` via `AuditEventPublisher` SPI (simple logging impl OK if no publisher bean yet)
- Fields: operation, latencyMs, upstreamHttpStatus, requestHash (masked payload hash), outcome SUCCESS|FAILURE
- Never put raw PII in audit metadata

**AC:**
1. Given successful call, When completed, Then one audit event with SUCCESS + latency ≥ 0 + status 200.
2. Given 401/5xx, When completed, Then FAILURE audit still emitted.
3. requestHash is hash of **masked** body (or headers-safe summary), not plaintext PAN.
4. Unit/integration test verifies publisher invoked (mock).

---

### P2-B1 · NFR-001 — Idempotency filter
**Owner:** Dev B · **Independence:** Fully independent of 1SB client

**Deliverables:**
- `IdempotencyPort` + in-memory store (ConcurrentHashMap + TTL optional/simple)
- Servlet filter or `HandlerInterceptor` requiring `Idempotency-Key` on `POST`/`PATCH`/`PUT` under `/v1/**` (and ready for future controllers)
- Replay same key+same body hash → cached response
- Same key+different body → 409
- Missing key → 400 `MISSING_IDEMPOTENCY_KEY` (add to `ErrorCodes` if absent)
- Stub controller or MockMvc test endpoint under `/v1/_idempotency_probe` (test profile only) **or** MockMvc with a `@RestController` test config

**AC:**
1. Missing header → 400 + `MISSING_IDEMPOTENCY_KEY`.
2. First POST stores response; second identical key+body returns first status/body without re-invoking controller logic (verify via counter bean).
3. Same key, different body → 409.
4. GET requests do not require key.

**Note:** Redis later; interface must allow swap (KISS in-memory now).

---

### P2-B2 · TECH-006 — Job store completion
**Owner:** Dev B · **Independence:** Independent of OneSbHttpClient

**Deliverables:**
- Harden `JobStorePort` + `HttpJobStoreAdapter` for status transitions: `PENDING|RUNNING|COMPLETED|PARTIAL|FAILED|TIMEOUT`
- Record external `reqId` via `updateJobPolling`
- Persistence: ensure PATCH status + offers APIs cover COMPLETED with offers; add **poll-attempt** HTTP if needed for poller (minimal `POST /internal/v1/jobs/{id}/poll-attempts`)
- Unit tests with MockRestServiceServer

**AC:**
1. `createJob` returns bank `jobId`; status PENDING.
2. `updateJobPolling` sets RUNNING + externalReqId.
3. `completeJob` → COMPLETED + offers persisted/retrievable via `findQuoteJob`.
4. `failJob` with reason TIMEOUT → TIMEOUT or FAILED per model enum consistency (document choice; prefer status TIMEOUT if enum has it).
5. Invalid transition attempts documented (best-effort; don't overbuild state machine).

---

### P2-B3 · TECH-007 — Async poller
**Owner:** Dev B · **Depends on:** P2-B2; uses port that Dev A’s client can satisfy (`OneSbPollPort` or generic exchange)

**Deliverables:**
- `adapter.onesb.polling.AsyncJobPoller` (+ config backoff: base 1s, multiplier 2, cap 30s, max attempts 20 — configurable)
- Runs on dedicated executor / virtual threads — **not** Tomcat request thread
- Poll until complete flag or max attempts → `TIMEOUT` via JobStore
- WireMock: pending then complete

**AC:**
1. Given WireMock returns incomplete then complete, When poller runs, Then job COMPLETED and poll attempts ≥ 2.
2. Given always incomplete until max, Then job TIMEOUT/FAILED with poll timeout reason; retryable error semantics documented.
3. Poller methods return immediately after scheduling (async) OR test uses poller.driveSynchronously for unit test — prefer testable `pollUntilDone` package-visible for unit + async wrapper for prod.
4. No blocking sleep on HTTP request thread in production API path (N/A until Phase 3 controllers; enforce in poller design).

---

## 4. Distribution summary

| Dev | Iteration 1 order | Parallel with other? |
|-----|-------------------|----------------------|
| **Dev A** | P2-A1 → P2-A2 → P2-A3 → P2-A4 | A1 parallel with B1/B2 |
| **Dev B** | P2-B1 → P2-B2 → P2-B3 | B1/B2 parallel with A*; B3 after B2 (wire client interface from A2) |

**Conflict zones:**
- Dev A owns `adapter/onesb/client|error|config` + `observability/PiiMasker`
- Dev B owns `adapter/onesb/polling`, idempotency package, `HttpJobStoreAdapter` / persistence poll-attempt API
- Shared: `ErrorCodes`, `application.yml` — append-only; communicate via STATUS.md
- Do not both edit `ArchitectureTest` without pull; append rules only

---

## 5. Commit convention

Each task = **one commit** (or stacked commits per task id):

```text
feat(p2-a1/COMP-002): PII masking utility
feat(p2-a2/TECH-004): OneSbHttpClient with Basic Auth and WireMock
feat(p2-a3/TECH-005): OneSb error normalisation
feat(p2-a4/COMP-001): outbound 1SB audit hook
feat(p2-b1/NFR-001): Idempotency-Key filter with in-memory store
feat(p2-b2/TECH-006): job store status transitions + poll-attempt API
feat(p2-b3/TECH-007): async job poller with backoff
```

Push to `cursor/phase1-foundations-c259`. Update `phase-2/STATUS.md` after each task.

---

## 6. Review gates (TL)

For each commit/task TL checks:
1. AC met with automated tests
2. Architecture boundaries (hex, no JPA in integration, 1SB only in adapter.onesb)
3. No secrets in logs/git
4. KISS — no Phase 3 LOB handlers unless stub required
5. TECH-DEBT updated for intentional shortcuts

Max **2** review→fix iterations after initial delivery.

---

## 7. Phase 2 exit checklist

- [x] All P2-A* and P2-B* tasks merged on branch
- [x] `./gradlew build` green
- [x] ACTION-PLAN Phase 2 exit criteria satisfied (WireMock)
- [x] TL final approval recorded in `phase-2/TL-REVIEW.md`
