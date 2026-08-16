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
