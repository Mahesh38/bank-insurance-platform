# Programme Realignment — R0 Distribution Platform

**Author:** Rajal — Principal Insurance Platform Product Owner (R1 / Board 3)
**Date:** 2026-08-15
**Status:** **PROPOSAL** — requires CR-010 ratification by PO + Architect ([14 §1](../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request))
**Supersedes nothing.** It re-frames [`03-PROGRAMME-TODO.md`](./03-PROGRAMME-TODO.md) into governed phases and reconciles it with [`state/CURRENT-STATE.yaml`](../../governance/state/CURRENT-STATE.yaml).

---

## 1. The question I was asked, answered first

> *Are we on the right track? Did we start at L0 or jump to L4/L5? Have we skipped anything?*

**We did not jump to L4. L0–L3 were done, and done well — but for the *programme*, while
delivery has been running against a *component*. The two were never connected, and the
governance layer quietly adopted the component as if it were the project.**

That is the single finding everything below elaborates. It is not an engineering failure — the
engineering is genuinely good. It is a **sequencing and scope-of-record failure**, which is mine
to own.

The sharpest way to see it:

> `PRODUCT-BACKLOG` and `CURRENT-STATE.yaml` govern **2 of the 14 R0 epics I listed myself** in
> [`03-PROGRAMME-TODO.md`](./03-PROGRAMME-TODO.md) Wave 2. The other 12 have no workstream, no
> gate, no priority and no owner in the governed state. WS-1 *is* `E-HUB`. WS-2 is roughly
> `E-ID`. Everything else — Lead, Customer, Consent, Suitability, Catalogue, Quote, Proposal,
> UW, Payment, Policy, Audit, RM workspace, Reporting — exists only as an unchecked box.

**Verdict: right engineering, wrong altitude, inverted sequence.** Correctable now, expensive in
three months.

---

## 2. Where we actually are — L0…L10 audit

