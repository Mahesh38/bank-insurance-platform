#!/usr/bin/env bash
# GLM-001 M5.2 — orphan import into empty GitLab projects (CR-017 / ADR-020 / AC-6).
#
# This script does NOT preserve personal-forge history. It copies the current
# tree, path-split, into new orphan repositories and makes one company-authored
# commit per project. git filter-repo history push is forbidden here.
#
# ============================ HARD PRECONDITIONS ============================
#   1. C-SEC-1  source full-history secret scan executed (Finding B known)
#   2. C-CMP-1  data residency confirmed permissible by Board 6
#   3. RISK-024 the source clone is NOT shallow
#   4. C-OPS-1  pre-gitlab-migration anchor present ON THE REMOTE
#   5. Finding B rotated or formally retired (RISK-026 / C-SEC-2)
#   6. COMPANY_GIT_NAME + COMPANY_GIT_EMAIL set to a non-personal identity
#   7. identity-guard.py exits 0 on each orphan repo before any push
# ===========================================================================
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
GUARD="$HERE/identity-guard.py"
SRC="${SRC:?SRC must point at a full, non-shallow clone of the origin}"
OUT="${OUT:?OUT is the directory that will hold orphan clones (not the GitLab remote)}"
SEALED="${SEALED:-}"
PUSH="${PUSH:-0}"
REHEARSE="${REHEARSE:-0}"

fail=0
gate() { if eval "$2"; then printf '  [OK]   %s\n' "$1"; else printf '  [STOP] %s\n' "$1"; fail=1; fi; }

if [ "$REHEARSE" = "1" ] && [ "$PUSH" = "1" ]; then
  echo "REFUSING TO RUN. REHEARSE=1 is M2.7 throwaway clones; it cannot PUSH=1."
  exit 1
fi

echo "== Preconditions =="
gate "source clone is not shallow (RISK-024)" \
     '[ "$(git -C "$SRC" rev-parse --is-shallow-repository)" = "false" ]'
gate "COMPANY_GIT_NAME set" '[ -n "${COMPANY_GIT_NAME:-}" ]'
gate "COMPANY_GIT_EMAIL set" '[ -n "${COMPANY_GIT_EMAIL:-}" ]'
gate "identity-guard.py present" '[ -f "$GUARD" ]'

if [ "$REHEARSE" = "1" ]; then
  echo "  [INFO] REHEARSE=1 — M2.7 throwaway; signed gates and remote tag not required"
else
  gate "rollback anchor exists on the remote (C-OPS-1, RISK-025)" \
       'git -C "$SRC" ls-remote --tags origin refs/tags/pre-gitlab-migration | grep -q .'
  gate "C-SEC-1 sign-off file present" '[ -f "${EVIDENCE_DIR:-./evidence}/C-SEC-1.signed" ]'
  gate "C-CMP-1 sign-off file present" '[ -f "${EVIDENCE_DIR:-./evidence}/C-CMP-1.signed" ]'
  gate "finding B disposition recorded (RISK-026)" '[ -f "${EVIDENCE_DIR:-./evidence}/finding-B.resolved" ]'
fi

if [ "$fail" -ne 0 ]; then
  echo
  echo "REFUSING TO RUN. One or more preconditions are unmet."
  echo "These gate the first push into the bank estate. Do not work around them."
  exit 1
fi

echo
echo "== Company identity (AC-6) =="
if ! python3 "$GUARD" --company-identity; then
  echo "REFUSING TO RUN. COMPANY_GIT_NAME / COMPANY_GIT_EMAIL failed the denylist."
  exit 1
fi

mkdir -p "$OUT"

if [ -n "$SEALED" ]; then
  echo
  echo "== AC-8 sealed bundle (offline; never a GitLab remote) =="
  mkdir -p "$(dirname "$SEALED")"
  git -C "$SRC" bundle create "$SEALED" --all
  echo "  wrote $SEALED"
fi

copy_paths() {
  local dest="$1"
  shift
  mkdir -p "$dest"
  local item
  for item in "$@"; do
    local src_item="$SRC/$item"
    if [ -e "$src_item" ]; then
      mkdir -p "$dest/$(dirname "$item")"
      cp -a "$src_item" "$dest/$item"
    fi
  done
}

