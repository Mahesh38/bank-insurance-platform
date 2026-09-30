# Governance Workflow Failure Runbook

**Owner (named recipient):** Shivanshi / SRE (Board 7)  
**Escalate if unacknowledged in one business day:** Kalpana / R12  
**CR-010 OPS-C1**  
**signature_status:** `AI-DRAFTED`

Workflows: `.github/workflows/governance.yml` (PR/push/weekly) and `governance-daily.yml` (01:17 UTC).

On failure both emit `::error::` naming this recipient. That is the detection mechanism. It is not an issue-tracker integration (issues:write is not granted to the PR workflow).

## Action

1. Open the failed run. Note job (`freshness` vs `ci-checks` vs `daily-signoff`).
2. Freshness exit 2 → treat as CS-1 halt (see GOVERNANCE-FAILURE-RUNBOOK).
3. `ci-checks.py` / context / autopilot tests → fix in a PR; do not `--no-verify`.
4. `governance-daily` failed to open the sign-off PR → check `GOVERNANCE_BOT_TOKEN` and "Allow GitHub Actions to create pull requests".
5. Record the incident against RISK-017 if automation was silent before this alarm existed.

Do not merge a red governance workflow with a skip.
