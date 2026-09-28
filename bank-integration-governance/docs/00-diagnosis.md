# 00 — Diagnosis: why AIGEM stopped the work it was meant to protect

Evidence date: **2026-09-28**, product repository `mahesh38/bank-insurance-platform` at
`cd58970`. Every figure below is reproducible from the cited path. Paths are relative to the
product repository root.

---

## 1. What AIGEM got right — and this model keeps

Nothing here argues that the old framework was careless. These parts are sound and are carried
forward unchanged in substance:

| Keep | Source | New home |
|---|---|---|
| Standing constraints and "never" lists — the platform's real invariants | `docs/governance/state/CURRENT-STATE.yaml` → `standing_constraints`, `never` | [`state/guardrails.yaml`](../state/guardrails.yaml) |
| The **change test** for critical controls (G1–G10): tier by what a change *does*, not what it is near | `docs/governance/11-REVIEW_GATES.md` §3, RG-5/RG-6 | [`06-assurance.md` §2](./06-assurance.md#2-control-changes--the-only-thing-that-always-needs-named-humans) |
| AI never manufactures a human approval; material risk acceptance stays human | `AGENTS.md` §2; `PERSONA-AUTHORITY-MATRIX.md` §4 | [`05-decisions.md` §4](./05-decisions.md#4-human-only-decisions) |
| Deadlock has a named human terminator | `PERSONA-AUTHORITY-MATRIX.md` §16.1 (PA-2) | [`05-decisions.md` §6](./05-decisions.md#6-conflict-ladder) |
| Contract-first / stub / simulator parallelisation | `DELIVERY-CONTROL-SYSTEM.md` §11 | [`04-flow.md` §4](./04-flow.md#4-breaking-dependencies) — promoted from a footnote to the default |
| "Do not scale blindly" diagnostic discipline | `AGENTS.md` §2 | [`personas/cards/reliability.md`](../personas/cards/reliability.md) |
| Nothing is Done without evidence | `13-DEFINITION_OF_DONE.md` | [`06-assurance.md` §4](./06-assurance.md#4-definition-of-done-by-risk) |

## 2. What went wrong — ten findings

### F1 — Stage is a mutex, and there are five of them

A workstream is in exactly one stage, and stage fit (SF0–SF5) is evaluated **before merit**
(`03-LIFECYCLE.md` §1). There are four lifecycle vocabularies plus one delivery-state model in
simultaneous use: AIGEM L0–L10, the Lifecycle Bible S00–S15 (`docs/application-lifecycle-bible/02-STAGE-MODEL.md`),
WS-1 Phase 0–6 (`service-ssot/ACTION-PLAN.md`), releases R0–R2, and DCS D0–D6
(`DELIVERY-CONTROL-SYSTEM.md` §6). WS-2's stage is literally recorded as `"L4/L6"`. Every input
must be mapped across these before anyone can say whether it may start.

**Effect:** capabilities with very different maturity (a hardened Term quote path, a green-field
consent service, an unstarted IaC estate) are forced to share one position.

### F2 — The gate waits for a signature after the evidence is in

`GATE-S08` has **10 of 10** exit criteria `MET` and sits in `CANDIDATE` with `approvals: []`
(`CURRENT-STATE.yaml`, WS-3 `current_gate`). The Freeze rule then restricts new admits on the
critical path to SF0/P1 (`04-STAGE_GATES.md` §4). Unfreezing WS-1 Phase 5 requires
`GATE-S08 PASSED` **and** `GATE-S11 PASSED` (`DECISION-REGISTER.md`, DEC-20260816-05).

**Effect:** completed work produces no flow until a human signs a stage transition.

### F3 — Decisions are the real bottleneck, and nothing measures them

21 ADR / DB-DEC rows are `Proposed` or `AI-DRAFTED`; one ADR is `Accepted`
(`DECISION-REGISTER.md`). GOV-001…003, the adoption of AIGEM itself, are still
`Pending ratification`. CR-016, the framework's own flow fix, is operating with all five approver
boxes unticked. The PO counter-signature on the current-state ratification is outstanding.

**Effect:** agents can produce work far faster than humans can decide. The system has no WIP limit
on *decisions*, no decision SLA and no default path while a decision is pending — so undecided
means blocked.

### F4 — The process was bypassed a third of the time

Of 69 suggestion-register rows, **23 are `ADMIT-BYPASS`** — implemented under a human override of
the process (`SUGGESTION-REGISTER.md`). Only 8 were parked through the normal path. AIGEM's own
metrics document says a rising bypass rate is "a process signal, not a discipline problem"
(`18-GOVERNANCE_METRICS.md` §2).

**Effect:** the most important work (CR-015 ULIP journey, CR-016 flow fix) went round the pipeline,
which means the pipeline was tuned for the work that mattered least.

### F5 — Triage costs more than most of the work it triages

Every input runs a 10-step pipeline (`README.md` §5) with SF, SC, necessity, evidence tier,
confidence, work type, stage-relative P1–P5 now **and** at target, dependency typing, and a record
in a register. "A suggestion is never implemented in the turn it is raised" applies even to a
one-line fix inside the file already being edited. The suggestion register alone is **279 KB**.

### F6 — Seven boards and ten personas, one human

CR-009 records that approvals for Product, Delivery, Security and Compliance were given "by the
**same human** who holds the R2 Architecture authority" (`CR-009` §9.1). The framework honestly
flags this — and then requires the same seat-by-seat sign-off again for every change.
Ten personas create 45 possible pairs; five bilateral protocol documents exist
(`docs/context/roles/shared/`).

**Effect:** review ceremony scales with the number of *roles*, while capacity scales with the
number of *people*. With one or two people holding every hat, each extra board is pure latency.

### F7 — Knowledge lives inside personas

Persona packages hold domain knowledge: Mahesh 316 KB, Product 148 KB, Deepali 96 KB, Shailja 92 KB,
Shivanshi 88 KB (`docs/context/roles/`). Duplicate identities exist side by side
(`mahesh-solution-architect.md` **and** `mahesh-principal-insurance-platform-architect/`;
`rajal-product-owner.md` **and** `principal-insurance-platform-product-owner/`; two
`*-agentic-ai-evolution.md` variants).

**Effect:** to know a fact about consent you must load a *person*. Cards (≤ 6 KB) were added as a
patch, but still route back into the packages.

### F8 — Context is expensive and routes by intent, not by change

`docs/` holds **5.7 MB of Markdown in 605 files** (the product `CLAUDE.md` still says 4.3 MB / 441,
which is itself a staleness signal). The agent context index has 41 capsule ids; `DOC-MAP.yaml`
routes every file. Governance + persona text was 53% of all docs at CR-009.

**Effect:** agents spend their window finding the rules. Routing by the *wording* of a request is
fragile — the same code change can resolve to different capsules depending on phrasing.

### F9 — Staleness halts everything

Past `review_due`, agents "must not admit new work" (Rule CS-1; `FreshnessCheck` exit 2). The
2026-09-11 refresh records clearing exactly such a HALT across all three workstreams
(`CURRENT-STATE.yaml` header comment).

**Effect:** a date in a YAML file can stop a whole programme. Freshness should cost a warning, not
a stoppage.

### F10 — Requirement churn is handled as change control

Within eight weeks the behaviour SSOT moved from Working Decisions (DOC-004) to the Rajal
September BRD pack (DOC-005); the Flutter app moved into the repo (DEC-20260816-12) and back out
(DOC-006); R0 went from three journeys to assisted-first (DEC-20260816-03) and then gained
Savings/ULIP (CR-015); the 1SB allowlist belief moved from NAT EIPs (ASM-012) to Apigee IPs
(ASM-015). Seven cross-BRD conflicts are open (`requirements/BRD-ALIGNMENT-2026-09-28.md` §3).
Each flip travelled through CRs, register rows and re-triage.

**Effect:** when the cost of changing direction is high, people either stop changing direction or
stop using the process. Both happened.

## 3. Root cause in one sentence

> AIGEM controls **when work may start**; a regulated programme under requirement churn needs to
> control **what may reach real customers, money and data** — and let everything else flow.

## 4. Where the constraint actually is

| Resource | Supply | Demand | Verdict |
|---|---|---|---|
| AI execution | Effectively unlimited, parallel | — | Not a constraint |
| Human review of changes | 1–3 people | Every PR | **Constraint #2** |
| Human decisions (ADRs, BRD conflicts, sign-offs) | 1–3 people, no SLA | 21 open ADRs, 7 BRD conflicts, 5 overdue external deps | **Constraint #1** |
| External bank dependencies (UAT slot, AD tech, VPN/DX, Apigee, cost envelope) | Outside the team | 5 overdue items | Constraint to **route around**, not wait on |

The new model therefore puts WIP limits on **human attention** and makes every decision and
external dependency carry a default path so that building never waits on them.