sanitize_tree() {
  local dest="$1"
  # Provenance lines — role only, no personal login or Gmail (C-CMP-2).
  local f
  for f in \
    "$dest/docs/au-bank-insurance-platform/references/2026-08-20-insurance-aggregation-and-provider-connectivity-notes.md" \
    "$dest/docs/au-bank-insurance-platform/references/2026-08-20-north-star-architecture-brainstorming-notes.md"
  do
    if [ -f "$f" ]; then
      # Strip any remaining parenthetical personal identity on the provenance line.
      sed -i 's/^\(\*\*Provided by:\*\* Repository owner\)(.*)$/\1/' "$f"
    fi
  done
  rm -rf "$dest/.github" \
         "$dest/docs/platform/gitlab-migration/m2-evidence"
}

orphan_commit() {
  local name="$1"
  local dest="$OUT/$name"
  git -C "$dest" init -b main
  git -C "$dest" add -A
  if git -C "$dest" diff --cached --quiet; then
    echo "  [STOP] $name tree is empty — refusing an empty import"
    return 1
  fi
  if ! python3 "$GUARD" --tree "$dest"; then
    echo "  [STOP] $name tree failed identity-guard"
    return 1
  fi
  GIT_AUTHOR_NAME="$COMPANY_GIT_NAME" \
  GIT_AUTHOR_EMAIL="$COMPANY_GIT_EMAIL" \
  GIT_COMMITTER_NAME="$COMPANY_GIT_NAME" \
  GIT_COMMITTER_EMAIL="$COMPANY_GIT_EMAIL" \
  git -C "$dest" -c commit.gpgsign=false commit -m "Initial import of the bank insurance platform"
  if ! python3 "$GUARD" --git "$dest"; then
    echo "  [STOP] $name commit failed identity-guard"
    return 1
  fi
  local count
  count="$(git -C "$dest" rev-list --count HEAD)"
  if [ "$count" != "1" ]; then
    echo "  [STOP] $name has $count commits — orphan import must be exactly one"
    return 1
  fi
  echo "  [OK]   $name orphan commit $(git -C "$dest" rev-parse --short HEAD)"
}

echo
echo "== Orphan splits (tree only; no history) =="

rm -rf "$OUT/frontend" "$OUT/backend" "$OUT/platform-governance"

echo "  frontend <- apps/rm-workspace-app/"
copy_paths "$OUT/frontend" "apps/rm-workspace-app"
orphan_commit frontend

echo "  backend <- services/ libs/ config/ gradle* Dockerfile docker-compose* render.yaml settings.gradle*"
# shellcheck disable=SC2046
copy_paths "$OUT/backend" \
  services libs config Dockerfile docker-compose.yml docker-compose.yaml render.yaml \
  settings.gradle.kts settings.gradle build.gradle.kts build.gradle gradle.properties \
  gradlew gradlew.bat gradle
orphan_commit backend

echo "  platform-governance <- docs/ scripts/ AGENTS.md CLAUDE.md .claude/"
copy_paths "$OUT/platform-governance" docs scripts AGENTS.md CLAUDE.md .claude
sanitize_tree "$OUT/platform-governance"
orphan_commit platform-governance

echo
echo "Orphan imports written under $OUT"
echo "  frontend             $(git -C "$OUT/frontend" rev-parse HEAD)"
echo "  backend              $(git -C "$OUT/backend" rev-parse HEAD)"
echo "  platform-governance  $(git -C "$OUT/platform-governance" rev-parse HEAD)"
echo
echo "History was NOT copied. filter-repo push is not this script."
echo "Git-object sync with the personal GitHub is forbidden (AC-7)."

if [ "$PUSH" = "1" ]; then
  : "${GITLAB_FRONTEND_URL:?GITLAB_FRONTEND_URL required when PUSH=1}"
  : "${GITLAB_BACKEND_URL:?GITLAB_BACKEND_URL required when PUSH=1}"
  : "${GITLAB_GOVERNANCE_URL:?GITLAB_GOVERNANCE_URL required when PUSH=1}"
  echo
  echo "== Push (PUSH=1) =="
  git -C "$OUT/frontend" remote add origin "$GITLAB_FRONTEND_URL"
  git -C "$OUT/backend" remote add origin "$GITLAB_BACKEND_URL"
  git -C "$OUT/platform-governance" remote add origin "$GITLAB_GOVERNANCE_URL"
  git -C "$OUT/frontend" push -u origin main
  git -C "$OUT/backend" push -u origin main
  git -C "$OUT/platform-governance" push -u origin main
else
  echo
  echo "PUSH is not 1 — orphan repos stay local. Inspect, then re-run with PUSH=1."
fi
