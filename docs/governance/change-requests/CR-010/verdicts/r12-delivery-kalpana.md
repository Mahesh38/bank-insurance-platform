# R12 — Delivery · Verdict on CR-010

> ## ⚠️ THIS IS AN AI-DRAFTED SIMULATION
>
> **This document is an AI simulation of Kalpana's delivery reasoning, authored by the Rajal
> (Product) agent.** It is **not** a verdict from Kalpana.
>
> R12 is not one of the seven review boards; it is the **Delivery Control** role that owns
> sequencing, the critical path, dependency escalation and gate `CANDIDATE` marking. Under
> [Rule PA-1](../../../16-DECISION_MODEL.md) Kalpana may force a decision's **timing** but never its
> **content**, and under the [gate model §4](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#4-transition-procedure)
> she may mark a gate `CANDIDATE` but may **never** mark it `PASSED` or edit `CURRENT-STATE.yaml`.
>
> Every sizing, date and capacity statement below is **a question for Kalpana**, not an answer.
> Product does not own delivery feasibility, and this simulation confers nothing.

**Role:** R12 — Delivery · **Simulated persona:** [Kalpana — Delivery Head](../../../../context/roles/kalpana-delivery-head/README.md)
**Reviewer type:** AGENT (Rajal agent simulating R12)
**Change request:** [CR-010](../../CR-010-context-module-and-safe-autopilot.md)
**Date:** 2026-08-16

---

# SIMULATED VERDICT: `APPROVE-WITH-MODIFICATION`

Six conditions (§5). **K-C1 is a block** in the simulated reading: a recovery increment with no
budget line is overhead, and overhead loses.

---

## 1. Decision requested of Delivery

Sequencing, the critical path, the **Foundation Recovery Increment as a funded budget line**, and
the **feature freeze on `services/` outside recovery scope**.

---

## 2. What was reviewed

| Artefact | Path |
|---|---|
| CR-010 | [`CR-010-context-module-and-safe-autopilot.md`](../../CR-010-context-module-and-safe-autopilot.md) |
| Realignment plan, five moves | [`03-REALIGNMENT-PLAN.md`](../../../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md) |
| Position assessment | [`01-POSITION-ASSESSMENT.md`](../../../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md) |
| Stage dependencies (movement rules §5.2) | [`02-STAGE-MODEL.md`](../../../../application-lifecycle-bible/02-STAGE-MODEL.md#52-dependency-not-sequence) |
| Gate states, transition procedure, waiver discipline | [`04-GATE-AND-SIGNOFF-MODEL.md`](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md) |
| S08 and S09 stage definitions | [S08](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md) · [S09](../../../../application-lifecycle-bible/stages/S09-platform-foundation.md) |
| Live gate evidence, blockers, follow-up dates | [`state/GATE-EVIDENCE.yaml`](../../../state/GATE-EVIDENCE.yaml) |
| Dependency register | [`registers/DEPENDENCY-REGISTER.md`](../../../registers/DEPENDENCY-REGISTER.md) |
| Delivery control system | [`DELIVERY-CONTROL-SYSTEM.md`](../../../DELIVERY-CONTROL-SYSTEM.md) |
| WS-3 charter, 19 enablers, `FRI-001` | [charter](../../../workstreams/WS-3-PLATFORM-CHARTER.md) · [S04 §4.5](../../../../application-lifecycle-bible/evidence/S04-product-definition-evidence.md#45-technical-enablers-made-visible--closes-d4-s04-vt-06) · [S00 §4.1](../../../../application-lifecycle-bible/evidence/S00-ideation-evidence.md#41-the-business-case) |

---

## 3. Findings

### 3.1 The sequence is right, and it is the only one available

[Rule 5.2](../../../../application-lifecycle-bible/02-STAGE-MODEL.md#52-dependency-not-sequence)
makes the dependencies hard, not preferential:

```
S08 ─┬─► S10 ─► S11 ─► S12 ─► S13 ─► S14
S09 ─┘         ▲
               │  also requires: S05 passed  +  no open P0 business gap (SM-4)
```

Consequences a delivery plan must accept:

1. **S10 cannot start before S08 and S09 both pass.** Not "should not" — its evidence is produced
   by their machinery.
2. **S11 has three prerequisites**, and the third — no open P0 — is currently **unmet** (GAP-006,
   GAP-007). It closes on a Compliance signature, which no amount of engineering capacity buys.
3. **S12 cannot pass for a system with no S08 and no S09.** WS-1 Phase 4 is bible-stage S12 and
   sits `BLOCKED` for exactly this reason.
4. **S05 is a parallel prerequisite to S11** and consumes design capacity, which engineering does
   not compete for.

### 3.2 The critical path

Two chains. In the simulated reading, **the compliance chain is the real critical path**, and it is
the one most likely to be mismanaged because it consumes no engineering capacity and is therefore
invisible in a capacity plan.

| Chain | Sequence | Constraint |
|---|---|---|
| **Engineering** | S08-G1 CI green → G3/G4 quality gates → G5 scanning → G6 test infrastructure → S09 IaC → environments → S10 → S11 | Capacity-bound. 8–10 weeks per [realignment §2](../../../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md#move-3--underpin-execute-s08-and-s09--810-weeks) |
| **Compliance** | Consent + suitability packs drafted ✅ → **Shailja's signature** → GAP-006/007 closed → SM-4 freeze lifts → S11 entry permitted | **Signature-bound, not capacity-bound.** No engineering effort shortens it |
| **Design** | S05 R0 slice: blueprint ✅ → screens ✅ → visual design → prototype → RM validation → S11 UI build | Design-capacity-bound; runs fully parallel |

> **S08-G1 — a green application-CI run — is the single highest-leverage item in the programme.**
> It converts every future gate claim from assertion to artefact. Four S08 criteria are blocked
> directly on it, and WS-1 criteria 4.1, 4.6 and 4.7 are blocked on machinery it starts.

### 3.3 The dependency picture is honest, and thin

`GATE-EVIDENCE.yaml` records real blockers with real follow-up dates: `GATE-4.1-SANDBOX-E2E`
(ENVIRONMENT, follow-up 2026-08-21), `DEP-001` (HARD), `DEP-002` (EXTERNAL — 1SB UAT and Distributor
ID), `DEP-003` (SOFT, 2026-08-24).

**Two delivery observations:**

- `DEP-002` is external to the organisation. It cannot be escalated internally, and criterion 4.3
  ("a bank caller exercises quote and proposal against UAT") cannot be scheduled until it lands.
  Re-parenting WS-1 under WS-3 at least makes the *bank caller* a named party rather than a
  hypothetical one.
- `execution_mode: EXTERNAL_REQUIRED` on 4.3 is correct and useful: it stops the item being
  re-planned as though effort could close it.

### 3.4 `BLOCKED` is underused, and it is a delivery instrument

[Rule GS-3](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#blocked-is-underused-and-matters-here):
a criterion whose blocker is a missing prerequisite is `BLOCKED`, not `OPEN`.

WS-1 criterion 4.1 sat `OPEN` while no CI existed — implying someone could close it by trying
harder. That single mis-state hid a missing stage for the duration of a phase.

**Delivery position:** `BLOCKED` with a named blocker is not an admission of failure. It is the
only state that makes a dependency **schedulable**. `OPEN` hides it, and hidden dependencies are
what turn an 8-week increment into a 20-week one.

### 3.5 The `FRI-001` funding line — the decisive item

[S00 evidence §4.1](../../../../application-lifecycle-bible/evidence/S00-ideation-evidence.md#41-the-business-case)
defines `FRI-001`: 8–10 weeks, two tranches, a release condition on S08-G1/G2/G5, a stop condition
at week 4, classified **strategic foundation + regulatory mandatory**.

**The classification is doing real work.** Classified as revenue-generating alone, foundation work
reads as pure cost and loses every prioritisation argument — which is
[mechanism 2](../../../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#7-how-this-happened--so-it-does-not-recur)
of how the foundation went missing.

**The problem:** `FRI-001` has **no approver**. GAP-010 leaves the executive sponsor unnamed, and a
funding line with no named approver is not a budget line. It is a proposal. **K-C1.**

**On the two-tranche structure** — in the simulated reading this is right and Delivery should
defend it. An open-ended foundation budget is how foundation work becomes indefinite; a
single-tranche budget is how it gets cut at week five. Two tranches with a named release condition
make the decision reviewable at the point where evidence exists.

**On sizing:** [S04 §4.5](../../../../application-lifecycle-bible/evidence/S04-product-definition-evidence.md#45-technical-enablers-made-visible--closes-d4-s04-vt-06)
lists 19 enablers with owners and gate criteria and **explicitly leaves sizing to Delivery.** That
is the correct division and the work is outstanding. **K-C2.**

### 3.6 The feature freeze on `services/`

[Realignment Move 1](../../../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md#move-1--stop-the-ascent--1-week):
*no new feature merges into `services/` outside the recovery scope*. Rationale: every new line
added before S08 is another line entering the estate untested.

**Delivery view: correct, and it needs a definition sharp enough to apply at a pull request.**
A freeze nobody can adjudicate becomes a freeze nobody honours. **K-C3.**

| Permitted during the freeze | Not permitted |
|---|---|
| Any change delivering an S08 or S09 gate criterion | New bounded contexts |
| Test-only additions to existing services | New endpoints, new LOB handlers |
| Defect fixes on the delivered Term path (WS-1 in-scope) | Feature breadth in `services/` |
| WS-2 IAM foundation work (explicitly not stopped) | WS-1 Phase 5 work of any kind |
| Refactors required to make existing code testable | Refactors of convenience |
| Documentation, rule packs, design, all governance work | — |

**Adjudicator:** Kalpana, on Amit's technical read. Disputes escalate to Mahesh + Rajal.

### 3.7 Perpetual CANDIDATE, and the decision-forcing power

The [gate anti-pattern table](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#7-gate-anti-patterns)
names **perpetual CANDIDATE** — a gate marked candidate whose evidence is never assembled — with
Kalpana's Rule PA-1 power as the countermeasure: a required-by date, then `OVERDUE`, then
escalation.

**This programme's measured record makes that live.** CR-009 recorded **0 of 7 and 0 of 6 gate exit
criteria closed** with every mechanical check green. KPI-09 (gate criteria closed per week) exists
because of it, with a GM-1 self-alarm.

**K-C4** applies the response clock to CR-010's own board verdicts. Four of seven boards have not
responded; a CR whose ratification table sits at PENDING indefinitely is the same anti-pattern one
level up.

---

## 4. What Delivery would push back on

| Item | Push-back |
|---|---|
| "8–10 weeks" for S08 + S09 | An estimate from a plan, not from a sized backlog. **It should not appear in a commitment** until K-C2 is done. Publishing it as a date is how a range becomes a promise |
| Retrofit penalty | ~20,200 lines at 16–29% test ratio. Retrofitting CI and tests onto untested code is materially harder than growing them alongside. The estimate must carry it explicitly, not absorb it |
| S05 and Compliance work "runs in parallel, free" | Parallel yes; free no. Design capacity and Shailja's review time are real constraints. **Shailja's signature is on the critical path and consumes no engineering capacity — which is exactly why it will be under-managed** |
| Three simultaneous journeys in R0 | Already resolved by Product's assisted-first modification. From a delivery standpoint this is the largest single risk reduction in CR-010 |

---

## 5. Conditions

| # | Condition | Test | Owner | Due |
|---|---|---|---|---|
| **K-C1** 🚫 | **`FRI-001` is approved as a named budget line with an envelope, two tranches and a stop condition, by a named human sponsor.** A recovery increment competing as overhead does not survive contact with feature pressure | A funding approval record exists naming an individual approver, an envelope, the tranche release condition (S08-G1/G2/G5 `MET`) and the week-4 stop condition | Executive Sponsor (**requires GAP-010 closed**) | 2026-09-12 |
| **K-C2** | **The 19 enablers are sized and dependency-mapped, and a critical path is drawn with no cycles** ([S04-VT-05](../../../../application-lifecycle-bible/stages/S04-product-definition.md#4-validation-tests)) | A sequenced plan exists; every enabler has an estimate and predecessors; the critical path is explicit | Kalpana + Amit + Shivanshi | 2026-09-12 |
| **K-C3** | **The `services/` feature freeze is published with the §3.6 permitted/not-permitted definition and a named adjudicator**, effective at CR-010 ratification | The definition is published; a sample PR is adjudicated against it without ambiguity | Kalpana + Amit | at ratification |
| **K-C4** | **CR-010's own board response clock runs.** Each pending board gets a required-by date; non-response is recorded as `NO_RESPONSE` against a named persona and escalates ([Rule RG-7](../../../11-REVIEW_GATES.md)) | The [verdict index](./README.md) carries a required-by date per board; overdue rows are marked | Kalpana | at ratification |
| **K-C5** | **Every gate criterion whose blocker is a missing prerequisite is `BLOCKED` with the prerequisite named** — never `OPEN`. Applied to WS-1 Phase 4 and to GATE-S08 | Audit both gates; zero criteria are `OPEN` where a named prerequisite is missing | Kalpana + Mahesh + Swapnali | 2026-08-29 |
| **K-C6** | **The evidence audit runs** ([Move 2](../../../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md#move-2--survey-the-truth--2-weeks-parallel-with-move-3-start)): for every "Done" claim in every phase STATUS file, name the artefact that proves it. Claims without artefacts become recovery backlog items | An audit record exists per claim, with the artefact or a backlog ID | Kalpana + Swapnali | 2026-09-26 |

**K-C6 will be uncomfortable and it is the most valuable of the six.** Its output is the delta
between the programme's believed state and its actual state, and that delta *is* the recovery
increment's real scope. Everything else is planning against an estimate.

---

## 6. What Delivery does not conclude

- **No gate is marked `PASSED` here.** Kalpana may mark `CANDIDATE`; only Mahesh + Rajal jointly
  mark `PASSED`. `CANDIDATE` is readiness for a decision, not the decision.
- **No content decision.** Rule PA-1 grants timing authority only. Delivery may force *when* a
  board decides, never *what* it decides.
- Scope is Rajal's; architecture is Mahesh's; sufficiency of testing is Swapnali's; residency and
  operational readiness are Deepali's and Shivanshi's; permissibility is Shailja's.
- **Delivery pressure never justifies weakening a binding control.** [CR-010 §2](../../CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary)
  forbids weakening a control because a reviewer or dependency is late, and that constraint binds
  R12 as much as it binds automation.

---

## 7. Signature status

```yaml
role: DELIVERY_R12
plan: CR-010
reviewer: "Kalpana — Delivery Head (AI SIMULATION by the Rajal agent)"
reviewer_type: AGENT
self_review: true             # Product agent simulating Delivery on Product-authored artefacts
date: "2026-08-16"
decision: APPROVE_WITH_MODIFICATION
conditions: [K-C1, K-C2, K-C3, K-C4, K-C5, K-C6]
blocking: [K-C1]
gate_marked_candidate: false  # no gate is marked CANDIDATE by this document
signature_status: "AI-DRAFTED — mandatory human signature outstanding"
```

> **`AI-DRAFTED — mandatory human signature outstanding`.**
>
> This is a simulation authored by the Product agent. Kalpana's decision-forcing authority
> (Rule PA-1), her `CANDIDATE` marking power, and her feasibility approval at S04 and S11 are
> **not** exercised by it. None of the six conditions binds anyone until she adopts them, and every
> date is a proposal.
>
> **Silence is not approval.** A board or role that does not respond is recorded as `NO_RESPONSE`
> against a named persona and escalates to a named human — which is exactly what K-C4 asks Delivery
> to operate for this CR.

**Drafted by:** the Rajal agent, Board 3 / R1, simulating R12 · 2026-08-16
**Awaiting:** Kalpana — Delivery Head, R12
