# Current state, backlog and decisions

> **Live authority:** [`governance/state/CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml). This page is a navigation aid; always use the live state file for an execution decision.

## Current workstream picture

| Workstream | Role | Current position | Current gate / next direction |
|---|---|---|---|
| **WS-3 — AU Bank Insurance Distribution Platform** | Primary platform | `S08 — Engineering Foundation`, with S09 overlapped | `GATE-S08` is the active foundation gate |
| **WS-1 — 1SB Insurance Integration** | Supplier to WS-3 | `L7 — Hardening`, Phase 4 consumer/UAT enablement | `GATE-P4` remains blocked; Life adapter work is also admitted under CR-014 without replacing the Term UAT exit gate |
| **WS-2 — Workforce Auth & Authorization** | Enabler to WS-3 | Foundation implementation | Next: Bank AD federation + production IdP decision |

The repository may advance after this page was written. Treat the state YAML as the answer when there is a mismatch.

## What the backlog words mean

```text
INPUT / SUGGESTION
      │
      ▼
    TRIAGE
      │
      ├── REJECT ─────────────► REJECTED
      ├── PARK ───────────────► PARKED
      ├── ESCALATE ───────────► CHANGE CONTROL / CR
      └── ADMIT
             │
             ▼
           READY
             │
             ▼
         IN-FLIGHT
             │
      ┌──────┴──────┐
      ▼             ▼
   BLOCKED       IN-REVIEW
                     │
                     ▼
                    DONE
```

### READY

Approved, dependency-ready work that may be selected according to ordering rules.

### BLOCKED

Valid work that cannot progress. A blocker must be named; once cleared, the item returns to READY rather than silently continuing.

### PARKED

Valid work that is deliberately **not for now**. It must have a target stage and an observable unpark trigger. Unparking causes re-triage; it does not automatically mean admission.

### IDEAS / P5

Potential future value with no committed delivery stage.

### REJECTED

Deliberately not being done. The reason remains recorded so the same idea is not continuously re-proposed.

## Where to see the live queues

| Need | Source |
|---|---|
| All suggestions and triage outcomes | [Suggestion register](../governance/registers/SUGGESTION-REGISTER.md) |
| Deferred work and unpark triggers | [Parked backlog](../governance/registers/PARKED-BACKLOG.md) |
| Decisions constraining future work | [Decision register](../governance/registers/DECISION-REGISTER.md) |
| Current stages/scope/gates | [`CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) |
| Governance backlog rules | [Backlog rules](../governance/08-BACKLOG_RULES.md) |
| Priority rules | [Priority model](../governance/05-PRIORITY_MODEL.md) |
| Dependency ordering | [Dependency model](../governance/07-DEPENDENCY_MODEL.md) |

## Decision hierarchy

Do not treat every decision-looking file as equally binding.

| Record | Purpose |
|---|---|
| `ADR-*` | Consequential architecture decision |
| `CR-*` | Formal controlled change request |
| `DEC-*` | Business/domain/governance decision package |
| `GOV-*` | Governance decision |
| `DB-DEC-*` | Database/data architecture decision |
| `RISK-*` | Risk statement/ownership rather than approval |

Look at the **status** inside the record: Accepted, Approved, Proposed, Candidate, AI-DRAFTED, Rework, Rejected, human-signature outstanding, etc.

## T4 items and human signatures

A T4 item is not automatically urgent; T4 describes review sensitivity. Priority is still P1–P5.

For current T4 or human-signature debt, search the repository for:

- `mandatory human T4`
- `signature outstanding`
- `PENDING RATIFICATION`
- `CANDIDATE`
- `AI-DRAFTED`

Then confirm the item is still active in the decision/suggestion registers before treating it as a blocker.

## Two different gates

Do not confuse:

- **Review gate** — asks whether one implementation/change is correct and sufficiently reviewed.
- **Stage gate** — asks whether an entire lifecycle stage has enough evidence to pass.

A mergeable PR does not automatically pass a stage, and an open stage gate does not mean every individual change is blocked.

## What “current” means

The state file carries `state_as_of` and `review_due`. If the review date expires, governance rules restrict new admission until the state is refreshed. This protects the team from prioritizing work against stale stage/scope information.
