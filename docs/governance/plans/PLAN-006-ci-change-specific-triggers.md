# PLAN-006 — Change-specific CI triggers

```yaml
# schema: implementation-plan
id: PLAN-006
work_item: INFRA-001
origin: SUG-20260930-cif
workstream: WS-3
risk_tier: T2
author: "agent:cursor-grok"
date: "2026-09-30"

objective: >
  A documentation-only change no longer compiles Java, runs CodeQL, or builds
  container images; each CI job runs only for the files it protects; heavy
  image/SBOM work runs before merge (non-draft PR / merge_group / main /
  schedule), not on every feature-branch push.

problem: >
  application-ci.yml and security-scanning.yml trigger on every pull_request and
  on push to every branch, with no in-job classification. A docs-only commit
  therefore runs the full Gradle suite, CodeQL Java (forced recompile), Trivy
  SCA, three container image builds, and SBOM generation. Push plus pull_request
  on an open PR doubles that. This is observed toil, not a missing control:
  S08-G1/G2/G5 remain MET, and S08-G9 measured Application CI only.

proposed_solution: >
  Keep workflow-level paths filters OFF on required checks (T-F01 / A-F05).
  Add scripts/ci/classify-changes.py. Required jobs always report the same
  check names; they short-circuit to success when the diff is irrelevant.
  Drop feature-branch push (retain pull_request, merge_group, push to main,
  schedule, workflow_dispatch). Gitleaks still runs on every PR including docs.
  CodeQL runs only when Java/Gradle that CodeQL would analyse changed. SCA/SBOM
  run on dependency manifests. Image scan runs only for the Phase-1 images the
  diff can change, and only on heavy events (non-draft PR, merge_group, main,
  schedule, dispatch). Gradle is module-scoped unless libs/ or root build files
  fan out.

alternatives:
  - option: "Restore workflow-level paths: filters on pull_request"
    rejected_because: "T-F01 / A-F05 — a skipped required check never reports; S08-G2 breaks."
  - option: "Keep push to every branch and only add paths filters"
    rejected_because: "The double-run (push + pull_request) is the largest waste; path filters alone do not remove it."
  - option: "Move CodeQL/SCA off the PR and onto schedule only"
    rejected_because: "Would weaken S08-G5 on Java/dependency PRs; Deepali's fail-closed gate must still bite before merge when the analysed artefact changed."

affected_components:
  - GitHub Actions Application CI
  - GitHub Actions Security Scanning
  - in-repo change classifier

files_expected:
  - scripts/ci/classify-changes.py
  - scripts/ci/test_classify_changes.py
  - .github/workflows/application-ci.yml
  - .github/workflows/security-scanning.yml
  - docs/governance/plans/PLAN-006-ci-change-specific-triggers.md
  - docs/governance/registers/SUGGESTION-REGISTER.md

data_changes: none
api_changes: none
security_impact: none
compliance_impact: none
backward_compatibility: compatible
performance_impact: "docs-only PRs drop from a full Java+image suite to classify + gitleaks; single-service PRs compile one module"
operational_impact: "CI trigger precision; runner minutes; required check names unchanged for S08-G2"

testing:
  unit:
    - "scripts/ci/test_classify_changes.py — docs-only, single module, libs fan-out, Dockerfile, lockfile, workflow, force-all"
  integration: []
  e2e: []
  other:
    - "Classifier invoked at the start of Application CI and Security Scanning"
    - "This PR exercises the new paths (workflow + docs + classifier)"

rollback: >
  Revert the branch. No runtime data, secrets, or production configuration change.
  Branch protection check names are unchanged, so revert does not require a ruleset edit.

dependencies: []
assumptions:
  - "Branch protection still requires the four named checks; this plan does not rename them."
  - "Image scan and SBOM are not required status checks today; job-level skip is therefore T-F01-safe."
risks:
  - risk: "Concurrency cancel-in-progress makes classify cancelled; fail-closed steps that treat any non-success as T-F01 failure red required checks on the superseded run (PR #130, 6bdfca8)."
    mitigation: "Required jobs fail-closed only when classify result is failure. cancelled/skipped no-op; successor run is the reporter. Image-scan/SBOM require classify success before expanding the matrix."
  - risk: "CodeQL or image scan omitted on a Java change."
    mitigation: "Classifier tests pin docs-only skip AND Java/persistence image mapping; libs/ and root Gradle fan out to full."
  - risk: "Module-scoped Gradle misses a shared-lib regression."
    mitigation: "Any libs/ or root Gradle/settings/wrapper/config change forces scope=all."

acceptance_criteria:
  - "A docs-only file list classifies run_codeql=false, run_image_scan=false, run_tests=false."
  - "A Java change under one service classifies module-scoped Gradle and CodeQL; unrelated Phase-1 images are not scanned."
  - "Required check names remain Java 21 tests and coverage gates, Secret scanning (gitleaks), SAST (CodeQL, Java), SCA (Trivy dependency scan)."
  - "Feature-branch push no longer triggers Application CI or Security Scanning; pull_request, merge_group, push to main, schedule and workflow_dispatch remain."
  - "Gitleaks still runs on documentation PRs."

out_of_scope:
  - "Renaming or removing required status checks"
  - "Adding image scans for every service Dockerfile"
  - "GitLab CI migration"
  - "Changing gitleaks, CodeQL query pack, Trivy version or fail-closed severity"
  - "Editing CURRENT-STATE.yaml stage fields or marking GATE-S08 PASSED"

estimate: S
reviews: []
variance_log: []
```
