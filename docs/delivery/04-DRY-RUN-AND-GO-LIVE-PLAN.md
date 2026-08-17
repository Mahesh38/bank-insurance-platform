# 04 — Cutover, the 15-Day Dry Run, and Go-Live

**Owner:** Shivanshi (operational readiness, deployment safety, dry-run design — `O/A/R`) with
Kalpana (orchestration and sequencing) · Rajal (business readiness and pilot cohort) ·
Swapnali (evidence sufficiency) · Shailja (lawfulness to sell) · Aarti (data recovery)
**Fixed dates:** cutover **2026-12-16** · dry run **2026-12-17 → 2026-12-31** · go-live **2027-01-01**

---

## 1. What the 15 days are for

Shivanshi's position from the [planning session](./00-STAKEHOLDER-BRAINSTORMING-SESSION.md#39-shivanshi--sre--operations-r10-board-7),
adopted as the design principle:

> **Fifteen days of production uptime proves almost nothing on its own.** Fifteen days in which we
> deliberately fail over, roll back, rotate a secret, restore a database, page an on-call engineer
> at 02:00 and run the incident simulation proves the thing we actually need to know.

The dry run is therefore an **operational rehearsal against real production**, not a soak test and
not an extended UAT. Its purpose is to answer four questions before a customer's money is involved:

1. **Does it work in production** — with production data paths, production secrets, production
   network policy, production identity? (Not "did it work in UAT".)
2. **Can we recover it** — restore, fail over, roll back, rotate, revoke, on real infrastructure?
3. **Can we operate it** — does the runbook match reality, does alerting fire, does on-call answer?
4. **Is it lawful** — do C1–C10 hold in the running production system, is evidence immutable, is
   data resident in India?

A dry run that only answers question 1 has wasted 15 days that cannot be recovered.

---

## 2. Cutover · 14–16 December

### Mon 14 Dec — Freeze and build

| Time | Activity | Owner | Evidence |
|---|---|---|---|
| 09:00 | **Code freeze.** `main` locked to release-blocker fixes only, by branch protection | Amit | Branch protection config |
| 10:00 | Final release candidate built from a tagged commit; SBOM generated | SQ-1 | Build artefact + SBOM |
| 11:00 | Image signed and scanned; zero critical/high findings, or waivers with expiry and compensating controls | Deepali | Scan report |
| 14:00 | Production data protection re-verified — encryption at rest, TLS, KMS CMK ownership | Deepali + Aarti | Verification record |
| 16:00 | Residency attestation refreshed — every store, backup, log and archive in AWS India | Shailja | Attestation |
| 17:00 | **Cutover go/no-go #1** — is the candidate deployable? | Kalpana convenes | Recorded decision |

### Tue 15 Dec — Rehearsal

| Time | Activity | Owner | Evidence |
|---|---|---|---|
| 09:00 | **Full deployment rehearsal on a production-identical stack**, from the same IaC | Shivanshi | Deployment log |
| 11:00 | Database migration executed forward on production-shaped data | Aarti | Migration log |
| 13:00 | **Rollback rehearsed** — deploy N-1, verify, redeploy N. Timed | Shivanshi | Rollback timing record |
| 15:00 | Migration reversibility verified | Aarti | Reversal record |
| 16:00 | Runbook walked step-by-step against the rehearsal; every mismatch corrected same day | Shivanshi | Corrected runbook |
| 17:00 | **Cutover go/no-go #2** — did the rehearsal succeed without improvisation? | Kalpana convenes | Recorded decision |

> **Rule:** any step performed during rehearsal that is **not in the runbook** is a runbook defect,
> fixed before cutover. Improvised steps at 15:00 on 15 December become 03:00 incidents in January.

### Wed 16 Dec — Production deployment · **Milestone M7**

| Time | Activity | Owner | Exit condition |
|---|---|---|---|
| 08:00 | Production deployment begins, progressive delivery at 0% traffic | Shivanshi | — |
| 09:00 | Health, readiness and dependency checks green across all services | SQ-1 | All green |
| 10:00 | Connectivity verified: 1SB (prod), CBS, AU Bank PG, Bank AD, SMS gateway | SQ-4 | All reachable, authenticated |
| 11:00 | **Access control verified** — default-deny proven by negative test in production | Deepali | Negative test evidence |
| 12:00 | **Residency verified in the running system** (`S12-E03-S05`) | Shailja | Live evidence |
| 14:00 | Observability confirmed — metrics, logs with correlation IDs, traces, audit pipeline separate | Shivanshi | Dashboards live |
| 15:00 | **No PII in logs, verified against live production log output** | Deepali | Automated scan result |
| 16:00 | **Deployment frozen.** No further change without the emergency change process | Kalpana | Freeze record |
| 17:00 | **M7 declared: production-ready application deployed** | Kalpana + Shivanshi | Milestone record |

