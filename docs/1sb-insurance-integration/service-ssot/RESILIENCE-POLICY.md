# 1SB egress resilience — poll, retry and circuit breaker

**Work items:** `NFR-007`, `NFR-004` · **Origin:** `CR-014` / `EPIC-002` / `SUG-20260903-lif`  
**Owner:** Amit (R3) with Shivanshi (R10) on operational policy

This is the binding policy for how `1sb-integration-service` talks to 1SB. Config keys live under `onesb.*` in `application.yml`.

---

## 1. Async poll (quote / proposal jobs)

| Parameter | Config | Default | Meaning |
|-----------|--------|---------|---------|
| Base delay | `onesb.poll.base-delay-ms` | `1000` | First wait before poll |
| Backoff multiplier | `onesb.poll.multiplier` | `2.0` | Exponential growth |
| Max delay cap | `onesb.poll.max-delay-ms` | `30000` | Ceiling between attempts |
| Max attempts | `onesb.poll.max-attempts` | `20` | **Stop** after this many polls |

**Stop conditions (must all be implemented):**

1. Upstream reports poll complete → job `SUCCEEDED` (or equivalent) with offers / application number.
2. Attempt count reaches `max-attempts` → job `TIMEOUT`; bank caller receives a **retryable** error (`QUOTE_TIMEOUT` / proposal equivalent).
3. Upstream returns a terminal business failure → job `FAILED`; map to normalised error; do not keep polling.
4. Process shutdown / job cancel → stop scheduling further attempts.

**Do not:** poll forever, reset attempt counters on transient network blips without counting, or treat TIMEOUT as success.

---

## 2. Synchronous HTTP retry (single request)

| Condition | Retry? | Notes |
|-----------|--------|-------|
| HTTP **401** / auth failure | **Never** | Map to `UPSTREAM_AUTH_FAILURE`; alert; fix credentials / allowlist |
| HTTP **408** / **429** / **5xx** | Limited | At most **2** automatic retries with short backoff; then fail retryable to caller |
| HTTP **4xx** (other) | Never | Client/contract error — fail closed |
| Connection timeout | Limited | Counts toward the same retry budget as 5xx |
| Circuit **open** | Never | Fail fast `503` retryable — see §3 |

Idempotent GETs (poll) may use the limited retry budget. Non-idempotent POSTs (quote/proposal submit) rely on bank idempotency keys + job store; do not blind-retry POST after an unknown response body without idempotency protection.

---

## 3. Circuit breaker / bulkhead (`NFR-004`)

| Setting | Intent |
|---------|--------|
| Scope | One breaker around **1SB HTTP egress** (`OneSbHttpClient`) |
| Open when | Consecutive failure threshold (config) or failure-rate window exceeded |
| While open | Callers get **503** with `retryable=true`; no upstream call |
| Half-open | Single probe; success → closed; failure → open again |
| Bulkhead | Life LOB handlers share the 1SB egress breaker in Phase 4b; per-LOB bulkheads land when Health/Motor arrive (Phase 5) so Motor storms cannot starve Term |

**Never** open the breaker on business validation failures (4xx that are our payload mistakes). Count upstream timeouts, 5xx, and connection errors.

---

## 4. Observability

Emit metrics (names may follow Micrometer conventions already in the service):

- poll attempts, poll timeouts, poll completions
- upstream 401 / 5xx counts
- circuit state (closed / open / half-open) and rejected calls

Logs: no PII; include `jobId`, `lob`, `attempt`, `breakerState` only.

---

## 5. Acceptance hooks

- `NFR-007`: unit/IT proves max-attempts exhaustion → TIMEOUT retryable; 401 never retried.
- `NFR-004`: unit/IT proves open breaker → 503 retryable without calling WireMock; recovery on half-open success.
- Runbook (`ACTION-PLAN` 4.5) must mention breaker-open and poll-timeout playbooks before Phase 4 exit sign-off where those paths are live.
