# Workstream strategy — North Star + service lanes

**Status:** Binding operating model under [CR-016](../change-requests/CR-016-north-star-and-service-workstream-strategy.md) / [ADR-020](../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md)  
**Approved (Architecture):** Mahesh · **2026-09-13**  
**Countersign outstanding:** Rajal (Product), Kalpana (Delivery) — required before `CURRENT-STATE.yaml` topology transcription  
**Custodian:** Mahesh (Architecture / governance) · Delivery hygiene: Kalpana (R12)

---

## 1. Why this exists

We chase **one North Star** as a team, continuously, until it is reached.

At the same time we need **many agents** to own **many microservices** independently, with
visible progress and honest blockers when another service must move first.

This document is the operating contract for that split.

## 2. Two tiers (do not conflate them)

| Tier | ID pattern | What it is | What it is not |
|---|---|---|---|
| **Program workstream** | `WS-NS` (North Star Continuity) | The single continuous programme lane: objective, stage, gate, in/out of scope | A microservice backlog |
| **Service workstream** | `SWS-<module>` | An agent ownership + progress lane for one deployable microservice | An independent lifecycle stage |

> **Rule WS-NS-1 — Stage fit evaluates against the program workstream.**  
> Triage SF codes, gates and standing constraints bind through `WS-NS` (today carried by WS-3
> until human transcription). An `SWS-*` never invents its own `current_phase`.

> **Rule WS-NS-2 — One in-flight work item per agent lane.**  
> An agent owning `SWS-lead-service` still obeys the single in-flight rule for that lane
> ([AGENTS.md](../../../AGENTS.md) §2). Parallelism comes from *different* service lanes, not from
> stacking items inside one lane.

## 3. North Star program workstream (`WS-NS`)

| Field | Value (until Product countersign changes the wording) |
|---|---|
| Purpose | Continuous programme chase until North Star |
| Current objective carrier | WS-3 `R0-ASSISTED-LIFE-SALE` (BOOT.md / CURRENT-STATE) |
| Open gate carrier | `GATE-S08` (and successor gates as the programme advances) |
| Owners | Product outcome: Rajal · Structure: Mahesh · Path: Kalpana |
| Ends when | Product declares the North Star outcome met — not when a single service ships |

Agents working any `SWS-*` must be able to answer: *how does this item move the North Star?*

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

Before either side marks a **cross-service** outcome Done:

| Check | Pass criteria |
|---|---|
| **Named dependency** | Both boards cite the same DEP id or shared work-item id |
| **Contract agreement** | OpenAPI / event / error-contract change reviewed by both owners (or Architecture if boundary dispute) |
| **Evidence** | Consumer can exercise the producer behaviour under test, or an explicit waiver with owner + expiry exists |

Fail any one → leave the item in **Waiting to unblock**. Do not claim Done on one side only.

Kalpana (R12) may force a decision *to happen* on an aged sync (PA-1); she may not invent the
contract content.

## 6. Agent operating rules

1. Adopt a persona card before deciding across authority boundaries.
2. Triage new inputs (`aigem-triage`) before implementing.
3. Pick **one** `SWS-*` (or `WS-NS` GOV/ARCH item) as the lane for the turn.
4. Update that service’s progress board in the same change that moves work.
5. If blocked by another service, write the wait on **both** boards.
6. Never edit `current_phase` / `stage_status` in `CURRENT-STATE.yaml`.
7. Never treat an `SWS-*` as a licence to bypass standing constraints.

## 7. Index

| Artefact | Path |
|---|---|
| This strategy | `docs/governance/workstreams/WORKSTREAM-STRATEGY.md` |
| Service progress boards | `docs/governance/workstreams/service-progress/` |
| Change request | `docs/governance/change-requests/CR-016-north-star-and-service-workstream-strategy.md` |
| ADR | `docs/platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md` |
| Catalogue | `docs/platform/engineering/backend-service-catalog.yaml` |
