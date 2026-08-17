# 02 — Sprint Backlog Allocation: every epic and story, placed

**Owner:** Kalpana (allocation and sequencing) · **Scope authority:** Rajal · **Story source:**
[`BACKLOG.yaml`](../application-lifecycle-bible/backlog/BACKLOG.yaml) — generated from
[`stages/*.md`](../application-lifecycle-bible/stages/), 447 stories across 16 stages.

This document answers: **how many stories in each sprint, which squad owns them, and why they sit
where they sit.**

---

## 1. Scope arithmetic — what of the 447 is in this window

| Stage | Epics | Stories | In R0 window? | Placed |
|---|---:|---:|---|---:|
| S00 Ideation & Business Case | 4 | 15 | Residual only — `GAP-010` sponsor naming | 1 |
| S01 Discovery | 5 | 18 | Complete (🟢) | 0 |
| S02 Regulatory & Compliance Framing | 6 | 28 | **Residual — 2 signatures (`S02-G3/G4`)** | 2 |
| S03 Business Requirements | 5 | 23 | Residual — `S03-OPEN-03/05/07/09` | 4 |
| S04 Product Definition | 5 | 21 | Residual — `S04-OPEN-01/07` | 2 |
| S05 Experience Design | 6 | 25 | **Residual — design system + usability evidence** | 8 |
| S06 Domain & Information Architecture | 5 | 21 | Residual — thin-context state models | 5 |
| S07 Solution & Security Architecture | 6 | 32 | **Residual — contracts freeze + NFR numbers** | 9 |
| **S08 Engineering Foundation** | 6 | **36** | **Fully in scope** | 36 |
| **S09 Platform Foundation** | 7 | **45** | **Fully in scope** | 45 |
| **S10 Integration & Connectivity** | 7 | 32 | In scope less `S10-E05-S03` (customer auth → R1) | 31 |
| **S11 Vertical Slice (MVP)** | 11 | **50** | **Fully in scope — the business case** | 50 |
| **S12 Hardening & Certification** | 6 | **31** | **Fully in scope** | 31 |
| S13 Expansion & Scale | 5 | 22 | **Out — R1/R2** | 0 |
| **S14 Production Readiness & Go-Live** | 5 | **25** | **Fully in scope** | 25 |
| S15 Operate & Evolve | 4 | 23 | **Out — starts at hypercare 2027-01-01** | 0 |
| | | **447** | | **249** |

**249 stories in 8 sprints ≈ 31 per sprint ≈ 5 per squad per sprint.** Plus **141 validation tests**
and **143 gate criteria**, which are executed by SQ-6 and TRK-G rather than counted as build.

> **Story count is a poor proxy for effort and is used here only for load balancing.** Sprint 4
> carries the fewest stories and the most risk: four compliance controls at 100% coverage. Sprint 2
> carries the most stories and many are small infrastructure configurations. Effort labels
> (S/M/L/XL) accompany each block below.

---

## 2. Allocation summary

| Sprint | Dates | Stories | Effort profile | Theme |
|---|---|---:|---|---|
| **S0** | 17–23 Aug | — | Mobilisation | Unblock 10 dependencies |
| **S1** | 24 Aug – 6 Sep | **38** | Many S/M | Pipeline + contract freeze |
| **S2** | 7–20 Sep | **48** | Many S/M, 3 L | Foundation + environments · `GATE-S08` |
| **S3** | 21 Sep – 4 Oct | **38** | Mixed, 4 L | Platform · `GATE-S09` · slice starts |
| **S4** | 5–18 Oct | **19** | **Few, mostly L/XL** ⚠ | **The four compliance controls** |
| **S5** | 19 Oct – 1 Nov | **23** | Mixed, 5 L | Money path + usability evidence |
| **S6** | 2–15 Nov | **22** | Mostly M ⚠ Diwali | Slice closure · `GATE-S11` |
| **S7** | 16–29 Nov | **35** | Execution-heavy | Certification execution |
| **S8** | 30 Nov – 13 Dec | **26** | Mixed + human gates | Production readiness · go/no-go |
| | | **249** | | |

---

## 3. Sprint 0 · 17–23 Aug · Mobilisation (no story commitment)

