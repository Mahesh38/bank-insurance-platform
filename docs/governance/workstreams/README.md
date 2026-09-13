# Workstream charters

**Purpose:** the long-form, human-authored definition behind each `workstreams:` entry in
[`state/CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml), plus the **operating model** for
multi-agent delivery.

`CURRENT-STATE.yaml` is machine-readable and deliberately terse — it is what an agent reads at
pipeline step 1. It carries the *what*: stage, scope lists, gate criteria, routing. It has nowhere
to carry the *why*: the reasoning behind a scope boundary, the evidence a stage assessment rests
on, the relationship between one workstream and another. A charter carries that, and is named in
the workstream's `authority` list so agents can reach it.

> **A charter is not state.** Only a human may write `CURRENT-STATE.yaml`
> ([04-STAGE_GATES §5](../04-STAGE_GATES.md); [gate model §4](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#4-transition-procedure)).
> A charter proposes content and records the reasoning; transcription is a separate, human act.

---

## Operating model (approved 2026-09-13)

| Artefact | Role |
|---|---|
| [`WORKSTREAM-STRATEGY.md`](./WORKSTREAM-STRATEGY.md) | Binding two-tier model: `WS-NS` + `SWS-*` + WS-NS-1…8 controls |
| [`SYNC-CHECK-EVIDENCE-BAR.md`](./SYNC-CHECK-EVIDENCE-BAR.md) | Minimum evidence for cross-SWS Done (Board 5 condition) |
| [`PROGRAMME-ROLLUP.md`](./PROGRAMME-ROLLUP.md) | Critical-path roll-up — prevents false parallel green |
| [`service-progress/`](./service-progress/README.md) | Per-microservice progress boards |
| [CR-016](../change-requests/CR-016-north-star-and-service-workstream-strategy.md) | Change control |
| [CONDITIONS-CLOSURE](../change-requests/CR-016/CONDITIONS-CLOSURE.md) | HOLD / must_fix → solution matrix |
| [ADR-020](../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md) | Architecture decision |

**Approved by:** Mahesh (Architecture / governance) · **2026-09-13**  
**Countersign outstanding:** Rajal (Product), Kalpana (Delivery) before `CURRENT-STATE.yaml` topology transcription.  
**Board concerns:** addressed in CONDITIONS-CLOSURE — human H1/H2 signatures still required.

---

## Index — program / legacy workstreams

| Workstream | Charter | Registered in `CURRENT-STATE.yaml` | Owner |
|---|---|---|---|
| **WS-NS** — North Star Continuity | [`WORKSTREAM-STRATEGY.md`](./WORKSTREAM-STRATEGY.md) | ❌ pending R12 transcription (CR-016) | Mahesh + Rajal + Kalpana |
| **WS-1** — 1SB Insurance Integration | *(none — authority is the [service SSOT](../../1sb-insurance-integration/service-ssot/README.md))* | ✅ | Mahesh + Amit |
| **WS-2** — Workforce Authentication & Authorization | *(none — authority is the [auth SSOT](../../platform/authentication-authorization/README.md))* | ✅ | Mahesh + Deepali |
| **WS-3** — AU Bank Insurance Distribution Platform | [`WS-3-PLATFORM-CHARTER.md`](./WS-3-PLATFORM-CHARTER.md) | ✅ (programme carrier until WS-NS transcription) | Rajal (Product) + Kalpana (Delivery) |

WS-1 and WS-2 have no charter here because each already has a mature SSOT serving the same purpose.
WS-3 needs one precisely because it had no execution-model presence at registration — that absence
was [GAP-D](../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#gap-d--the-platform-is-not-a-governed-workstream--structural).

Under ADR-020, WS-1 / WS-2 / WS-3 also act as **domain cluster labels** on service progress boards
while `WS-NS` is the continuous programme chase.

---

## Why WS-3 still matters

Governance evaluates stage fit against a **program** workstream ([Rule LC-1](../03-LIFECYCLE.md);
[WS-NS-1](./WORKSTREAM-STRATEGY.md#2-two-tiers-do-not-conflate-them)). Until `WS-NS` is
transcribed into `CURRENT-STATE.yaml`, WS-3 remains the programme carrier for North Star stage
fit. Service workstreams (`SWS-*`) never replace that role.

---

## Related

- [`application-lifecycle-bible/01-POSITION-ASSESSMENT.md`](../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md) — where the platform actually is
- [`application-lifecycle-bible/03-REALIGNMENT-PLAN.md`](../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md) — the five moves, of which registering WS-3 is Move 4
- [`application-lifecycle-bible/evidence/`](../../application-lifecycle-bible/evidence/README.md) — retroactive S00–S05 stage evidence for WS-3
- [`change-requests/CR-010/verdicts/`](../change-requests/CR-010/verdicts/README.md) — the board verdicts on CR-010
- [`change-requests/CR-016-north-star-and-service-workstream-strategy.md`](../change-requests/CR-016-north-star-and-service-workstream-strategy.md) — North Star + service lanes