Assessed per [03 §2](../../governance/03-LIFECYCLE.md#2-canonical-lifecycle-l1), against artefacts
that exist in this repository, not against intent.

| Stage | Programme (R0 platform) | WS-1 · 1SB adapter | WS-2 · Workforce IAM |
|---|---|---|---|
| **L0** Discovery | ✅ **Done** — charter, stakeholder session, discovery backlog, stakeholder map, SOURCE-INDEX | inherits | ⚠️ inherited, never stated |
| **L1** Business design | 🟡 **Partial** — vision, capability map, value stream, process catalogue, BRD overview, PRD-R0, R0-SCOPE all exist; **BRD chapters unwritten (GAP-008)**, consent pack (GAP-006), suitability pack (GAP-007), quote rules (GAP-012) all Open | ✅ PO view in `00-po-architect-design-session` | ❌ **none** — went straight to architecture |
| **L2** Domain / aggregate | 🟡 **Partial** — information model, glossary, canonical `contexts.md` exist; **attribute sheets missing (GAP-016)**, product matrix undefined (GAP-013) | ✅ contexts + universal LOB journey | 🟡 implicit only |
| **L3** Technical design | 🟡 **Complete but unratified** — `architecture-review/01–08` is thorough; ARCH-001…022 carry status **`Proposed`**, and **zero ADRs exist** (`id_allocation.ADR: 1`) | ✅ D1–D14 accepted | ✅ ARCH-018…022 Accepted |
| **L4** Foundation | ❌ **Not started** — no landing zone, EKS, platform CI/CD, mesh | ✅ Done (Phase 1) | 🟡 In progress |
| **L5** Connectivity | ❌ | ✅ Done (Phase 2) | ❌ (its Phase 2) |
| **L6** Vertical slice | ❌ **no journey slice exists** | ✅ Done (Phase 3, Term) | 🟡 (its Phase 1) |
| **L7** Hardening | ❌ | 🔶 **current — 0 of 7 gate criteria closed** | ❌ |
| **L8–L10** | ❌ | future | future |

**Read the L4 row across.** The programme has no foundation, yet a component is at L7. That is
the inversion in one line: **we are hardening the second floor of a building with no ground
floor.**

---

## 3. What we skipped — eight findings, each evidenced

| # | Finding | Evidence | Severity |
|---|---|---|---|
| **S1** | **The programme is ungoverned.** WS-1 + WS-2 cover 2 of 14 R0 epics. The other 12 have no workstream, gate, backlog or priority. | `CURRENT-STATE.yaml workstreams[]` vs `03-PROGRAMME-TODO.md` Wave 2 | **P0** |
| **S2** | **Wave 0 never exited, yet delivery ran four phases.** My own rule: *"No delivery sprint commit until Wave 0 exit criteria met."* Sponsor sign-off on Working Decisions + R0-SCOPE is still unchecked. | `03-PROGRAMME-TODO.md` header rule + Wave 0 exit boxes | **P0** |
| **S3** | **Architecture was never ratified, and its route is broken.** ARCH-001…022 sit at `Proposed`; zero ADRs minted; `CURRENT-STATE.yaml routing.ARCH` points at `docs/architecture-review/08-…`, **a path that does not exist** (real path is `docs/platform/architecture-review/…`). ARCH work has nowhere to land. | `routing.ARCH`; filesystem; `id_allocation.ADR: 1` | **P0** |
| **S4** | **Self-service and hybrid have no delivery path.** A2 locks *RM + Self-service + Hybrid Day 1*. WS-2 explicitly parks **retail-customer authentication** to a *"later bounded context"*. Two of three Day-1 channels have no identity vehicle. | `R0-SCOPE.md` A2 vs `CURRENT-STATE.yaml` WS-2 `out_of_scope` | **P0 — direct contradiction** |
| **S5** | **"Sold" has no delivery vehicle.** A6/GAP-002 define Sold = issued **+ confirmation + reconcilable + ops-trackable**. Nothing in WS-1/WS-2 delivers reconciliation or ops tracking. Our definition of success is unbuilt and unassigned. | `R0-SCOPE.md` A6; GAP-002 "Closed"; no recon epic | **P0** |
| **S6** | **Mandatory pre-quote gates are missing while the quote path is already live.** A7 makes suitability mandatory *before quote*; A8 makes consent mandatory. D11 ships `consentRef` as **WARN-only** in P0. We built and are now hardening a quote path that our own locked decisions say may not be reached that way. | A7/A8 vs D11; `FUNC-002` delivered | **P0 — ordering inversion** |
| **S7** | **NFR numbers were never set (GAP-017)** — which is precisely why gate criterion 4.6 (*p95 quote under nominal concurrency*) cannot close. The criterion has no threshold to test against. | GAP-017 Open; GATE-P4 4.6 | **P1** |
| **S8** | **L2 attribute depth missing (GAP-016).** Without attribute sheets for Lead/Consent/Suitability/Quote/Proposal/Payment/Policy, the 12 ungoverned epics cannot produce estimable, DoR-passing stories. | GAP-016 Open | **P1** |

S4, S5 and S6 are the ones that should worry us. They are not "work we have not got to yet" —
they are **contradictions between decisions we have already locked and code we have already
shipped**. Each is an [O5 regulatory / O3 domain-model](../../governance/05-PRIORITY_MODEL.md#3-hard-p1-overrides)
class of problem, not a backlog item.

---

## 4. What I am *not* changing

Discipline matters more than a clean redesign:

- **WS-1 is not cancelled, paused, or rewritten.** It is good work. It de-risked the hardest
  external dependency (1SB) and proved the adapter pattern. ARCH-006 explicitly retains it.
- **The 1SB decisions D1–D14 stand.** They were properly taken in a PO ↔ Architect session.
- **The architecture review's conclusions stand** as recommendations. My proposal is to *ratify*
  them, not re-open them. Re-litigating settled design is exactly the drift
  [17](../../governance/17-DRIFT_CONTROL.md) forbids.
- **Nothing here is a Product decision about topology.** Service boundaries, the Integration Hub
  and Journey Orchestration remain **Mahesh's (R2)** calls. I own scope, phasing, sequence and
  priority — [03 §2–3](../../context/roles/principal-insurance-platform-product-owner/03-authority-and-decision-rights.md).

**The one substantive re-scope I *am* proposing:** GATE-P4 is currently written as if it were a
*programme* gate. It is not — it is an **epic-level gate for E-HUB**. Criterion 4.3 ("a bank
caller exercises quote + proposal against UAT") cannot be satisfied by an adapter in isolation;
it needs a journey, a consent gate and a suitability gate that do not exist. Left as written,
GATE-P4 is unpassable by construction. See §8.

---

## 5. Realigned structure — phases and sub-phases

Programme **R0**, six phases. Each maps to a canonical stage, has one exit question, and holds
sub-phases. WS-1 and WS-2 fold in as named epics rather than standing beside the programme.

| Phase | Name | Stage | Exit question |
|---|---|---|---|
| **R0.0** | **Freeze** | L1 | Is the business scope signed, and are the mandatory rule packs executable? |
| **R0.1** | **Ratify & model** | L2/L3 | Are the design and the information model binding, not proposed? |
| **R0.2** | **Platform foundation** | L4 | Can we build and run more than one service safely? |
| **R0.3** | **Core journey slice** | L5/L6 | Does one customer go end to end, RM-assisted, and reach *Sold*? |
| **R0.4** | **Compliance & ops hardening** | L7 | Is it provable, auditable and operable? |
| **R0.5** | **Pilot & launch** | L7/L9 | Will real RMs and real customers transact on it? |

### Sub-phases

```text
R0.0  Freeze                          ← WE ARE HERE (and should have been since day one)
      R0.0.a  Sponsor & governance closure
      R0.0.b  Mandatory rule packs — consent, suitability, quote/compare
      R0.0.c  Requirements depth — BRD chapters, product matrix, NFR sheet

R0.1  Ratify & model
      R0.1.a  Ratify ARCH-001…022 into ADRs; fix the ARCH routing path
      R0.1.b  Canonical information model + attribute sheets (GAP-016)
      R0.1.c  Journey spec JRN-001 — RM / Self / Hybrid swimlanes
      R0.1.d  Bank-canonical API outline, contract-first

R0.2  Platform foundation
      R0.2.a  Landing zone, EKS, platform CI/CD, mesh + observability
      R0.2.b  Shared libs hardened for multi-service reuse
      R0.2.c  Workforce identity  (WS-2 continues here)
      R0.2.d  Customer identity   ← the S4 gap; new, and Day-1 critical

R0.3  Core journey slice
      R0.3.a  Journey Orchestration + Integration Hub  (WS-1 lands here as E-HUB)
      R0.3.b  Lead → Customer → Consent → Suitability
      R0.3.c  Quote → Compare → Select
      R0.3.d  Proposal → UW tracking
      R0.3.e  Payment → Issuance → Sold confirmation + reconciliation

R0.4  Compliance & ops hardening
      R0.4.a  Audit & compliance end to end
      R0.4.b  Notification + administration/config
      R0.4.c  Security review, DR drill, NFR load test
      R0.4.d  Reconciliation + ops exception console

R0.5  Pilot & launch
      R0.5.a  Group B redirect journey
      R0.5.b  Reporting — funnel + Sold
      R0.5.c  UAT with RM cohort
      R0.5.d  Go / no-go
```

---

## 6. Epics

IDs reuse my own `E-*` scheme from [`03-PROGRAMME-TODO.md`](./03-PROGRAMME-TODO.md) rather than
inventing a parallel one. **New** marks epics that did not exist and that the findings force.

| Epic | Title | Phase | Type | Covers finding | Notes |
|---|---|---|---|---|---|
| `E-FREEZE` | Scope freeze & sponsor sign-off | R0.0 | GOV | S2 | **New** — Wave 0 as a governed epic |
| `E-RULES` | Consent, suitability & quote rule packs | R0.0 | COMP | S6 | GAP-006/007/012 |
| `E-REQ` | BRD depth, product matrix, NFR sheet | R0.0 | DOC | S7, S8 | GAP-008/013/017 |
| `E-ADR` | Ratify architecture into ADRs | R0.1 | ARCH | S3 | **New** |
| `E-MODEL` | Canonical information model + attribute sheets | R0.1 | ARCH | S8 | GAP-016 |
| `E-JRN` | Journey specification JRN-001 | R0.1 | FUNC | S4 | **New** |
| `E-PLAT` | Platform foundation — landing zone, CI/CD, mesh | R0.2 | INFRA | L4 gap | **New** |
| `E-ID` | Workforce identity & RM session | R0.2 | SEC | — | **= WS-2** |
| `E-CUSTID` | **Customer identity for self-service + hybrid** | R0.2 | SEC | **S4** | **New — Day-1 critical** |
| `E-HUB` | Integration Hub Phase A (1SB adapter) | R0.3 | INFRA | — | **= WS-1** |
| `E-ORCH` | Journey Orchestration service | R0.3 | ARCH | S5 | **New** (ARCH-005) |
| `E-LEAD` | Lead create / resume | R0.3 | FUNC | — | |
| `E-CUST` | Customer lookup / prefill (CIF) | R0.3 | FUNC | — | |
| `E-CONSENT` | Consent capture & gate | R0.3 | COMP | S6 | Gates quote |
| `E-SUIT` | Suitability & recommendation record | R0.3 | COMP | S6 | Gates quote |
| `E-PROD` | Product catalogue & eligibility matrix | R0.3 | FUNC | — | |
| `E-QUOTE` | Quote, compare, select | R0.3 | FUNC | — | Consumes E-HUB |
| `E-PROP` | Proposal dynamic form & submit | R0.3 | FUNC | — | Consumes E-HUB |
| `E-UW` | Underwriting status tracking (lite) | R0.3 | FUNC | — | |
| `E-PAY` | Payment session & status | R0.3 | FUNC | — | AU Bank PG |
| `E-POL` | Policy issuance visibility & documents | R0.3 | FUNC | — | |
| `E-SOLD` | **Sold confirmation & reconciliation** | R0.3 | FUNC | **S5** | **New — owns the A6 definition** |
| `E-AUDIT` | Audit trail & attribution | R0.4 | COMP | — | |
| `E-NOTIF` | Notification & communications | R0.4 | FUNC | — | GAP-020 |
| `E-ADMIN` | Administration & configuration | R0.4 | FUNC | — | GAP-022 |
| `E-OPS` | Ops exception console | R0.4 | OPS | S5 | GAP-019 |
| `E-REDIR` | Group B redirect journey | R0.5 | FUNC | — | **New** as an epic |
| `E-RM` | RM workspace | R0.5 | FUNC | — | |
| `E-REP` | Pilot reporting & funnel | R0.5 | FUNC | — | GAP-021 |

**29 epics. Two are in flight. That ratio is the realignment in a single number.**

---

## 7. Stories, priorities and sequence

### 7.1 A deliberate limit on how far I write stories

I am writing **DoR-quality stories for R0.0 and R0.1 only**, and epic-level outcomes with story
*stubs* beyond that. This is not laziness — it is
[Rule DR-1](../../governance/12-DEFINITION_OF_READY.md#5-ready-is-stage-scoped): **READY expires
at a stage boundary.** Detailed R0.3 stories written today would be re-checked and largely
rewritten after the rule packs land, because the rule packs *define their acceptance criteria*.
Writing them now would manufacture false precision and guarantee rework.

**R0.3 stories become writable the day E-RULES closes.** That is a feature of the sequence, not
a gap in this document.

### 7.2 R0.0 — Freeze (current phase)

Priorities computed with `SCORE = 2N + 2S + 2B + 2R + D − E`
([05 §4](../../governance/05-PRIORITY_MODEL.md#4-the-scoring-model)), stage fit assessed against
R0.0. Factors shown so the numbers are auditable rather than asserted.

| ID | Story | Epic | N | S | B | R | D | E | Score | **P** |
|---|---|---|---|---|---|---|---|---|---|---|
| `R0.0-01` | Name the executive sponsor; publish RACI and steering cadence | E-FREEZE | 4 | 4 | 3 | 3 | 2 | 0 | **30** | **P1** |
| `R0.0-02` | Sponsor sign-off on Working Decisions v1 + R0-SCOPE | E-FREEZE | 4 | 4 | 3 | 3 | 2 | 0 | **30** | **P1** |
| `R0.0-03` | Consent rule pack v1 — sequencing, wording, evidence | E-RULES | 4 | 4 | 3 | 3 | 1 | 2 | **27** | **P1** |
| `R0.0-04` | Suitability rule pack v1 — content, gate, override rules | E-RULES | 4 | 4 | 3 | 3 | 1 | 2 | **27** | **P1** |
| `R0.0-05` | BRD chapters §1–4, §6–8, §10–11 with acceptance criteria | E-REQ | 4 | 4 | 3 | 2 | 1 | 3 | **24** | **P1** |
| `R0.0-06` | Quote / compare rule pack v1 (Group A) | E-RULES | 4 | 4 | 2 | 2 | 1 | 1 | **24** | **P1** |
| `R0.0-07` | Product catalogue matrix v0 — Life, Group A/B flags | E-REQ | 4 | 4 | 2 | 1 | 1 | 1 | **22** | **P2** |
| `R0.0-08` | Resolve S4 — decide the customer-identity path for self-service | E-FREEZE | 4 | 4 | 2 | 3 | 1 | 1 | **26** | **P1** |
| `R0.0-09` | Resolve S6 — decide whether the live quote path is compliant pre-gates | E-FREEZE | 4 | 4 | 2 | 3 | 0 | 0 | **26** | **P1** |
| `R0.0-10` | Screen inventory: Figma → CJ/RMJ/JRN, MVP Y/N | E-REQ | 2 | 4 | 1 | 1 | 0 | 1 | **15** | **P3** |

Seven P1s at a freeze phase is correct, not inflation: **every one is a Wave 0 exit criterion**,
and R0.0 exists precisely to close them. `R0.0-08` and `R0.0-09` are new — they are findings S4
and S6 converted into decisions with owners.

### 7.3 R0.1 — Ratify & model (next phase)

Scored at **their target stage**; `priority_now` is capped by
[PRI-2](../../governance/05-PRIORITY_MODEL.md) because they are SF2 from R0.0.

| ID | Story | Epic | Score @ target | **P now / target** |
|---|---|---|---|---|
| `R0.1-01` | Ratify ARCH-001…022 as ADR-001…0NN; set status Accepted | E-ADR | 24 | **P4 / P1** |
| `R0.1-02` | Fix `routing.ARCH` path in CURRENT-STATE.yaml (broken today) | E-ADR | 20 | **P2 / P2** ¹ |
| `R0.1-03` | Attribute sheets: Lead, Consent, Suitability, Quote, Proposal, Payment, Policy | E-MODEL | 24 | **P4 / P1** |
| `R0.1-04` | JRN-001 swimlane — RM / Customer / Platform / Hub / Insurer | E-JRN | 22 | **P4 / P2** |
| `R0.1-05` | Self-service + hybrid mode-switch journey detail (GAP-023) | E-JRN | 22 | **P4 / P2** |
| `R0.1-06` | Bank-canonical API outline for P0 capabilities, contract-first | E-JRN | 20 | **P4 / P2** |
| `R0.1-07` | NFR sheet — SLA, p95 targets, retention, RTO/RPO (GAP-017) | E-REQ | 18 | **P4 / P2** |

¹ `R0.1-02` keeps **P2 now**: it is a two-line correction to a *currently broken* governance
route, and it satisfies the SF2 absorption test in full (small, no new dependency, no new
decision, gate-neutral). Everything else in R0.1 parks.

### 7.4 Sequence

Dependency-ordered per [07 §5](../../governance/07-DEPENDENCY_MODEL.md). Order is **computed,
not chosen**:

```text
R0.0-01 ─┬─► R0.0-02 ─┬─► R0.0-03 ─┐
         │            ├─► R0.0-04 ─┼─► R0.0-06 ─► R0.0-07
         │            └─► R0.0-05 ─┘
         ├─► R0.0-08 ──────────────────► E-CUSTID  (R0.2.d)
         └─► R0.0-09 ──────────────────► GATE-P4 re-scope (§8)

                 R0.0 exit
                     │
         ┌───────────┼───────────┐
    R0.1-01/02   R0.1-03    R0.1-04/05/06/07
     (ADRs)     (model)        (journey)
         └───────────┼───────────┘
                     ▼
                R0.1 exit ──► R0.2 (foundation, parallelisable: a/b/c/d)
                                 │
                                 ▼
                              R0.3  ← E-HUB (WS-1) rejoins here
```

**Critical path:** `R0.0-01 → R0.0-02 → rule packs → R0.1 model → R0.2 foundation → R0.3`.
`R0.0-01` (naming a sponsor) is a **single unstaffed decision blocking a 29-epic programme**.
It is the highest-leverage item in this document and costs one conversation.

**Parallel now, blocked by nothing:** `R0.1-02` (routing fix), `R0.0-10` (screen inventory), and
WS-1's criterion 4.5 runbook sign-off.

---

## 8. What this means for WS-1 / GATE-P4

WS-1 is one criterion from being genuinely useful and six from a gate it cannot pass. Concretely:

- **4.3** (*a bank caller exercises quote + proposal against UAT*) requires a caller, a journey,
  a consent gate and a suitability gate. Three of those four are unbuilt and, per S6, **must**
  precede quote. As written, 4.3 is unpassable by an adapter in isolation.
- **4.6** has no threshold because the NFR sheet does not exist (S7 / GAP-017).

**My recommendation as PO** — Architect's concurrence required, hence CR-010:

1. **Re-designate GATE-P4 as an epic gate for `E-HUB`**, not a programme stage gate.
2. **Move 4.3 to the R0.3 journey gate**, where a real caller can exist. Keep it — do not waive
   it. It is the right criterion at the wrong altitude.
3. **Park 4.6** until the NFR sheet sets a number, then re-admit it with a threshold.
4. **Close 4.5 now** — the runbook is drafted and awaits only Shivanshi's Board 7 verdict.
5. **Keep 4.1, 4.2, 4.4, 4.7 on E-HUB.** They are all legitimately adapter-scoped.

This converts an unpassable gate into a passable one **without waiving a single requirement** —
each criterion keeps its force, at the altitude where it can actually be met.

---

## 9. Decisions I need, and from whom

I own scope, phasing, sequence and backlog order. These are outside that and block the plan:

| # | Decision | Authority | Blocks | Urgency |
|---|---|---|---|---|
| D-A | Name the executive sponsor (GAP-010) | Bank / Head of Insurance | Everything | **Now** |
| D-B | Sign off Working Decisions v1 + R0-SCOPE | Sponsor + Steering | R0.0 exit | **Now** |
| D-C | **S4** — customer identity for self-service/hybrid: build `E-CUSTID` in R0.2, or formally drop self-service from Day 1 | PO + Architect + Security (Deepali) | E-CUSTID, R0.3 | **Now** |
| D-D | **S6** — is the shipped quote path compliant without suitability/consent gates? | Compliance (Shailja) + PO | E-QUOTE, GATE-P4 | **Now** |
| D-E | Ratify ARCH-001…022 into ADRs | Architect (Mahesh) | R0.1, R0.2 | R0.1 |
| D-F | Re-scope GATE-P4 per §8 | Architect + PO (CR-010) | WS-1 exit | R0.0 |
| D-G | Consent sequencing (GAP-006) and suitability content (GAP-007) | Compliance (Shailja) | E-RULES | **Now** |
| D-H | NFR numbers (GAP-017) | Infosec + SRE (Shivanshi) + PO | 4.6, R0.4 | R0.1 |

**D-C and D-D are the two I would escalate today.** Both are cases where shipped or planned work
contradicts a decision we have already locked. They do not get cheaper by waiting, and D-D
carries regulatory exposure ([O5](../../governance/05-PRIORITY_MODEL.md#3-hard-p1-overrides)).

---

## 10. Honest self-assessment

Three of these findings are mine to own, and I would rather write that down than let it sit
unsaid:

- I wrote *"No delivery sprint commit until Wave 0 exit criteria met"* and then let four delivery
  phases run without Wave 0 exiting (**S2**).
- I locked *"suitability mandatory before quote"* (A7) and accepted a P0 that ships `consentRef`
  as WARN-only, with no suitability gate at all (**S6**).
- I locked *"RM + Self-service + Hybrid Day 1"* (A2) while the only identity workstream
  explicitly parked retail-customer authentication (**S4**).

The governance framework did not catch these because it was pointed at the component, not the
programme — which is exactly what S1 says, and exactly what CR-010 is for.

**The good news is the expensive part is done and sound.** Discovery, business design, the
architecture review and a working, well-built 1SB adapter all exist. What is missing is
mostly *decisions and documents*, not code — and decisions are the cheapest thing on this list
to fix, provided we fix them now rather than after twelve more epics assume them.
