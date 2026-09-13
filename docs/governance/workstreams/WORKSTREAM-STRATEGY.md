# Workstream strategy — North Star + service lanes

**Status:** Binding operating model under [CR-016](../change-requests/CR-016-north-star-and-service-workstream-strategy.md) / [ADR-020](../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md)  
**Approved (Architecture):** Mahesh · **2026-09-13**  
**Countersign outstanding:** Rajal (Product), Kalpana (Delivery) — required before `CURRENT-STATE.yaml` topology transcription  
**Conditions closure:** [CR-016/CONDITIONS-CLOSURE.md](../change-requests/CR-016/CONDITIONS-CLOSURE.md)  
**Custodian:** Mahesh (Architecture / governance) · Delivery hygiene: Kalpana (R12)

---

## 1. Why this exists

We chase **one North Star** as a team, continuously, until it is reached.

At the same time we need **many agents** to own **many microservices** independently, with
visible progress and honest blockers when another service must move first.

Board review of CR-016 confirmed the concerns are real. This strategy therefore includes the
**controls** that close those concerns — not only the lane model.

## 2. Two tiers (do not conflate them)

| Tier | ID pattern | What it is | What it is not |
|---|---|---|---|
| **Program workstream** | `WS-NS` (North Star Continuity) | The single continuous programme lane: objective, stage, gate, in/out of scope | A microservice backlog |
| **Service workstream** | `SWS-<module>` | An agent ownership + progress lane for one deployable microservice | An independent lifecycle stage |

> **Rule WS-NS-1 — Stage fit evaluates against the program workstream.**  
> Triage SF codes, gates and standing constraints bind through `WS-NS` (today carried by WS-3
> until human transcription). An `SWS-*` never invents its own `current_phase` / `stage_status`.
> Triage that uses an `SWS-*` as the lifecycle workstream target is **SF4 / REJECT**.

> **Rule WS-NS-2 — One in-flight work item per agent lane.**  
> An agent owning `SWS-lead-service` still obeys the single in-flight rule for that lane
> ([AGENTS.md](../../../AGENTS.md) §2). Parallelism comes from *different* service lanes, not from
> stacking items inside one lane.

> **Rule WS-NS-3 — Interim dual-topology routing (until R12 transcription).**  
> `CURRENT-STATE.yaml` still lists WS-1 / WS-2 / WS-3. Until Kalpana’s transcription PR lands:
> - **Lifecycle / SF / gates** → evaluate against the WS-3 carrier (programme).  
> - **Execution ownership / progress** → use `SWS-*` boards under this strategy + ADR-020.  
> - Do not invent a fourth programme workstream row in state files.

## 3. North Star program workstream (`WS-NS`)

| Field | Value (until Product countersign changes the wording) |
|---|---|
| Purpose | Continuous programme chase until North Star |
| Current objective carrier | WS-3 `R0-ASSISTED-LIFE-SALE` (BOOT.md / CURRENT-STATE) |
| Open gate carrier | `GATE-S08` (and successor gates as the programme advances) |
| Owners | Product outcome: Rajal · Structure: Mahesh · Path: Kalpana |
| Ends when | Product declares the North Star outcome met — not when a single service ships |

Agents working any `SWS-*` must be able to answer: *how does this item move the North Star?*

Programme-level “are we green?” answers use
[`PROGRAMME-ROLLUP.md`](./PROGRAMME-ROLLUP.md) — not SWS Completed counts.

## 4. Transition from WS-1 / WS-2 / WS-3

| Legacy ID | Role under this strategy |
|---|---|
| **WS-3** | Historical + current programme carrier for `WS-NS` until R12 transcription |
| **WS-1** | Domain cluster label for 1SB / integration-supplier services |
| **WS-2** | Domain cluster label for workforce IAM services |

Do **not** delete legacy IDs from evidence, gates or registers. Re-parent language: service
boards name their legacy cluster where useful (`cluster: WS-1`).

## 5. Service workstreams (`SWS-*`)

### 5.1 Membership

Every module in
[`backend-service-catalog.yaml`](../../platform/engineering/backend-service-catalog.yaml)
has exactly one board under
[`service-progress/`](./service-progress/README.md).

