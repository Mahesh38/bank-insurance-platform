# 03 — Dependency and Parallelisation Map

**Owner:** Kalpana (critical path, sequencing, dependency ageing) · dependency *content* remains
with the owning Product/Architecture/Engineering/SRE/Security/Database/QA/Compliance authority.
**Method:** [Kalpana §4–§6, §9](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md)
**Extends:** [`registers/DEPENDENCY-REGISTER.md`](../governance/registers/DEPENDENCY-REGISTER.md)

This document answers: **what can run in parallel, what genuinely cannot, and which three things
can move the production date today.**

---

## 1. The parallelisation principle applied

The governing question for every claimed dependency, per §9 of the planning model:

> **Does B need A's implementation, or only A's contract or decision?**

Almost everything in this programme needs only the **contract**. That single distinction is what
converts a 29-week sequential plan into a 17-week parallel one.

| Claimed sequence | Real dependency type | Parallelisation applied |
|---|---|---|
| Backend → Frontend | **Contract-dependent** | Contracts frozen Sprint 1; Flutter builds against contract mocks from Sprint 2, real BFF from Sprint 3 |
| Code → Test automation | **Contract-dependent** | Suites authored from Given/When/Then acceptance criteria in the *same* sprint as the story |
| Code → Security review | **Decision-dependent** | Threat model and control review run from design (Sprint 1–3), not from running code |
| Code → Compliance review | **Decision-dependent** | Rule packs already exist with 38 + 48 testable rules; Shailja reviews behaviour spec, not implementation |
| Environments → Application build | **Independent** | SQ-1 builds IaC while SQ-2/3/4/5 build services; they meet at first deploy in Sprint 3 |
| 1SB live → Quote path | **Data-dependent** | WireMock harness (`S08-E03-S02`) simulates 1SB; real sandbox certification is a separate, later milestone |
| CBS live → Customer prefill | **Data-dependent** | Contract + synthetic CIF fixtures; real CBS integration Sprint 3 |
| Suitability service → Quote service | **Implementation-dependent** ✅ | **Genuine.** C1 must reject before quote exists. Sequenced: suitability Sprint 3–4, quote Sprint 4 |
| Payment → Issuance → Reconciliation | **Implementation-dependent** ✅ | **Genuine.** Money is not eventually consistent. Sequenced within Sprint 5 |