Ten dependency actions, listed in
[01 §5](./01-DELIVERY-TIMELINE-AND-SPRINT-PLAN.md#sprint-0--1723-aug--mobilise-and-unblock).
Three are `DL0`: AWS landing zone request, rule-pack signatures, pentest RFQ.

Plus setup: squads staffed · `jira-import.csv` loaded · DoR/DoD adopted (`S04-E04-S01/S02`) ·
T4 human sign-off slots booked for 9–11 December.

---

## 4. Sprint 1 · 24 Aug – 6 Sep · Pipeline and contracts — 38 stories

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-1 | `S08-E01` Continuous integration pipeline | S01–S07 | 7 | M |
| SQ-6 | `S08-E02` Quality gates in the pipeline | S01–S05 | 5 | M |
| SQ-6 | `S08-E04` Security in the pipeline | S01–S06 | 6 | L |
| SQ-1 | `S09-E01` IaC — repo, module standard, remote state, **region pinning to India** | S01, S02, S06 | 3 | M |
| SQ-1 | `S08-E05` Engineering standards — coding, secure coding, branching, DoD | S01, S02, S03, S06 | 4 | S |
| SQ-3 | `S07-E02` **Communication architecture — contract freeze** | S01–S05 | 5 | L |
| SQ-2 | `S07-E05` NFR numbers (closes `GAP-017`) | S01–S04 | 4 | M |
| SQ-5 | `S05-E03` Design system — components, tokens, responsive, motion | S01–S04 | 4 | M |
| TRK-G | `S00-E04` Executive sponsor named (`GAP-010`) | S01 | 1 | External |
| | | | **38** | |

**Sprint 1 exit condition:** API contracts frozen. Every downstream squad builds against them from
Sprint 2. This is the single dependency-breaking move the whole plan rests on
([Kalpana §5](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md#5-dependency-breaking-techniques)).

---

## 5. Sprint 2 · 7–20 Sep · Foundation and environments — 48 stories · `GATE-S08`

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-6 | `S08-E03` **Test infrastructure** — Testcontainers, WireMock, fixtures, contract, E2E, synthetic data, perf harness | S01–S07 | 7 | **L** — closes `TD-014` |
| SQ-1 | `S08-E05` PII-masking standard proven; observability standard for code | S04, S05 | 2 | M |
| SQ-1 | `S08-E06` Developer experience — one-command env, fast feedback, scaffolding, onboarding | S01–S04 | 4 | M |
| SQ-1 | `S09-E01` Network, compute, data foundation; IaC scanning; drift detection | S03, S04, S05, S07, S08 | 5 | **L** |
| SQ-1 | `S09-E02` **Environments** — dev/UAT/prod, promotion, isolation, config, ephemeral, no prod data below | S01–S06 | 6 | L |
| SQ-1 | `S09-E03` Deployment — automated deploy, progressive delivery, **tested rollback**, migration in path, gating | S01–S05 | 5 | L |
| SQ-1 | `S09-E04` **Secrets and keys** — real secrets mgmt (closes `TD-006`), no secrets in code/image, KMS hierarchy, rotation, revocation, workload identity | S01–S06 | 6 | L |
| SQ-4 | `S10-E01` Integration catalogue, sandbox access, criticality tiers, readiness tracking | S01–S04 | 4 | S |
| SQ-4 | `S10-E02` **1SB adapter re-certified** against new machinery (service exists) | S01–S06 | 6 | S — re-cert |
| SQ-2 | `S10-E05` Identity — AD federation, token-hiding BFF, certification metadata (WS-2 `GATE-IAM-P1`) | S01, S02, S04 | 3 | M |
| | | | **48** | |

**`GATE-S08` target PASS 20 Sep** — criteria S08-G1…G10.
**Cascade:** closing `S08-G3` unblocks WS-1 criterion **4.7**; the E2E harness unblocks **4.1** and,
via `DEP-003`, **4.6**.

---

## 6. Sprint 3 · 21 Sep – 4 Oct · Platform live, slice begins — 38 stories · `GATE-S09`

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-1 | `S09-E05` Observability — metrics, logs, traces, dashboards, alerting, **separate audit pipeline** | S01–S06 | 6 | L |
| SQ-1 | `S09-E06` **Data protection** — encryption at rest, TLS, backup, **prove restore**, **7-year immutable retention**, purge, **residency attestation** | S01–S07 | 7 | **L** |
| SQ-1 | `S09-E07` Platform security baseline — segmentation, least-privilege IAM, container hardening, runtime monitoring, edge, security logging | S01–S06 | 6 | L |
| SQ-4 | `S10-E03` **CBS integration** — CIF lookup, KYC/demographic prefill, relationship verification, degraded behaviour | S01–S04 | 4 | M |
| SQ-6 | `S10-E07` **Resilience** — timeouts, retry+backoff, circuit breakers, bulkheads, rate limiting, dependency health | S01–S06 | 6 | M |
| SQ-3 | `S11-E01` Journey state machine; journey telemetry | S01, S04 | 2 | L |
| SQ-3 | `S11-E02` Create a lead | S01 | 1 | M |
| SQ-2 | `S11-E03` Need-analysis questionnaire; suitability evaluation; consent capture | S01, S02, S05 | 3 | **L** |
| SQ-2 | `S11-E07` Audit event per regulated action (append-only store) | S01 | 1 | L |
| SQ-5 | `S11-E06` RM authentication; journey screens begin | S01, S02 | 2 | M |
| | | | **38** | |

**`GATE-S09` target PASS 4 Oct.**
**Hard entry check before any `S11-*` story starts:** `S02-G3` **and** `S02-G4` signed. Non-waivable
(Rajal condition C5). If unsigned on 21 September, S11 stories do not start and
[lever L1](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md) is pulled the same day.

---

## 7. Sprint 4 · 5–18 Oct · The four compliance controls — 19 stories ⚠

**Fewest stories, highest risk, largest average effort.** These are the stories that make the
business lawful. Swapnali enforces 100% coverage on all of them (`S08-E02-S06`).

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-2 | `S11-E03` **C1 suitability hard-gate (403 without valid evaluation ID)**; override handling; **C2 consent enforcement**; **C3 attribution** | S03, S04, S06, S07 | 4 | **XL — non-waivable** |
| SQ-6 | `S08-E02` **100% coverage enforced on compliance-gate code** | S06 | 1 | **L — gate** |
| SQ-4 | `S11-E05` **C4 payment device isolation** | S01 | 1 | **XL — non-waivable** |
| SQ-4 | `S10-E04` Payment session and link generation; status callback handling | S01, S02 | 2 | L |
| SQ-3 | `S11-E02` Identify ETB customer via CBS; snapshot customer profile | S02, S03 | 2 | M |
| SQ-3 | `S11-E04` Generate Term quote; present with disclosed basis; enforce validity | S01, S02, S03 | 3 | L |
| SQ-3 | `S11-E08` R0 catalogue seed (Life / Group A / Term) | S01 | 1 | M |
| SQ-5 | `S11-E06` **Customer-device hand-off** | S03 | 1 | **L** |
| TRK-G | `S03`/`S04` residuals — `S03-OPEN-03` agentId source, `S03-OPEN-05` physical schema, `S04-OPEN-01` insurer/product values, `S04-OPEN-07` RM walkthrough | — | 4 | External |
| | | | **19** | |

**Milestone: C1–C4 implemented and 100% covered by 18 Oct.** From this point Swapnali's `Q0` hold
criteria are satisfiable and the platform is, for the first time, lawfully shippable in principle.

---

## 8. Sprint 5 · 19 Oct – 1 Nov · Money path and usability evidence — 23 stories

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-4 | `S11-E05` Payment status; **issuance trigger and confirmation**; **reconciliation**; **`Sold` determination**; policy document delivery | S02–S06 | 5 | **L** |
| SQ-4 | `S10-E04` Status reconciliation by polling; **uncertain-payment case**; refund and failure paths | S03, S04, S05 | 3 | **L** |
| SQ-4 | `S10-E06` Notification — SMS for OTP and payment links, email for documents, template versioning | S01–S03 | 3 | M |
| SQ-3 | `S11-E04` Proposal capture; underwriting outcomes; document upload | S04, S05, S06 | 3 | L |
| SQ-3 | `S11-E01` Journey compensations; save and resume | S02, S03 | 2 | L |
| SQ-2 | `S11-E07` **Prove journey reconstruction from audit records alone** | S02 | 1 | L |
| SQ-5 | `S11-E06` Error/empty/degraded states; offline and poor-connectivity behaviour | S04, S05 | 2 | M |
| SQ-5 | `S05-E06` **Usability with ≥5 RMs; ≥5 customers on device hand-off; accessibility audit; iterate on findings** | S01–S04 | 4 | M — external |
| | | | **23** | |

**Milestone: usability evidence captured by 24 Oct**, matching the dates already recorded in
[S05 evidence](../application-lifecycle-bible/evidence/S05-experience-evidence.md). Closes `S05-G6`
and `S05-G5`.
**Parallel:** SQ-6 runs the **internal pre-pentest security assessment** and authors load-test
scenarios, so Sprint 7's external test is not spent on findings we could have found ourselves.

---

## 9. Sprint 6 · 2–15 Nov · Slice closure — 22 stories ⚠ Diwali, 80% capacity · `GATE-S11`

Deliberately light and deliberately integration-shaped. No new foundational build lands here.

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-2 | `S11-E07` Journey KPI instrumentation; journey dashboard | S03, S04 | 2 | M |
| SQ-3 | `S11-E08` Consent/disclosure version publication; workforce and partner scopes; safe config activation | S02, S03, S04 | 3 | M |
| SQ-3 | `S11-E09` **Insurer representative** — partner-visible cases, UW requirement response, collaboration | S01–S03 | 3 | M |
| SQ-4 | `S11-E10` **Operations** — exception queue, reconciliation mismatches, UW escalation, post-issuance tracking | S01–S04 | 4 | M |
| SQ-5 | `S11-E11` **Management oversight** — branch, regional/sales, business-head, **role separation (`S11-VT-16`)** | S01–S04 | 4 | M |
| SQ-6 | `S12-E01` Build the regression suite | S04 | 1 | L |
| SQ-1 | `S09-E03` Deployment observability | S06 | 1 | S |
| TRK-G | **WS-1 `GATE-P4` closure** — criteria 4.1, 4.4, 4.5, 4.6 now unblocked by S08/S09 | — | 4 | M |
| | | | **22** | |

**`GATE-S11` target PASS 15 Nov.**
**Milestone M3 — the one that matters: a complete R0 sale, end to end, in UAT.** Lead → need
analysis → suitability → consent → quote → proposal → payment on the customer's device → issued →
reconciled → audited. Through the real Flutter UI. This is the stage the programme has never
attempted.

---

## 10. Sprint 7 · 16–29 Nov · Certification execution — 35 stories

Execution, not authoring. SQ-6 has written suites in-sprint since Sprint 2.

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-6 | `S12-E01` Functional certification — full suite, negative/boundary/exception, data-variant suite, traceability matrix | S01, S02, S03, S05 | 4 | L |
| Deepali + SQ-6 | `S12-E02` **Security certification** — **independent pentest**, authorization negative testing, secrets/keys in a running system, trust boundaries as built, threat-model refresh | S01, S03, S04, S05, S06 | 5 | **XL — external** |
| Shailja | `S12-E03` **Compliance certification** — **C1–C10 certified in the running system**, audit schema review, consent/suitability evidence end to end, retention and immutability live, **residency live** | S01–S05 | 5 | **XL — human** |
| SQ-1 + SQ-6 | `S12-E04` Performance and resilience — **load at projected peak incl. Q4 tax-season multiplier**, soak, failure injection, resilience under load, baseline | S01–S05 | 5 | L |
| SQ-1 | `S12-E05` Operational certification — runbook written **and exercised**, alerting verified, **rollback verified under realistic conditions**, failure-mode analysis | S01–S05 | 5 | L |
| SQ-4 | `S12-E06` Consumer enablement — publish contracts, support a real consumer, integration docs | S01–S03 | 3 | M |
| SQ-1 | `S14-E01` **Production provisioned from the same IaC as UAT**; credentials, data protection, capacity sizing, access control | S01–S05 | 5 | L |
| TRK-G | WS-1 `GATE-P4` criterion 4.3 — bank consumer UAT (`DEP-002`) | — | 1 | External |
| Shailja | `S12-E03-S06` Regulatory evidence pack — assembly begins | S06 | 1 | L |
| SQ-6 | `S12-E01-S06` Defect triage begins | S06 | 1 | M |
| | | | **35** | |

---

## 11. Sprint 8 · 30 Nov – 13 Dec · Production readiness and go/no-go — 26 stories

| Squad | Epic | Stories | # | Effort |
|---|---|---|---:|---|
| SQ-6 | `S12-E01-S06` Defect closure; regression re-run | — | 1 | L |
| Deepali + SQ-6 | `S12-E02-S02` **Remediate pentest findings per SLA** | S02 | 1 | **XL — risk carrier** |
| SQ-1 + Rajal | `S14-E02` **SLIs/SLOs published; error-budget policy agreed**; production dashboards; production alerting; end-to-end traceability verified in production | S01–S05 | 5 | M |
| SQ-1 | `S14-E03` **ORR against O1–O13**; on-call established; runbooks verified against production; **incident simulation**; support model | S01–S05 | 5 | **L** |
| SQ-1 + Aarti | `S14-E04` **DR configuration; DR test executed; production-grade restore test; DR decision rehearsed** | S01–S04 | 4 | **L** |
| Kalpana + Rajal | `S14-E05` Go-live checklist; launch approach; rollback and contingency; **business readiness**; hypercare plan; **go-live approvals** | S01–S06 | 6 | M |
| Shailja | `S12-E03-S06` **Regulatory evidence pack complete** | — | 1 | L |
| TRK-G | **Mandatory human T4 sign-offs** — Architecture (Mahesh's human), Security (Deepali's human), Risk & Compliance (Shailja's human) | — | 3 | **Human — booked in Sprint 0** |
| | | | **26** | |

**`GATE-S12` and `GATE-S14` target PASS 11 Dec. Go/no-go decision 11 Dec.**

---

## 12. Story allocation by squad across the window

| Squad | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | Total |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| **SQ-1 Platform & Pipeline** | 14 | 28 | 19 | 0 | 0 | 1 | 10 | 14 | **86** |
| **SQ-2 Trust & Compliance** | 4 | 3 | 4 | 5 | 1 | 2 | 5 | 0 | **24** |
| **SQ-3 Journey & Sales** | 5 | 0 | 3 | 6 | 5 | 6 | 0 | 0 | **25** |
| **SQ-4 Money & Fulfilment** | 0 | 10 | 4 | 3 | 11 | 4 | 9 | 0 | **41** |
| **SQ-5 Experience** | 4 | 0 | 2 | 1 | 6 | 4 | 0 | 0 | **17** |
| **SQ-6 Quality & Certification** | 11 | 7 | 6 | 1 | 0 | 1 | 10 | 2 | **38** |
| **TRK-G / TRK-E** | 1 | 0 | 0 | 4 | 0 | 4 | 2 | 10 | **21** |
| | **38** | **48** | **38** | **19** | **23** | **22** | **35** | **26** | **249** |

### 12.1 What this table shows, and what it warns about

- **SQ-1 carries 86 of 249 stories (35%).** The platform stream is the largest single load, and it
  is front-loaded into Sprints 2–3. **Under-staffing SQ-1 is the fastest way to lose this date.**
- **SQ-2's count (24) badly understates its risk.** It owns C1–C4. Four stories in Sprint 4 carry
  more regulatory weight than the other 245 combined.
- **SQ-5 is light on count (17) and heavy on external dependency** — usability sessions with real
  RMs and real customers cannot be compressed by adding engineers.
- **SQ-3 and SQ-4 finish in Sprint 6.** From Sprint 7 they are redeployed to defect burn-down and
  consumer enablement, which is how Sprints 7–8 absorb 61 stories with no new hiring.

---

## 13. Definition of Ready and Done applied to this window

**DoR** (`S04-E04-S01`) — a story is not started without: a traced requirement ID, Given/When/Then
acceptance criteria, a named squad, a frozen contract if it crosses a service seam, and its test
approach agreed with SQ-6.

**DoD** (`S04-E04-S02`) — a story is not done without: tests green in CI, coverage threshold met
(**100% if it is compliance-gate code**), ArchUnit clean, evidence linked to the gate criterion it
serves, observability present, and **no PII in logs proven by test**.

> **In a compressed window the DoD is the thing under most pressure and the thing least safe to
> relax.** A story marked done without evidence is a story that will be re-done in December, when
> there is no December left. Kalpana will not accept a sprint increment where DoD is asserted
> rather than evidenced — that is precisely the failure mode
> [the position assessment](../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#7-how-this-happened--so-it-does-not-recur)
> identified as cause #3.
