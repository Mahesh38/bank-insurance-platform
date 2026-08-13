# 02 — Realignment Proposal

**Raised by:** Platform Product Owner
**Date:** 2026-08-12
**Status:** 🟠 PROPOSAL — requires stakeholder review and `CR-002` approval
**Evidence base:** [01-PROCESS-GAP-ANALYSIS.md](./01-PROCESS-GAP-ANALYSIS.md)

---

## 1. The proposal in one paragraph

We do not restart, and we do not park anything that works. We run **two tracks in parallel with
one explicit seam between them**. **Track A (Foundation-first)** does the L0–L2 work that was
skipped — signed scope, testable requirements, journey design, canonical domain model — and
delivers it **flow by flow**, not as one big documentation phase. **Track B (Harden & hold)**
finishes what is already built to its existing gates and **stops adding breadth**. The seam is a
single enforceable rule: *Track B may only close work that an existing gate criterion or debt ID
already names; anything new needs a Track A requirement ID.* Within four to six weeks the two
tracks converge, and from that point every flow is built the right way round: requirement →
design → build → prove → demo.

---

## 2. Principles the room is being asked to accept

| # | Principle | What it rules out |
|---|-----------|-------------------|
| **P1** | **Nothing built is thrown away.** All five services map to R0 epics. | "Park it and start again" |
| **P2** | **Nothing new is built without a requirement ID.** Every new line of feature code cites a `BR-` / PRD story. | Engineering inventing product decisions in code |
| **P3** | **Requirements are delivered flow by flow, not as a documentation phase.** Track A ships one journey's worth of signed requirements per sprint. | A three-month "BRD phase" with nothing demonstrable |
| **P4** | **One flow, end to end, demoed every sprint.** Business sees working software every two weeks. | Progress measured in documents |
| **P5** | **The bank's canonical model leads; the aggregator adapts to it.** Not the reverse. | 1SB's data model becoming the bank's product |
| **P6** | **Traceability is retro-fitted, not re-implemented.** Existing code is mapped to requirements, not rewritten to match them. | Rework disguised as compliance |
| **P7** | **The war room decides; agents and individuals do not.** Recorded as `CR-002`. | Silent scope drift, in either direction |

P5 and P6 are the two the room is most likely to argue about. They are also the two that decide
whether this programme owns its product in six months.

---

## 3. The dual-track model

```text
                    ┌──────────────────────────────────────────────────────────┐
  TRACK A           │  FOUNDATION-FIRST  (the work that was skipped)           │
  "do it right"     │  L0 scope sign-off → L1 requirements + AC → L2 canonical │
                    │  domain → L3 contracts → build the next flow properly    │
                    └───────────────────────────┬──────────────────────────────┘
                                                │
                                    ┌───────────▼───────────┐
                                    │      THE SEAM         │  canonical contracts,
                                    │  (§4 — the one rule)  │  requirement IDs, ADRs
                                    └───────────▲───────────┘
                                                │
                    ┌───────────────────────────┴──────────────────────────────┐
  TRACK B           │  HARDEN & HOLD  (what is already built)                  │
  "make it prove    │  WS-1 Phase 4 gate → WS-2 Phase 1 gate → debt paydown →  │
   itself"          │  retro-fit traceability. No new breadth.                 │
                    └──────────────────────────────────────────────────────────┘
```

### Track A — Foundation-first

