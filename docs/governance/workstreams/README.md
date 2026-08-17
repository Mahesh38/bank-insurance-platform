# Workstream charters

**Purpose:** the long-form, human-authored definition behind each `workstreams:` entry in
[`state/CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml).

`CURRENT-STATE.yaml` is machine-readable and deliberately terse — it is what an agent reads at
pipeline step 1. It carries the *what*: stage, scope lists, gate criteria, routing. It has nowhere
to carry the *why*: the reasoning behind a scope boundary, the evidence a stage assessment rests
on, the relationship between one workstream and another. A charter carries that, and is named in
the workstream's `authority` list so agents can reach it.

> **A charter is not state.** Only a human may write `CURRENT-STATE.yaml`
> ([04-STAGE_GATES §5](../04-STAGE_GATES.md); [gate model §4](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#4-transition-procedure)).
> A charter proposes content and records the reasoning; transcription is a separate, human act.

---

## Index

| Workstream | Charter | Registered in `CURRENT-STATE.yaml` | Owner |
|---|---|---|---|
| **WS-1** — 1SB Insurance Integration | *(none — authority is the [service SSOT](../../1sb-insurance-integration/service-ssot/README.md))* | ✅ | Mahesh + Amit |
| **WS-2** — Workforce Authentication & Authorization | *(none — authority is the [auth SSOT](../../platform/authentication-authorization/README.md))* | ✅ | Mahesh + Deepali |
| **WS-3** — AU Bank Insurance Distribution Platform | [`WS-3-PLATFORM-CHARTER.md`](./WS-3-PLATFORM-CHARTER.md) | ❌ **not yet** — proposed under [CR-010](../change-requests/CR-010-context-module-and-safe-autopilot.md) | Rajal (Product) + Kalpana (Delivery) |

WS-1 and WS-2 have no charter here because each already has a mature SSOT serving the same purpose.
WS-3 needs one precisely because it has no execution-model presence at all — that absence is
[GAP-D](../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#gap-d--the-platform-is-not-a-governed-workstream--structural),
the root cause the realignment exists to fix.

---

## Why WS-3 matters more than a third row in a table

Governance evaluates stage fit against a workstream ([Rule LC-1](../03-LIFECYCLE.md)). Work
belonging to no workstream triages as out of scope. Until WS-3 is registered, application CI,
infrastructure-as-code, the Consent service and the Flutter application have no legitimate home in
the model — so the framework keeps correctly excluding the foundation it is meant to protect.

---

## Related

- [`application-lifecycle-bible/01-POSITION-ASSESSMENT.md`](../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md) — where the platform actually is
- [`application-lifecycle-bible/03-REALIGNMENT-PLAN.md`](../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md) — the five moves, of which registering WS-3 is Move 4
- [`application-lifecycle-bible/evidence/`](../../application-lifecycle-bible/evidence/README.md) — retroactive S00–S05 stage evidence for WS-3
- [`change-requests/CR-010/verdicts/`](../change-requests/CR-010/verdicts/README.md) — the board verdicts on CR-010
