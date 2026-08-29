#!/usr/bin/env bash
# Local validation. Runs everything the bootstrap pipeline runs, except plan.
# Requires no credentials and reaches no network beyond provider download.
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0
step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }
ok()   { printf '  [PASS] %s\n' "$1"; }
bad()  { printf '  [FAIL] %s\n' "$1"; fail=1; }

step "Preconditions"
command -v terraform >/dev/null 2>&1 && ok "terraform present" || { bad "terraform not installed"; exit 1; }

step "backend.tf must not exist until M1.6 and SEC-F07 resolve"
if [ -f backend.tf ]; then
  bad "backend.tf exists — read backend.tf.deferred before adding it (RISK-027)"
else
  ok "backend.tf absent, as intended"
fi

step "No secret may be committed in a tfvars file (C-SEC-9, baseline 4.5)"
if compgen -G "*.tfvars" >/dev/null || compgen -G "*.auto.tfvars" >/dev/null; then
  bad "tfvars file present — secrets must never reach Terraform variables"
else
  ok "no tfvars files"
fi
if grep -rIlE '(token|password|secret)[[:space:]]*=[[:space:]]*"[^"$]{8,}"' --include='*.tf' . >/dev/null 2>&1; then
  bad "literal credential-shaped assignment found in HCL"
else
  ok "no literal credential assignment in HCL"
fi

step "terraform fmt"
terraform fmt -check -recursive && ok "formatting clean" || bad "run: terraform fmt -recursive"

step "terraform validate"
terraform init -backend=false -input=false >/dev/null && ok "init (no backend)" || bad "init failed"
terraform validate && ok "configuration valid" || bad "validate failed"

step "prevent_destroy guards present (baseline 11.2)"
for m in gitlab-group gitlab-project; do
  grep -q "prevent_destroy = true" "modules/$m/main.tf" \
    && ok "$m guarded" || bad "$m missing prevent_destroy"
done

step "Parent group is read, never created (IMP-11 #3)"
grep -q 'data "gitlab_group" "insurance"' data.tf \
  && ok "parent group is a data source" || bad "parent group is not a data source"
grep -qE 'resource "gitlab_group" "insurance"' ./*.tf 2>/dev/null \
  && bad "parent group appears as a managed resource" || ok "parent group not managed"

printf '\n'
[ "$fail" -eq 0 ] && { echo "validate.sh: PASS"; exit 0; } || { echo "validate.sh: FAIL"; exit 1; }
