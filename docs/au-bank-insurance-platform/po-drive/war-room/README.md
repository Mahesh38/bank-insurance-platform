# War Room — Process Realignment

**Raised by:** Platform Product Owner
**Date raised:** 2026-08-12
**Status:** 🟠 **PROPOSAL — awaiting stakeholder review.** Nothing in this folder is approved.
**Governance route:** [`CR-002`](../../../governance/registers/DECISION-REGISTER.md#3-change-requests) ·
triaged as [`SUG-20260812-p1r`](../../../governance/registers/SUGGESTION-REGISTER.md)

---

## Why this folder exists

The programme built software before it wrote and signed the requirements that the software is
supposed to satisfy. That is now visible in the repository's own history, and it has a cost we
can still pay down cheaply. This pack states the problem with evidence, proposes a recovery
that **parks nothing and restarts nothing**, and asks each stakeholder for a recorded verdict.

> **The one-line ask:** approve a **dual-track** operating model — one track does the discovery
> and design work that was skipped, one track hardens what is already built — with an explicit
> rule that stops the built track running any further ahead of the signed track.

---

## Read in this order

| # | Document | What it answers | Read time |
|---|----------|-----------------|-----------|
| 1 | [01-PROCESS-GAP-ANALYSIS.md](./01-PROCESS-GAP-ANALYSIS.md) | What actually happened, with dated evidence, and what it costs us | 10 min |
| 2 | [02-REALIGNMENT-PROPOSAL.md](./02-REALIGNMENT-PROPOSAL.md) | The PO's proposal: dual-track recovery, the seam between tracks, what we are *not* doing | 12 min |
| 3 | [03-DELIVERY-MODEL-AND-FLOW-PLAN.md](./03-DELIVERY-MODEL-AND-FLOW-PLAN.md) | The corrected phase model, flow-by-flow chunking, sprint and demo cadence, team split | 15 min |
| 4 | [04-WAR-ROOM-RUNSHEET.md](./04-WAR-ROOM-RUNSHEET.md) | Agenda, timeboxes, and the 9 decisions the room must take | 5 min |
| 5 | [05-STAKEHOLDER-REVIEW-SHEET.md](./05-STAKEHOLDER-REVIEW-SHEET.md) | Your role's review questions and your sign-off block | 5 min |

**Minimum pre-read before the war room:** document 1 (all), document 2 §§1–4, and your own row
in document 5. If you read nothing else, read [01 §2](./01-PROCESS-GAP-ANALYSIS.md#2-what-the-repository-history-actually-shows).

---

## The four findings in one screen

| # | Finding | Evidence |
|---|---------|----------|
| **F1** | **The build ran ahead of the requirements.** Services and shared libs landed on day 1; the charter, BRD map, PRD and R0 scope landed on day 2; governance on day 8; the target architecture on day 11. | Git first-commit dates, [01 §2](./01-PROCESS-GAP-ANALYSIS.md#2-what-the-repository-history-actually-shows) |
| **F2** | **We are four delivery phases into a build that Wave 0 never authorised.** The PO's own rule reads *"No delivery sprint commit until Wave 0 exit criteria met."* Wave 0 exit is still open; WS-1 is at Phase 4 Hardening. | [03-PROGRAMME-TODO.md](../03-PROGRAMME-TODO.md) Wave 0 · [CURRENT-STATE.yaml](../../../governance/state/CURRENT-STATE.yaml) |
| **F3** | **The product itself is not a tracked workstream.** Governance tracks WS-1 (1SB adapter) and WS-2 (workforce auth) — both engineering. The Distribution Platform's own discovery/design work has no stage, no objective and no gate, so triage cannot admit it. | [CURRENT-STATE.yaml](../../../governance/state/CURRENT-STATE.yaml) `workstreams[]` |
| **F4** | **The real exposure is traceability, not wasted code.** All five services map to R0 epics. What is missing is a link from any line of built code back to a signed business requirement — which is what compliance and UAT sign-off will ask for. | [01 §4](./01-PROCESS-GAP-ANALYSIS.md#4-are-the-five-services-the-wrong-scope) |

**F4 is the finding that changes the plan.** If the built services were out of scope, the answer
would be to park them. They are not out of scope — they are *unproven against a requirement*.
So the answer is to retro-fit traceability, not to stop.

---

## What this pack does not do

- It does **not** stop, pause, or park any service that exists today.
- It does **not** propose a rewrite, a re-platform, or a change of tech stack.
- It does **not** re-open decisions already recorded in
  [DECISION-LOG.md](../../DECISION-LOG.md) or the
  [architecture decision log](../../../platform/architecture-review/08-architecture-decision-log.md).
- It does **not** change any lifecycle stage or gate. Only the war room, recorded as `CR-002`,
  can do that — an agent may raise a change request and never approve one
  ([14 §3, Rule CC-1](../../../governance/14-CHANGE_CONTROL.md#3-procedure)).

---

## Outcome required from the war room

The room ends with **nine recorded decisions** ([04 §4](./04-WAR-ROOM-RUNSHEET.md#4-decisions-the-room-must-take))
and **eight sign-offs** ([05](./05-STAKEHOLDER-REVIEW-SHEET.md)). Any decision the room cannot
take is recorded as open, with a named owner and a date — not left implied.

If `CR-002` is approved, the mechanical consequences are listed in
[02 §9](./02-REALIGNMENT-PROPOSAL.md#9-what-changes-in-the-repository-if-cr-002-is-approved) and
must be applied by a human before the next sprint starts.
