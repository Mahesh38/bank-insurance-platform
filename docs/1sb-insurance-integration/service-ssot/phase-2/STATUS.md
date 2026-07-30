# Phase 2 — Status

**Branch:** `cursor/phase1-foundations-c259`  
**Kickoff:** [TL-KICKOFF.md](./TL-KICKOFF.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| P2-A1 COMP-002 PII masking | Dev A | Done | `6f0b7ed` |
| P2-A2 TECH-004 OneSbHttpClient | Dev A | Done | `7e971ff` |
| P2-A3 TECH-005 Error normalisation | Dev A | Done | `0cd9ed3` |
| P2-A4 COMP-001 Outbound audit | Dev A | Done | `4990a36` |
| P2-B1 NFR-001 Idempotency filter | Dev B | Done | `596a3e0` |
| P2-B2 TECH-006 Job store | Dev B | Done | `aa73b6f` |
| P2-B3 TECH-007 Async poller | Dev B | Done | `6b1c294` |

## TL reviews

| Iteration | Date | Outcome |
|-----------|------|---------|
| 1 | 2026-07-30 | **APPROVED** — all P2-A1..A4 / P2-B1..B3; no P0. See [TL-REVIEW-ITER1.md](./TL-REVIEW-ITER1.md). Optional P1 hygiene only for iter-2. |
| 2 | 2026-07-30 | Dev A P1: 5xx FAILURE audit + masked≠plaintext requestHash. Dev B P1: `@Qualifier("persistenceRestClient")` on `HttpJobStoreAdapter` (`fd7ad1b`); `JobPollAttemptApiTest` poll-attempt save + 404 (`0d9d04e`). |
| Final | 2026-07-30 | **APPROVED — Phase 2 complete.** See [TL-REVIEW.md](./TL-REVIEW.md). |

## Dev B notes

- **P2-B1:** `IdempotencyPort` + in-memory store; filter on POST/PUT/PATCH `/v1/**`; `MISSING_IDEMPOTENCY_KEY` added to `ErrorCodes`. Redis swap deferred (TD-010).
- **P2-B2:** `JobStatus` → `PENDING|RUNNING|COMPLETED|PARTIAL|FAILED|TIMEOUT`; `failJob(POLL_TIMEOUT)` → `TIMEOUT`; poll-attempt HTTP on persistence (TD-015 partial).
- **P2-B2 iter-2 (P1):** `HttpJobStoreAdapter` → `@Qualifier("persistenceRestClient")` (`fd7ad1b`); persistence `JobPollAttemptApiTest` for POST save + missing-job 404 (`0d9d04e`).
- **P2-B3:** `AsyncJobPoller` + `onesb.poll.*` backoff (arch §7.4); `OneSbPollPort` via `OneSbHttpClientPollAdapter`; WireMock pending→complete and max-attempts→`TIMEOUT`; `schedulePoll` is non-blocking.

## Dev A notes

- **P2-A1:** `PiiMasker` utility landed (`6f0b7ed`).
- **P2-A2:** `OneSbHttpClient` + `adapter.onesb.config` (Basic Auth, timeouts, `get`/`post`/`exchange`); WireMock 200/401; HTTP/1.1 for WireMock compatibility.
- **P2-A3:** `OneSbErrorNormaliser` maps 401 / business 4xx (`errors[]`) / 5xx → bank `ServiceException`; wired into client.
- **P2-A4:** Outbound audit hook on `OneSbHttpClient` via `LoggingAuditEventPublisher`; requestHash = SHA-256 of PiiMasker-masked body.
- **Iter-2 (P1):** `OneSbHttpClientTest` — WireMock 503 emits FAILURE audit; requestHash asserts `hash(masked) ≠ hash(plaintext)`.
