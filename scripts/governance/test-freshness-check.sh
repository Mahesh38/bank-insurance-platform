#!/usr/bin/env bash
# Fixture tests for FreshnessCheck.
#
# The checker reads YAML with a hand-written subset parser, so the property that
# actually matters is FAIL CLOSED: anything it cannot fully understand must exit 2,
# never pass quietly. These fixtures assert that, plus every CS-1 halt condition.
#
# Run: bash scripts/governance/test-freshness-check.sh
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CHECK="$ROOT/scripts/governance/FreshnessCheck.java"
TODAY="2026-08-10"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pass=0; fail=0

# valid baseline the mutations start from
read -r -d '' VALID <<'YAML'
schema_version: "1.0"
project:
  name: "Test Project"
state_as_of: "2026-08-09"
review_due: "2026-09-09"
ratified_by: "PO + Architect"
provisional: false
workstreams:
  - id: WS-1
    name: "Test workstream"
    lifecycle:
      current_phase: "Phase 4"
      stage_status: IN_PROGRESS
    current_objective:
      description: >
        Prove the checker's fail-closed behaviour with the SSOT's own quoting quirks.
    current_scope:
      in_scope: ["a", "b"]
      out_of_scope:
        - item: "c"
          revisit_at: "later"
    current_gate:
      state: OPEN
      exit_criteria:
        - id: "4.1"
          criterion: "something"
          state: OPEN
standing_constraints: ["never do X"]
routing:
  FUNC: ["backlog.md"]
id_allocation:
  collision_resistant: [SUG, DEP]
  pattern: "^(SUG|DEP)-[0-9]{8}-[0-9a-z]{3}$"
  sequential:
    RISK: 1
YAML

run_case() {  # name, expected_exit, yaml, [register content]
  local name="$1" want="$2" yaml="$3"
  local dir="$TMP/$(echo "$name" | tr -c 'a-zA-Z0-9' '_')"
  mkdir -p "$dir/docs/governance/state" "$dir/docs/governance/registers"
  printf '%s\n' "$yaml" > "$dir/docs/governance/state/CURRENT-STATE.yaml"
  # seed every tracked artefact, or cases warn for reasons unrelated to the fixture
  for f in 01-CURRENT_STATE.md 04-STAGE_GATES.md 02-PROJECT_SCOPE.md; do
    : > "$dir/docs/governance/$f"
  done
  for f in SUGGESTION-REGISTER PARKED-BACKLOG DEPENDENCY-REGISTER ASSUMPTION-REGISTER; do
    : > "$dir/docs/governance/registers/$f.md"
  done
  # register content, when a case supplies it, goes in exactly one file — writing it
  # to all of them would itself be a duplicate-ID collision
  printf '%s\n' "${4:-}" > "$dir/docs/governance/registers/RISK-REGISTER.md"
  mkdir -p "$dir/docs/1sb-insurance-integration/service-ssot"
  : > "$dir/docs/1sb-insurance-integration/service-ssot/TECH-DEBT.md"
  java "$CHECK" --root "$dir" --today "$TODAY" --quiet --no-color >/dev/null 2>&1
  local got=$?
  if [ "$got" -eq "$want" ]; then
    printf '  ok    %-52s exit %d\n' "$name" "$got"; pass=$((pass+1))
  else
    printf '  FAIL  %-52s exit %d (want %d)\n' "$name" "$got" "$want"; fail=$((fail+1))
  fi
}

echo "FreshnessCheck fixture tests (today=$TODAY)"
echo
echo "  -- must PASS or WARN --"
# 1 = warn only (fixtures have no git history, so artefact ages fall back to mtime = today)
run_case "valid document"                        0 "$VALID"
run_case "apostrophe in folded block scalar"     0 "$VALID"

echo
echo "  -- CS-1 halts (exit 2) --"
run_case "state_as_of missing"        2 "$(printf '%s\n' "$VALID" | grep -v '^state_as_of')"
run_case "state_as_of unparseable"    2 "$(printf '%s\n' "$VALID" | sed 's/^state_as_of.*/state_as_of: "not-a-date"/')"
run_case "review_due missing"         2 "$(printf '%s\n' "$VALID" | grep -v '^review_due')"
run_case "review_due unparseable"     2 "$(printf '%s\n' "$VALID" | sed 's/^review_due.*/review_due: soon/')"
run_case "review_due in the past"     2 "$(printf '%s\n' "$VALID" | sed 's/^review_due.*/review_due: "2026-08-01"/')"
run_case "state_as_of over 30 days"   2 "$(printf '%s\n' "$VALID" | sed 's/^state_as_of.*/state_as_of: "2026-06-01"/')"
run_case "workstreams missing"        2 "$(printf '%s\n' "$VALID" | sed '/^workstreams:/,/^standing_constraints/d')"
run_case "workstream without lifecycle" 2 "$(printf '%s\n' "$VALID" | sed '/^    lifecycle:/,/      stage_status: IN_PROGRESS/d')"
run_case "gate without exit criteria" 2 "$(printf '%s\n' "$VALID" | sed '/^      exit_criteria:/,/          state: OPEN/d')"
run_case "criterion without state"    2 "$(printf '%s\n' "$VALID" | sed '/^          state: OPEN$/d')"
run_case "project.name missing"       2 "$(printf '%s\n' "$VALID" | sed '/^  name: "Test Project"/d')"

echo
echo "  -- fail closed on constructs the parser does not support --"
run_case "tab indentation"            2 "$(printf '%s\n' "$VALID" | sed 's/^  name: "Test Project"/\tname: "Test Project"/')"
run_case "yaml anchor"                2 "$(printf '%s\n' "$VALID" | sed 's/^standing_constraints.*/standing_constraints: \&anchor ["x"]/')"
run_case "yaml alias"                 2 "$(printf '%s\n' "$VALID"; printf 'extra: *anchor\n')"
run_case "multi-document"             2 "$(printf '%s\n---\nother: 1\n' "$VALID")"
run_case "flow mapping"               2 "$(printf '%s\n' "$VALID" | sed 's/^routing:/routing: {FUNC: [x]}/')"
run_case "unterminated quote"         2 "$(printf '%s\n' "$VALID" | sed 's/^  name: "Test Project"/  name: "Test Project/')"
run_case "not a mapping at top level" 2 "- just
- a list"
run_case "empty file"                 2 ""

echo
echo "  -- ID allocation and uniqueness --"
run_case "counter behind its register" 2 \
  "$(printf '%s\n' "$VALID" | sed 's/^    RISK: 1/    RISK: 3/')" \
  "| RISK-005 | already in use |"
run_case "counter ahead of its register" 0 \
  "$(printf '%s\n' "$VALID" | sed 's/^    RISK: 1/    RISK: 6/')" \
  "| RISK-005 | already in use |"
run_case "duplicate ID definition in one register" 2 "$VALID" \
  "| RISK-005 | first definition |
| RISK-005 | second definition |"

echo
echo "$pass passed, $fail failed"
[ "$fail" -eq 0 ]