Shared libraries under `libs/` are **not** service workstreams. Library changes attach to the
consuming `SWS-*` (or a GOV/ARCH item on `WS-NS` when cross-cutting).

### 5.2 Board sections (mandatory)

Each `SWS-*.md` board keeps three lists current:

1. **Active** — the one in-flight item (plus READY queue if needed).
2. **Completed** — Done items with evidence links (PR, test report, ADR).
3. **Waiting to unblock** — blocked on another `SWS-*` / DEP id / named contract.

### 5.3 Dependency sync check

Before either side marks a **cross-service** outcome Done, satisfy the
[`SYNC-CHECK-EVIDENCE-BAR.md`](./SYNC-CHECK-EVIDENCE-BAR.md) (E1 named dependency · E2 contract
artefact · E3 consumer proof — or a dated waiver).

Fail any one → leave the item in **Waiting to unblock**. Do not claim Done on one side only.

Kalpana (R12) may force a decision *to happen* on an aged sync (PA-1); she may not invent the
contract content.

### 5.4 Board hygiene rules

> **Rule WS-NS-4 — Work-item IDs on every Active/Completed row.**  
> Cite `FUNC-###`, plan id, CR id, or `GOV-CR-016-seed` (bootstrap only). Free-text-only Active
> rows are invalid and must be corrected in the same PR.

> **Rule WS-NS-5 — Claim gate.**  
> Do not claim ownership of an `SWS-*` or open implementation PRs solely to populate a board.
> Claim requires a READY/in-flight governed work item for that module (skeleton modules stay
> seed boards until work exists).

> **Rule WS-NS-6 — No PII or secrets on boards.**  
> Use work-item IDs and service names only. No policy numbers, PAN, phone, quote payloads,
> tokens, or credentials.

> **Rule WS-NS-7 — Standing constraints still bind.**  
> Ownership of an adapter SWS never authorises bank→1SB/DB direct calls, Hub bypass, or Flutter
> OAuth tokens. Deepali’s trust boundaries are unchanged.

> **Rule WS-NS-8 — Boards are not regulatory or gate evidence.**  
> Progress boards are delivery attribution. They do not satisfy GATE-S08, Board 7 readiness,
> or IRDAI/audit evidence packs (Shailja / Swapnali).

## 6. Agent operating rules

1. Adopt a persona card before deciding across authority boundaries.
2. Triage new inputs (`aigem-triage`) before implementing.
3. Pick **one** `SWS-*` (or `WS-NS` GOV/ARCH item) as the lane for the turn.
4. Update that service’s progress board in the **same** change that moves work.
5. If blocked by another service, write the wait on **both** boards and age it per PROGRAMME-ROLLUP §4.
6. Never edit `current_phase` / `stage_status` in `CURRENT-STATE.yaml`.
7. Never treat an `SWS-*` as a licence to bypass standing constraints.
8. Never answer programme status with board-count green — use PROGRAMME-ROLLUP.

## 7. Explicit non-goals

- No second programme workstream competing with `WS-NS`.
- No lifecycle stage per microservice.
- No LOB / channel / DIY / hybrid scope expansion via CR-016.
- No agent transcription of `CURRENT-STATE.yaml` workstream topology.
- No substitution of board hygiene for Product acceptance or QA evidence.

## 8. Index

| Artefact | Path |
|---|---|
| This strategy | `docs/governance/workstreams/WORKSTREAM-STRATEGY.md` |
| Conditions closure | `docs/governance/change-requests/CR-016/CONDITIONS-CLOSURE.md` |
| Sync-check evidence bar | `docs/governance/workstreams/SYNC-CHECK-EVIDENCE-BAR.md` |
| Programme roll-up | `docs/governance/workstreams/PROGRAMME-ROLLUP.md` |
| Service progress boards | `docs/governance/workstreams/service-progress/` |
| Rajal countersign brief | `docs/governance/change-requests/CR-016/countersign-rajal-north-star-continuity.md` |
| Kalpana transcription checklist | `docs/governance/change-requests/CR-016/countersign-kalpana-transcription-checklist.md` |
| Change request | `docs/governance/change-requests/CR-016-north-star-and-service-workstream-strategy.md` |
| ADR | `docs/platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md` |
| Catalogue | `docs/platform/engineering/backend-service-catalog.yaml` |
