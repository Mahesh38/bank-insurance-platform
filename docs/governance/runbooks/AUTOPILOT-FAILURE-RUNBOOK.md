# Autopilot Failure Runbook

**Owner:** Deepali / Security + Shivanshi / SRE  
**CR-010 SEC-C1 / R-C1**  
**signature_status:** `AI-DRAFTED`

## Symptoms

- `REFUSED:` on stderr from `autopilot.py`
- `GOVERNANCE ALARM: …` on stderr
- New lines in `docs/governance/autopilot/proposals/alarms.jsonl` (gitignored)

## Causes

| Kind | Meaning |
|---|---|
| `path_escape` | `--output` contained `..` |
| `symlink` | Target is a symlink |
| `outside_proposals` | Target not under `docs/governance/autopilot/proposals/` |
| `protected_path` | Target resolved under `state/` or `change-requests/` |
| `unsafe policy` | Evidence file policy is not proposal-only |
| `gate evidence incomplete` | `propose-transition` before all criteria MET/WAIVED |

## Action

1. Do **not** bypass the check.
2. If a human needed a candidate file, write it under `docs/governance/autopilot/proposals/`.
3. If the alarm shows an attempt to write state or a CR, treat it as a security event: named recipient Deepali, copy Shivanshi.
4. Autopilot still must not mark `PASSED` or populate approvals.
