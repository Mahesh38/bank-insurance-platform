# CR-016 — Parallel microservice executor lanes (not new personas / not per-service workstreams)

**Date:** 2026-09-21  
**Type:** GOV (with DELIVERY / ENG consequences)  
**Raised by:** agent (Cloud Agent) from AI-team architectural direction  
**Workstream:** cross-cutting — primarily WS-3 Foundation Recovery; also affects WS-1 and WS-2 lane clarity  
**Stage:** WS-3 S08 — Engineering Foundation · WS-1 Phase 4 · WS-2 IAM Phase 1  
**Decision:** **PENDING** — agents must not implement persona-roster or workstream-taxonomy changes until HUMAN APPROVED ([14 §3](../14-CHANGE_CONTROL.md#3-procedure); Rule CC-1)  
**Origin:** [`SUG-20260921-pws`](../registers/SUGGESTION-REGISTER.md#sug-20260921-pws--parallel-microservice-executor-lanes)  
**Required approvers:** Mahesh (R2 Architecture) · Rajal (R1 Product) · Amit (R3 Engineering) · Kalpana (R12 Delivery) — consult Deepali / Swapnali / Shivanshi / Shailja on the review-after-code flow only

---

## 1. Current position

| Authority | What it says today |
|---|---|
| [`CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml) | Exactly **three** governance workstreams: **WS-1** (1SB integration), **WS-2** (workforce IAM), **WS-3** (platform / R0 assisted Life). Stage fit is evaluated **per workstream** (Rule LC-1). |
| [`14-CHANGE_CONTROL.md` §1.1](../14-CHANGE_CONTROL.md#11-persona-roster-control) | Persona roster is **closed as of CR-009**. Nine named accountability personas. Adding a persona requires a CR and admission tests **PR-1–PR-4**, plus Rule **CC-2** (no net persona growth). |
| [`AGENTS.md` §2](../../AGENTS.md) / [`09-AI_EXECUTION_RULES.md`](../09-AI_EXECUTION_RULES.md) | Exactly **one work item in flight per agent/owner**. **Independent, dependency-safe owners may progress in parallel.** |
| [`amit-engineering.card.md`](../../context/personas/amit-engineering.card.md) | **Do not create a second Principal Engineer identity.** Microservice implementation sits under Amit (R3). |
| [`kalpana-delivery.card.md`](../../context/personas/kalpana-delivery.card.md) · package `04` | Kalpana owns **safe parallelization**. Delivery “streams” (quote, payments, integrations, …) are a **planning decomposition**, not new rows in `CURRENT-STATE.yaml` `workstreams:`. |
| BOOT / S08 deliverable | ~21 services are **scaffolded**; presence of a module is not delivery of its bounded context. Foundation Recovery still needs concurrent, non-blocking implementation of independent services. |

The intake asks to (a) treat **each microservice as a workstream**, (b) mint **four to five developer personas**, (c) assign tasks only to those personas for parallel AI development, and (d) run lead / compliance / security / SRE review before merge. The **outcome** (unblock false serialisation; develop independent services in parallel) is real. The **mechanism** (new governance workstreams + new canonical personas) collides with the closed roster and the three-workstream model.

---

## 2. Proposed change (for human decision)

### 2.1 Preferred option — **ADMIT as operating model** (no persona growth, no workstream explosion)

Record and implement a **Microservice Executor Lane Map** under existing authorities:

| Element | Owner | What changes |
|---|---|---|
| **Lane map** (DOC) | Kalpana (R12) drafts · Amit (R3) owns lane assignment · Mahesh (R2) confirms service↔bounded-context fit | A single binding table: service / bounded context → governance workstream (WS-1\|WS-2\|WS-3) → executor lane id → in-flight work-item rule → contract dependencies |
| **Executor lanes** | Amit (R3) | 4–5 **named lanes** (e.g. `LANE-IAM`, `LANE-1SB`, `LANE-LEAD-JOURNEY`, `LANE-CONSENT-SUIT`, `LANE-PLATFORM-SHARED`). Lanes are **executor labels**, not personas, not boards, not `CURRENT-STATE` workstreams. |
| **Parallel rule** | Kalpana + Amit | Two lanes may run simultaneously only when the dependency register classifies the edge as Independent or Contract-dependent (interface frozen). Implementation-dependent edges stay blocked. |
| **Review-after-code** | Existing boards | After a lane’s PR is READY: Amit tech review → Deepali / Shailja / Shivanshi / Swapnali as tier requires → merge. **No new review board.** |
| **Auth example** | WS-2 | Authentication / authorization services stay **WS-2**. They get one or two **lanes** under WS-2, not a fourth governance workstream and not a new persona. |

Documents to add or extend **only if APPROVED**:

1. `docs/governance/registers/EXECUTOR-LANE-MAP.md` (or equivalent under Delivery Control System) — lane ↔ service ↔ WS ownership.  
2. Short addendum on [`DELIVERY-CONTROL-SYSTEM.md`](../DELIVERY-CONTROL-SYSTEM.md) distinguishing **governance workstream** vs **delivery/executor lane**.  
3. Optional: Amit package note on how AI agents adopt a **lane**, not a new identity (extend package; do **not** add a persona card).

### 2.2 Rejected unless PR-1–PR-4 and CC-2 pass — **new developer personas**

Creating four to five “dev personas” as canonical identities would:

- fail **PR-2** (jurisdiction already sits with Amit / existing WS owners);
- fail **PR-3** (parallel engineering authorities / second Principal Engineer);
- fail **CC-2** (net persona growth) unless an equal number of personas are merged or retired in the same CR;
- recreate the CR-002…CR-008 failure mode documented in [14 §1.1](../14-CHANGE_CONTROL.md#11-persona-roster-control) (persona docs crowding out product delivery).

Expertise for a service belongs in **service SSOT + lane assignment**, not a new top-of-matrix identity ([Rule CC-3](../14-CHANGE_CONTROL.md#11-persona-roster-control)).

### 2.3 Rejected as default — **one CURRENT-STATE workstream per microservice**

Promoting each of ~21 scaffolded services to a `workstreams:` entry would:

- explode stage-fit / freshness / gate bookkeeping;
- blur Rule LC-1 (stage fit is per *capability* workstream, not per deployable);
- turn every cross-service contract into an inter-workstream governance event.

Delivery streams and executor lanes already express independence **without** that cost.

---

## 3. Driver

**Business / delivery priority change** (AI team architectural direction, 2026-09-21): false blocking between work that is actually independent is slowing Foundation Recovery and multi-service progress. Agents need a clear, parallelizable ownership model so independent microservices (and WS-2 IAM vs WS-1 adapter vs WS-3 platform contexts) can advance simultaneously, with specialist review before merge.

“It would be cleaner to have more personas” is **not** the driver. The driver is **measured serialisation of independent work**.

---

## 4. What this CR does not do

- Edit `current_phase`, `stage_status`, or gate `state` in `CURRENT-STATE.yaml`.
- Add, rename, or retire any named persona without a separate, CC-2-compliant CR.
- Register WS-4…WS-N for individual microservices.
- Convert PENDING into APPROVED, or treat listing approvers as a HUMAN verdict.
- Implement service code, scaffolds, or CI changes in the turn the suggestion was raised.
- Waive Deepali / Shailja / Shivanshi / Swapnali review gates for any risk tier.
- Allow one agent lane to hold multiple in-flight work items.

---

## 5. Impact analysis (pipeline steps 2–8, as if preferred option approved)

| Step | Preferred option |
|---|---|
| Stage fit | **SF1** for S08 / current WS-1 & WS-2 delivery — clarifies how foundation services are staffed in parallel. |
| Scope fit | **SC1** — serves Foundation Recovery / GATE-S08 progress and existing WS objectives; names beneficiary as unblocking independent service delivery. |
| Necessity | **SHOULD** now (throughput) · **MUST** before broad multi-service feature fill if false dependencies remain the critical-path drag. |
| Classification | **GOV** + DOC · story (lane map) · risk tier **T2** (process); raising personas/workstreams would be **T4 GOV**. |
| Priority | **P2** now · **P2** at target for the lane map. Persona/workstream explosion stays **REJECT** / not scored. |
| Dependencies | Lane starts still honour [`DEPENDENCY-REGISTER.md`](../registers/DEPENDENCY-REGISTER.md). Contract-dependent work may start on frozen interfaces. |
| Effort | **S–M** for the map + DCS addendum. **XL** and high-risk if Option 2.2/2.3 chosen instead. |

### Anti-over-engineering (preferred option)

| Test | Result |
|---|---|
| X1 Named consumer | Yes — AI executor agents blocked by false serialisation today |
| X6 Simplest sufficient | Yes — lanes under Amit/Kalpana; not new personas/workstreams |
| X8 Cognitive cost | Bounded — one new map; reuses existing WS and boards |
| X9 Problem observed | Yes — intake reports cross-workstream blockage |
| X10 Do nothing | Parallel capacity stays unused; GATE-S08 stays open longer |

---

## 6. Alternatives considered

| Option | Consequence |
|---|---|
| **A — Executor lanes (preferred)** | Parallel AI development inside WS-1/2/3; roster and workstream count unchanged; review boards unchanged. |
| **B — Do nothing** | Independent services remain queued behind unrelated incomplete work; Foundation Recovery stays serialised. |
| **C — New workstream per microservice** | Governance explosion; LC-1 noise; high undo cost. |
| **D — Four to five new developer personas** | Violates closed roster / CC-2 / Amit single-identity rule unless net-zero merge; high docs cost, low product throughput (see 14 §1.1 history). |
| **E — One mega sequential queue** | Explicitly what the intake rejects. |

---

## 7. Decision record (human only)

```yaml
change_request:
  id: CR-016
  raised_by: "agent:cloud"
  date: "2026-09-21"
  type: GOV
  origin: SUG-20260921-pws
  decision: PENDING          # PENDING | APPROVED | REJECTED | DEFERRED
  preferred_option: A        # executor lanes
  approvers:
    - "Mahesh (R2)"
    - "Rajal (R1)"
    - "Amit (R3)"
    - "Kalpana (R12)"
  decided_on: null
  conditions: []
```

---

## 8. Related

- Origin: [`SUG-20260921-pws`](../registers/SUGGESTION-REGISTER.md#sug-20260921-pws--parallel-microservice-executor-lanes)
- Persona roster control: [`14-CHANGE_CONTROL.md` §1.1](../14-CHANGE_CONTROL.md#11-persona-roster-control)
- Prior persona addition pattern (contrast): [`CR-003`](./CR-003-principal-dba-and-persona-authority-matrix.md)
- Delivery parallelization: Kalpana package `04-delivery-planning-critical-path-and-parallelization.md`
- Standing rule: independent dependency-safe owners may progress in parallel (`AGENTS.md` §2)
