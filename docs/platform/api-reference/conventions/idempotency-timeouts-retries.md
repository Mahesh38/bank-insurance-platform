# Idempotency, timeouts and retries

[← API reference home](../README.md)

A human is waiting → the call is **synchronous**, with a named timeout and a named failure.
A side effect of a completed step (audit, notification, compensation) → **asynchronous via the
outbox**, at-least-once, never silently dropped. Quote and proposal **submit** are async-poll with
**no automatic retry on submit** — re-sending a request the provider may already have processed is
the failure this rule exists to prevent ([HLD §4.1](../../../architecture/R0-HLD.md#41-the-rule)).

---

## 1. Idempotency

### 1.1 The platform standard (`INV-IDM-01`, `03` §5.2)

| Aspect | Rule |
|---|---|
| Header | `Idempotency-Key`, **required on every mutating platform API** |
| Who supplies it | The client at the edge (NIP-APP generates a UUID per user action). **Server-derived** on internal hops from owning aggregate id + operation, e.g. `quote:{quoteId}:submit` — so a retried internal call cannot invent a new key |
| Where it is stored | In the **owning service's own store**, written in the **same transaction** as the business change: a `<schema>.idempotency_record` row in Aurora, or an `IDEM#{key}` item in the same `TransactWriteItems` for the DynamoDB working tables (`DB-DEC-0005`) |
| Never stored in | Valkey / ElastiCache (`ADR-011` refuses this by name — a cache eviction would allow a double proposal or a double charge) |
| Retention | 24 hours |
| Same key, same body | Return the stored response with its original status |
| Same key, different body | `409 IDEMPOTENCY_CONFLICT` |
| Same key, first still running | `409 REQUEST_IN_PROGRESS` |
| Missing key | `400 MISSING_IDEMPOTENCY_KEY` |
| Money path extra | Payment also refuses a new attempt while a prior one is `AUTHORISED` or `UNCERTAIN` (`INV-PAY-04`) — idempotency alone does not stop two different keys charging twice |

### 1.2 What the code does today

| Service | Enforcement | Store | Gap |
|---|---|---|---|
| `1sb-integration-service` | `IdempotencyFilter` on every `POST`/`PUT`/`PATCH` under `/v1/**` except `/v1/master-data/**`. SHA-256 of the body is the fingerprint. Responses with status `< 500` are cached; 5xx are not (so the caller may retry) | `InMemoryIdempotencyStore` (per pod) **and** `integration_job.idempotency_key UNIQUE` in `bank_persistence` | Per-pod memory; multi-instance store parked (Phase 5.4, BOOT WS-1 out-of-scope) |
| `workforce-access-bff` | Forwards the app's `Idempotency-Key` on lead create to lead-service | — (lead-service stores it) | Other mutations: contracted (HLD §4.3) |
| `lead-service` | Create honours `Idempotency-Key` when present (replay → same lead; other body → `409 IDEMPOTENCY_CONFLICT`); duplicate *business* create is also caught by the unfinished-lead rule (`409 CONFLICT`) | `lead_lms.idempotency_record`, 24 h | Header optional until NIP-APP sends it |
| Domain skeletons | — | `<schema>.idempotency_record` designed in every DDL | Built with each service |

### 1.3 Idempotency record shape (DDL, every owning schema)

| Column | Type | Meaning |
|---|---|---|
| `idempotency_key` | `VARCHAR(128)` PK | The key |
| `request_hash` | `VARCHAR(64)` | SHA-256 of the canonical request |
| `response_ref` | `VARCHAR(64)` | Id of the resource created (the stored response is rebuilt from it) |
| `created_at` / `expires_at` | timestamptz | 24 h window |

---

## 2. Seam budgets — every hop, its timeout, retry and failure

Source: [`03-solution-architecture-r0.md` §5](../../ws3-platform/03-solution-architecture-r0.md#5-seam-catalogue--synchronous-vs-asynchronous-with-semantics).

| Seam | From → to | Style | Timeout / retry | On failure |
|---|---|---|---|---|
| S-01 | NIP-APP → BFF | Sync | 5 s edge budget | Typed error; journey stage unchanged |
| S-02 | BFF → PDP | Sync | **300 ms, no retry** | **Fail closed — deny** |
| S-03 | BFF → Journey | Sync (single-call commands) / **202 + command poll** (chained commands) | **4 s** read (was 3 s; §2.2) | No partial stage advance |
| S-04 | Journey → Customer (snapshot) | Sync, inside an **async journey command** | **7 s, no retry at journey** (was 2 s + 1 retry; §2.2) | Journey holds at `INITIATED` |
| S-05 | Customer → CBS (via Apigee) | Sync | **3 s per attempt** (connect ≤ 1 s inside it); 1 retry on 5xx/connect **for snapshots only**, none for interactive search (§2.2) | Snapshot inside the `CUSTOMER.POLICY.snapshotFreshnessMinutes` window (1440), else fail. Never stale-unbounded identity |
| S-06 | Journey → Consent | Sync, inside an async journey command | check **2 s**; capture **3 s**; no retry | Fail closed; cannot advance |
| S-07 | Journey → Suitability | Sync, inside an async journey command | 3 s, no retry | Fail closed; quote refused |
| S-08 | Quotation → Suitability (gate read) | Sync | **500 ms, no retry** | **Fail closed** — `403 SUITABILITY_REQUIRED` |
| S-09 | Quotation → Hub (create) | Async-poll | 3 s connect / 30 s read, **no retry on submit** | Poll the external reference to recover |
| S-10 | Hub → 1SB adapter | Async-poll | Adapter `onesb.client.*` budgets | `PARTIAL` is success; zero offers → `FAILED` |
| S-11 | Quote poll loop | Async-poll | Backoff 1 s × 2 → 30 s cap, 20 attempts | Budget exhausted → `TIMEOUT` / `TIMED_OUT` |
| S-12 | Proposal → Hub (submit) | Async-poll | **No retry on submit** | Poll; exhaustion → `SUBMISSION_FAILED` + ops task |
| S-13 | Payment → AU Bank PG (session) | Sync | 3 s / 15 s, no retry | `REJECTED`; proposal stays `AWAITING_PAYMENT` |
| S-14 | PG → Payment (callback) | Callback, at-least-once | Dedup on `pgTxnId` | Missing → `UNCERTAIN` |
| S-15 | PG settlement → Payment | Batch | Daily + on demand | Unmatched past SLA → `RECONCILIATION_BREAK` |
| S-16 | Policy → Hub (issuance confirm) | Sync + scheduled re-check | 3 s / 15 s | `CONFIRMATION_OVERDUE` → `ISSUANCE_DISPUTED` |
| S-17 | Any → Audit | Outbox → MSK | At-least-once, until acknowledged | Business commits; `SOLD` blocked until the audit write lands |
| S-18 | Payment/Policy → Notification | Outbox → MSK | At-least-once | Never blocks the journey; ops task |
| S-19 | Journey → compensation | Durable task | Bounded | `MANUAL_INTERVENTION` |
| S-20 | BFF → Lead | Sync | 2 s | Fail closed |
| S-21 | Any → Configuration | Sync, cached to TTL | 300 ms, no retry | **Fail closed** — no compiled-in default |
| S-22 | Partner surface → BFF → gated read | Sync | 3 s | Out-of-scope rows absent, never `403`-by-id |
| S-23 | outbox-publisher → MSK | Async publish | Unbounded backoff | Row stays unpublished; alert on outbox age |
| S-24 | MSK → consumer | Async consume | Bounded, then DLQ | DLQ = ops task |
| S-25 | Any → Valkey | Sync, best effort | **50 ms, no retry** | A miss is a read, never an error |
| S-26 | Customer / IdP → bank systems | Sync | Per S-05 / WS-2 | Typed `503`; `dev` may stub, `uat`/`prod` may not |

### 2.1 Budgets configured in code today

| Service | Property | Value |
|---|---|---|
| `identity-provider-adapter-service` | `identity.ad-verify.connect-timeout` / `read-timeout` (env `BANK_AD_VERIFY_CONNECT_TIMEOUT` / `BANK_AD_VERIFY_READ_TIMEOUT`, E8) | 2 s / 3 s |
| `identity-provider-adapter-service` | `identity.provider.keycloak.connect-timeout` / `read-timeout` (token, user-info, admin, JWKS; env `KEYCLOAK_CONNECT_TIMEOUT` / `KEYCLOAK_READ_TIMEOUT`, E8) | 2 s / 3 s |
| `identity-authorization-service` | `identity.provider-adapter.connect-timeout` / `read-timeout` (provisioning) | 1 s / 3 s |
| `customer-service` | `customer.apigee.connect-timeout-ms` / `read-timeout-ms` | 3000 / 30000 ms |
| `1sb-integration-service` | `onesb.client.connect-timeout-ms` / `read-timeout-ms` | 3000 / 30000 ms |
| `1sb-integration-service` | `onesb.poll.base-delay-ms` / `multiplier` / `max-delay-ms` / `max-attempts` | 1000 / 2.0 / 30000 / 20 |
| `1sb-integration-service` | `onesb.circuit-breaker.failure-threshold` / `open-wait-ms` / `half-open-success-threshold` | 5 / 30000 / 1 |
| `workforce-access-bff` | `workforce.session.pending-login-ttl` / `completion-code-ttl` / `session-ttl` | 5 min / 1 min / 8 h |

> **BFF budgets in code (`SUG-20261021-lft`).** Each downstream has its own client
> (`workforce.downstream.timeouts.*`, `DownstreamTimeouts`):
>
> | Downstream | Connect / read | Why |
> |---|---|---|
> | PDP decisions | 300 ms / 300 ms (`workforce.authorization.decision-timeout`) | S-02, fail closed |
> | lead-service | 500 ms / **2 s** | S-20 |
> | customer-service | 500 ms / 4 s | customer search reaches CBS (S-05: 3 s + 1 retry) — 4 s stays inside the 5 s edge budget (S-01) |
> | identity resolve | 500 ms / 2 s | local lookup in the PDP's store |
> | IdP adapter | 1 s / **4.5 s** | login path; inside the 5 s edge budget (S-01) — was 6 s. The adapter's own AD-verify / Keycloak calls are 2 s / 3 s (E8), so a slow read answers first; env `WORKFORCE_IDP_CONNECT_TIMEOUT` / `WORKFORCE_IDP_READ_TIMEOUT` |
>
> A call that gets no HTTP answer in budget is a typed `503 UPSTREAM_UNAVAILABLE`.
> lead-service's own PDP re-check uses `lead.authorization.decision-timeout` (300 ms).

### 2.2 Nested budgets — engineering decision `ED-R2-01` (2026-10-22)

**Status:** engineering decision taken under `SUG-20261022-rdy` (AI-DRAFTED), awaiting Mahesh's
review. Technical timeouts are Spring properties (E7 allows that for pure technical values); no
business value is involved.

**Problem.** The seam table had two contradictions. (1) S-03 gave BFF → Journey 3 s, yet one journey
command chains PDP (0.3 s) + consent check (2 s) + consent capture or Suitability (3 s) or Quotation,
and S-07 alone is 3 s. (2) S-04 gave Journey → Customer 2 s with a retry, yet Customer → CBS (S-05)
is 3 s **plus** a retry — Journey would time out while Customer was still legitimately waiting,
and both levels retrying multiplies load on CBS.

**Rules (all seams).**

1. **An outer budget covers the sum of its inner worst cases plus 10 %.** A synchronous hop never
   contains an async-poll step or a chain whose worst case exceeds it.
2. **Retry at exactly one level** — the service that owns the external dependency (customer for CBS,
   the Hub for providers). Its callers never retry the same hop inside their own budget.
3. **A human-facing synchronous path stays inside S-01 (5 s).** Anything longer is accepted
   (`202 Accepted`) and completed by a worker; the client polls.

**What changes.**

| Path | Before | After | Worst case inside |
|---|---|---|---|
| S-01 NIP-APP → BFF | 5 s | 5 s (unchanged) | BFF ≤ 0.5 s + PDP 0.3 s + S-03 4 s |
| S-03 BFF → Journey | 3 s | **4 s** read; Journey's own synchronous deadline **3.5 s** | PDP 0.3 s + **one** owning call ≤ 2 s + local transaction |
| Journey **chained** commands — `startNeedAnalysis`, `evaluateSuitability`, `checkOrCaptureConsent`, `requestQuote`, `submitProposal`, `initiatePayment`, `retryPartySnapshot` and the party snapshot of `startJourney` | sync, 3 s | **`202 Accepted` + `JourneyCommand`** (`GET /journeys/{id}/commands/{commandId}`); worker deadline `journey.command.deadline.<command>` | see next rows |
| S-04 Journey → Customer snapshot | 2 s, 1 retry | **7 s, no retry** (async) | S-05: 2 attempts × 3 s + 200 ms jitter = 6.2 s |
| S-05 Customer → CBS | 3 s, 1 retry | 3 s **per attempt**; retry for snapshots only | — |
| BFF → Customer search (interactive) | 4 s read; CBS 3 s + retry = 6.2 s (conflict) | 4 s read; customer-service makes **one** CBS attempt for search (`customer.cbs.search.retry-enabled=false`) | 3 s |
| S-06 Journey → Consent | 2 s | check 2 s; capture 3 s | capture = configuration 0.3 s + customer contact 1 s + OTP dispatch 1 s |
| Command deadlines (defaults) | — | `startNeedAnalysis` / `checkOrCaptureConsent` / `requestQuote` / `submitProposal` **8 s**; `retryPartySnapshot` / snapshot of `startJourney` **8 s**; `initiatePayment` **20 s** (S-13 3 s / 15 s inside) | PDP 0.3 + check 2 + capture/suitability/create 3 s = 5.3 s (+ margin) |

**Alternatives rejected.** Raising S-03 to 7 s breaks S-01 (5 s), and a 7 s spinner on an RM tablet
is the failure the async rule exists to avoid. Shrinking the consent re-check to 1 s leaves the
CBS conflict unsolved and gives the compliance gate the thinnest budget in the chain.

**Consequences.** NIP-APP and the BFF poll a command (or the journey) for chained steps; idempotency
keys are unchanged (the command is the idempotent unit: same key → same `commandId`). A timed-out
command is `FAILED` with `UPSTREAM_TIMEOUT` and the stage unchanged — never a partial advance.

---

## 3. Resilience per dependency class

| Dependency | Timeout | Retry | Breaker | Bulkhead | Degraded mode |
|---|---|---|---|---|---|
| PDP | 300 ms | none | none | shared | **None — fail closed** |
| CBS | 3 s per attempt | 1 on 5xx/connect (snapshot only; none for interactive search, §2.2) | yes | per dependency | Snapshot within `CUSTOMER.POLICY.snapshotFreshnessMinutes` |
| Product catalogue | 500 ms | 1 | yes | shared | Read cache |
| Hub → adapter | per adapter | **none on submit**, bounded on poll | **per provider** | **per provider** | Partial-quote success |
| AU Bank PG | 3 s / 15 s | none | yes | dedicated | **None on the money path** |
| Configuration | 300 ms | none | no | shared | None — cached values to TTL, then refuse |
| Outbox → MSK | 2 s | unbounded backoff | no | dedicated worker | Queue in outbox; never drop |
| MSK → consumer | per consumer | bounded, then DLQ | no | per consumer group | DLQ + ops task |
| Valkey | 50 ms | none | no | shared | Read through to the owning store |

**Per-provider bulkheads are an architecture property**: one slow insurer must not consume the
connection budget that makes every other insurer look down.

## 4. Sources

- [Decisions 2026-10-22](../../architecture-review/DECISIONS-2026-10-22.md) (E7, E8; `ED-R2-01` above)
- [R0 solution architecture §5](../../ws3-platform/03-solution-architecture-r0.md#5-seam-catalogue--synchronous-vs-asynchronous-with-semantics) · [HLD §4](../../../architecture/R0-HLD.md#4-communication)
- `services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/adapter/idempotency/IdempotencyFilter.java` · `src/main/resources/application.yml`
- [1SB resilience policy](../../../1sb-insurance-integration/service-ssot/RESILIENCE-POLICY.md)
