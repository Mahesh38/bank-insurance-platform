# WS-3 — Measurable NFR Catalogue

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform (proposed under CR-010)
**Stage:** S07 — Solution & Security Architecture (epic S07-E05) · **Closes GAP-017**
**Owners:** Mahesh (Architecture) + Rajal (business derivation) + Shivanshi (feasibility and
verification), per [`stages/S07-solution-architecture.md`](../../application-lifecycle-bible/stages/S07-solution-architecture.md)
**Status:** AI-DRAFTED. S07-G6 requires a **signed** NFR sheet; signatures are outstanding.

---

## 1. What was actually missing

[GAP-017](../../au-bank-insurance-platform/po-drive/02-GAP-REGISTER.md) reads *"NFR numbers missing
(SLA, retention, RTO)"*. That is close but not precise, and the imprecision matters because it
changes what has to be done.

**Numbers do exist.** [`architecture-review/06-security-compliance-and-nfrs.md`](../architecture-review/06-security-compliance-and-nfrs.md)
carries availability tiers, a p50/p99 latency table and `RTO ≤ 1 hour / RPO ≤ 5 min`.
[`1sb-integration-service-architecture.md §7`](../../1sb-insurance-integration/architecture/1sb-integration-service-architecture.md)
carries latency, timeout, retry, concurrency and retention numbers for the adapter.

**What is missing is everything that makes a number an NFR:**

| Missing property | Consequence today |
|---|---|
| An identifier | Cannot be referenced by a gate criterion, a test or a backlog item |
| A **measurement method** | WS-1 criterion 4.6 says *"p95 quote under nominal concurrency"*. Neither `p95` nor `nominal concurrency` is defined anywhere, so the criterion cannot be evaluated — an S07 gap surfacing as an S12 blocker four stages later |
| A **verification stage** | Nobody owns proving it, so nobody does |
| A **derivation** from a business reason | A number nobody can justify is a number nobody defends when it becomes inconvenient |
| p95 alongside p50/p99 | The SRE canon states journey SLOs in p95; the architecture review states p50/p99. They cannot be compared |

So this catalogue does not invent an NFR set. It **ratifies the existing numbers, assigns
identifiers, adds the missing percentile, and supplies a measurement method and a verification
stage for every one**. Where a number genuinely does not exist and cannot be derived without a
business input, it is recorded as an assumption with an owner — never invented.

---

## 2. Capacity assumptions — S07-E05-S04

Every throughput and capacity NFR below derives from these. **None of them is an approved business
baseline**; the repository contains no RM count, journey volume or peak forecast. They are recorded
as explicit assumptions so that the derived numbers are auditable and correctable, and so that no
reader mistakes them for Product commitments.

| ID | Assumption | Basis | Owner to confirm |
|---|---|---|---|
| CAP-A1 | R0 pilot: **250 RMs** across pilot branches | Pilot-scale assumption; no source in the repository | Rajal |
| CAP-A2 | **20%** of RMs concurrently active in a business hour | Standard branch-workforce concurrency assumption | Rajal + Shivanshi |
| CAP-A3 | **2 journey starts per active RM per hour** | Assisted Term sale duration | Rajal |
| CAP-A4 | **5 insurers** fanned out per multi-quote | Group A insurer set per R0-SCOPE A4 | Rajal |
| CAP-A5 | Business window **09:00–19:00 IST**, weekdays | RM-assisted journeys are business-hours (SRE canon §4) | Shivanshi |
| CAP-A6 | **Q4 (Jan–Mar) peak multiplier ×4** on the business-as-usual rate | Indian term-insurance tax-season seasonality, named in the SRE canon §7 | Rajal |
| CAP-A7 | Self-service journeys are **≤ 10%** of R0 volume | Self-service depends on an unresolved customer-identity model (see the security architecture §3.1) | Rajal |

### 2.1 Derived demand

```
CAP-A1 × CAP-A2            = 50 concurrently active RMs
50 × CAP-A3                = 100 journey starts / hour  ≈ 1.7 / minute
1.7 × CAP-A4               ≈ 8.5 provider quote calls / minute (BAU)
Q4 peak (CAP-A6)           ≈ 6.8 journey starts / minute, ≈ 34 provider calls / minute
```

