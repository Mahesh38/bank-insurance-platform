#!/usr/bin/env bash
# GLM-001 M5.2 — the split push. REHEARSED at M2.7 (18/18 checks); this is the
# real run.
#
# ============================ HARD PRECONDITIONS ============================
# This script REFUSES to run until every one of these is satisfied. They are not
# advisory and they are not ordered by preference.
#
#   1. C-SEC-1  clean full-history secret scan
#   2. C-CMP-1  data residency confirmed permissible by Board 6
#   3. RISK-024 the source clone is NOT shallow
#   4. C-OPS-1  pre-gitlab-migration anchor present ON THE REMOTE
#   5. Finding B rotated or formally retired (RISK-026)
# ===========================================================================
set -euo pipefail

SRC="${SRC:?SRC must point at a full, non-shallow clone of the origin}"
fail=0
gate() { if eval "$2"; then printf '  [OK]   %s\n' "$1"; else printf '  [STOP] %s\n' "$1"; fail=1; fi; }

echo "== Preconditions =="
gate "source clone is not shallow (RISK-024)" \
     '[ "$(git -C "$SRC" rev-parse --is-shallow-repository)" = "false" ]'
gate "rollback anchor exists on the remote (C-OPS-1, RISK-025)" \
     'git -C "$SRC" ls-remote --tags origin refs/tags/pre-gitlab-migration | grep -q .'
gate "C-SEC-1 sign-off file present" '[ -f "${EVIDENCE_DIR:-./evidence}/C-SEC-1.signed" ]'
gate "C-CMP-1 sign-off file present" '[ -f "${EVIDENCE_DIR:-./evidence}/C-CMP-1.signed" ]'
gate "finding B disposition recorded (RISK-026)" '[ -f "${EVIDENCE_DIR:-./evidence}/finding-B.resolved" ]'

if [ "$fail" -ne 0 ]; then
  echo
  echo "REFUSING TO RUN. One or more preconditions are unmet."
  echo "These gate the first push into the bank estate. Do not work around them."
  exit 1
fi

echo
echo "All preconditions met. Split definitions (verified at M2.7):"
echo "  frontend             -> apps/rm-workspace-app/"
echo "  backend              -> services/ libs/ config/ gradle* Dockerfile docker-compose* render.yaml"
echo "  platform-governance  -> docs/ scripts/ AGENTS.md CLAUDE.md .claude/"
echo
echo "Run git filter-repo per split, then verify with M2.7's tree-hash comparison"
echo "BEFORE pushing. Content identity is proven by tree hash, never by inspection."
