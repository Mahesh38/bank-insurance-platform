# Board 7 — Operations · Verdict on CR-010

**Change request:** [CR-010 — Reusable Context Module and Safe Autopilot Foundation](../../CR-010-context-module-and-safe-autopilot.md)
**Plan:** [PLAN-001](../../../plans/PLAN-001-context-module-and-safe-autopilot.md)
**Board:** 7 — Operations · **Role:** R10 — DevOps / SRE
**Persona:** Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head
**Reviewer type:** `AGENT` · **Self-review:** false · **Change tier:** T4 · **Date:** 2026-08-16

> ### ⚠ AI-drafted simulation of Shivanshi's operational reasoning
> Drafted by the Architecture agent (Mahesh persona) as an input to Board 7, using the persona
> package at [`shivanshi-sre/`](../../../../context/roles/shivanshi-sre/README.md). It is not
> Shivanshi's verdict and does not carry her authority. Board 7 owns the operational conclusion;
> Architecture does not.

> ## Verdict: `APPROVE-WITH-MODIFICATION`
> **Operational severity:** `O2` — degraded but usable; workarounds exist. **No `O0` or `O1`
> finding on CR-010's change surface.**
>
> **`signature_status: AI-DRAFTED — mandatory human signature outstanding`**

---

## 1. Decision requested

CR-010 §4 asks Operations to conclude on **CI/CD, scheduling, recovery and operational evidence**.

---

## 2. The workload, before anything else

I do not open a verdict with a resource metric. I open it with the business workload, because every
capacity conclusion below depends on which workload we are talking about, and CR-010 involves two
that are routinely conflated.

### 2.1 The business workload — unchanged by this CR

The workload this platform exists to carry is the **RM-assisted Term insurance sale**: an RM sitting
with an ETB customer, working through need analysis, suitability, consent, a multi-insurer quote
fan-out, proposal capture, a payment link to the customer's device, and issuance.

Its shape, from the capacity assumptions in
[`ws3-platform/05 §2`](../../../../platform/ws3-platform/05-nfr-catalogue.md) — and I want it noted
that these are **assumptions, not an approved baseline**:

```
250 RMs × 20% concurrent          =  50 active RMs
50 × 2 journey starts/hour        = 100 journey starts/hour ≈ 1.7/min
1.7 × 5 insurers (fan-out)        ≈ 8.5 provider calls/min BAU
Q4 tax season, ×4                 ≈ 6.8 journey starts/min, ≈ 34 provider calls/min
```

**CR-010 changes none of it.** It adds no service, no pod, no connection, no provider call. Runtime
capacity impact: **zero**. That is the first thing Board 7 needs to establish and it is the reason
this is not a hard review.

The seasonality is the part worth flagging for the plan behind this CR: Indian term insurance peaks
hard in **January–March**. A foundation recovery increment running 8–10 weeks from now lands its
first real production load in exactly that window if the schedule slips. That is a Delivery
conversation (Kalpana), not a Board 7 block, but it is mine to name.

### 2.2 The engineering workload — genuinely changed by this CR

The workload CR-010 does change is **pull-request validation**:

```
Estimated engineer count            small team, ~5 concurrent contributors
PRs per day                         ~5-10 including agent-authored branches
Jobs per PR after this CR           1 application job + 2 governance jobs
Serial build duration               single Gradle job, timeout ceiling 30 minutes
```

---

## 3. The bottleneck — and it is not CPU or memory

`application-ci.yml` is **one job**: checkout, JDK 21, Gradle setup, then
`./gradlew --no-daemon test jacocoTestReport jacocoTestCoverageVerification`, then artefact upload.

If PR feedback becomes slow, the limiting resource will be, in this order:

| Rank | Candidate bottleneck | Why |
|---|---|---|
| 1 | **Serial task graph within one job** | Test, report and verification run in sequence across all six modules on one runner. There is no module-level parallelism and no split between fast unit feedback and slower verification |
| 2 | **Gradle dependency cache cold-start** | `gradle/actions/setup-gradle@v4` caches, but a cache miss pays full dependency resolution on every module |
| 3 | **Runner concurrency quota** | Three jobs per PR against the account's concurrent-runner limit. With 5–10 PRs a day and no `concurrency` group, superseded pushes keep running and consume the quota |
| 4 | Runner CPU/memory | **Last, and only for JVM heap under Testcontainers, which does not exist yet** |

Adding a bigger runner would be the classic wrong answer here. The job is not CPU-bound; it is
serialised, and superseded runs are competing with live ones for the same quota. **Rank 3 is the
cheapest fix and it is one line.**