This is a **small** workload, and saying so is the useful architectural finding. The R0 platform is
not throughput-constrained; it is correctness-, evidence- and recovery-constrained. Any NFR
discussion that begins with scaling is solving the wrong problem — the same conclusion Shivanshi's
capacity model reaches from the operations side.

---

## 3. NFR catalogue

Columns: **Target** is the number · **Measure** is the method that produces it · **Verify at** is
the stage where it must be proven, per the evidence-strength ladder in
[`04-GATE-AND-SIGNOFF-MODEL.md §2`](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md).
`Derived from` is the business reason (S07-E05-S02).

### 3.1 Latency — NFR-LAT

Measured server-side at the service boundary unless stated. Percentiles over a 5-minute rolling
window under the CAP-A concurrency profile.

| ID | Requirement | Target | Measure | Verify at | Derived from |
|---|---|---|---|---|---|
| NFR-LAT-01 | RM workspace read (journey state, catalogue) | p50 < 150 ms · p95 < 300 ms · p99 < 400 ms | Micrometer timer at the BFF, per endpoint | S11 instrumented; S12 load test | An RM navigating in front of a customer |
| NFR-LAT-02 | Quote creation acknowledgement | p50 < 300 ms · p95 < 600 ms · p99 < 800 ms | Timer on `POST /quotes` to `202` | S12 load test | The acknowledgement is what the RM sees; the fan-out is asynchronous |
| NFR-LAT-03 | **Quote result available end to end** | **p95 < 5 s** · p99 < 12 s | Time from quote request to `QUOTED`/`PARTIALLY_QUOTED`, measured on the aggregate | S12 load test | SRE canon §3: *"beyond ~5 s the conversation breaks"* |
| NFR-LAT-04 | Quote poll read | p50 < 100 ms · p95 < 200 ms · p99 < 300 ms | Timer on the poll endpoint | S12 | Polled repeatedly; cost compounds |
| NFR-LAT-05 | Proposal schema fetch, cache hit | p95 < 150 ms | Timer, cache-hit label | S12 | Form render blocks the RM |
| NFR-LAT-06 | Proposal submit acknowledgement | p50 < 500 ms · p95 < 1.2 s · p99 < 1.5 s | Timer on `POST /proposals` to `202` | S12 | |
| NFR-LAT-07 | Payment session creation | p50 < 1 s · p95 < 2.5 s · p99 < 3 s | Timer, including the PG round trip | S12 | Money path; the customer is waiting for a link |
| NFR-LAT-08 | Application status check | p50 < 800 ms · p95 < 1.6 s · p99 < 2 s | Timer | S12 | |
| NFR-LAT-09 | PDP authorisation decision | p95 < 100 ms · p99 < 300 ms | Timer at the PDP; budget is 300 ms (seam S-02) | S11 | On every request; it is pure overhead |

NFR-LAT-01/02/04/06/07/08 restate the existing architecture-review figures with p95 added.
NFR-LAT-03 and NFR-LAT-09 are new and are the two that were actually absent.

> **NFR-LAT-03 is the number WS-1 criterion 4.6 needs.** "p95 quote under nominal concurrency"
> becomes *"p95 quote result < 5 s at 6.8 journey starts per minute (Q4 peak, CAP-A6)"* — a
> statement a load test can pass or fail.

### 3.2 Throughput and capacity — NFR-THR

