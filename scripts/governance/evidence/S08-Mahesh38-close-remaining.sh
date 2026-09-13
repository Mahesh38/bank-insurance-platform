#!/usr/bin/env bash
# S08 — one-shot closer for the two remaining HUMAN_REQUIRED criteria (G2 + G10).
# Actor: Mahesh38 (repo admin). Agent tokens get HTTP 403 on ruleset write.
#
# Usage:
#   export GH_TOKEN=<admin-pat-with-repo-admin>
#   ./scripts/governance/evidence/S08-Mahesh38-close-remaining.sh [pr-number]
#
# What it does:
#   1) apply required-status ruleset on main (S08-G2)
#   2) verify exit 0
#   3) blocked-merge demo against PR (default 104)
#   4) print exact G10 attestation copy/fill/commit steps
#
# Does NOT edit stage_status, gate PASSED, or T4 approvals.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PR="${1:-104}"
APPLY="$ROOT/scripts/governance/evidence/S08-G2-apply-required-checks.sh"
VERIFY="$ROOT/scripts/governance/evidence/S08-G2-verify-required-checks.sh"
DEMO="$ROOT/scripts/governance/evidence/S08-G2-demo-blocked-merge.sh"
TEMPLATE="$ROOT/scripts/governance/evidence/S08-G10-onboarding-attestation.TEMPLATE.md"
ATTEST="$ROOT/scripts/governance/evidence/S08-G10-onboarding-attestation.md"
STAMP="$(date -u +%Y-%m-%dT%H:%MZ)"

echo "=== S08 Mahesh38 one-shot closer — ${STAMP} — PR #${PR} ==="
echo "NOT a MET declaration by this script alone — human-review still required."
echo

if [[ -z "${GH_TOKEN:-${GITHUB_TOKEN:-}}" ]]; then
  echo "ERROR: export GH_TOKEN=<admin-pat-with-repo-admin> first." >&2
  exit 2
fi

for f in "$APPLY" "$VERIFY" "$DEMO" "$TEMPLATE"; do
  if [[ ! -f "$f" ]]; then
    echo "ERROR: missing $f" >&2
    exit 3
  fi
done

echo "--- [1/3] Apply required checks on main ---"
bash "$APPLY"

echo
echo "--- [2/3] Verify required checks ---"
if ! bash "$VERIFY"; then
  echo "ERROR: verify failed — do not declare S08-G2 MET." >&2
  exit 1
fi

echo
echo "--- [3/3] Blocked-merge demo (PR #${PR}) ---"
if ! bash "$DEMO" "$PR"; then
  echo "ERROR: blocked-merge demo failed — investigate before MET." >&2
  exit 1
fi

echo
echo "=== S08-G2 mechanical steps DONE ==="
echo "Next (human-review): declare S08-G2 MET with verify transcript +"
echo "  scripts/governance/evidence/S08-G2-blocked-merge.out"
echo
echo "=== S08-G10 remaining (attestation) ==="
if [[ -f "$ATTEST" ]]; then
  echo "Found existing $ATTEST — review, ensure blanks filled, commit if needed."
else
  echo "Copy and fill attestation, then commit:"
  echo "  cp $TEMPLATE $ATTEST"
  echo "  \$EDITOR $ATTEST"
  echo "  git add $ATTEST && git commit -m 'docs(S08): G10 onboarding attestation' && git push"
fi
echo "Then declare S08-G10 MET on human-review."
echo
echo "Never edit stage_status / gate PASSED / T4 approvals from this script."
