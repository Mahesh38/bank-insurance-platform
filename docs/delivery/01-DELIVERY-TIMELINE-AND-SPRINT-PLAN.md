# 01 — R0 Delivery Timeline and Sprint Plan

**Owner:** Kalpana — Delivery Head (R12). Integrated delivery plan and milestones are `O/A/R`
within delivery jurisdiction; scope remains Rajal's, and every specialist gate remains its owner's.
**Derived from:** [00 — stakeholder brainstorming session](./00-STAKEHOLDER-BRAINSTORMING-SESSION.md)
**Baseline date:** 2026-08-17 · **Go-live:** 2027-01-01

---

## 1. The fixed points

| # | Milestone | Date | Fixed by |
|---|---|---|---|
| M0 | Planning baseline | 2026-08-17 | Today |
| M1 | `GATE-S08` PASSED — engineering foundation | 2026-09-20 | Plan |
| M2 | `GATE-S09` PASSED — platform foundation, environments live | 2026-10-04 | Plan |
| M3 | First end-to-end R0 sale in UAT — **the business case proven** | 2026-11-15 | Plan |
| M4 | `GATE-S11` PASSED — vertical slice complete | 2026-11-15 | Plan |
| M5 | `GATE-S12` PASSED — hardening and certification complete | 2026-12-11 | Plan |
| M6 | `GATE-S14` PASSED + go/no-go decision | 2026-12-11 | Plan |
| M7 | **Production deployment — application production-ready** | **2026-12-16** | **Business constraint** |
| M8 | 15-day production dry run | 2026-12-17 → 2026-12-31 | **Business constraint** |
| M9 | **Go-live — live selling** | **2027-01-01** | **Business constraint** |

**Total build window: 17.3 weeks (2026-08-17 → 2026-12-16).**

---

## 2. The calendar

Sprint length is **2 weeks**, Monday to Sunday, with review/retro on the final Thursday and
planning on the first Monday. Sprint 0 is a **one-week mobilisation**, not a delivery sprint.

| Sprint | Dates | Working days | Holiday impact | Net capacity | Sprint goal |
|---|---|---:|---|---:|---|
| **S0** | Mon 17 Aug – Sun 23 Aug | 5 | — | 5 | **Mobilise and unblock.** Squads formed, backlog in Jira, three `DL0` dependencies chased |
| **S1** | Mon 24 Aug – Sun 6 Sep | 10 | 26 Aug (regional) | **9** | **Pipeline + contracts.** CI green on every PR; API contracts frozen; AWS request in flight |
| **S2** | Mon 7 Sep – Sun 20 Sep | 10 | 14 Sep (Ganesh Chaturthi) | **9** | **`GATE-S08` PASS.** Quality gates, test infrastructure, environments provisioned |
| **S3** | Mon 21 Sep – Sun 4 Oct | 10 | 2 Oct (Gandhi Jayanti) | **9** | **`GATE-S09` PASS.** Observability, secrets, retention live. **S11 build starts** |
| **S4** | Mon 5 Oct – Sun 18 Oct | 10 | — | **10** | **Compliance gates built.** C1–C4 implemented and 100% covered |
| **S5** | Mon 19 Oct – Sun 1 Nov | 10 | 20 Oct (Dussehra) | **9** | **Money path.** Payment, issuance, reconciliation. Usability evidence captured |
| **S6** | Mon 2 Nov – Sun 15 Nov | 10 | 9–10 Nov (Diwali week) | **8** ⚠ | **`GATE-S11` PASS. First end-to-end sale in UAT.** |
| **S7** | Mon 16 Nov – Sun 29 Nov | 10 | 24 Nov (Guru Nanak) | **9** | **Certification execution.** Pentest, load, compliance, functional |
| **S8** | Mon 30 Nov – Sun 13 Dec | 10 | — | **10** | **`GATE-S12` + `GATE-S14` PASS.** Remediation, ORR, DR test, go/no-go |
| **CUT** | Mon 14 Dec – Wed 16 Dec | 3 | — | 3 | **Production cutover.** Deploy, verify, freeze |
| **DRY** | Thu 17 Dec – Thu 31 Dec | 15 cal. | 25 Dec (Christmas) | — | **15-day production dry run** (see [04](./04-DRY-RUN-AND-GO-LIVE-PLAN.md)) |
| **LIVE** | Fri 1 Jan 2027 | — | — | — | **Go-live. Hypercare begins.** |

