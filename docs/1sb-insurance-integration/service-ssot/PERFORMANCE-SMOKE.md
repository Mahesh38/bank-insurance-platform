# Performance smoke — quote submit

**Satisfies (partly):** WS-1 Phase 4 exit criterion **4.6** — "Performance smoke: p95 quote
under nominal concurrency" ([04-STAGE_GATES.md §6](../../governance/04-STAGE_GATES.md#6-project-gates-l3))
**Harness:** `QuoteLatencySmokeIT` · **Owner:** Engineering · **Threshold owner:** PO + Architect

> **What is done and what is not.** The measurement exists, is repeatable, and is gated in a way
> that survives changing hardware. What does **not** exist is a ratified p95 SLO — no target
> latency is defined anywhere in the SSOT, and inventing one would be a scope decision dressed
> up as an engineering result. §4 says what is needed to close 4.6 fully.

---

## 1. What is measured

The synchronous part of `POST /v1/quotes` — the accept a bank caller waits on before receiving a
`jobId` — with 1SB replaced by WireMock holding a fixed delay.

That isolates **this service's own cost**: request handling, validation, idempotency body
hashing, the persistence round-trip, and job scheduling.

**What is deliberately not measured:** how long a customer waits for a quote. That is dominated
by 1SB's real response time, which we neither control nor can regression-test. A number that
moves when a third party has a bad day is worse than no number. End-to-end timing is a UAT
observation — criterion 4.3.

---

## 2. Baseline (2026-08-13)

Measured on the development container: 4 vCPU, 16 GB, with the load generator, the service and
both WireMock servers **co-located in one JVM**.

| Measure | Value | Gate | Verdict |
|---|---|---|---|
| Per-request overhead (serial p50 − upstream delay) | **21.3 ms** | ≤ 100 ms | ✅ PASS |
| Concurrency gain (throughput ÷ serial rate) | **10.1×** | ≥ 3.0× | ✅ PASS |

Supporting numbers, **not gated** — see §3 for why:

| Measure | Value |
|---|---|
| Serial p50 | 141.3 ms (120 ms of which is the stubbed upstream) |
| Serial rate | 7.1 req/s |
| Concurrent throughput @ 25 | 71.3 req/s |
| p50 / p95 / p99 @ 25 | 274.5 / 661.1 / 862.2 ms |

Conditions: concurrency 25, 500 measured requests after 50 warm-up, 30 serial calibration
requests, simulated 1SB delay 120 ms.

### Reproduce

```bash
./gradlew :services:1sb-integration-service:test \
    --tests '*QuoteLatencySmokeIT' -Dperf.enabled=true
```

Report lands at `services/1sb-integration-service/build/reports/perf/quote-latency.md`.
Tunables: `perf.concurrency`, `perf.requests`, `perf.warmup`, `perf.upstreamDelayMs`,
`perf.overheadBudgetMs`, `perf.minConcurrencyGain`.

---

## 3. Why the gate is not a p95 threshold

The harness originally asserted a wall-clock p95 budget of 750 ms. It was measuring the machine,
not the code: the **same commit** produced p95 678 ms and 745 ms on consecutive runs, one of them
5 ms from failing. On a shared CI runner that gate would fail for reasons no engineer can act on,
and a gate people learn to ignore is worse than no gate at all.

The two gated measures were chosen because both are **properties of the code that hold across
hardware**:

**Per-request overhead** is measured one request at a time, so contention cannot inflate it. If
it rises, something was added to the synchronous submit path — a second persistence call before
the 202, a duplicated body read, a blocking lookup. That is a real regression on any machine.

**Concurrency gain** is a ratio against the same run's own serial rate, so a slow machine slows
both halves equally. If quote polling were moved onto the request thread, or a lock introduced on
the submit path, throughput would collapse toward the serial rate — on fast hardware as surely as
on slow. This is the check that protects the architecture's central async decision.

The percentiles are still recorded on every run. They are useful as trend data on stable
hardware; they are not a pass/fail signal here.

---

## 4. What is still outstanding for criterion 4.6

| Part | State | Owner |
|---|---|---|
| Repeatable measurement harness | ✅ Done | Eng |
| Regression gates that survive hardware change | ✅ Done | Eng |
| Baseline recorded | ✅ Done | Eng |
| **A ratified p95 target under defined nominal load** | ❌ **Outstanding** | PO + Architect |
| **Measurement on UAT-like infrastructure** | ❌ **Outstanding** | Eng + Ops |

Two things are missing, and neither is an engineering question:

1. **"Nominal concurrency" is undefined.** Nothing in the SSOT states expected concurrent quote
   volume. The harness assumes **25 concurrent submits**, reasoned from the RM-assisted branch
   model — a quote is one step of an adviser's conversation with a customer, so concurrency is
   bounded by advisers mid-conversation, not by site traffic. That is recorded as **ASM-009** in
   the [assumption register](../../governance/registers/ASSUMPTION-REGISTER.md) and needs the
   PO's number, not Engineering's guess.

2. **A p95 target is a product decision.** "Fast enough" for an adviser sitting with a customer
   is a service-design judgement. Engineering can measure anything; only the PO can say which
   number is acceptable.

Once both exist, re-run on UAT infrastructure with the load generator on a **separate host**
(co-location is what makes the current percentiles unusable as an SLO), record the result here,
and add the ratified p95 as a third gate.

---

## 5. Related

| Document | Why |
|---|---|
| [OPERATIONS-RUNBOOK.md](./OPERATIONS-RUNBOOK.md) §5.2 | Poll backoff and timeout behaviour under upstream slowness |
| [ASSUMPTION-REGISTER.md](../../governance/registers/ASSUMPTION-REGISTER.md) | ASM-009 — the nominal-concurrency assumption |
| `TermJourneyE2EIT` | Correctness of the same path; this document is only about cost |
| [architecture §7.4](../architecture/1sb-integration-service-architecture.md) | Async polling design the concurrency gate protects |