---

## 3. The fifteen-day dry run, day by day

**17–31 December.** Four phases, deliberately ordered so that **destructive testing happens while
there is still runway to fix what it breaks**. Putting the DR test on 30 December would be theatre.

| Phase | Days | Dates | Purpose | Traffic |
|---|---|---|---|---|
| **A — Synthetic verification** | 1–3 | 17–19 Dec | Does production behave like UAT? | Synthetic only |
| **B — Controlled live pilot** | 4–8 | 20–24 Dec | Do real RMs and real customers complete real journeys? | Real, capped |
| **C — Destructive rehearsal** | 9–12 | 25–28 Dec | Can we break it and get it back? | Synthetic + real |
| **D — Freeze and certify** | 13–15 | 29–31 Dec | Is the evidence complete and signed? | Observation only |

---

### Phase A — Synthetic verification · Days 1–3 · 17–19 Dec

**Question:** does production behave identically to the certified UAT?

| Day | Activity | Owner | Pass condition |
|---|---|---|---|
| **D1 · Thu 17** | Full E2E journey suite executed against **production with synthetic customers** | Swapnali | 100% pass; zero P1 defects |
| D1 | **C1 suitability hard-gate verified in production** — quote request without a valid evaluation ID returns 403 | Swapnali + Shailja | 403, audited, no insurer call |
| D1 | **C2 consent verified** — proposal without an unexpired grant is refused | Swapnali + Shailja | Refused, audited |
| **D2 · Fri 18** | **C3 attribution verified** — `distributorId` server-injected; caller-supplied value rejected; expired SP certification → 403 | Swapnali + Shailja | All three behaviours confirmed |
| D2 | **C4 payment device isolation verified** — no API path issues a payment link into an RM session | Deepali + Swapnali | No path exists |
| D2 | Performance baseline measured in production; compared to the S12 baseline | SQ-6 + Shivanshi | Within 15% of baseline |
| **D3 · Sat 19** | **Journey reconstruction from audit records alone** for 5 synthetic journeys | Shailja | Full reconstruction, all five |
| D3 | Alerting verified — each alert deliberately triggered, each pages the right rota | Shivanshi | Every alert fires and routes |
| D3 | **Phase A gate** — proceed to real customers? | Kalpana convenes; Swapnali + Shailja decide | Recorded |

> **Phase A stop condition:** any of C1–C4 failing in production stops the dry run. It does not
> get a workaround; it gets a fix, a re-deploy and a repeat of Phase A. This is Swapnali's `Q0`
> and Shailja's block authority, and both apply in production exactly as they apply in UAT.

---

### Phase B — Controlled live pilot · Days 4–8 · 20–24 Dec

**Question:** do real RMs sell real policies to real customers, correctly and completely?

This is a **real sale with real money and a real issued policy**. It is not a test transaction.
Every policy sold in this phase is a genuine customer contract and is treated as such.

| Day | Activity | Owner | Pass condition |
|---|---|---|---|
| **D4 · Sun 20** | Progressive delivery opens to **2 RMs, 1 branch**. Target: 2 complete sales | Rajal + Shivanshi | 2 sales issued, reconciled, audited |
| **D5 · Mon 21** | Widen to **5 RMs**. Target: 5 sales. First reconciliation cycle run | Rajal | Reconciliation 100% matched |
| **D6 · Tue 22** | Widen to the **full pilot cohort**. Target: 10 cumulative sales | Rajal | Funnel KPIs emitting |
| D6 | **Exception paths exercised deliberately** — UW rejection, payment failure, insurer timeout, consent withdrawal, KYC mismatch | Rajal + Ops | Every path has a defined resolution and an ops owner |
| **D7 · Wed 23** | Operations runs the exception queue for a full day, unaided by engineering | Ops + Shivanshi | Zero engineering escalations for known exceptions |
| **D8 · Thu 24** | **Business readiness review.** Cumulative target: **20 clean sales** | Rajal + Dilip lens | 20 sales: issued + confirmed + reconciled + persisted |
| D8 | **Phase B gate** — is the business case proven? | Rajal decides; Kalpana records | Recorded |

**Success measure, per the sponsor lens:** *twenty real, clean, reconciled, audited sales are worth
more than two hundred with a reconciliation backlog.* The number to beat is **reconciliation
completeness and audit completeness, not volume.**