## 4. The downstream limit

For the engineering workload: the **GitHub Actions concurrent-runner quota**. Every job added
without a cancellation policy consumes it, and the failure mode is not an error — it is a queue.
Queued PR checks look identical to slow PR checks and get diagnosed as "CI is slow" for weeks.

For the business workload, the downstream limits that will actually govern this platform — and none
of which CR-010 touches:

| Limit | Owner of the number | Status |
|---|---|---|
| 1SB / insurer TPS, concurrency, maintenance windows | 1SB contract | **Unknown.** NFR-OPEN-2 |
| AU Bank Payment Gateway throughput and settlement cadence | Bank PG team | **Unknown.** NFR-OPEN-3 |
| Aurora max connections vs Σ(pods × pool) | Aarti | Designed as NFR-THR-06; **no environment exists to measure it** |
| Outbox drain rate vs audit SLA | Architecture | Designed as NFR-DAT-05; unmeasured |

I record these because the NFR catalogue asks me to accept verification ownership for the S09, S12
and S14 rows, and I will — **conditionally**. A latency target I can measure is useful. A throughput
target derived from an assumed RM count and an unknown provider limit is a hypothesis, and I will
not sign it as a commitment until both ends are real numbers.

## 5. Safe scale range

| Dimension | Safe range | What happens at the edge |
|---|---|---|
| CI runner concurrency | Up to the account quota; **add cancellation before adding runners** | Queue, not failure. Diagnosed late because it looks like slowness |
| Application pods (future) | Bounded by NFR-THR-06: Σ(pods × pool) ≤ 60% of Aurora max connections | Beyond it, scaling the application **collapses the database**. Not a theoretical concern — 20 pods × 20 connections against a 500-connection limit is already 400 |
| Provider concurrency (future) | Per-provider bulkhead ≤ 40% of outbound capacity (NFR-THR-03) | One slow insurer consumes the pool and every insurer appears down |
| Poller threads (existing adapter) | core 10 / max 50 / queue 200, separate from HTTP threads | Correct design — poll starvation cannot block API responses. Good work, keep it |

None of these is triggered by CR-010. They are the ranges the recovery increment must respect when
it starts provisioning anything.

## 6. Recovery behaviour

### 6.1 CR-010's own recovery — genuinely clean

`git revert` the branch. No runtime data, no external tracker state, no production configuration,
no persistent state written. Rollback is **real**, and this is one of the few changes in this
repository where "revert the commit" is a sufficient strategy.

I state it that plainly because my own contract §8 says `revert the commit` is *not* adequate for
changes that altered persistent or external state — and I want the contrast on the record, because
almost nothing in the recovery increment that follows will have this property.

### 6.2 The recovery posture CR-010 does *not* change

| Capability | State |
|---|---|
| Deployment rollback | **Never designed, never tested.** `render.yaml` is a manual dashboard Blueprint apply |
| Backup | **Never configured** |
| Restore | **Never performed.** A backup that has never been restored is a hypothesis |
| DR / RTO / RPO | Numbers exist (≤ 1 h / ≤ 5 min); **no exercise has ever been run** |
| Observability backend | **Absent.** `bank-common-observability` is a library with nowhere to send |
| On-call, alert routing, runbooks | **Absent** |

CR-010 neither improves nor worsens any of these. They are S09 and they are why S09 must run
overlapped with S08 rather than after it.

### 6.3 The scheduled job nobody owns — my one substantive finding

`governance.yml` runs on `schedule: cron "0 6 * * 1"` — weekly, Monday 06:00 UTC (11:30 IST). The
comment explains the reasoning well: *a stale state file is exactly the failure that produces no
diff*. That is a good control and I endorse it.

**When it fails on a Monday morning, who is told?**

There is no notification step, no issue-creation step, no routing. A scheduled workflow that fails
on the default branch produces a red mark on a page nobody has a reason to open. Rule SR-2 in the
readiness canon is about pages with no runbook; this is the adjacent failure — **a check with no
recipient**. The freshness check exists precisely to catch the silent failure mode, and its own
failure mode is silent.

The same applies to the backlog-drift step: `python3 scripts/lifecycle/generate-backlog.py` followed
by `git diff --exit-code`. Good control. On failure it prints a diff and nothing tells the reader
that the fix is *regenerate and commit* rather than *hand-edit the generated file*. That is a
one-line runbook entry and its absence is exactly the kind of thing that turns a correct control
into an ignored red build.

## 7. Operational evidence — the part of CR-010 I value most

