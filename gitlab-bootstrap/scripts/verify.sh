#!/usr/bin/env bash
# Post-apply verification against the live instance. Read-only: this script
# never writes to GitLab. Produces the acceptance evidence for GLM-001 M4.5,
# M6.10 and M9.1.
set -euo pipefail
cd "$(dirname "$0")/.."

: "${GITLAB_TOKEN:?GITLAB_TOKEN must be set — the dedicated automation account (C-SEC-5)}"

fail=0
ok()  { printf '  [PASS] %s\n' "$1"; }
bad() { printf '  [FAIL] %s\n' "$1"; fail=1; }

echo "== Convergence: a second plan must show no changes =="
if terraform plan -detailed-exitcode -input=false >/tmp/verify-plan.out 2>&1; then
  ok "plan is empty — configuration has converged"
else
  case $? in
    2) bad "plan proposes changes — not converged (baseline 14: idempotent)";;
    *) bad "plan errored — see /tmp/verify-plan.out";;
  esac
fi

echo "== No destructive change is ever proposed =="
grep -qE '^\s*[-~]\s|will be destroyed' /tmp/verify-plan.out 2>/dev/null \
  && bad "plan contains destroy or replace — STOP and investigate" \
  || ok "no destroy or replace in plan"

echo "== Project count matches the AC-2 gate =="
want=$(terraform output -raw project_count 2>/dev/null || echo "?")
gov=$(terraform output -raw platform_governance_created 2>/dev/null || echo "?")
echo "  projects managed: $want (platform-governance created: $gov)"
[ "$gov" = "false" ] && [ "$want" = "8" ] && ok "eight projects, AC-2 gate holding"
[ "$gov" = "true" ]  && [ "$want" = "9" ] && ok "nine projects, Appendix C exception recorded"

echo "== Controls this instance cannot enforce (CR-016) =="
terraform output -json unavailable_controls 2>/dev/null \
  | python3 -c 'import json,sys; [print("  -",c) for c in json.load(sys.stdin)]' 2>/dev/null \
  || echo "  (no output yet)"

printf '\n'
[ "$fail" -eq 0 ] && { echo "verify.sh: PASS"; exit 0; } || { echo "verify.sh: FAIL"; exit 1; }
