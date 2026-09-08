# Backlog & Parking Dashboard

> **Git-native navigation view.** The registers below are already Markdown and therefore render directly in GitHub/GitLab. This page intentionally links to authority instead of copying the queue into a second source of truth.

## Live queues

- [Suggestion Register](../../governance/registers/SUGGESTION-REGISTER.md) — all triaged inputs and dispositions.
- [Parked Backlog](../../governance/registers/PARKED-BACKLOG.md) — deferred work, target stage and unpark trigger.
- [Dependency Register](../../governance/registers/DEPENDENCY-REGISTER.md) — blocking/ordering dependencies.
- [Risk Register](../../governance/registers/RISK-REGISTER.md) — known risks and ownership.
- [Backlog Rules](../../governance/08-BACKLOG_RULES.md) — READY/BLOCKED/PARKED/IDEAS/REJECTED/ESCALATED semantics.

## Status meanings

| State | Meaning |
|---|---|
| `ADMIT` / `ADMITTED` | Valid for the current-stage backlog; not automatically implemented. |
| `ADMIT-BYPASS` | Human-authorized process bypass recorded for traceability. |
| `READY` | Approved and dependency-ready for pickup. |
| `BLOCKED` | Valid work that cannot proceed until a named blocker clears. |
| `PARKED` | Valid work deliberately deferred with a target/unpark trigger. |
| `IDEAS` / `P5` | Future value without a committed stage. |
| `REJECTED` | Deliberately not being done; reason remains recorded. |
| `ESCALATED` | Requires change control or accountable human decision. |
