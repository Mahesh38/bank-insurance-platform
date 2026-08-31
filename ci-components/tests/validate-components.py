#!/usr/bin/env python3
"""Structural tests for the CI components. GLM-001 M7.8.

These are not a substitute for running the components — nothing here has been
executed against a GitLab instance. What they do catch is the class of error a
reader will not: a component whose gating job quietly allows failure, an
artifact with a default expiry where retention is supposed to be a decision, or
YAML that does not parse because GitLab's `$[[ inputs.x ]]` interpolation was
left unquoted inside a flow sequence.
"""
import sys, glob, yaml, os

# Jobs that gate a stage criterion. Each MUST carry allow_failure: false —
# IMP-4 / C-ENG-3: a job that does not report cannot gate, and one that reports
# green on failure is worse than no job at all.
GATING = {
    "java-test", "java-build", "secret-detection", "secret-detection-history",
    "sast", "dependency-scan", "container-scan", "sbom", "docker-build",
    "flutter-analyze", "flutter-test", "node-build", "node-test",
    "contract-validate", "contract-compatibility", "terraform-plan",
    "gitops-promotion",
}
# terraform-apply is deliberately NOT gating: it is manual-and-main-only, and
# its rules carry allow_failure: false at the rule level instead.

fails = []
def check(ok, msg):
    print(f"  [{'PASS' if ok else 'FAIL'}] {msg}")
    if not ok:
        fails.append(msg)

files = sorted(glob.glob("templates/*.yml"))
check(len(files) >= 15, f"at least 15 components present (found {len(files)})")

for path in files:
    name = os.path.basename(path)
    raw = open(path, encoding="utf-8").read()

    try:
        docs = list(yaml.safe_load_all(raw))
    except yaml.YAMLError as e:
        check(False, f"{name}: YAML parses ({e.__class__.__name__})")
        continue

    check(len(docs) == 2, f"{name}: has a spec header and a pipeline body")
    if len(docs) != 2:
        continue

    spec, body = docs
    check(isinstance(spec, dict) and "inputs" in (spec.get("spec") or {}),
          f"{name}: declares spec:inputs")

    jobs = {k: v for k, v in body.items()
            if isinstance(v, dict) and not k.startswith(".")
            and k not in ("stages", "variables", "default")}
    check(bool(jobs), f"{name}: defines at least one job")

    for job, cfg in jobs.items():
        if job in GATING:
            has = cfg.get("allow_failure")
            rules_ok = any(r.get("allow_failure") is False
                           for r in cfg.get("rules", []) if isinstance(r, dict))
            check(has is False or rules_ok,
                  f"{name}: gating job '{job}' sets allow_failure: false")

        # C-CMP-3 — retention is a decision, not a default.
        art = cfg.get("artifacts")
        if isinstance(art, dict) and art.get("paths"):
            check("expire_in" in art,
                  f"{name}: job '{job}' sets an explicit artifact expiry")

# terraform-apply must never be automatic, and never from an MR pipeline.
apply_body = list(yaml.safe_load_all(open("templates/terraform-apply.yml", encoding="utf-8")))[1]
rules = apply_body["terraform-apply"]["rules"]
check(all(r.get("when") == "manual" for r in rules),
      "terraform-apply: every rule is when: manual (baseline 11.1)")
check(all("CI_DEFAULT_BRANCH" in r.get("if", "") for r in rules),
      "terraform-apply: default branch only, never an MR pipeline")
check("environment" not in apply_body["terraform-apply"],
      "terraform-apply: declares no environment block (CR-016 — no decorative gate)")

# The three C-ENG-2 mechanisms must be asserted by java-test, not assumed.
jt = open("templates/java-test.yml", encoding="utf-8").read()
for token, mech in [("jacoco", "coverage verification"),
                    ("ArchUnit", "ArchUnit rules"),
                    ("PII", "no-PII-in-logs test")]:
    check(token.lower() in jt.lower(), f"java-test: asserts {mech} ran (C-ENG-2)")

print()
if fails:
    print(f"FAILED — {len(fails)} problem(s)")
    sys.exit(1)
print("PASS — all component structure checks")