`GATE-EVIDENCE.yaml` is the first machine-readable operational ledger this repository has had, and
its shape is right for Board 7: per criterion it carries `owner`, `verifier`, `execution_mode`,
`required_evidence_level`, `evidence[]`, `blockers[]` with follow-up dates, and `last_verified_at`.

That is the difference between "operations says the runbook is handled" and an operational claim
with a name, a date and an artefact attached to it. My own evidence contract §4 says a strong Board 7
verdict cites observable evidence rather than statements like *monitoring is handled*; this file is
the mechanism that makes that enforceable rather than cultural.

Two observations on it:

1. `last_verified_at` is `null` on every criterion. That is honest — nothing has been verified —
   and it must not stay null once evidence starts arriving, or the field becomes decoration.
2. WS-1 criterion 4.5 (operations runbook) is listed with `owner: "R10 / Operations"`.
   [CR-008](../../CR-008-add-shivanshi-sre-persona.md) named that owner: it is **Shivanshi**.
   `CURRENT-STATE.yaml` already carries the corrected form; the evidence ledger still has the
   unnamed one. An approver with no person is why 4.5 could not close in the first place, and the
   inconsistency should not be reintroduced through the new file.

## 8. Board 7 checklist O1–O8, plus the platform-specific checks

| # | Check | Result |
|---|---|---|
| O1 | Deployability — config, env vars, secrets, migrations, ordering | **N/A.** No runtime deployment in this CR. No migration, no config change |
| O2 | Observability — metrics, logs, traces, correlation IDs | **N/A for runtime.** For the pipeline itself: run history and uploaded reports exist; no pipeline-health dashboard (S08-E01-S06) |
| O3 | Alerting — what pages someone, on what threshold | **Gap.** The scheduled freshness job has no recipient (§6.3) |
| O4 | Failure modes and blast radius | **Bounded.** Worst case is a red build or a stale governance check. No customer-facing or money-path blast radius |
| O5 | Rollback — tested and sufficient given data written | **Sufficient.** No data written; `git revert` is genuinely adequate here (§6.1) |
| O6 | Capacity and cost | **Small increase** in CI minutes. No runtime cost. No autoscaling implication |
| O7 | Runbook updates needed | **Yes** — two entries: freshness-check failure, and backlog-drift failure (§6.3) |
| O8 | Backward compatibility during rolling deploy | **N/A.** No rolling deploy |
| O9 | DR exercised | **No, and unchanged by this CR.** S09 |
| O10 | Backup and restore proven | **No, and unchanged.** S09 |
| O11 | Data residency attested | **No.** Unverified on the live Render deployment. Concurs with Board 4 SEC-F03/SEC-C4 |
| O12 | Provider protection — rate limits, timeouts, breakers | Designed in the adapter; **no measured provider limits** (NFR-OPEN-2) |
| O13 | Retention and purge jobs operating | **No.** S09 |

---

## 9. Conditions

| # | Condition | Severity | Owner | Required by |
|---|---|---|---|---|
| **OPS-C1** | Route failures of the scheduled governance workflow to a named recipient — issue creation, notification, or an equivalent that produces an owned action. A check whose own failure is silent does not defend against silent failure. | `O2` | Shivanshi + Amit | With CR-010 ratification |
| **OPS-C2** | Add two runbook entries: (a) freshness check failed — how to read exit codes 1 vs 2 and what to do; (b) backlog drift detected — regenerate and commit, never hand-edit the generated file. | `O2` | Shivanshi | With CR-010 ratification |
| **OPS-C3** | Add a `concurrency` group with `cancel-in-progress: true` to both workflows **before** any consideration of larger or additional runners. Fix the queue before buying capacity. | `O3` | Amit | With CR-010 |
| **OPS-C4** | Correct WS-1 criterion 4.5 ownership in `GATE-EVIDENCE.yaml` from `R10 / Operations` to `Shivanshi / SRE`, matching `CURRENT-STATE.yaml` and CR-008. | `O3` | Orchestrator | With CR-010 ratification |
| **OPS-C5** | Measure actual p95 pipeline duration over the first 20 runs and record it against NFR-ENG-01 before any optimisation. The 30-minute timeout ceiling is three times the 10-minute target; measure before tuning. | `O3` | Amit + Shivanshi | S08 |
| **OPS-C6** | I accept verification ownership for the S09, S12 and S14 rows of the NFR catalogue **conditional on**: CAP-A1…A7 confirmed against an approved business baseline (Rajal), and contractual provider limits obtained for 1SB and the AU Bank PG (NFR-OPEN-2, NFR-OPEN-3). Until then those throughput NFRs are hypotheses and I will report them as such. | `O2` | Shivanshi + Rajal | Before S12 |
| **OPS-C7** | S09 runs overlapped with S08, not after it. The deployment pipeline is an extension of the platform; built in series we get a pipeline with nowhere to deploy followed by a platform with nothing proven to deploy onto. Concurs with Board 1 D-5. | `O1` | Kalpana + Shivanshi | Increment planning |
| **OPS-C8** | Before the recovery increment schedules its production window, check it against the **Q4 (Jan–Mar) tax-season peak**. A capacity model built on annual averages fails exactly when the business most needs the platform. | `O2` | Kalpana + Rajal | Increment planning |
| **OPS-C9** | `last_verified_at` in `GATE-EVIDENCE.yaml` is populated whenever evidence is attached. A permanently-null verification timestamp turns the ledger into decoration. | `O3` | Kalpana | Ongoing |