| ID | Requirement | Target | Measure | Verify at | Derived from |
|---|---|---|---|---|---|
| NFR-THR-01 | Sustained journey starts | ≥ 10 / minute with all latency NFRs held | Load test at the sustained rate for 30 minutes | S12 | CAP-A6 peak (6.8/min) + 45% headroom |
| NFR-THR-02 | Sustained provider quote calls | ≥ 50 / minute across ≤ 5 providers | Load test; per-provider counter | S12 | CAP-A4 × NFR-THR-01 |
| NFR-THR-03 | Per-provider concurrency cap | Configured per provider; bulkhead prevents one provider consuming > 40% of outbound capacity | Bulkhead configuration + a failure-injection test with one provider held at maximum latency | S12 | Shivanshi §8: one failing insurer must not make every insurer look down |
| NFR-THR-04 | Saturation point identified | Known and documented, with the failure mode at saturation | Stress test to failure | S12 | An unknown saturation point is an unplanned outage |
| NFR-THR-05 | Recovery after load removal | System returns within latency NFRs in < 5 minutes with no manual action | Spike test | S12 | Queue-drain behaviour after a campaign burst |
| NFR-THR-06 | Database connection budget | Σ(pods × pool size) ≤ 60% of the store's maximum connections at maximum replica count | Computed from the autoscaling maximum and the Aurora limit; asserted in the IaC review | S09 design; S12 measured | Shivanshi §5: scaling pods must not collapse the database |

NFR-THR-03 and NFR-THR-06 are architecture constraints, not aspirations: they bound what an
autoscaling policy is allowed to do.

### 3.3 Availability and error budget — NFR-AVL

| ID | Requirement | Target | Measure | Verify at | Derived from |
|---|---|---|---|---|---|
| NFR-AVL-01 | Sale-path services (BFF, Journey, Identity, Quotation, Proposal, Payment, Policy) | **99.9%** monthly | Successful requests / total, per service, excluding client errors | S14 baseline; S15 continuous | Architecture review tier 1; the sale cannot proceed without them |
| NFR-AVL-02 | Advisory and read-heavy services (Catalogue, Suitability) | 99.5% monthly | Same | S14 | Degraded cached reads acceptable — **except** the suitability gate check, which fails closed |
| NFR-AVL-03 | Reporting and batch | Best-effort, business hours | Same | S15 | Eventual consistency explicitly acceptable |
| NFR-AVL-04 | Quote journey success rate | ≥ 99.5% of quote requests yield at least one offer or a typed refusal | Journey SLI | S12 | SRE canon §3 |
| NFR-AVL-05 | Payment link issuance success | ≥ 99.9% | Journey SLI | S12 | Failure loses the sale at the last step |
| NFR-AVL-06 | Issuance confirmation received | ≥ 99.9% of captured payments confirmed within the issuance SLA | Journey SLI | S12 | The four-part "sold" definition depends on it |
| NFR-AVL-07 | Error budget policy | 99.9% → 43 min/month. Consumption thresholds at 50 / 75 / 100% per the SRE canon §3 | Error-budget dashboard | S14 | Rajal agrees the policy; Shivanshi reports it |

### 3.4 Recovery — NFR-DR

| ID | Requirement | Target | Measure | Verify at | Derived from |
|---|---|---|---|---|---|
| NFR-DR-01 | RTO, transactional core | ≤ **1 hour** | DR exercise, measured wall-clock from declaration to service restored | S09 first exercise; S14 ORR | Architecture review; a business day cannot absorb more |
| NFR-DR-02 | RPO, transactional core | ≤ **5 minutes** | DR exercise; measured data loss window | S09; S14 | Money and proposal state |
| NFR-DR-03 | RPO, audit and raw payload archive | **0** | Object replication verification | S09 | Regulatory evidence cannot have a loss window |
| NFR-DR-04 | Restore proven, per store | Restore executed to a working state and **timed** against NFR-DR-01 | Restore test record | **S09 (S09-G7)** | *A backup that has never been restored is a hypothesis* |
| NFR-DR-05 | Rollback proven | A deliberately broken release rolled back in UAT, previous version restored, data intact, timed | Rollback execution record | **S09 (S09-G4)** | Rule SR-3: untested rollback is no rollback |
| NFR-DR-06 | Reconciliation recovery | 100% of payments reconciled within 24 hours; a break is raised, never auto-resolved | Reconciliation job metrics | S12 | SRE canon §3: financial control, non-negotiable |

### 3.5 Data and retention — NFR-DAT