| KPI (`S04-E05-S01`) | Target for Phase B |
|---|---|
| Sales completed end to end (`Sold` = issued + confirmed + reconciled + persisted) | ≥ 20 |
| Reconciliation completeness | **100%** — no exceptions carried into January |
| Audit completeness — every regulated action has an attributable event | **100%** |
| Journey drop-off by step | Measured and explained; no unexplained cliff |
| Time to issue | Measured; baseline established |
| RM adoption — RMs completing a journey unaided | ≥ 80% of cohort |

---

### Phase C — Destructive rehearsal · Days 9–12 · 25–28 Dec

**Question:** can we break production and get it back, with people, at real hours?

**This phase is why the dry run is 15 days and not 3.** It is deliberately placed with three days
of runway behind it.

| Day | Exercise | Owner | Pass condition |
|---|---|---|---|
| **D9 · Fri 25** | **Production-grade restore test** — restore the primary datastore from backup to a parallel stack, verify integrity and completeness | **Aarti** | RPO met; data verified; **restore is a fact, not a configuration** |
| D9 | 7-year immutable retention verified — attempt to mutate and to delete a consent, suitability and audit record | Aarti + Shailja | **Both attempts fail.** Object Lock holds |
| **D10 · Sat 26** | **DR failover exercise** — fail over to the DR configuration, verify service, fail back | Shivanshi + Aarti | RTO met; no data loss; failback clean |
| D10 | **DR decision rehearsed** — who declares a disaster, on what signal, with what authority | Shivanshi + Kalpana | Named human, documented trigger |
| **D11 · Sun 27** | **Rollback under realistic conditions** — deploy a deliberately faulty build, detect via alerting, roll back | Shivanshi | Detected by alert not by human; rollback within target |
| D11 | **Secret rotation and emergency revocation** exercised in production — 1SB credential, DB credential, KMS key | Deepali + Shivanshi | Rotation with zero downtime; revocation blocks access immediately |
| **D12 · Mon 28** | **Incident simulation** — unannounced, out of hours, page the on-call rota | Shivanshi | On-call answers within SLA; runbook followed; no improvisation |
| D12 | **Uncertain-payment incident** simulated — the hardest real case: payment state unknown at the insurer | Rajal + Aarti + Amit + Shailja | **Financial correctness preserved over availability.** No duplicate charge, no phantom policy |
| D12 | **Phase C gate** — is it operable and recoverable? | Shivanshi decides; Kalpana records | Recorded |

> **Availability must not outrank financial correctness.** The D12 uncertain-payment exercise is
> the direct rehearsal of that principle, and it is the exercise most likely to find a real defect.

---

### Phase D — Freeze and certify · Days 13–15 · 29–31 Dec

**Question:** is the evidence complete, and is anyone prepared to sign?

| Day | Activity | Owner | Output |
|---|---|---|---|
| **D13 · Tue 29** | **Full change freeze.** No deploys. Emergency process only | Kalpana | Freeze record |
| D13 | All defects from Phases A–C triaged: fixed, waived with expiry and compensating control, or carried with a named owner | Swapnali | Defect disposition |
| D13 | Dry-run evidence pack assembled — every exercise, its result, its artefact | Kalpana + Swapnali | Evidence pack |
| **D14 · Wed 30** | **Compliance final review** — C1–C10 confirmed operating in production over 13 days of real evidence | **Shailja** | Compliance conclusion |
| D14 | **Security final review** — production trust boundaries, pentest remediation confirmed live, no PII in 13 days of logs | **Deepali** | Security conclusion |
| D14 | **Operational final review** — ORR re-confirmed against real production behaviour; on-call rota confirmed for January | **Shivanshi** | Board 7 conclusion |
| D14 | **Quality final review** — evidence sufficiency across all phases | **Swapnali** | Quality-exit recommendation |
| **D15 · Thu 31** | **Final go/no-go for live selling** | Kalpana convenes; **Rajal, Shailja, Deepali, Swapnali, Shivanshi, Mahesh decide** | **Recorded decision** |
| D15 | Hypercare rota confirmed and staffed for 1–14 January | Shivanshi + Kalpana | Rota published |
| D15 | Rollback and withdrawal criteria re-confirmed and communicated to the cohort | Rajal + Kalpana | Communicated |

---

## 4. Go/no-go decision — 31 December

The decision is not Kalpana's. Kalpana convenes it, assembles the evidence and records the outcome.

