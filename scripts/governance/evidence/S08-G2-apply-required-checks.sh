#!/usr/bin/env bash
# S08-G2 — apply required status checks on main (ADMIN TOKEN REQUIRED).
# Not runnable by the agent integration (POST/PUT rulesets return HTTP 403).
# Usage:
#   GH_TOKEN=<admin-pat-with-repo-admin> ./scripts/governance/evidence/S08-G2-apply-required-checks.sh
# Dry-run (print payload only):
#   DRY_RUN=1 ./scripts/governance/evidence/S08-G2-apply-required-checks.sh
set -euo pipefail

REPO="${REPO:-Mahesh38/bank-insurance-platform}"

PAYLOAD=$(cat <<'JSON'
{
  "name": "Require GATE-S08 CI checks",
  "target": "branch",
  "enforcement": "active",
  "conditions": {
    "ref_name": { "include": ["refs/heads/main"], "exclude": [] }
  },
  "rules": [
    {
      "type": "required_status_checks",
      "parameters": {
        "strict_required_status_checks_policy": true,
        "required_status_checks": [
          { "context": "Java 21 tests and coverage gates" },
          { "context": "Secret scanning (gitleaks)" },
          { "context": "SAST (CodeQL, Java)" },
          { "context": "SCA (Trivy dependency scan)" }
        ]
      }
    },
    { "type": "non_fast_forward" },
    { "type": "deletion" }
  ]
}
JSON
)

echo "Target repo: ${REPO}"
if [[ "${DRY_RUN:-}" == "1" ]]; then
  echo "$PAYLOAD" | python3 -m json.tool
  echo "DRY_RUN=1 — no API call made."
  exit 0
fi

if [[ -z "${GH_TOKEN:-${GITHUB_TOKEN:-}}" ]]; then
  echo "ERROR: set GH_TOKEN to an admin PAT (repo administration scope)." >&2
  exit 2
fi

echo "Creating ruleset via POST /repos/${REPO}/rulesets ..."
echo "$PAYLOAD" | gh api -X POST "repos/${REPO}/rulesets" --input -

echo "Best-effort: enable existing anti-force-push ruleset 20028494..."
gh api -X PUT "repos/${REPO}/rulesets/20028494" --input - <<'JSON' || true
{
  "name": "No force push allowed",
  "target": "branch",
  "enforcement": "active",
  "conditions": {
    "ref_name": { "include": ["refs/heads/main"], "exclude": [] }
  },
  "rules": [
    { "type": "non_fast_forward" },
    { "type": "deletion" }
  ]
}
JSON

echo "Done. Next: bash scripts/governance/evidence/S08-G2-verify-required-checks.sh"
echo "Then perform a blocked-merge demo and declare S08-G2 MET on human-review."