| ID | Requirement | Target | Measure | Verify at | Derived from |
|---|---|---|---|---|---|
| NFR-DAT-01 | Regulatory retention | 7 years from event time for `RET-7Y-IMMUTABLE` and `RET-7Y` classes | Retention configuration test | S09 | IRDAI; control C7 |
| NFR-DAT-02 | Immutability | Deletion of an object under Object Lock is **refused** | Deletion-attempt test (FF-10) | S09 (S09-G8) | Control C7 |
| NFR-DAT-03 | Payment reconciliation lag | ≤ 24 hours; alert at 12 | Reconciliation metric | S12 | The bounded-eventual-consistency limit in the domain model §5.2 |
| NFR-DAT-04 | Audit completeness | 100% of regulated actions produce an audit event; gapless `sequence_no` per journey | Per-journey completeness test | S11 | Control C8 |
| NFR-DAT-05 | Audit outbox lag | p95 < 30 s; alert at 5 minutes | Outbox age metric | S11 | Journey cannot reach `SOLD` until audit confirms (INV-JRN-05) |
| NFR-DAT-06 | Data residency | 100% of stores, backups, logs and archives in an India region | IaC policy check + residency enumeration | S09 (S09-G9) | Control C6; licence condition |
| NFR-DAT-07 | Retention purge | Data past its horizon disposed of, with a disposal audit record | Purge job execution record | S09 | Retention is a two-sided obligation |

### 3.6 Security NFRs — NFR-SEC

Security *outcomes* are Deepali's; the measurable properties are recorded here so they sit in one
catalogue.

| ID | Requirement | Target | Measure | Verify at |
|---|---|---|---|---|
| NFR-SEC-01 | PII in logs | **Zero** matches for regulated field patterns across a full suite run | Log-scan test (FF-05) | S08 (S08-G7) |
| NFR-SEC-02 | Secrets in artefacts | **Zero** | Secret scan of repository history and images (FF-11) | S08 (S08-G5) |
| NFR-SEC-03 | Critical SCA findings with a reachable path | **Zero** unremediated at release | Dependency scan report | S08, then every release |
| NFR-SEC-04 | Compliance-gate code coverage | **100% branch** on control paths C1–C10 | Filtered coverage report (FF-07) | S08 (S08-G3) |
| NFR-SEC-05 | Authorisation default-deny | No path reaches a resource without an explicit allow | Negative authorisation test suite | S11 |
| NFR-SEC-06 | Vulnerability remediation SLA | S0 immediate · S1 next release · S2 two releases · S3 backlog | Tracked in the risk register with ageing | S08 onward |
| NFR-SEC-07 | Credential rotation | Every credential class rotated at least once without outage | Rotation exercise record | S09 (S09-G5) |

### 3.7 Platform-tier NFRs — NFR-NET, NFR-CAC, NFR-EVT, NFR-OBS

Added 2026-08-24 with the five layers `ADR-009` … `ADR-013`. Every one of them exists because the
layer it measures is otherwise a claim: a standby path nobody failed over to, a cache nobody lost,
a broker nobody replayed and an index nobody checked for PII are four ways of having installed
something rather than having a capability.