| Authority | Question they answer | Veto? |
|---|---|---|
| **Shailja — Compliance** | Is it lawful to sell on 1 January? C1–C10 operating? Evidence pack complete? | **Yes — binding** |
| **Deepali — Security** | Are the S0 controls operating in production? Pentest findings remediated? | **Yes — binding** |
| **Swapnali — QA** | Is the evidence sufficient? Are the four gates at 100%? | **Yes — `Q0` within QA jurisdiction** |
| **Shivanshi — Operations** | ORR passed? On-call live? Recovery proven? Board 7 verdict? | **Yes — Board 7** |
| **Aarti — Database** | Is the data correct, recoverable and retained? | Within jurisdiction |
| **Mahesh — Architecture** | Is the system as built the system as designed? | Within jurisdiction |
| **Rajal — Product** | Does R0 deliver its outcome? Is the cohort ready? | **Scope and launch decision** |
| **Named humans** | T4 Architecture, Security, Risk & Compliance signatures | **Mandatory, non-delegable** |

**No majority vote overrides a binding Security or Compliance decision.** A single binding veto is
a no-go regardless of how the other seven vote — Rule 3 of the conflict rules, applied here in the
one place it matters most.

### 4.1 Partial go-live options if the decision is not a clean yes

| Outcome | Meaning | Trigger |
|---|---|---|
| **GO** | Full pilot cohort sells from 1 January | All authorities clear |
| **GO-NARROW** | Single branch, 2–5 RMs, daily review, widen weekly under flag | A `DL1` concern with a compensating control |
| **GO-OBSERVE** | System live, RMs sell, **manual compliance verification on every case** before issuance | Compliance evidence complete but automated verification immature |
| **NO-GO** | Selling deferred; system stays deployed and monitored; new date set | Any binding veto |

> **A no-go is not a failure of the plan; it is the plan working.** The system is deployed, the
> evidence exists, and the deferral is measured in weeks rather than in a regulatory finding.
> Under Rajal's and the sponsor lens's position, a January no-go still leaves most of the Q4
> tax season available.

---

## 5. Go-live · 1 January 2027 and hypercare

| Period | Mode | Detail |
|---|---|---|
| **1 Jan** | Go-live | Selling opens to the approved cohort. War room staffed. Every sale reviewed same-day |
| **1–3 Jan** | Hypercare tier 1 | Engineering + ops on-site rota. All hands. Any P1 defect stops selling |
| **4–14 Jan** | Hypercare tier 2 | On-call rota with escalation. Daily reconciliation review. Daily compliance sample |
| **15 Jan** | Hypercare exit review | Kalpana + Shivanshi + Rajal. Transition to steady-state operations |
| **From 15 Jan** | **S15 — Operate, Evolve & Continuous Assurance** | The stage begins here. R1 planning starts from pilot evidence |

### 5.1 Withdrawal criteria — what would stop selling after go-live

Defined now, when it is a calm decision, rather than in the moment. Rajal owns the business
decision; the trigger conditions are each authority's.

| Trigger | Authority | Action |
|---|---|---|
| Any C1–C4 control failing in production | Shailja / Deepali | **Immediate stop.** Selling suspended, not degraded |
| Reconciliation mismatch unresolved > 24h | Rajal + Aarti | Stop new sales; resolve backlog first |
| A policy issued against a non-reconciled payment | Shailja | **Immediate stop** — standing constraint violated |
| Any PII found in production logs | Deepali | Stop; contain; assess reportability |
| Data residency breach | Shailja | **Immediate stop**; regulatory notification assessment |
| Availability below SLO with error budget exhausted | Shivanshi | Narrow the cohort; freeze change |
| RM completion rate below 50% | Rajal | Not a stop — a product problem. Pause widening, fix journey |

---

## 6. What the dry run will probably find

Stated in advance so that finding something is treated as success rather than as alarm. Based on
the estate's current position, the highest-probability discoveries are:

1. **Runbook drift** — steps that work in UAT and differ in production. Near-certain. Phase A/C.
2. **Alert routing gaps** — alerts that fire into a channel nobody watches at 02:00. Likely. D3.
3. **Reconciliation edge cases** — the uncertain-payment case, partial refunds, timing windows.
   Likely, and the reason D12 exists.
4. **Restore duration exceeding RTO** — restores are always slower than the estimate. Likely. D9.
5. **Secret rotation causing brief connection failures** — common where connection pools cache
   credentials. Likely. D11.
6. **Production performance below the UAT baseline** — different network path, different data
   volume. Possible. D2.

> **A dry run that finds nothing has usually not been run properly.** The plan budgets Phase D for
> disposition precisely because Phases A–C are expected to produce findings. Six of the six items
> above are cheaper to find on 27 December than on 3 January.