> **Mocks are temporary delivery accelerators, not proof that real integration works**
> ([§5](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md#5-dependency-breaking-techniques)).
> Every mock in this plan has a named real-integration evidence milestone: 1SB → Sprint 7
> (`GATE-P4` 4.1/4.3), CBS → Sprint 3, PG → Sprint 5, AD → Sprint 2, notification → Sprint 5.

---

## 2. What runs in parallel — the eight-lane view

```
              S0    S1    S2    S3    S4    S5    S6    S7    S8   CUT  DRY
             ────  ────  ────  ────  ────  ────  ────  ────  ────  ───  ───
SQ-1 Platform  ·   ████  ████  ████   ··    ··    ·    ███   ███   ██   ███
   CI/IaC/env      pipe  GATE  GATE               obs  perf  ORR  depl  DR
                        S08   S09                      cert  DR

SQ-2 Trust     ·    ▓▓    ▓▓   ████  ████   ▓▓    ▓▓   ███    ·     ·   ▓▓
   C1-C4/audit     NFR   IAM   build  GATES  audit KPI  cert

SQ-3 Journey   ·   ████    ·   ███   ████  ████  ███     ·     ·     ·    ·
   orch/quote      CONTR        orch   cust  prop  admin
                   FREEZE       lead   quote

SQ-4 Money     ·     ·   ████  ███   ███   ████  ███   ███     ·     ·   ▓▓
   pay/issue            1SB   hub    PG    ISSUE  ops   cons
                        cat   CBS         RECON

SQ-5 Exper.    ·   ███     ·    ▓▓    ███  ████  ███     ·     ·     ·    ▓▓
   Flutter/BFF     desgn        auth   hand  UX    mgmt
                   system       scrn   off   TEST

SQ-6 Quality   ·   ████  ████  ███    ▓▓    ▓▓    ▓▓   ████   ███    ▓▓  ███
   test/cert       gates  TEST  resil  100%  pre-  regr  PEN   defect     exec
                          INFRA        cov   pen         TEST  burn

TRK-G Govern.  ██   ▓▓    ▓▓    ▓▓    ███    ▓▓   ███   ▓▓   ████   ██   ███
   gates/T4       chase gates gates  resid       P4gate      T4SIGN

TRK-E Extern.  ██   ███   ███   ███   ▓▓     ▓▓    ▓▓    ▓▓    ▓▓    ·    ·
   AWS/1SB/PG      AWS   AWS   PEN   values
                        LANDS  BOOK
```

`████` peak load · `███` heavy · `▓▓` light/support · `··` idle or redeployed · `·` not engaged

**Maximum concurrency is Sprint 3: eight lanes active simultaneously.** That is the sprint where
the foundation completes and the slice starts, and it is the sprint most likely to expose a
coordination failure rather than a technical one. Mitigation: the daily 15-minute cross-squad sync
exists specifically for Sprint 3 onward.

---

## 3. The genuine dependencies — what truly cannot be parallelised

These are `implementation-dependent` or `external-dependent`. Everything else in this programme has
been broken with a contract, a mock, a fixture or a flag.

### 3.1 Hard technical sequence — inside the product

| # | Predecessor | Successor | Why it is genuine | Sprints |
|---|---|---|---|---|
| D-1 | Application CI green | Any DoD claim | Evidence is an artefact, not an assertion. Nothing is "done" before CI can prove it | S1 → all |
| D-2 | Test infrastructure (`S08-E03`) | In-sprint automation | Cannot write Testcontainers-backed tests without Testcontainers | S2 → S3+ |
| D-3 | AWS landing zone | All of S09 | No account, no VPC, no Terraform state, no environments | S0/S1 → S2 |
| D-4 | Environments (`S09-E02`) | First real deployment | Nothing deploys to nowhere | S2 → S3 |
| D-5 | **Suitability service (C1)** | **Quote generation** | **A quote generated without a valid suitability evaluation id is a `never` constraint.** The gate must exist before the thing it gates | S3/S4 → S4 |
| D-6 | **Consent service (C2)** | **Proposal submission** | No proposal submitted without an unexpired consent grant — standing constraint | S3/S4 → S5 |
| D-7 | Quote | Proposal | Proposal is submitted against a selected, unexpired quote | S4 → S5 |
| D-8 | **Payment reconciled** | **Policy issuance / `Sold`** | A policy is never issued against a payment that is not RECONCILED — standing constraint. **Money is not eventually consistent** | S5 → S5/S6 |
| D-9 | Audit store (`S11-E07-S01`) | Journey reconstruction proof | Cannot reconstruct from records that were never written | S3 → S5 |
| D-10 | Journey orchestration | Save/resume, compensations | Compensations need a state machine to compensate | S3 → S5 |
| D-11 | **Feature-complete UAT** | **Penetration test** | Pentesting an incomplete system tests the wrong system | S6 → S7 |
| D-12 | Pentest findings | Remediation | Cannot fix what has not been found | S7 → S8 |
| D-13 | **All S12 certification** | **Go/no-go** | The decision is the certification, summed | S7/S8 → 11 Dec |
| D-14 | Production provisioned from same IaC as UAT | Production deployment | A cutover onto an un-rehearsed stack is not a cutover, it is an experiment | S7 → 16 Dec |
| D-15 | Backup implemented | Restore proven | — | S3 → S3, re-proven S8 |

### 3.2 Hard governance sequence — signatures, not builds

| # | Predecessor | Successor | Authority | Required-by |
|---|---|---|---|---|
| G-1 | **`S02-G3` + `S02-G4` signed** | **S11 entry — 50 stories** | Shailja (E2, human) | **28 Aug** |
| G-2 | `GATE-S08` PASS | `GATE-S09` candidate | Amit, Swapnali, Mahesh, Deepali, Shivanshi | 20 Sep |
| G-3 | `GATE-S09` PASS | Real deployment of slice services | Shivanshi, Deepali, Aarti, Mahesh | 4 Oct |
| G-4 | `GATE-S11` PASS | S12 certification entry | Rajal, Amit, Swapnali, Shailja, Mahesh | 15 Nov |
| G-5 | `GATE-S12` + `GATE-S14` PASS | Production cutover | Swapnali, Deepali, Shailja, Shivanshi, Kalpana | 11 Dec |
| G-6 | **Human T4 sign-offs** (Architecture, Security, Risk & Compliance) | Go-live approval | **Named humans only** | 11 Dec |
| G-7 | `GAP-010` sponsor named | `FRI-001` funding approval | Rajal → Bancassurance | 28 Aug |

> **G-1 is the single highest-leverage item in the entire programme.** It is a signature on
> documents that are already content-complete, and it gates 50 stories — 20% of the window's
> backlog and 100% of the business case. Shailja has committed to 28 August. Kalpana treats any
> slip as `DL0` and escalates to Shailja's accountable human on day one.

---

## 4. External dependencies — the ones we do not control

Per Rule DEP-3: **every external edge needs an owner and a follow-up date, or it is not tracked —
it is hoped for.**

| ID | Dependency | Owner | Required-by | Severity | Consequence if late | Fallback |
|---|---|---|---|---|---|---|
| **X-1** | **AWS landing zone** — account, VPC, CIDR, egress policy, infosec approval, CMK ownership | Shivanshi + bank infrastructure | **2026-09-20** | **`DL0`** | **No production exists. Date is lost.** | **None.** No engineering workaround. Escalate to sponsor at first slip |
| **X-2** | **Pentest vendor slot** | Deepali + procurement | **2026-10-04** | **`DL0`** | Deepali blocks go-live, correctly | Internal assessment does **not** substitute. Only lever: book two vendors |
| **X-3** | Consent + suitability E2 signature (`S02-G3/G4`) | Shailja | **2026-08-28** | **`DL0`** | S11 cannot start | None — it is the entry condition |
| **X-4** | Executive sponsor named (`GAP-010`) | Rajal → Bancassurance | 2026-08-28 | `DL1` | `FRI-001` unfunded | Interim sponsor delegate with written decision rights |
| **X-5** | 1SB: UAT + prod credentials, `distributorId`, IP allowlist (`GAP-015`) | Rajal + Bancassurance | 2026-09-20 | `DL1` | Quote path cannot certify against real sandbox | WireMock to Sprint 6; real cert must land by Sprint 7 |
| **X-6** | Group A insurer + product commercial values (`S04-OPEN-01`) | Bancassurance | 2026-10-04 | `DL1` | Cannot sell a product whose values nobody confirmed | Catalogue seeded with placeholder; **cannot go live on placeholder** |
| **X-7** | CBS integration slot + test CIFs | Bank core banking team | 2026-09-20 | `DL1` | No ETB prefill; journey degrades to manual entry | Degraded-behaviour path (`S10-E03-S04`) already in scope — usable but poorer |
| **X-8** | AU Bank PG integration + merchant credentials | Bank payments team | 2026-10-04 | `DL1` | **No payment. No `Sold`. No business case** | None — payment is the journey |
| **X-9** | Bank AD technology confirmed (`DEP-010`) | Mahesh | 2026-08-28 | `DL2` | WS-2 Phase 2 design cannot start | Adapter stays federation-agnostic; Keycloak carries R0 |
| **X-10** | Bank consumer UAT slot (`DEP-002`) | Rajal | 2026-09-20 | `DL2` | `GATE-P4` 4.3 cannot close | Bank app team slot is WS-1 only; does not block R0 go-live |
| **X-11** | **Pilot cohort committed** — branches, RMs, customer volume | Rajal + Sales Head | 2026-10-04 | `DL1` | Nobody to sell on 1 January | Reduce to a single branch (lever L2) |
| **X-12** | **Year-end change-freeze exemption** for the pilot estate | Kalpana | 2026-09-20 | `DL1` | Dry run becomes read-only observation | Move destructive rehearsal (Phase C) earlier, into 20–24 Dec |
| **X-13** | Human T4 sign-off calendar slots, 9–11 Dec | Kalpana | **booked 2026-08-21** | `DL0` | Signatures unobtainable in mid-December | **Book in Sprint 0.** There is no fallback for a full calendar |

### 4.1 Existing register edges, re-dated for this window

The [dependency register](../governance/registers/DEPENDENCY-REGISTER.md) already carries DEP-001…
DEP-011. Under this plan their resolution sprints are:

| Edge | Resolves in | How |
|---|---|---|
| DEP-001 (4.3 ← 4.2 OpenAPI) | S7 | `S12-E06-S01` publishes contracts |
| DEP-002 (bank app team UAT) | S7 | X-10 |
| DEP-003 (4.6 ← 4.1 E2E harness) | S2 | `S08-E03-S05` E2E harness built |
| DEP-004 (TD-014 → 4.1) | S2 | `S08-E03` closes TD-014 |
| DEP-005 (Phase 5 ← Phase 4 gate) | **Not resolved** | Phase 5 LOB expansion is **out of this window** by design |
| DEP-006 (TD-010 Redis ← scale-out ADR) | S2 | Multi-instance is now required; ADR lands with `S09-E02` |
| DEP-007 (TD-006 ← AWS target) | S2 | `S09-E04-S01` real secrets management |
| DEP-008 (4.4 → TD-023 capture breadth) | S6 | Compliance review closes in `GATE-P4` closure |
| DEP-009 / DEP-010 (WS-2 Phase 2) | Deferred | Out of R0 window; Keycloak carries R0 behind the adapter |
| DEP-011 (TD-007 ArchUnit tightening) | Deferred | Requires LOB expansion packages — R1 |

---

## 5. The critical path

The longest chain of genuinely dependent work. Every day lost on this path is a day lost from the
date; days lost elsewhere are absorbed by float.

```
X-1  AWS landing zone request ────────────────────────────► 20 Sep   [EXTERNAL, no float]
      │
      ▼
S09-E01/E02  Network, compute, environments ──────────────► 20 Sep   [GATE-S08 same date]
      │
      ▼
S09-E03/E04  Deploy + rollback + secrets + KMS ───────────►  4 Oct   [GATE-S09]
      │
      ▼
S11-E03  Suitability (C1) + Consent (C2)  ────────────────► 18 Oct   [entry gated on X-3]
      │
      ▼
S11-E04  Quote → Proposal ────────────────────────────────►  1 Nov
      │
      ▼
S11-E05  Payment (C4) → Issuance → Reconciliation → Sold ─► 15 Nov   [GATE-S11, M3]
      │
      ▼
S12-E02  Independent penetration test ────────────────────► 29 Nov   [gated on X-2, booked 4 Oct]
      │
      ▼
S12-E02-S02  Remediation to SLA ──────────────────────────► 11 Dec   [GATE-S12 / GATE-S14]
      │
      ▼
G-6  Human T4 sign-offs ──────────────────────────────────► 11 Dec   [booked 21 Aug]
      │
      ▼
Production cutover ───────────────────────────────────────► 16 Dec   [M7]
      │
      ▼
15-day dry run ───────────────────────────────────────────► 31 Dec
      │
      ▼
GO-LIVE ──────────────────────────────────────────────────►  1 Jan   [M9]
```

**Total float on this path: zero.** Every milestone is a finish-to-start with no buffer. That is
the honest structural statement about a 17-week window carrying 29 weeks of sequential work, and it
is why the confidence in [05](./05-FORECAST-CONFIDENCE-AND-DESCOPE-LEVERS.md) is 72% rather than 90%.

### 5.1 The three items most capable of moving the date today

Answering §6 of the planning model — *if this answer is unknown, the delivery plan is not under
control.*

| Rank | Item | Owner | Required-by | Why it is first |
|---|---|---|---|---|
| **1** | **AWS landing zone (X-1)** | Shivanshi + bank infra | **20 Sep** | It is an external queue, not an engineering task. **It has no workaround and no substitute.** Every one of the 45 S09 stories and every deployment sits behind it. If it lands after 5 October, the date is gone regardless of team performance |
| **2** | **Consent + suitability signature (X-3)** | Shailja | **28 Aug** | A signature on already-complete content that gates 50 stories and the entire business case. Highest ratio of impact to effort in the programme |
| **3** | **Pentest vendor slot (X-2)** | Deepali + procurement | **4 Oct** | Lead time is external and December is the worst month to discover a booking problem. Deepali will block, correctly, and no amount of engineering recovers a vendor's calendar |

**Kalpana's standing action on all three:** publish the required-by date, mark `OVERDUE` on the
[decision register](../governance/registers/DECISION-REGISTER.md) the day it passes, convene the
owning authority within the `DL0` window (1 working day), escalate to the accountable human, and
**never supply the answer** (Rule PA-1).

---

## 6. Float — where slippage is survivable

Not everything is on the critical path. These carry real float and are the correct place to absorb
a bad sprint:

| Work | Float | Why survivable |
|---|---|---|
| `S11-E09` Insurer representative | ~2 weeks | Insurer rep can work by email in pilot; the journey completes without the screen |
| `S11-E10` Operations console | ~2 weeks | Runbook + spreadsheet is a viable day-one substitute (lever L5) |
| `S11-E11` Management oversight | ~3 weeks | Pilot is one branch cohort; a weekly export answers the same question |
| `S10-E06-S02` Email for documents | ~3 weeks | SMS covers OTP and payment link — the regulated paths |
| `S08-E06` Developer experience | ~4 weeks | Slows the team; blocks nothing |
| `S12-E06` Consumer enablement | ~2 weeks | Serves WS-1's external consumer, not R0 go-live |
| `S09-E02-S05` Ephemeral environments | ~4 weeks | Efficiency, not capability |
| WS-1 `GATE-P4` closure | to Jan | Supplier workstream gate; does not gate R0 go-live |

**Total absorbable float: roughly 2.5 sprints of squad effort — but none of it is on the critical
path.** Float in SQ-3/SQ-5 does not buy a day for SQ-1's AWS dependency. This is the point of
[§10 of the planning model](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md#10-capacity-planning):
**adding developers to a single-threaded external bottleneck does not shorten the critical path.**

---

## 7. Cycle check

Per [07 §6 of the dependency model](../governance/07-DEPENDENCY_MODEL.md), recurring cycles are an
architecture signal, not a planning one.

**Cycles detected: one, and it is resolved.**

> `Quote` needs `Suitability` (C1 gate) · `Suitability` needs the `Journey` state · `Journey` needs
> a `Quote` stage to transition into.

**Resolution (Mahesh, Architecture jurisdiction):** Journey Orchestration holds **stage and
references only, never another context's business decision** — the existing standing constraint.
Journey therefore depends on the *stage enum*, not on Quote's implementation. The cycle is broken
by the constraint that already exists, which is what a good standing constraint is for. Sequence
becomes: Journey state machine (S3) → Suitability (S3/S4) → Quote (S4). No cycle.

---

## 8. Weekly dependency operating rhythm

| Day | Action | Owner |
|---|---|---|
| **Monday** | Re-compute the execution view. Recompute on every completion or blocker change; never reuse a stale view | Kalpana |
| **Monday** | Chase X-1, X-2, X-3 explicitly, by name, every week without exception | Kalpana |
| **Thursday** | Age every open dependency. Anything past required-by → `OVERDUE`, register entry, convene owner | Kalpana |
| **Thursday** | Gate evidence review — which criteria moved from OPEN to evidenced this week | Kalpana + gate owners |
| **Friday** | Publish the delivery forecast with confidence and the current top-three date movers | Kalpana |

> **`DECISION-BLOCKED` is never averaged into a green forecast.** If X-1 is overdue, the forecast
> says so, in the same sentence as the date.
