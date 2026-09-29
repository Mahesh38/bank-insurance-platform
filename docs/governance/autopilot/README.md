# Safe Autopilot Operating Mode

Autopilot is a work-conserving scheduler and evidence assembler, not an approval substitute.

## Commands

```bash
python scripts/governance/autopilot.py status
python scripts/governance/autopilot.py next
python scripts/governance/autopilot.py next --workstream WS-1
python scripts/governance/autopilot.py next --workstream WS-1 --include-manual
python scripts/governance/autopilot.py propose-transition --workstream WS-1
```

`next` ignores `BLOCKED`, `MET` and `WAIVED` criteria and, by default, excludes
`HUMAN_REQUIRED` and `EXTERNAL_REQUIRED` work. It orders automation-eligible gate work by
priority, enablement count and effort. `--include-manual` exposes coordination work without
allowing the controller to impersonate its owner. A blocked or manual item therefore does not
stall unrelated READY work.

`propose-transition` refuses while any criterion is `OPEN`, `PARTIAL` or `BLOCKED`. Even when all
criteria are evidenced, its output is only `CANDIDATE`, names missing human approvals and states
`may_mark_passed: false`.

## Daily sign-off pull request

[`governance-daily`](../../../.github/workflows/governance-daily.yml) runs every day at 01:17 UTC
(06:47 IST) and on demand. It runs every governance check, regenerates only the derived views
(`BOOT.md`, `DOC-MAP.yaml`, the lifecycle backlog) and writes
[`DAILY-SIGNOFF.md`](./DAILY-SIGNOFF.md) with
[`daily-governance-report.py`](../../../scripts/governance/daily-governance-report.py). It then
opens or refreshes one pull request, labelled `human-signoff`, on the `governance/daily-signoff`
branch.

The report lists everything waiting on a named human: gates at `CANDIDATE` and the verdicts still
owed, change requests awaiting ratification or a counter-signature, external dependencies and
gate blockers past their dates, state-file refresh and ratification actions, and escalated
suggestions. Each AIGEM seat returns an automated `CONCUR` or `DISSENT` from the checks in its own
domain. `UNANIMOUS CONCUR` means the daily update is internally consistent. It is not a board
approval and it never satisfies a T4 human sign-off.

The workflow never approves or merges a pull request. It never edits `CURRENT-STATE.yaml`
(including `state_as_of`), `GATE-EVIDENCE.yaml`, registers or change requests. If a human pushes
to the sign-off branch, the workflow stops overwriting it and posts later reports as comments.

```bash
python3 scripts/governance/daily-governance-report.py            # preview locally, changes nothing
```

## One-way control flow

```text
stage Markdown ──generate──► BACKLOG.yaml / Jira CSV       (structure only)
Jira/CI/docs ──evidence──► GATE-EVIDENCE.yaml              (observed state)
GATE-EVIDENCE ──evaluate──► CANDIDATE transition package   (proposal only)
named human approvals ──authorise──► separate state PR      (stage change)
merged state PR ──trigger──► unpark re-triage               (never auto-admit)
```

## Never automatic

- `PASSED`, go-live or production risk acceptance;
- Security, Compliance, Product, Architecture, QA or Operations verdicts;
- regulatory/control waivers;
- treating `NO_RESPONSE` as approval;
- two-way Jira/YAML synchronization;
- execution of arbitrary commands stored in evidence YAML.

The default policy is schema-enforced as `proposal-only`, human pass required, silence does not
approve and automatic waivers disabled.
