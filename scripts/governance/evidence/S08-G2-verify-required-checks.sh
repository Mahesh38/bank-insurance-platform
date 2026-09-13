#!/usr/bin/env bash
# S08-G2 — read-only verifier: are the four required checks enforced on main?
# Safe for agents (GET only). Exit 0 = configured; exit 1 = not yet (criterion stays OPEN).
set -euo pipefail

REPO="${REPO:-Mahesh38/bank-insurance-platform}"
export REPO

python3 <<'PY'
import json, os, subprocess, sys, datetime

REPO = os.environ["REPO"]
REQUIRED = [
    "Java 21 tests and coverage gates",
    "Secret scanning (gitleaks)",
    "SAST (CodeQL, Java)",
    "SCA (Trivy dependency scan)",
]

def gh_json(args):
    try:
        out = subprocess.check_output(["gh", "api", *args], stderr=subprocess.STDOUT, text=True)
        return json.loads(out)
    except subprocess.CalledProcessError as e:
        return {"_error": e.output.strip() if e.output else str(e)}

print(f"S08-G2 verify — {datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%MZ')} — repo {REPO}")
print("NOT a MET declaration — prints configuration gaps only.")
print()

print("== Classic branch protection on main ==")
prot = gh_json([f"repos/{REPO}/branches/main/protection"])
if "_error" in prot:
    print("GET branches/main/protection → not readable (likely HTTP 403)")
else:
    rsc = prot.get("required_status_checks") or {}
    contexts = rsc.get("contexts") or []
    checks = rsc.get("checks") or []
    names = list(contexts) + [c.get("context") for c in checks if isinstance(c, dict)]
    print("contexts:", names or "(none)")
print()

print("== Repository rulesets ==")
rulesets = gh_json([f"repos/{REPO}/rulesets"])
if isinstance(rulesets, dict) and "_error" in rulesets:
    print("ERROR listing rulesets:", rulesets["_error"])
    sys.exit(2)

active_required = []
has_active_required_ruleset = False
for rs in rulesets:
    rid = rs["id"]
    detail = gh_json([f"repos/{REPO}/rulesets/{rid}"])
    enf = detail.get("enforcement", rs.get("enforcement"))
    name = detail.get("name", rs.get("name"))
    print(f"— ruleset {rid} '{name}' enforcement={enf}")
    ctxs = []
    for rule in detail.get("rules") or []:
        if rule.get("type") == "required_status_checks":
            for c in (rule.get("parameters") or {}).get("required_status_checks") or []:
                ctx = c.get("context") or ""
                if ctx:
                    ctxs.append(ctx)
    for c in ctxs:
        print(f"    check: {c}")
    if enf == "active" and ctxs:
        has_active_required_ruleset = True
        active_required.extend(ctxs)

print()
print("== Required four vs active rulesets ==")
missing = []
for need in REQUIRED:
    if need in active_required:
        print(f"OK   {need}")
    else:
        print(f"MISS {need}")
        missing.append(need)

print()
if not has_active_required_ruleset:
    print("RESULT: no active ruleset with required_status_checks — S08-G2 remains OPEN.")
    sys.exit(1)
if missing:
    print("RESULT: active ruleset present but missing required check(s) — S08-G2 remains OPEN.")
    sys.exit(1)
print("RESULT: four required checks appear enforced via an active ruleset.")
print("Still need blocked-merge demo + human-review MET declaration (not performed by this script).")
sys.exit(0)
PY