| Aspect | Definition |
|--------|-----------|
| **Purpose** | Produce the signed, testable business foundation the build never had, and then build the next flows on top of it |
| **Governance home** | **New workstream `WS-0 — AU Bank Insurance Distribution Platform`** (see [F3](./01-PROCESS-GAP-ANALYSIS.md#7-finding-f3--the-product-is-not-a-tracked-workstream)) |
| **Entry stage** | L0/L1 — Discovery close-out into business design |
| **First deliverables** | Sponsor named · Working Decisions + R0 scope **signed** · consent and suitability rule packs · acceptance criteria for flow F1–F3 · canonical domain model for Lead / Consent / Suitability |
| **Output format** | `BR-xxx` requirement → PRD story → Gherkin AC → canonical contract → work item. Every artefact carries an ID that downstream code must cite |
| **Cadence** | Two-week sprints; **each sprint ends with a signed flow specification and a demo of the previous flow's build** |
| **Does not** | Touch existing service code, re-open recorded decisions, or produce documents nobody has to sign |

### Track B — Harden & hold

| Aspect | Definition |
|--------|-----------|
| **Purpose** | Take the five existing services to their existing gates and make them provable |
| **Governance home** | Existing `WS-1` (Phase 4 Hardening) and `WS-2` (Phase 1 Foundation) — **unchanged** |
| **Work allowed** | Only: (a) an open gate exit criterion, (b) an existing `TD-` / `QA-` ledger ID, (c) a defect on a delivered path, (d) traceability retro-fit (§5) |
| **Work not allowed** | Any new outward-facing capability, any second LOB, any new service, any new contract, any refactor not on the debt ledger |
| **Named backlog today** | WS-1: 4.1 sandbox E2E in CI · 4.2 OpenAPI/consumer collection (partial) · 4.3 bank caller UAT · 4.4 compliance review of audit schema · 4.5 ops runbook · 4.6 perf smoke · 4.7 coverage (QA-001). WS-2: A.1–A.6 · TD-006, TD-009, TD-010, TD-014, TD-022, TD-023 |
| **Cadence** | Same two-week sprint, same demo, evidence-based Definition of Done |
| **Exit** | WS-1 Phase 4 gate PASSED and WS-2 Phase 1 gate PASSED. At that point Track B **stops existing as a separate track** and its people join the flow teams |

**This answers the PO's "boilerplate removal and hardening in parallel" directly:** it is Track
B, it is bounded by a written list, and it has an end date defined by two gates rather than by
appetite.

---

## 4. The seam — the one rule that makes parallel work safe

Running two tracks is easy. Stopping the built track from drifting further ahead of the signed
track is the hard part, and it is the failure that created this situation in the first place.
One rule, enforceable in pull-request review:

> ### Rule S-1 — The Track B cap
> **Track B may only close work that an existing gate criterion, debt ID, or defect already
> names. Any work that adds a new outward-facing behaviour requires a Track A requirement ID in
> the pull request description. No ID, no merge.**

Three supporting rules:

| Rule | Statement | Enforced by |
|------|-----------|-------------|
| **S-2 · Contracts flow one way** | Canonical API and domain contracts are **owned by Track A**. Track B implements against them; it never mints a new canonical contract. Adapter-internal types stay adapter-internal. | Architect at design review; ArchUnit already prevents 1SB types leaking |
| **S-3 · Traceability is a Definition-of-Done item** | No work item closes in either track without citing the requirement ID it satisfies — including retro-fitted IDs on existing code. | [13-DEFINITION_OF_DONE.md](../../../governance/13-DEFINITION_OF_DONE.md) amendment, part of `CR-002` |
| **S-4 · One escalation path** | A conflict between the tracks is resolved by PO + Architect jointly and recorded in the decision register. It is never resolved by whoever is closer to the keyboard. | [16-DECISION_MODEL.md](../../../governance/16-DECISION_MODEL.md) |

**Why S-1 works where "be careful" does not:** it is binary, visible in every PR, and it costs a
developer nothing when they are doing sanctioned work. The moment someone starts building
something new, they need a requirement ID they cannot mint themselves — which routes the
decision back to the PO by construction rather than by discipline.

---

## 5. Retro-fitting traceability — how, concretely

This is the operation that converts five untraceable services into a compliant asset. It is
analysis work, not engineering rework.

```text
For each existing service:
  1. List the outward behaviours it actually implements today (from the OpenAPI spec and
     the FUNC-/TECH- backlog rows — not from the code).
  2. For each behaviour, ask Track A: which BR-xxx does this satisfy?
       → BR exists            : link it. Record the FUNC ↔ BR mapping.
       → BR does not exist    : Track A writes it, retrospectively, to current standard,
                                and the room decides whether the built behaviour is correct.
       → No BR is justified   : the behaviour is unrequested scope. Record it as a
                                decision (keep / flag / remove) — do NOT silently delete.
  3. Write the acceptance criteria the behaviour should have had; run them as tests.
       → Pass : the behaviour is now proven. Traceability closed.
       → Fail : a defect, correctly discovered, in the cheapest place to find it.
  4. Record the mapping in a Requirements Traceability Matrix (RTM), owned by the BA.
```

**Expected outcome, stated honestly so nobody is surprised in week two:** most behaviours will
map cleanly and pass. A minority will surface as genuine gaps — most likely around consent
sequencing, suitability gating, and audit attribution, because those are exactly the areas where
the rule packs (`GAP-006`, `GAP-007`, `GAP-014`) are still open. **Those discoveries are the
point of the exercise, not a sign it is going wrong.**

The RTM is the artefact Compliance and the bank's auditors will ask for. It does not exist today,
and it cannot be produced retrospectively at speed in month four.

---

## 6. Staffing — the split the PO proposed

Stated as ratios and roles, because the war room owns the names. Assumption to confirm in the
room: a team of 8–10 delivery people plus the PO/BA/QA/Architect functions.

| Track | Share of engineering capacity | Roles |
|-------|:-----------------------------:|-------|
| **Track A — Foundation-first** | **~40%** early, rising to 100% at convergence | PO (lead) · BA (requirements + RTM) · Solution Architect (canonical model, contracts) · 1 senior engineer (contract-first scaffolding, dev-env) · UX (journey/screen reconciliation) · QA Lead (AC review + test design) |
| **Track B — Harden & hold** | **~60%** early, falling to 0 at gate pass | Tech Lead · 2–3 engineers · QA engineer · DevOps/Ops for runbook + perf smoke · Security Architect for WS-2 A.1–A.3 |

**Non-negotiable staffing conditions** — the room should accept these or reject the model:

1. **The Solution Architect sits in Track A, not Track B.** If the architect is consumed by
   hardening, the canonical model does not get built and P5 fails.
2. **The BA is full-time on Track A.** Requirements with acceptance criteria are the critical
   path for everything; a part-time BA makes Track A the bottleneck within one sprint.
3. **QA Lead reviews acceptance criteria before they are signed, not after.** AC written without
   a tester are AC that cannot be tested — this is how we ended up with `GAP-008`.
4. **Nobody is on both tracks.** Split attention between "prove the old" and "design the new"
   reliably produces neither. Rotation between sprints is fine; simultaneous membership is not.

---

## 7. Convergence — when the two tracks become one

| Milestone | Condition | Expected |
|-----------|-----------|----------|
| **C1 · Foundation signed** | Sponsor named; Working Decisions + R0 scope signed; WS-0 workstream live in governance | End of Sprint 1 |
| **C2 · First traced flow** | One flow (F1) has BR → AC → contract → build → passing test → demo, with an RTM row | End of Sprint 2 |
| **C3 · Track B gates passed** | WS-1 Phase 4 gate PASSED, WS-2 Phase 1 gate PASSED | End of Sprint 3 (WS-1 dependency: 4.3 bank caller — external) |
| **C4 · Single track** | Track B dissolved; all capacity on flow delivery under WS-0; WS-1/WS-2 continue as module workstreams under WS-0's requirements | Sprint 4 |
| **C5 · RTM complete for existing build** | Every delivered behaviour in all five services carries a requirement ID and a test | End of Sprint 4 |

**Track B is deliberately time-boxed by gates, not by a date.** If the WS-1 Phase 4 gate cannot
pass because criterion 4.3 depends on an external bank caller (`RISK-002`, `DEP-002`), that is
escalated as a dependency — it does **not** justify Track B inventing new work to stay busy.
Freed capacity moves to Track A. That is the rule that prevents this proposal decaying back into
the situation it is fixing.

---

## 8. Risks of this proposal (stated by the proposer)

A proposal that lists no risks has not been thought through. These are the four that could sink
it:

| # | Risk | Likelihood | Mitigation |
|---|------|:----------:|-----------|
| **R1** | **Track A becomes a documentation phase** — three months of BRD writing, no working software, business loses confidence | Medium | P3 + P4: requirements ship flow by flow; every sprint demos working software from Track B or a built flow. If a sprint produces only documents, that is a red flag raised at the retro |
| **R2** | **Track B starves and the gates never close** — capacity drains to the shiny new track | Medium | Track B holds ~60% of engineering capacity until C3. Gate criteria are on the sprint board with named owners, not in a background queue |
| **R3** | **Retro-fit surfaces a real design flaw in built code** — e.g. consent sequencing is wrong | Low–Medium | This is a *success* of the process, discovered at the cheapest possible moment. Handled as a normal defect with a `CR` if it changes scope. Budget one sprint of contingency |
| **R4** | **The war room agrees and nothing changes** — the classic outcome | **High** | Every decision in [04 §4](./04-WAR-ROOM-RUNSHEET.md#4-decisions-the-room-must-take) has a named owner and a date; `CR-002` is recorded in the decision register; the first sprint after the war room is planned in the room itself, not afterwards |

**R4 is the highest-likelihood risk in this document.** Alignment is easy to obtain and hard to
convert. The countermeasure is that the war room does not end with agreement — it ends with a
sprint plan and eight signatures.

---

## 9. What changes in the repository if `CR-002` is approved

Mechanical consequences, to be applied **by a human** ([14 §3](../../../governance/14-CHANGE_CONTROL.md#3-procedure),
Rule CC-1 — an agent may raise a change request, never approve one):

| # | Change | File | Owner |
|---|--------|------|-------|
| 1 | Add workstream **WS-0 — AU Bank Insurance Distribution Platform** with lifecycle stage L1, objective, scope and gate | [`governance/state/CURRENT-STATE.yaml`](../../../governance/state/CURRENT-STATE.yaml) | Delivery Lead (edits) · Architect + PO (ratify) |
| 2 | Add the WS-0 narrative block; re-state that WS-1/WS-2 are modules **within** WS-0 | [`governance/01-CURRENT_STATE.md`](../../../governance/01-CURRENT_STATE.md) | Delivery Lead |
| 3 | Add the WS-0 stage map (L0→L1→L2→L3 …) and its gates | [`governance/03-LIFECYCLE.md`](../../../governance/03-LIFECYCLE.md) §6 · [`04-STAGE_GATES.md`](../../../governance/04-STAGE_GATES.md) | Architect |
| 4 | Add **Rule S-1 (Track B cap)** and **S-3 (traceability in DoD)** | [`governance/13-DEFINITION_OF_DONE.md`](../../../governance/13-DEFINITION_OF_DONE.md) · [`08-BACKLOG_RULES.md`](../../../governance/08-BACKLOG_RULES.md) | Architect + PO |
| 5 | Add routing for WS-0 work types (`FUNC`/`DOC`/`COMP` → platform backlog, not the 1SB module backlog) | [`governance/state/CURRENT-STATE.yaml`](../../../governance/state/CURRENT-STATE.yaml) `routing` | Delivery Lead |
| 6 | Create the **Requirements Traceability Matrix** and its first rows | `docs/au-bank-insurance-platform/requirements/RTM.md` *(new)* | BA |
| 7 | Record the decision, and the PO counter-signatures outstanding on GOV-004 / CR-001 | [`governance/registers/DECISION-REGISTER.md`](../../../governance/registers/DECISION-REGISTER.md) | Architect |
| 8 | Close or re-target `GAP-010` (sponsor), `GAP-018` (team boundary) with the war room's answers | [`02-GAP-REGISTER.md`](../02-GAP-REGISTER.md) | PO |

**Nothing above is done yet.** Items 1–5 change governed state and are human-only actions; items
6–8 follow the war room's decisions. This pack, the change request, the triage record and the two
new risks are the complete set of changes made in raising the proposal.

---

## 10. If the room rejects this

The proposal is not the only option, and the room should see the alternatives it is choosing
against:

| Option | Consequence |
|--------|-------------|
| **A · Do nothing; carry on building** | Fastest short-term velocity. Accepts the cost curve in [01 §9](./01-PROCESS-GAP-ANALYSIS.md#9-cost-of-doing-nothing): stalled UAT and compliance sign-off, and a product that converges on the aggregator's model. **Requires the room to formally waive the Wave 0 rule** — which is a recorded decision, not a silence |
| **B · Stop everything and do discovery properly** | Cleanest process. Costs 6–10 weeks of zero demonstrable delivery, wastes the maturity of the 1SB adapter, and will not survive contact with the steering committee |
| **C · Dual track (this proposal)** | Keeps delivery visible, repairs the foundation, converges in 4–6 weeks. Costs ~40% of engineering capacity redirected for three sprints, and requires the discipline of Rule S-1 |
| **D · Dual track without the seam rule** | What we are doing today, with a name. The built track keeps running ahead. **Explicitly not recommended** — the seam is the proposal |

**The PO recommends Option C.** Option A is defensible only if the room records the waiver and
accepts the audit exposure by name.

→ Delivery mechanics: [03-DELIVERY-MODEL-AND-FLOW-PLAN.md](./03-DELIVERY-MODEL-AND-FLOW-PLAN.md)
→ Run the session: [04-WAR-ROOM-RUNSHEET.md](./04-WAR-ROOM-RUNSHEET.md)