---

## 10. Record

```yaml
capacity_assessment:
  business_volume: >
    Unchanged by CR-010. Assumed R0 pilot: 250 RMs, 50 concurrently active, ~1.7 journey
    starts/min BAU, ~6.8/min at Q4 peak, ~34 provider calls/min at peak. These are assumptions,
    not an approved business baseline.
  peak_window: "09:00-19:00 IST weekdays; Q4 January-March tax season, x4 multiplier"
  amplification: "1 journey -> 5 provider quote calls (5 Group A insurers)"
  sustainable_capacity: "Not measurable — no environment exists to measure it"
  headroom: "Not established"
  limiting_dependency: >
    For the engineering workload: serial single-job pipeline, then runner concurrency quota.
    NOT CPU or memory. For the business workload: unknown 1SB and payment-gateway limits, then
    the Aurora connection ceiling.
  provider_limits: ["1SB TPS/concurrency — UNKNOWN", "AU Bank PG throughput — UNKNOWN"]
  scale_policy: >
    None applicable. CR-010 adds no runtime capacity. Add job cancellation before adding runners.
  failure_mode_at_limit: "CI: queued checks that present as slowness. Runtime: not applicable."
  safe_degraded_mode: "N/A — no runtime path affected"
  evidence:
    - "read both workflow files in full: triggers, schedule, jobs, steps, timeout, permissions"
    - "confirmed application-ci.yml is one serial job with timeout-minutes 30 and no concurrency group"
    - "confirmed governance.yml weekly cron with no failure notification or issue creation"
    - "read GATE-EVIDENCE.yaml: owners, verifiers, execution modes, blockers, last_verified_at all null"
    - "verified S09 stage capability table against the repository: no IaC, no environments, no observability backend, no backup, no rollback"
    - "reviewed ws3-platform/05-nfr-catalogue.md sections 2-4 for feasibility and verification ownership"
  next_review_trigger: >
    First provisioned environment; or arrival of contractual provider limits; or GATE-S08 candidate.

operations_readiness:
  verdict: APPROVED_WITH_CONDITIONS
  operational_severity: O2
  business_criticality: "Governance and engineering tooling. No customer-facing or money path."
  expected_peak: "5-10 PRs/day, 3 jobs each"
  slo_health: "No SLOs defined yet — S14 obligation"
  capacity_headroom: "Not established for runtime; adequate for CI at current PR volume"
  limiting_dependencies: ["runner concurrency quota", "Gradle cache warmth"]
  provider_limits: []
  rollback_or_recovery: "git revert — genuinely sufficient; no persistent or external state written"
  dashboards: []
  alerts: []
  runbooks: ["MISSING — two entries required, see OPS-C2"]
  open_operational_findings:
    - "Scheduled governance workflow failure has no recipient (OPS-C1)"
    - "No concurrency group; superseded runs consume quota (OPS-C3)"
    - "GATE-EVIDENCE.yaml WS-1 4.5 owner is unnamed R10 rather than Shivanshi (OPS-C4)"
    - "Data residency unverified on the live deployment (O11) — concurs with Board 4"
    - "No rollback, backup, restore, DR exercise or observability backend — unchanged by this CR, S09 scope"
  required_human_or_specialist_actions:
    - "Rajal: confirm capacity assumptions CAP-A1..A7"
    - "Kalpana: schedule the recovery increment against the Q4 peak"
    - "Deepali + Shailja: resolve the residency question"
  reviewer_type: AGENT
  ai_simulated: true
  drafted_by: "Architecture agent (Mahesh persona)"
  signature_status: "AI-DRAFTED — mandatory human signature outstanding"
  date: 2026-08-16
```

---

**Persona:** Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head · Board 7 / R10
**Drafted by:** Architecture agent under the Mahesh persona, as an input to Board 7
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`