### 2.1 Capacity warnings that shape the plan

> **Sprint 6 is the Diwali sprint and it is also the sprint carrying `GATE-S11`.** That is the
> worst possible collision in the calendar and it is unavoidable — moving `GATE-S11` later
> compresses certification below viable. Mitigation: Sprint 6 is planned at **80% capacity**, its
> scope is deliberately light (closure and integration, not new build), and the heavy build lands
> in Sprints 4 and 5.

> **Sprint 8 ends 13 December, three days before cutover.** There is **no buffer sprint**. That is
> the direct consequence of a 17-week window against a 29-week sequential plan, and it is the
> largest structural risk in this schedule. The mitigation is not a buffer we do not have — it is
> the descope levers in [05](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md), pulled early rather
> than late.

> **Bank year-end change freeze.** Most scheduled banks operate a change freeze across the last
> week of December. Our dry run sits inside it. **Kalpana action, Sprint 0:** obtain a written
> freeze exemption for the pilot estate, or the dry run becomes a read-only observation window.

---

## 3. Squad model

Eight parallel lanes: **six delivery squads** and **two standing tracks**. This is the
parallelisation engine from
[Kalpana §9](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md#9-parallelization-engine)
applied concretely.

| Squad | Name | Persona lead(s) | Owns | Live from |
|---|---|---|---|---|
| **SQ-1** | Platform & Pipeline | Shivanshi + Amit | S08 CI/quality/security-in-pipeline, S09 IaC, environments, deploy/rollback, secrets, observability, retention | S1 → S8 (**permanent stream**) |
| **SQ-2** | Trust & Compliance Core | Deepali + Shailja + Amit | Consent (#6), Suitability (#7), Audit (#16), attribution, C1–C4 | S3 → S8 |
| **SQ-3** | Journey & Sales | Mahesh + Amit | Journey Orchestration (#9), Lead (#5), Customer (#4), Catalogue (#8), Quotation (#10), Proposal (#11) | S3 → S7 |
| **SQ-4** | Money & Fulfilment | Amit + Aarti | Payment (#12), Policy & Issuance (#13), reconciliation, AU Bank PG, 1SB adapter re-cert | S3 → S7 |
| **SQ-5** | Experience | Design + Amit | RM Workspace BFF (#2), Flutter app (`apps/rm-workspace-app`), design system, usability evidence | S1 → S7 |
| **SQ-6** | Quality & Certification | Swapnali + Deepali | Test infrastructure, in-sprint automation, contract/E2E suites, performance harness, pentest coordination, S12 certification | S1 → S8 |
| **TRK-G** | Governance & Decisions | Kalpana + Rajal + R11 | Gate evidence, registers, decision-forcing, required-by dates, T4 human sign-off scheduling | S0 → LIVE |
| **TRK-E** | External Dependencies | Kalpana | AWS landing zone, 1SB commercials, CBS, AU Bank PG, Bank AD, pentest vendor, pilot cohort | S0 → LIVE |

### 3.1 Why six squads and not three

Because the critical path is not a single thread. Three things must proceed **simultaneously** or
the date is arithmetically impossible:

1. **The foundation** (SQ-1) — nothing deploys without it.
2. **The journey** (SQ-2/3/4/5) — the business case, and the only stage never attempted.
3. **The evidence** (SQ-6) — four weeks of certification is only enough if the suites already exist.

Running these sequentially costs 29 weeks. Running them in parallel against frozen contracts costs
17. The entire plan rests on that substitution, and on the contracts being frozen in Sprint 1.

### 3.2 Where squads are *not* independent

Adding people does not shorten these — they are single-threaded specialist or external bottlenecks
([Kalpana §10](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md#10-capacity-planning)):

- Shailja's compliance review capacity (C1–C10 certification, S12-E03) — one reviewer, 31 stories
  of certification depend on her.
- Deepali's security review and Board 4 verdict.
- Aarti's schema and migration authority across all six new contexts.
- The pentest vendor's calendar.
- Bank infrastructure's landing-zone queue.

**Delivery consequence:** these are scheduled *first* in each sprint, not last, and Kalpana
publishes a required-by date for each per Rule PA-1.

---

## 4. What actually gets built

Per Mahesh's ADR recorded in the session — **R0 needs six thin contexts, not sixteen services.**

### 4.1 Build inventory

| Context | # | R0 form | Squad | Sprints |
|---|---|---|---|---|
| 1SB Adapter | #15 | **Exists** — re-certified against new machinery | SQ-4 | S2, S7 |
| Identity & Access (workforce) | #3 | **Exists, partial** — WS-2 Phase 1 completed | SQ-2 | S2–S3 |
| RM Workspace BFF | #2 | New service — hosts the module contexts below | SQ-5 | S3–S6 |
| Lead Service | #5 | Thin module | SQ-3 | S3–S4 |
| Customer Service | #4 | Thin module (CBS lookup + prefill) | SQ-3 | S4 |
| **Consent Service** | #6 | **Thin service — C2, non-negotiable** | SQ-2 | S3–S4 |
| **Suitability & Recommendation** | #7 | **Thin service — C1, non-negotiable** | SQ-2 | S3–S4 |
| Product Catalogue | #8 | Thin module — R0 matrix only (Life/Group A/Term) | SQ-3 | S4 |
| Journey Orchestration | #9 | Thin service — stage and references only | SQ-3 | S3–S5 |
| Quotation | #10 | Thin module over the existing 1SB path | SQ-3 | S4–S5 |
| Proposal & UW Tracking | #11 | Thin module | SQ-3 | S5 |
| **Payment Service** | #12 | **Thin service — C4 device isolation** | SQ-4 | S4–S5 |
| Policy & Issuance | #13 | Thin module — visibility + reconciliation | SQ-4 | S5–S6 |
| Integration Hub | #14 | Thin routing layer in front of #15 | SQ-4 | S3 |
| **Audit & Compliance** | #16 | **Append-only evidence store, immutable** | SQ-2 | S3–S5 |
| Flutter RM application | — | **Exists** at `apps/rm-workspace-app` — wired to real BFF | SQ-5 | S3–S6 |

**Net new deployables: 5** (Consent, Suitability, Journey Orchestration, Payment, Audit) **plus the
BFF**. Everything else is a module or already exists. That is the difference between a plan that
fits in 17 weeks and one that does not.

### 4.2 Explicitly not built before go-live

Confirmed out by Rajal and already recorded in the WS-3 charter — restated here so the sprint plan
is not read as omitting them by accident: **DIY journey, hybrid journey, Group B insurers, ULIP,
Savings, Health, Motor, Travel, NTB onboarding, V-KYC, customer BFF, customer identity, admin UI,
MIS/reporting beyond pilot funnel, notification beyond OTP and payment link, renewals, servicing,
multi-aggregator routing, vernacular content, branch kiosk, executive control tower.**

Corresponding backlog stages **S13 (Expansion & Scale)** and **S15 (Operate & Evolve)** are out of
this window by design. S15 begins at hypercare on 2027-01-01.

---

## 5. Sprint-by-sprint plan

### Sprint 0 · 17–23 Aug · Mobilise and unblock

**Goal:** every `DL0` dependency has an owner, a required-by date and a first chase. No code
target.

| # | Item | Owner | Required-by |
|---|---|---|---|
| 1 | **AWS landing-zone request submitted** to bank infrastructure | Shivanshi | **21 Aug** |
| 2 | **Consent + suitability rule packs submitted for E2 signature** (`S02-G3`, `S02-G4`) | Rajal → Shailja | **21 Aug** (sign by 28 Aug) |
| 3 | Pentest vendor RFQ issued | Deepali | 21 Aug |
| 4 | Executive sponsor named (`GAP-010`) — unblocks `FRI-001` funding | Rajal → Bancassurance | **28 Aug** |
| 5 | Bank year-end change-freeze exemption requested for the pilot estate | Kalpana | 28 Aug |
| 6 | 1SB: UAT credentials, `distributorId`, IP allowlist confirmed (`GAP-015`) | Rajal + Bancassurance | 28 Aug |
| 7 | CBS and AU Bank PG integration slots requested | Kalpana | 28 Aug |
| 8 | Bank AD technology confirmed (`DEP-010`) | Mahesh | 28 Aug |
| 9 | Pilot cohort named — branches, RM count, customer volume | Rajal + Sales Head | 28 Aug |
| 10 | Squads staffed; `jira-import.csv` loaded; DoR/DoD adopted | Kalpana + Amit | 21 Aug |

**Exit:** all ten have a named owner and a date on the
[dependency register](../governance/registers/DEPENDENCY-REGISTER.md). Items 1, 2 and 3 are `DL0`.

---

### Sprint 1 · 24 Aug – 6 Sep · Pipeline and contracts

**Goal:** every PR builds, tests and scans. Every API contract that downstream work depends on is
frozen.

| Squad | Focus | Epics |
|---|---|---|
| SQ-1 | CI on every module, every PR; branch protection; artefact publishing | `S08-E01` |
| SQ-1 | IaC repo, module standard, remote state, region pinning | `S09-E01` (S01, S02, S06) |
| SQ-6 | Coverage, ArchUnit and static analysis enforced in CI | `S08-E02` |
| SQ-6 | Secret scanning, SAST, SCA, image scanning, SBOM | `S08-E04` |
| SQ-5 | Design system, screen inventory reconciled to Figma (`S05-OPEN-01/02`) | `S05-E03` residual |
| SQ-3 | **Contract-first API definitions for all six thin contexts — frozen 6 Sep** | `S07-E02` residual |
| SQ-2 | NFR numbers closed (`GAP-017`) | `S07-E05` |
| TRK-E | AWS landing zone in flight; pentest procurement; 1SB commercials | — |

**Milestone:** **API contracts frozen (6 Sep).** Every squad from Sprint 2 onward builds against
these rather than against each other.

---

### Sprint 2 · 7–20 Sep · `GATE-S08` and environments

**Goal:** the foundation is provable, and there are three real environments.

| Squad | Focus | Epics |
|---|---|---|
| SQ-1 | Network, compute and data foundation; dev/UAT/prod provisioned; promotion model; isolation | `S09-E01`, `S09-E02` |
| SQ-1 | Automated deploy, progressive delivery, tested rollback, migration in the deploy path | `S09-E03` |
| SQ-1 | Real secrets management (closes `TD-006`), KMS hierarchy, rotation, workload identity | `S09-E04` |
| SQ-6 | **Testcontainers, WireMock, fixtures, contract tests, E2E harness** (closes `TD-014`) | `S08-E03` |
| SQ-6 | PII-free synthetic data generator; performance harness | `S08-E03` (S06, S07) |
| SQ-2 | WS-2 Phase 1 completion — `GATE-IAM-P1` A.1–A.6 | `S10-E05` |
| SQ-5 | BFF skeleton against frozen contracts; Flutter wired to contract mocks | `S11-E06` prep |
| SQ-4 | 1SB adapter re-certified against the new machinery; integration catalogue | `S10-E01`, `S10-E02` |
| SQ-1 | Engineering + secure coding standards; DoD; one-command local env | `S08-E05`, `S08-E06` |

**Gate: `GATE-S08` — target PASS 20 Sep.** All ten criteria S08-G1…G10. Approvers: Amit, Swapnali,
Mahesh, Deepali, Shivanshi.
**Knock-on:** closes `S08-G3`, which unblocks WS-1 gate criterion **4.7**, and the test harness
unblocks **4.1** and **4.6**.

---

### Sprint 3 · 21 Sep – 4 Oct · `GATE-S09` and the slice begins

**Goal:** the platform can be observed, recovered and audited — and the vertical slice starts.

| Squad | Focus | Epics |
|---|---|---|
| SQ-1 | Metrics, logs, traces, dashboards, alerting, **separate audit event pipeline** | `S09-E05` |
| SQ-1 | Encryption at rest/in transit, automated backup, **prove restore**, **7-year S3 Object Lock**, residency attestation | `S09-E06` |
| SQ-1 | Network segmentation, least-privilege IAM, container hardening, edge protection | `S09-E07` |
| SQ-2 | **Consent service** and **Suitability service** — build starts | `S11-E03` (S01, S02, S05) |
| SQ-2 | **Audit & Compliance** append-only evidence store | `S11-E07-S01` |
| SQ-3 | **Journey Orchestration** state machine; **Lead service** | `S11-E01`, `S11-E02` |
| SQ-4 | Integration Hub routing layer; CBS integration | `S10-E03` |
| SQ-5 | Flutter RM auth + first journey screens against real BFF | `S11-E06` (S01, S02) |
| SQ-6 | In-sprint automation for every story delivered; resilience patterns | `S10-E07` |
| TRK-E | **Pentest vendor confirmed — required-by 4 Oct (`DL0`)** | — |

**Gate: `GATE-S09` — target PASS 4 Oct.**
**Entry condition check:** S11 build may only proceed once `S02-G3` and `S02-G4` are **signed**.
This is Rajal condition C5 and is non-waivable.

---

### Sprint 4 · 5–18 Oct · The compliance gates

**Goal:** the four controls that make this business lawful exist in code, at 100% coverage. **This
is the most important sprint in the plan.**

| Squad | Focus | Epics |
|---|---|---|
| SQ-2 | **C1 suitability hard-gate — quote returns 403 without a valid evaluation ID** | `S11-E03-S03` |
| SQ-2 | **C2 consent capture + enforcement — customer-device OTP, append-only evidence** | `S11-E03-S05/S06` |
| SQ-2 | **C3 attribution — `distributorId` server-side, certification expiry → 403** | `S11-E03-S07` |
| SQ-2 | Override handling (R0 = no override, per `SUIT-ALGO-LIFE-v1.0`) | `S11-E03-S04` |
| SQ-3 | Customer service (CBS lookup, prefill, snapshot); product catalogue seed | `S11-E02`, `S11-E08-S01` |
| SQ-3 | Quote generation, disclosed basis, validity enforcement | `S11-E04` (S01–S03) |
| SQ-4 | **C4 payment device isolation**; payment session and link generation | `S11-E05-S01`, `S10-E04` (S01, S02) |
| SQ-5 | Journey screens complete; **customer-device hand-off** | `S11-E06` (S02, S03) |
| SQ-6 | **100% coverage enforced on compliance-gate code**; negative-path suites | `S08-E02-S06`, `S12-E01-S02` prep |

**Milestone:** **C1–C4 implemented and 100% covered (18 Oct).** Swapnali's `Q0` hold criteria are
satisfiable from this point.

---

### Sprint 5 · 19 Oct – 1 Nov · The money path

**Goal:** money moves correctly, a policy issues, and it reconciles. Plus the usability evidence
the pilot cannot launch without.

| Squad | Focus | Epics |
|---|---|---|
| SQ-4 | Payment status, uncertain-payment handling, refund and failure paths | `S10-E04` (S03–S05), `S11-E05-S02` |
| SQ-4 | **Issuance trigger and confirmation; reconciliation; `Sold` determination; policy document** | `S11-E05` (S03–S06) |
| SQ-3 | Proposal capture, underwriting outcomes, document upload | `S11-E04` (S04–S06) |
| SQ-3 | Journey compensations; save and resume | `S11-E01` (S02, S03) |
| SQ-2 | Audit event per regulated action; journey reconstruction proof | `S11-E07` (S01, S02) |
| SQ-5 | Error, empty, degraded states; offline/poor-connectivity behaviour | `S11-E06` (S04, S05) |
| SQ-5 | **Usability tests: ≥5 practising RMs, ≥5 customers on device hand-off; accessibility audit** | `S05-E06` (`S05-OPEN-05/06/07/08`) |
| SQ-4 | SMS for OTP and payment links; email for documents | `S10-E06` |
| SQ-6 | **Internal pre-pentest security assessment**; load-test scenarios authored | `S12-E02` prep |

**Milestone:** **Usability evidence captured (24 Oct)** — aligns with the dates already recorded in
[S05 evidence](../application-lifecycle-bible/evidence/S05-experience-evidence.md). `S05-G6`
closable.

---

### Sprint 6 · 2–15 Nov · `GATE-S11` — the business case, proven ⚠ Diwali

**Goal:** one RM sells one Term policy to one ETB customer, end to end, in UAT. Planned at **80%
capacity**; scope is closure and integration, not new build.

| Squad | Focus | Epics |
|---|---|---|
| SQ-2 | Journey KPI instrumentation; journey dashboard | `S11-E07` (S03, S04) |
| SQ-2 | Consent and disclosure version publication | `S11-E08-S02` |
| SQ-3 | Workforce and partner scope administration; safe integration config activation | `S11-E08` (S03, S04) |
| SQ-3 | **Insurer representative collaboration** — partner-visible cases, UW requirements | `S11-E09` |
| SQ-4 | **Operations and lifecycle control** — exception queue, reconciliation mismatches, escalation, post-issuance tracking | `S11-E10` |
| SQ-5 | **Management oversight** — branch/regional/sales/business-head views, role separation | `S11-E11` |
| SQ-6 | **End-to-end journey suite green in UAT**; regression suite built | `S12-E01-S04` |
| SQ-1 | Deployment observability; UAT hardened to production parity | `S09-E03-S06` |

**Gate: `GATE-S11` — target PASS 15 Nov.**
**Milestone M3: first complete R0 sale in UAT.** This is the milestone the programme has never
reached, and it is the one that makes the business case real.

---

### Sprint 7 · 16–29 Nov · Certification execution

**Goal:** run the evidence. Because SQ-6 wrote suites in-sprint since Sprint 2, this sprint
**executes** rather than writes.

| Squad | Focus | Epics |
|---|---|---|
| SQ-6 | Full functional suite; negative/boundary/exception; data-variant suite; traceability matrix | `S12-E01` |
| SQ-6 + Deepali | **Independent penetration test executes** against feature-complete UAT | `S12-E02-S01` |
| SQ-2 + Deepali | Authorization negative testing; secrets/key handling verified in a running system; trust boundaries as built; threat model refresh | `S12-E02` (S03–S06) |
| Shailja | **C1–C10 certified in the running system**; audit schema review; consent/suitability evidence verified end to end; retention, immutability and residency verified live | `S12-E03` |
| SQ-1 + SQ-6 | **Load at projected peak** (incl. Q4 tax-season multiplier); soak; failure injection; resilience under load; performance baseline | `S12-E04` |
| SQ-1 | Operational runbook written **and exercised**; alerting verified; rollback verified; failure-mode analysis | `S12-E05` |
| SQ-4 | API contracts published; consumer integration supported | `S12-E06` |
| SQ-1 | **Production provisioned from the same IaC as UAT**; credentials, data protection, capacity, access control | `S14-E01` |

**Note:** WS-1 gate criteria **4.1, 4.3, 4.4, 4.5, 4.6, 4.7** all close inside this sprint as a
by-product — they were blocked on machinery that now exists.

---

### Sprint 8 · 30 Nov – 13 Dec · Production readiness and go/no-go

**Goal:** remediate, review, and earn the signatures.

| Squad | Focus | Epics |
|---|---|---|
| SQ-6 | Defect triage and closure; regression re-run; **remediation of pentest findings to SLA** | `S12-E01-S06`, `S12-E02-S02` |
| SQ-1 | Production dashboards, alerting, end-to-end traceability verified in production | `S14-E02` (S03–S05) |
| SQ-1 + Rajal | **SLIs/SLOs published; error-budget policy agreed** | `S14-E02` (S01, S02) |
| SQ-1 | **ORR against O1–O13**; on-call established; runbooks verified against production; **incident simulation**; support model | `S14-E03` |
| SQ-1 + Aarti | **DR configuration; DR test executed; production-grade restore test; DR decision rehearsed** | `S14-E04` |
| Kalpana + Rajal | Go-live checklist; launch approach; rollback and contingency; **business readiness**; hypercare plan | `S14-E05` (S01–S05) |
| Shailja | **Regulatory evidence pack assembled** | `S12-E03-S06` |
| TRK-G | **Mandatory human T4 sign-offs scheduled and obtained** — Architecture, Security, Risk & Compliance | `S14-E05-S06` |

**Gates: `GATE-S12` and `GATE-S14` — target PASS 11 Dec.**
**Milestone M6: go/no-go decision, 11 December.**

> **T4 human sign-offs must be diarised in Sprint 0, not requested in Sprint 8.** Architecture,
> Security and Risk & Compliance signatures are human, non-delegable, and cannot be satisfied by
> any AI persona. Senior bank stakeholders in mid-December have calendars that are already full.
> Kalpana books these slots in **Sprint 0** for **9–11 December**.

---

### Cutover · 14–16 Dec · Production deployment

| Day | Activity | Owner |
|---|---|---|
| **Mon 14 Dec** | Code freeze. Final release candidate built, signed, scanned. Production data protection re-verified | Amit + Deepali |
| **Tue 15 Dec** | Production deployment rehearsal on a production-identical stack; rollback rehearsed | Shivanshi |
| **Wed 16 Dec** | **Production deployment.** Smoke, health, connectivity, residency and access-control verification. Deployment frozen | Shivanshi + Amit |

**Milestone M7: production-ready application deployed, 16 December.**

---

### Dry run · 17–31 Dec · 15 days

Full plan in [04 — dry run and go-live plan](./04-DRY-RUN-AND-GO-LIVE-PLAN.md). Four phases:
**A** synthetic verification (17–19) · **B** controlled live pilot (20–24) ·
**C** destructive rehearsal — DR, restore, rollback, incident, secret rotation (25–28) ·
**D** freeze, final evidence and sign-off (29–31).

---

### Go-live · 1 Jan 2027

Selling opens to the pilot cohort. Hypercare begins. **S15 — Operate, Evolve & Continuous
Assurance** starts here.

---

## 6. Gate schedule

| Gate | Workstream | Target | Approvers | Currently |
|---|---|---|---|---|
| `S02-G3` / `S02-G4` | WS-3 | **28 Aug** | Shailja (E2, human) | Content-complete, signature pending |
| `GATE-IAM-P1` | WS-2 | 20 Sep | Architect, Security, Compliance, QA | 6 criteria OPEN |
| **`GATE-S08`** | WS-3 | **20 Sep** | Amit, Swapnali, Mahesh, Deepali, Shivanshi | 10 criteria OPEN |
| **`GATE-S09`** | WS-3 | **4 Oct** | Shivanshi, Deepali, Aarti, Mahesh | Not opened |
| `GATE-P4` | WS-1 | 29 Nov | Architect, PO, QA, Compliance, Shivanshi | BLOCKED — clears via S08/S09 |
| `GATE-S10` | WS-3 | 15 Nov | Mahesh, Amit, Deepali | Not opened |
| **`GATE-S11`** | WS-3 | **15 Nov** | Rajal, Amit, Swapnali, Shailja, Mahesh | Not opened. **Entry blocked on `S02-G3/G4`** |
| **`GATE-S12`** | WS-3 | **11 Dec** | Swapnali, Deepali, Shailja | Not opened |
| **`GATE-S14`** | WS-3 | **11 Dec** | Shivanshi, Kalpana + **human T4 sign-offs** | Not opened |

> Kalpana marks a gate `CANDIDATE` when evidence is assembled. **`CANDIDATE` is readiness for
> decision, not stage-transition approval** (Rule 9, §16). Every PASS above belongs to its listed
> approvers.

---

## 7. Cadence

| Cadence | When | Who | Decides |
|---|---|---|---|
| Daily stand-up (per squad) | Daily 09:30 | Squad | Impediments |
| **Cross-squad sync** | Daily 10:15, 15 min | Squad leads + Kalpana | Cross-squad blockers only |
| **Dependency chase** | Mon + Thu | Kalpana + TRK-E | Required-by dates, `OVERDUE` marking |
| Sprint planning | Day 1 Monday | All | Sprint commitment |
| Backlog refinement | Wednesday week 1 | Rajal, R11, squad leads | DoR compliance |
| **Gate evidence review** | Thursday week 2 | Kalpana + gate owners | `CANDIDATE` status |
| Sprint review + retro | Thursday week 2 | All + stakeholders | Increment acceptance |
| **Steering** | Bi-weekly, Friday week 2 | Sponsor, Rajal, Mahesh, Kalpana | Scope, funding, descope levers |
| **Risk and decision review** | Friday week 2 | Kalpana + register owners | Register hygiene, escalation |

---

## 8. What this plan does not claim

Stated plainly, because a forecast that hides its assumptions is not a forecast:

1. It does **not** claim 17 weeks is a comfortable window for 29 weeks of sequential work. It
   claims parallelisation makes it *possible*, at **72% confidence** for a narrowed pilot width.
2. It does **not** claim the AWS landing zone will arrive on time. That is an external queue.
   **If it slips past 5 October, no engineering response recovers the date.**
3. It does **not** carry a buffer sprint. There is none available.
4. It does **not** substitute any AI persona for the mandatory human T4 sign-offs.
5. It does **not** assume the pentest finds nothing. Sprint 8 is remediation, and if findings are
   severe, [descope lever L4](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md) is pulled.
