# 00 — R0 Go-Live Planning Session: every stakeholder, on the record

**Session:** Integrated delivery planning for a fixed external date
**Convened by:** Kalpana — Principal Insurance Platform Delivery Head (R12), under
[Rule PA-1](../governance/PERSONA-AUTHORITY-MATRIX.md#kalpana--r12-decision-forcing-authority)
(R12 may compel a decision to happen; R12 may never supply its content)
**Date:** 2026-08-17
**Constraint given:** Go-live **2027-01-01**. Production-ready application **deployed by
2026-12-16**. A **15-day dry run** on production between those two dates.
**Status:** Working session record. Every position below belongs to its named authority. Nothing
here is a gate approval, a stage transition, or a substitute for a mandatory human signature.

---

## 1. Who was in the room

Personas are taken from the canonical repository set. Human organisational roles are taken from
the [stakeholder catalogue](../au-bank-insurance-platform/knowledge-base/06-stakeholders.md); where
a real human signature is required, that is stated rather than simulated.

### 1.1 Repository decision authorities (the nine + delivery)

| # | Persona | Canonical identity | Governing question they answered |
|---|---|---|---|
| 1 | **Rajal** | Principal Insurance Platform Product Owner (R1 / Board 3) | What is R0, and what may be cut? |
| 2 | **R11 / Principal BA** | Lead Bancassurance Business Analyst | Is intent expressed as testable, traceable behaviour? |
| 3 | **Mahesh** | Principal Insurance Platform Architect (R2 / Board 1) | What is the correct build order? |
| 4 | **Amit** | Technical Head / Engineering (R3) | Can it be implemented in the window? |
| 5 | **Deepali** | Principal Security Architect (Board 4) | What security outcome is non-negotiable? |
| 6 | **Aarti** | Principal Insurance Data & Database Architect | Will the data survive and be recoverable? |
| 7 | **Swapnali** | Principal Quality Engineering / QA Lead (Board 5) | What evidence must exist before release? |
| 8 | **Shailja S** | Compliance & Risk Head (Board 6) | Is it lawful to sell on 1 January? |
| 9 | **Shivanshi** | Principal Platform SRE / Reliability Head (R10 / Board 7) | Can it be run, observed and recovered? |
| 10 | **Kalpana** | Delivery Head / Delivery Lead (R12) | What is the truthful forecast? |

### 1.2 Business and sponsor lens

| Lens | Source | Contribution |
|---|---|---|
| **Executive Sponsor perspective (Dilip lens)** | [sponsor perspective package](../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/README.md) | Business value of the date, budget, pilot success measures. **AI perspective — not a sponsor signature.** |

### 1.3 Human stakeholder groups represented by proxy

From [Volume 04 / stakeholder catalogue](../au-bank-insurance-platform/knowledge-base/06-stakeholders.md).
These are **real organisational roles**. They are represented in this session by the persona who
holds the matching jurisdiction; they are **not** simulated as approvers.

| Group | Roles | Represented by | Signature still required from a human? |
|---|---|---|---|
| Executive & governance | Business Sponsor, Programme Sponsor, Steering, EA Review Board | Rajal + Mahesh + Dilip lens | **Yes** — GAP-010 open |
| Compliance, Infosec, Legal, Audit | — | Shailja + Deepali | **Yes** — T4 sign-offs |
| Sales & distribution | RM, Branch Manager, Regional Manager, Sales Head, Insurance Business Head | Rajal (S11-E11 actors) | Pilot cohort commitment required |
| Customers | ETB customer, nominee | Rajal + Design | Usability evidence required (S05-G6) |
| Operations | Ops executive, back office, policy servicing, customer support | Shivanshi + Rajal (S11-E10) | Support model sign-off required |
| Insurance partners | Partner insurer, underwriting, insurer ops, partner RM | Rajal + Amit (S11-E09) | Group A panel confirmation required |
| Platform administration | PO, BA, platform admin, config admin, reporting admin | Rajal + R11 (S11-E08) | — |
| Technology & delivery | Technology, DevOps, integration, data, QA | Amit, Shivanshi, Aarti, Swapnali | — |
| External systems | 1SilverBullet, AU Bank PG, CBS, Bank AD, notification | Kalpana (external dependency track) | **Yes** — commercial and technical |

---

## 2. The opening statement of the problem

Kalpana opened by putting three repository facts next to the requested date, because a plan that
does not start from the true position is a wish.

**Fact 1 — where we actually are.** The
[position assessment](../application-lifecycle-bible/01-POSITION-ASSESSMENT.md) records: of 19
bounded contexts, **2.5 exist**. S08 Engineering Foundation and S09 Platform Foundation are
**missing**. S11 — the vertical slice that proves the business case — **has never been attempted**
at platform level. `GATE-S08` has **10 of 10 criteria OPEN**.

**Fact 2 — what the date demands.** Go-live 2027-01-01 with a 15-day production dry run means the
production system must be **deployed, certified and frozen by 2026-12-16**. That is **17.3 weeks**
from today.

**Fact 3 — what the plan of record says it takes.** The
[realignment plan](../application-lifecycle-bible/03-REALIGNMENT-PLAN.md) budgets **8–10 weeks for
S08 + S09 alone**, and then S10 → S11 → S12 → S14 in sequence.

> **Kalpana's opening position.** Executed as written, the sequence is roughly **29 weeks**. We
> have **17.3**. The date is therefore not achievable by sequencing. It is achievable **only** by
> aggressive, legitimate parallelisation — and only if a named set of external dependencies land
> on named dates. My job in this session is to produce the parallelised plan, name those
> dependencies, and state a confidence that is true rather than comfortable.
>
> Per [§7 of my planning model](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md#7-timeline-estimation):
> **a requested date is a constraint and an input, not proof of feasibility.**

---

## 3. Round one — each authority states its position and its immovable constraint

### 3.1 Rajal — Product (R1, Board 3)

**Position.** R0 is already correctly and narrowly scoped, and I will not let it widen to meet a
date. `R0-ASSISTED-TERM-SALE` is: *one RM sells one Term Life policy to one ETB customer from one
Group A insurer, end to end, with consent and suitability evidence, payment on the customer's own
device, an issued and reconciled policy, and a complete audit trail.*

**What I bring to the date.** The scope is already the thin slice. DIY, hybrid, Group B, ULIP,
Savings, Health, Motor, NTB, renewals, the customer BFF, admin UI, MIS and the control tower are
**already out** with named revisit triggers. There is nothing left to cut from the *journey*.

**My immovable constraint.** `Sold` means **issued + confirmed + reconciled + persisted** — all
four. I will not accept a go-live where "sold" is inferred from payment. That definition is
[D-007](../au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md) and it stands.

**What I can move.** Not the journey — the **population**. My descope levers are pilot *width*,
not journey *depth*: number of branches, number of RMs, number of insurer products in the
catalogue, and whether Operations gets a console or a runbook on day one. See
[05 — descope levers](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md).

**What I need, by when.** The named executive sponsor (**GAP-010**, open, P0) by **2026-08-28**.
It blocks `FRI-001` funding approval, and an unfunded increment is not a plan.

---

### 3.2 R11 — Principal Business Analyst

**Position.** The requirement base is in better shape than the code base, and that asymmetry is
our one advantage in a compressed window. [S03 evidence](../application-lifecycle-bible/evidence/S03-requirements-evidence.md)
carries 60 Given/When/Then criteria and 12 exception criteria. The consent pack has **38 testable
rules**; the suitability pack has **48**, with `SUIT-ALGO-LIFE-v1.0` deterministic and no override
in R0.

**What this buys the plan.** Because acceptance criteria exist *before* the code, QA automation,
security review and the Flutter UI can all start from the contract rather than from a running
service. That is the single largest parallelisation opportunity available to us, and it exists
only because S03 was done properly.

**My immovable constraint.** Three open analysis items still gate build:
`S03-OPEN-03` (where `agentId` and SP certification expiry are sourced from — depends on WS-2),
`S03-OPEN-05` (Aarti's physical mapping), `S03-OPEN-07` (business stakeholder acceptance).
The first is on the critical path for **C3 attribution** and cannot be assumed.

**What I need, by when.** WS-2 confirms the certification-metadata source by **Sprint 2 close
(2026-09-20)**, or C3 is built against a stub and re-worked later at a cost I will record now.

---

### 3.3 Mahesh — Architecture (R2, Board 1)

**Position.** The build order is the whole game, and it is already written:

> *Foundation → one journey through few services → then breadth.* The nineteen contexts are a
> **target architecture, not a build order.**
> — [Realignment plan §4](../application-lifecycle-bible/03-REALIGNMENT-PLAN.md#4-sequencing-constraint-that-must-not-be-violated)

**My ruling on scope of build.** R0 does **not** need sixteen new services. It needs **six thinly
implemented**, plus the two that exist:

| # | Context | State | R0 treatment |
|---|---|---|---|
| #15 | 1SB Adapter | **Exists** (147 files) | Re-certify, do not rebuild |
| #3 | Identity & Access (workforce) | **Exists**, partial | Complete WS-2 Phase 1 |
| #5 | Lead Service | Absent | Build thin |
| #6 | Consent Service | Absent | Build thin — **non-negotiable, C2** |
| #7 | Suitability & Recommendation | Absent | Build thin — **non-negotiable, C1** |
| #9 | Journey Orchestration | Absent | Build thin — holds stage + references only |
| #12 | Payment Service | Absent | Build thin — **C4 device isolation** |
| #16 | Audit & Compliance | Absent | Build thin — append-only evidence |
| #4, #8, #10, #11, #13, #14 | Customer, Catalogue, Quotation, Proposal, Policy, Integration Hub | Partial/absent | Implement as **modules behind the RM Workspace BFF**, not as separate deployables, until S13 justifies splitting |

That last row is my material decision for this window, and I am recording it as an ADR: **R0
collapses six contexts into modules behind one BFF and the existing adapter.** Bounded contexts
are a *logical* boundary; deployable-per-context is a *physical* choice, and eight new deployables
in seventeen weeks would consume the window in operational overhead alone. The boundaries stay
enforced by ArchUnit at package level, so the split remains cheap later.

**My immovable constraint.** The standing constraints do not bend for the date. Specifically: no
platform service calls a provider adapter directly (traffic routes through the Integration Hub);
Journey Orchestration holds stage and references only, never another context's business decision;
and the Flutter client never calls 1SB or a database directly.

**What I need, by when.** Contract-first API definitions frozen by **end of Sprint 1
(2026-09-06)**. Everything parallel downstream of that depends on the contract, not on the code.

---

### 3.4 Amit — Engineering (R3)

**Position.** I can build the six thin contexts. What I cannot do is build them *and* retrofit the
foundation *and* absorb the retrofit penalty on 20,200 lines of existing Java, sequentially.

**The honest engineering number.** The realignment plan's "8–10 weeks for S08+S09" assumes S08 and
S09 are a *project*. In this window they have to be a *stream* — a permanently staffed platform
squad running from Sprint 1 to Sprint 8, not a phase we exit. I am comfortable with that provided
nobody treats S08 as "done" and reassigns the squad in October.

**What is already banked.** Since the position assessment, three CI workflows exist
(`application-ci.yml`, `security-scanning.yml`, `governance.yml`), all ten SCA findings are
cleared, ArchUnit is enforced, and the **Flutter RM workspace application now exists** at
`apps/rm-workspace-app` with suitability-gate, consent-gate, payment-device-isolation and
PII-redaction tests. GAP-A is materially closed; the position assessment's "no Flutter app" line
is a superseded baseline.

**My immovable constraint.** No feature merges into `services/` outside recovery scope until
`GATE-S08` passes. Every line added before the gate is another line entering the estate untested,
and this window has no room for that debt.

**What I need, by when.** The AWS landing zone. Not a promise of one — an account, a VPC and
Terraform state, by **end of Sprint 2 (2026-09-20)**. Everything in S09 is downstream of it.

---

### 3.5 Deepali — Security (Board 4)

**Position.** I hold `B` block authority and I will state now, in advance, exactly what I will
block on, so nobody discovers it in December.

**Non-bypassable at go-live (S0):**

1. No secret in code, image or config; real secrets management, not the stub (`TD-006`).
2. KMS key hierarchy with CMK ownership, and encryption at rest everywhere.
3. Trust boundaries as **built**, verified — not as designed.
4. Default-deny authorization proven by negative test.
5. No PII in logs, proven by an automated test that scans emitted logs (`S08-G7`).
6. **An independent penetration test, with findings remediated to SLA.**

**My immovable constraint, and the one that will hurt.** Item 6 has a **procurement and scheduling
lead time measured in weeks, not days**. A pentest booked in November is a pentest report in
January. It must be **commissioned in Sprint 3 (by 2026-10-04)** and **executed in Sprint 7**, with
Sprint 8 reserved for remediation. If we book it late, I will block go-live, and I will be right to.

**What I will not do.** I will not accept a "pentest after go-live" waiver on a regulated
financial application handling PAN, Aadhaar and health data. That is not a lower-severity eligible
exception; it is the control itself.

**What I need, by when.** Pentest vendor engaged and slot confirmed by **2026-10-04**.

---

### 3.6 Aarti — Database & Data Architecture

**Position.** Two things in this plan are mine and neither is negotiable by schedule pressure.

**First: restore, not backup.** Backup is a configuration. **Restore is a fact.** `S09-E06-S04`
("Prove restore") and `S14-E04-S03` ("Execute a production-grade restore test") are separate
stories on purpose. I will sign neither on the basis of the other.

**Second: 7-year immutable retention.** IRDAI retention is a statutory obligation with, today,
**no technical control behind it**. S3 Object Lock in `ap-south-1`, configured and evidenced. This
is `S09-E06-S05`, and it cannot be a December discovery.

**My immovable constraint.** The migration path into production must be **the same path exercised
in UAT**, and it must be reversible. `S09-E03-S04` (database migration in the deployment path) is
built in Sprint 2, not improvised at cutover.

**What I need, by when.** Physical schema mapping for the six new thin contexts — `S03-OPEN-05` —
closed by **end of Sprint 2**, so schema and migration are written once rather than twice.

---

### 3.7 Swapnali — Quality Engineering (Board 5)

**Position.** "Hardened" is a claim about evidence, and the evidence infrastructure does not exist
yet. That is `GAP-E`, and it is mine.

**What compression does to quality.** In a compressed window the pressure lands on certification,
because certification is at the end. I am refusing that in advance by moving evidence generation
*forward*: test automation is written from acceptance criteria **in the same sprint as the
story**, not in a certification sprint at the end. S12 then becomes *execution and defect
burn-down* of suites that already exist — four weeks of running, not four weeks of writing.

**My immovable constraint — the non-bypassable gates.** Four controls get **100% coverage, no
waiver, no interim floor**:

| Control | Behaviour | Story |
|---|---|---|
| **C1 Suitability hard-gate** | Quote API returns 403 without a valid, unexpired suitability evaluation ID | `S11-E03-S03` |
| **C2 Consent** | No proposal submitted without an unexpired consent grant, captured via customer-device OTP | `S11-E03-S05/S06` |
| **C3 Attribution** | `distributorId` injected server-side, never caller-supplied | `S11-E03-S07` |
| **C4 Payment device isolation** | Payment executes only on the customer's device | `S11-E05-S01` |

`S08-E02-S06` ("100% coverage on compliance-gate code") is how that is enforced in CI. I will hold
`Q0` on any release candidate where those four are below 100%.

**What I need, by when.** Test infrastructure — Testcontainers, WireMock, contract tests, E2E
harness — operational by **end of Sprint 2**. Without it, every sprint after that produces code
faster than it produces evidence, and the gap compounds to December.

---

### 3.8 Shailja S — Compliance & Risk (Board 6)

**Position.** I will answer the only question that matters on 1 January: **is it lawful to sell?**

**The two P0 gaps are content-complete and signature-pending.** `GAP-006` (consent) and `GAP-007`
(suitability) both read **CONTENT-COMPLETE, RATIFICATION-PENDING**. The rule packs exist and are
testable. What is missing is **my E2 signature** against `S02-G3` and `S02-G4`.

**Why this is the single most schedule-critical item in the room.** Per the WS-3 entry condition
(Rajal condition C5, non-waivable): *no WS-3 stage enters S11 while GAP-006 or GAP-007 is open.*
S11 is 50 stories and the entire business case. **My signature is the gate on the largest stage in
the plan**, and it is a signature, not a build.

**My commitment to this session.** I will complete the review and sign or return
`CHANGES_REQUIRED` on both packs by **2026-08-28 — inside Sprint 0**. I am treating this as
`DL0`. It is not acceptable for a 50-story stage to wait on a document review.

**My immovable constraints for go-live.**

1. All ten controls C1–C10 certified in the **running** system (`S12-E03-S01`), not in design.
2. Data residency verified in the running system — regulated data, backups, logs and archives
   inside AWS India regions (`S12-E03-S05`). Render.com is dev-preview only and is never a data
   path for PII.
3. The regulatory evidence pack assembled (`S12-E03-S06`) before go-live, not after.
4. Consent, suitability and audit evidence **immutable and non-deletable**.

**What I will block on.** Selling a regulated product on 1 January without C1–C10 certified. I say
this now so that it is a plan input rather than a December surprise.

---

### 3.9 Shivanshi — SRE & Operations (R10, Board 7)

**Position.** I own the question "can it be run", and my answer today is no, because there is no
production to run it in. There is a `render.yaml` starter-plan service. That is the whole estate.

**The lead time nobody has costed.** An AWS landing zone inside a bank is not `terraform apply`.
It is account vending, network peering to bank infrastructure, infosec review of the VPC design,
CIDR allocation, egress policy, and CMK ownership sign-off. In a scheduled bank that is **4 to 8
weeks of calendar**, most of it waiting rather than working. If we start it in Sprint 1, we have
production in Sprint 4. **If we start it in Sprint 3, we do not have production in December.**

> **This is the number one threat to the date, and it is an external dependency with a queue,
> not an engineering task with an estimate.**

**My immovable constraints.**

1. **Rollback exercised, not designed** (`S12-E05-S04`, `S09-E03-S03`). A rollback that has never
   been run is a hope.
2. **ORR against O1–O13** (`S14-E03-S01`) before go-live approval.
3. **On-call established with a real rota** before 1 January, not after the first incident.
4. **A DR test executed** (`S14-E04-S02`), not a DR configuration written.
5. Progressive delivery, so the pilot cohort can be widened and narrowed without a redeploy.

**On the 15-day dry run — my strongest contribution to this plan.** Treat it as an **operational
rehearsal**, not a soak test. Fifteen days of production uptime proves almost nothing on its own.
Fifteen days in which we deliberately **fail over, roll back, rotate a secret, restore a database,
page an on-call engineer at 02:00, and run the incident simulation** proves the thing we actually
need to know. My dry-run design is in
[04 — dry run and go-live plan](./04-DRY-RUN-AND-GO-LIVE-PLAN.md).

**What I need, by when.** AWS account and landing-zone request **submitted in Sprint 0, this
week** — 2026-08-21 at the latest.

---

### 3.10 Dilip lens — Executive Sponsor perspective

> Recorded as **AI Executive Sponsor Perspective**. Not a decision by the named individual, and
> not a substitute for the sponsor signature that GAP-010 still requires.

**Position.** The date has a business reason and it should be stated, because a date without a
reason gets traded away in the first difficult week. **1 January is the start of the Q4 tax
season**, which is the single largest Term Life buying window in the Indian retail market. A
platform that goes live on 1 February misses the season and waits a year for the next one. That is
the value of the date, and it is why it is worth compressing for.

**What I would ask of this plan.** Do not buy the date with the pilot's credibility. A go-live
on 1 January that produces **twenty real, clean, reconciled, audited sales** is worth more than one
that produces two hundred with a reconciliation backlog. The pilot exists to prove the business
case, and a messy pilot disproves it just as effectively as a late one.

**My recommendation on scope.** Take Rajal's descope levers on pilot **width** every time before
touching journey **depth** or any control. A narrow, correct, auditable pilot in January expands in
February. A broad, uncertified one gets stopped by the regulator and expands never.

---

## 4. Round two — the conflicts, resolved in the room

Four genuine conflicts surfaced. Each is resolved here per
[§16 conflict rules](../governance/PERSONA-AUTHORITY-MATRIX.md#16-conflict-and-escalation-rules),
with the resolving authority named.

### Conflict 1 — Kalpana vs Amit: is the S08/S09 estimate a phase or a stream?

- **Kalpana:** 8–10 weeks of S08+S09 before S11 starts consumes 58% of the window and leaves S11
  four weeks. That is not survivable.
- **Amit:** the estimate assumes it is done as a phase, with the team exiting at the end.

**Resolution (Kalpana, within delivery jurisdiction — sequencing, not scope).** S08 and S09 become
a **permanently staffed platform stream (SQ-1)** running Sprint 1 → Sprint 8, with **gate-critical
subsets front-loaded**: `GATE-S08` targets Sprint 2 close, `GATE-S09` targets Sprint 3 close, and
the remaining S09 stories continue as a stream behind those gates. S11 starts in Sprint 3, in
parallel, against contracts. *This is a sequencing decision and sits inside R12's authority. It
changes no scope and waives no criterion.*

### Conflict 2 — Deepali vs the calendar: pentest lead time

- **Deepali:** pentest is non-bypassable, and its lead time is external.
- **Kalpana:** a pentest in Sprint 7 leaves one sprint for remediation.

**Resolution (Deepali retains the control; Kalpana forces the date).** The control is not
weakened. What changes is *when it is commissioned*: **procurement starts Sprint 1, vendor
confirmed by 2026-10-04 (`DL0`)**, test executes Sprint 7 against a feature-complete UAT, Sprint 8
is remediation. Additionally, a **pre-pentest internal security assessment in Sprint 5** surfaces
the obvious findings early, so the external test is not spent on them. *Deepali's S0 list is
unchanged.*

### Conflict 3 — Rajal vs Swapnali: scope of the pilot vs depth of evidence

- **Rajal:** pilot width is my lever; I need enough branches to prove the case.
- **Swapnali:** every additional branch, product and insurer multiplies the data-variant suite
  (`S12-E01-S03`), and the suite has to run inside the window.

**Resolution (both jurisdictions preserved).** R0 launches with **one Group A insurer, one Term
product, one branch cohort**. Width expands **during the pilot under a feature flag**, not before
go-live — which means the pre-go-live certification surface is fixed and small, and Rajal's
expansion does not require a release. *Product keeps the lever; QA keeps a bounded suite.*

### Conflict 4 — Shivanshi vs Aarti: what the 15 days are actually for

- **Shivanshi:** operational rehearsal — failover, rollback, paging.
- **Aarti:** production-grade restore, which needs quiet and a real dataset.

**Resolution (complementary, not conflicting — Step 2 of the deadlock protocol: both positions
are simultaneously satisfiable).** The 15 days are **structured into four phases**, with the
destructive exercises (restore, failover, rollback) placed in **Phase C**, deliberately *before*
the final freeze, so a failure there still has days of runway. Detail in
[04](./04-DRY-RUN-AND-GO-LIVE-PLAN.md#3-the-fifteen-day-dry-run-day-by-day).

---

## 5. Round three — what everyone agreed

### 5.1 Agreed shape of the plan

| Decision | Agreed by | Recorded as |
|---|---|---|
| **8 sprints of 2 weeks**, plus Sprint 0 mobilisation, plus a 3-day cutover window | All | [01 §2](./01-DELIVERY-TIMELINE-AND-SPRINT-PLAN.md#2-the-calendar) |
| **Six delivery squads + two standing tracks**, not one sequential queue | Kalpana, Amit, Shivanshi | [01 §3](./01-DELIVERY-TIMELINE-AND-SPRINT-PLAN.md#3-squad-model) |
| **Contract-first**: API contracts frozen Sprint 1; UI, tests and security review proceed against contracts | Mahesh, Amit, Swapnali, Deepali | [03 §2](./03-DEPENDENCY-AND-PARALLELISATION-MAP.md) |
| **Six thin contexts as BFF modules**, not eight new deployables | Mahesh (ADR) | [01 §4](./01-DELIVERY-TIMELINE-AND-SPRINT-PLAN.md#4-what-actually-gets-built) |
| **Evidence generated in-sprint**, so S12 executes rather than writes | Swapnali | [02](./02-SPRINT-BACKLOG-ALLOCATION.md) |
| **Descope on pilot width, never on journey depth or a control** | Rajal, Shailja, Deepali, Dilip lens | [05 §4](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md) |
| **15 days = operational rehearsal in four phases**, not a soak | Shivanshi, Aarti, Swapnali | [04](./04-DRY-RUN-AND-GO-LIVE-PLAN.md) |

### 5.2 Agreed non-negotiables — the list that does not move for the date

Every persona was asked to name what they would block on. Consolidated:

1. C1 suitability hard-gate, C2 consent, C3 attribution, C4 payment device isolation — **100%
   covered, in the running system** (Swapnali, Shailja, Deepali).
2. Controls C1–C10 certified in the running system before go-live (Shailja).
3. Independent penetration test executed and findings remediated to SLA (Deepali).
4. No PII in logs, proven by automated test (Deepali, Swapnali).
5. Data residency — all regulated data, backups, logs and archives in AWS India regions (Shailja).
6. 7-year immutable retention implemented, not planned (Aarti, Shailja).
7. Restore proven and a DR test executed (Aarti, Shivanshi).
8. Rollback exercised under realistic conditions (Shivanshi).
9. ORR passed against O1–O13; on-call rota live (Shivanshi).
10. `Sold` = issued + confirmed + reconciled + persisted (Rajal).
11. Mandatory human T4 sign-offs — Architecture, Security, Risk & Compliance — **actually signed
    by humans**. No AI persona satisfies these.

> **Rule agreed in the room:** if the date is at risk, we cut **scope width**, we do not cut this
> list. Any proposal to cut from this list is an escalation to the accountable human risk owner,
> not a delivery decision.

---

## 6. Kalpana's closing forecast

Stated per [§8 of the planning model](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md#8-forecast-confidence),
with the assumptions named rather than hidden.

| Outcome | Date | Confidence | Governing assumption |
|---|---|---|---|
| Full R0 scope deployed to production, certified | 2026-12-16 | **40%** | Every external dependency lands on its first named date |
| **R0-Core** (journey + all controls, narrowed pilot width) deployed and certified | 2026-12-16 | **72%** | AWS landing zone by 2026-09-20; pentest slot by 2026-10-04; Shailja's signatures by 2026-08-28 |
| R0-Core live and selling | 2027-01-01 | **72%** | As above, plus a clean 15-day dry run |
| Any go-live at all on 2027-01-01 | 2027-01-01 | **< 20%** if the AWS landing zone slips past 2026-10-05 | Single point of failure; no engineering workaround |

**The three unresolved items most capable of moving the production date today**, per §6 of the
planning model:

| # | Item | Owner | Required-by | Severity | If it slips |
|---|---|---|---|---|---|
| 1 | **AWS landing zone** — account, VPC, Terraform state | Shivanshi + bank infra | **2026-09-20** | `DL0` | No production to deploy to. No workaround exists. Date is lost. |
| 2 | **Shailja's E2 signature** on consent + suitability packs (`S02-G3`, `S02-G4`) | Shailja | **2026-08-28** | `DL0` | S11 — 50 stories, the whole business case — cannot start |
| 3 | **Pentest vendor slot confirmed** | Deepali + procurement | **2026-10-04** | `DL0` | Deepali blocks go-live in December, correctly |

Two further items sit at `DL1`: the named executive sponsor (**GAP-010**, blocking `FRI-001`
funding, required 2026-08-28) and the Group A insurer/product commercial values (**S04-OPEN-01**,
required 2026-10-04 — we cannot sell a product whose values nobody has confirmed).

**My statement as Delivery Head.** This plan meets the date. It meets it with **no slack on three
external dependencies I do not control**, which is why the confidence is 72% and not 90%. I will
publish required-by dates for each, mark them `OVERDUE` the day they pass, convene the owning
authority, and escalate to the accountable human — and at no point will I substitute an answer for
one of them, or average a `DECISION-BLOCKED` into a green forecast.

---

## 7. Where this goes next

| Document | Contains |
|---|---|
| [01 — Delivery timeline and sprint plan](./01-DELIVERY-TIMELINE-AND-SPRINT-PLAN.md) | The calendar, the squads, sprint goals, milestones, gates |
| [02 — Sprint backlog allocation](./02-SPRINT-BACKLOG-ALLOCATION.md) | Every epic and story, placed in a sprint, with an owning squad |
| [03 — Dependency and parallelisation map](./03-DEPENDENCY-AND-PARALLELISATION-MAP.md) | What runs in parallel, what is genuinely dependent, the critical path |
| [04 — Dry run and go-live plan](./04-DRY-RUN-AND-GO-LIVE-PLAN.md) | Cutover, the 15 days day-by-day, go/no-go, hypercare |
| [05 — Forecast, confidence and descope levers](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md) | Confidence model, risks, the levers and their order of use |

---

**Session closed 2026-08-17.** Positions above are attributed to their authorities. Gate
approvals, stage transitions and mandatory human sign-offs are **not** granted by this record.
