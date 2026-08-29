# Disaster recovery

## What this state is worth

The bootstrap state can delete every repository in the estate — **including the
one holding the governance model that would tell you what was lost.** It is the
highest-value target in the programme, and it is why `SEC-F07` bars storing it
inside the estate.

## Scenarios

| Scenario | Recovery |
|---|---|
| State lost or corrupted | Restore from backend versioning, then `plan` and confirm **no** destructive change before any apply |
| A group or project deleted despite the guard | GitLab retention window, then re-import into state. Never let Terraform recreate it — recreation loses history, members and settings |
| Automation token compromised | Revoke, issue a new dedicated account, re-run `plan`. The token is not in state, so state itself needs no rotation |
| The bootstrap repository is lost | The estate keeps running; only the ability to change it as code is lost. Reconstruct from this repository's history — which is why it is a governed project, not a local directory |
| A drift plan proposes destruction | **Stop.** Never apply. Investigate what changed manually and reconcile forward |

## The untested-restore rule

`C-SEC-6` requires a **tested** restore. Not a documented one.

Test: restore the state to a scratch location, run `plan` against it, confirm the
plan is empty. A restore that has never been executed is a belief about a control,
and beliefs fail at exactly the moment they are needed.

## What is not yet in place

- No backend exists (`M1.6` + `SEC-F07`), so no restore has been tested.
- The rollback anchor is not on the remote (`RISK-025`).
- No apply has run, so there is no state to lose. **This is the cheapest possible
  moment to get these right**, and the last one at which they are cheap.