| ID | Requirement | Target | Measure | Verify at | Derived from |
|---|---|---|---|---|---|
| NFR-NET-01 | **Bank-path failover** — DX loss falls to VPN without human action | Recovery **< 120 s** with no dropped journey beyond in-flight requests | Deliberate DX withdrawal in UAT, wall-clock timed, journeys running | **S09 (S09-G7 evidence pack)** | `ADR-009`. An untested standby path is a claim |
| NFR-NET-02 | **Egress inspection coverage** | **100%** of egress traverses the firewall; zero route tables with a non-TGW default route | IaC policy-as-code pre-apply (`FF-22`) + a runtime probe attempting direct egress from a pod | S09 | `ADR-010`. A control with one bypass is not a control |
| NFR-NET-03 | **Allowlisted EIP set published and verified** | 1SB and the AU Bank PG both confirm the **inspection-VPC** addresses before UAT opens | Written confirmation from both parties, address-by-address | **Before S11 entry** | §2.3 of the LLD. The addresses moved; a stale allowlist is indistinguishable from none |
| NFR-NET-04 | CBS latency over the private path | p95 **< 300 ms** for a CIF lookup, inside the `NFR-LAT-01` budget | Timer at `#4`, labelled by path (DX / VPN) | S11 instrumented; S12 load | `S-05`. The RM is waiting in front of a customer |
| NFR-CAC-01 | **Cache failover** | Automatic failover completes **< 60 s**; no session lost that was valid at the start | Forced failover in UAT with active sessions | S09 | `ADR-011`. A single-node vault makes an AZ event a mass logout |
| NFR-CAC-02 | **Session survival** | 100% of valid sessions survive a rolling pod restart and a deploy | Rolling restart with active sessions held | S09 | The property the in-process alternative never had |
| NFR-CAC-03 | **Cache is not on the correctness path** | With the cache unavailable: **zero** authorisation or configuration outcomes change; latency degrades only | Chaos test — cache down, full journey suite green (`S-25`) | S11 | `ADR-011`. A cache that can change an answer is a system of record |
| NFR-EVT-01 | **Publish lag** — outbox row to topic | p95 **< 5 s**; alert at 60 s | Age of the oldest unpublished outbox row | S11 | `ADR-012`. This is the metric that shows a stalled publisher |
| NFR-EVT-02 | **Consumer lag** | p95 **< 30 s** per consumer group; alert at 5 minutes; drains within 15 min of a 1-hour outage backlog | MSK consumer-group lag, per group | S11; drain proven S12 | The audit SLA (`NFR-DAT-05`) is the binding constraint |
| NFR-EVT-03 | **Replay drill** | A consumer group replayed from the outbox reaches an identical evidence state — **zero duplicates, zero gaps** | Deliberate replay in UAT against a reconstructed broker | **S09 (`S09-G7`)** | `ADR-012` / LLD `D14`. This is what makes "no broker in DR" a design rather than a gap |
| NFR-EVT-04 | **No evidence only in a topic** | **Zero** gate, audit or regulatory queries served from MSK; `SOLD` unreachable on a topic acknowledgement alone | `FF-26` + a negative journey test | S11 | The rule `ADR-012` cannot trade |
| NFR-OBS-01 | **Log searchability** | A journey's correlated events across BFF, services, hub, adapter, firewall and broker are retrievable by `X-Correlation-Id` in **< 60 s** from emission | Timed query in UAT during the first end-to-end journey | S11 | `ADR-013`. The reason the pipe exists |
| NFR-OBS-02 | **No PII in the index** | **Zero** matches for regulated field patterns in the index mapping or a sampled document scan | `FF-27`, scheduled | S09, then continuously | A log pipeline is the commonest route to an unclassified PII store |
| NFR-OBS-03 | **Operational retention closes** | Indices deleted at `RET-OPERATIONAL` (90 days) with a disposal record; **no index without a lifecycle policy** | ISM policy assertion + disposal audit record | S09 | `NFR-DAT-07`. An index with no lifecycle grows until it is an incident |

Two things these rows deliberately do **not** measure. There is no throughput target for the broker,
the cache or the search domain: at CAP-A volumes they are sized for availability and evidence, and
a throughput NFR would invite exactly the scaling conversation §2.1 says is the wrong one. And
there is no availability tier for the search domain — it is an operational tool, so its loss is an
SRE inconvenience rather than a journey failure (`NFR-AVL` does not apply to it).

### 3.8 Engineering flow NFRs — NFR-ENG

These are NFRs of the delivery system rather than the product, and S08 gates on them.

| ID | Requirement | Target | Measure | Verify at |
|---|---|---|---|---|
| NFR-ENG-01 | PR pipeline feedback | p95 < 10 minutes over ≥ 50 runs | Pipeline metrics | S08 (S08-G9) |
| NFR-ENG-02 | Pipeline flake rate | < 1% over ≥ 50 runs | Pipeline metrics | S08 (S08-G9) |
| NFR-ENG-03 | Coverage floors | libs line ≥ 80% / branch ≥ 70%; services per policy, **build fails below** | JaCoCo verification | S08 (S08-G3) |
| NFR-ENG-04 | Build reproducibility | Same commit → identical artefact on two agents | Comparison test | S08 |
| NFR-ENG-05 | Local unit-test loop | < 3 minutes | Measured locally | S08 |

