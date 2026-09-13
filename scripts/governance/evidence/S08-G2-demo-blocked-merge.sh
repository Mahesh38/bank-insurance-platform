#!/usr/bin/env bash
# S08-G2 — blocked-merge demonstration (run AFTER apply + verify exit 0).
# Attempts to merge a PR that is not fully green and expects GitHub to refuse.
#
# Usage:
#   ./scripts/governance/evidence/S08-G2-demo-blocked-merge.sh [pr-number]
#   PR defaults to 104 (this GATE-S08 evidence PR) if omitted.
#
# Exit codes:
#   0  merge correctly blocked (evidence captured under scripts/governance/evidence/)
#   1  protection not configured yet (run apply + verify first)
#   2  unexpected (merge succeeded or API error) — investigate before declaring MET
#   3  usage / missing tools
set -euo pipefail

REPO="${REPO:-Mahesh38/bank-insurance-platform}"
PR="${1:-104}"
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
VERIFY="$ROOT/scripts/governance/evidence/S08-G2-verify-required-checks.sh"
OUT="$ROOT/scripts/governance/evidence/S08-G2-blocked-merge.out"
STAMP="$(date -u +%Y-%m-%dT%H:%MZ)"

if [[ ! -x "$VERIFY" ]]; then
  echo "ERROR: verify script missing or not executable: $VERIFY" >&2
  exit 3
fi

echo "S08-G2 blocked-merge demo — ${STAMP} — repo ${REPO} PR #${PR}"
echo "NOT a MET declaration by itself — capture this transcript as E4 evidence."
echo

set +e
"$VERIFY"
VERIFY_RC=$?
set -e
if [[ "$VERIFY_RC" -ne 0 ]]; then
  echo
  echo "ABORT: required checks are not enforced yet (verify exit ${VERIFY_RC})."
  echo "Mahesh38 (admin) must run S08-G2-apply-required-checks.sh first."
  exit 1
fi

echo
echo "== PR mergeability snapshot =="
gh pr view "$PR" --repo "$REPO" --json number,mergeable,mergeStateStatus,statusCheckRollup \
  --jq '{number,mergeable,mergeStateStatus,checks:(.statusCheckRollup|map({name:.name,conclusion:.conclusion}))}' \
  | tee /tmp/s08-g2-pr-snap.json

echo
echo "== Attempt merge via API (expect failure while checks incomplete / ruleset active) =="
# Intentionally omit --admin; we want the ruleset to block.
set +e
MERGE_BODY=$(gh api -X PUT "repos/${REPO}/pulls/${PR}/merge" \
  -f merge_method=squash \
  -f commit_title="S08-G2 blocked-merge probe (should fail)" \
  2>&1)
MERGE_RC=$?
set -e

{
  echo "S08-G2 blocked-merge demo — ${STAMP}"
  echo "repo=${REPO} pr=${PR}"
  echo "verify_exit=0"
  echo "--- pr snapshot ---"
  cat /tmp/s08-g2-pr-snap.json
  echo
  echo "--- merge attempt ---"
  echo "exit=${MERGE_RC}"
  echo "$MERGE_BODY"
} | tee "$OUT"

if [[ "$MERGE_RC" -eq 0 ]]; then
  echo
  echo "UNEXPECTED: merge succeeded. Do NOT declare S08-G2 MET — ruleset may be bypassable."
  exit 2
fi

echo
echo "OK: merge refused (exit ${MERGE_RC}). Transcript: $OUT"
echo "Next: attach this file as E4 evidence and declare S08-G2 MET on human-review."
exit 0