---

## 4. Verification ownership

| Stage | What is proven | Owner |
|---|---|---|
| **S08** | NFR-SEC-01…04, NFR-SEC-06, NFR-ENG-01…05 | Amit + Swapnali + Deepali |
| **S09** | NFR-DR-01…05, NFR-DAT-01/02/06/07, NFR-SEC-07, **NFR-NET-01/02, NFR-CAC-01/02, NFR-EVT-03, NFR-OBS-02/03** | Shivanshi + Aarti + Deepali |
| **S11** | NFR-LAT-09, NFR-DAT-04/05, NFR-SEC-05, **NFR-NET-04, NFR-CAC-03, NFR-EVT-01/02/04, NFR-OBS-01** | Amit + Swapnali |
| **S12** | NFR-LAT-01…08, NFR-THR-01…06, NFR-AVL-04…06, NFR-DR-06, NFR-DAT-03 | Shivanshi + Swapnali |
| **S14** | NFR-AVL-01…03, NFR-AVL-07, DR re-exercise | Shivanshi |
| **S15** | Continuous SLO and error-budget reporting | Shivanshi + Rajal |
| **Before S11 entry** | **NFR-NET-03** — 1SB and the PG have confirmed the inspection-VPC addresses | Shivanshi + Kalpana |

S07-VT-05 pass condition — *zero qualitative NFRs; every one has a named verification test*. Every
row in §3 carries a number, a measurement method and a verification stage. **72 NFRs, no
adjectives** — 58 from the original catalogue plus the 14 platform-tier rows added on 2026-08-24.

**Seven of the fourteen are drills rather than dashboards**, and that is the point: a failover, a
replay and a session-holding restart are the only forms of evidence that distinguish an installed
layer from a working one. They land in the S09 `P8` proof band (LLD §12.1), where nothing can be
produced in the week the gate is reviewed.

---

## 5. Open items

| ID | Item | Owner | Target |
|---|---|---|---|
| NFR-OPEN-1 | CAP-A1…A7 confirmed against an approved business baseline. Every throughput NFR moves with them | Rajal | Before S11 entry |
| NFR-OPEN-2 | 1SB and insurer contractual TPS, concurrency and maintenance windows. NFR-THR-02/03 are currently bounded by our own design, not by the provider's stated limits | Shivanshi + Rajal | Before S12 |
| NFR-OPEN-3 | AU Bank PG throughput and settlement-file cadence — NFR-DAT-03's 24 hours assumes a daily file | Shivanshi + Finance | Before S11 entry |
| NFR-OPEN-4 | Retention horizons confirmed against the final IRDAI/DPDP position (D-011 open) | Shailja | Before S11 entry |
| NFR-OPEN-5 | Cost model per issued policy | Shivanshi + Kalpana | S14 |
| NFR-OPEN-6 | **Run-rate cost of the five 2026-08-24 layers**, per environment, against the §1.4 shapes. Three stateful services, an inspection VPC per environment and two circuits, at ~100 journey starts an hour — the fixed cost now dominates the variable cost, and nobody has priced it | Shivanshi + Kalpana (`RISK-012`) | **S09 output, before first `apply` to `uat`** |
| NFR-OPEN-7 | Bank-side confirmation of VPN termination, prefixes, firewall change and the DX order date (`DEP-20260824-dx1`). `NFR-NET-01` and `NFR-NET-04` cannot be measured until it exists | Shivanshi + bank network | Before S11 entry |

---

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding (S07-G6 requires a signed NFR sheet: Mahesh AP, Shivanshi AP, Aarti AP, Deepali AP/B/H, Shailja AP/B, Rajal RV)`
**Date:** 2026-08-16 · **revised** 2026-08-24 (R0 robustness round — §3.7 platform-tier NFRs, NFR-OPEN-6/7)
