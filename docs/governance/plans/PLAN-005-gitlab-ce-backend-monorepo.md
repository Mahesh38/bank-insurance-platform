# PLAN-005 — GitLab CE backend monorepo cutover plan (docs)

```yaml
# schema: implementation-plan
id: PLAN-005
work_item: ADR-020
origin: SUG-20260909-glc
workstream: WS-3
risk_tier: T3
author: "agent:cursor-grok (persona: Mahesh)"
date: "2026-09-09"

objective: >
  The GitLab CE cutover plan matches the Insurance group already created: one Gradle
  backend project (nip-backend), frontend left to nip-app, no GitHub integration,
  and the previous per-service GitLab project list is parked.

problem: >
  GITLAB-REPO-STRUCTURE.md recommended one GitLab project per Gradle module at
  cutover. The bank GitLab is CE; most services are skeletons; GATE-S08-G1 needs
  one pipeline that builds every module; shared libs are project() dependencies.
  A 20-project split now would fail S08 and is hard to undo. The owner already
  created platform/nip-governance, frontend/nip-app, backend/nip-backend.
  Evidence: GITLAB-REPO-STRUCTURE.md (old hierarchy); ADR-019 deferred split;
  backend-service-catalog.yaml status skeleton vs implemented; application-ci.yml
  S08-G1/G2 comments; SUG-20260825-arb (GitLab CI/CD).

proposed_solution: >
  Rewrite GITLAB-REPO-STRUCTURE.md for H0: team-split polyrepo + Gradle monorepo
  in nip-backend; air-gapped git bundle procedure; nip-governance = CI templates
  only. Draft ADR-020 as Proposed. Park per-service extract. Owner sequencing
  (SUG-20260910-w1s): Wave 1 = all libs + bank-persistence + 1sb-integration;
  later services copy into the same project when mature. Do not push to GitLab,
  do not add .gitlab-ci.yml (DevOps), do not split the GitHub tree, do not claim T4.

alternatives:
  - option: "Keep the per-service GitLab project map and create those projects now"
    rejected_because: >
      X3/X5/X6 fail at S08; CE lacks MR dependencies and required CODEOWNERS;
      Maven publishing of bank-common-* is a new programme.
  - option: "Only answer in chat; leave GITLAB-REPO-STRUCTURE.md recommending 20 repos"
    rejected_because: "The next agent or DevOps would create the wrong projects."
  - option: "Add a production .gitlab-ci.yml in this change"
    rejected_because: "Shivanshi owns CI/CD platform mechanics; the user assigned CI to DevOps."

affected_components:
  - docs/platform/engineering/GITLAB-REPO-STRUCTURE.md
  - docs/platform/architecture-review/ADR-020-gitlab-ce-backend-monorepo.md
  - docs/platform/architecture-review/ADR-019-microservice-skeleton-scaffold.md
  - docs/platform/engineering/backend-service-catalog.yaml (comment only)
  - templates/microservice-skeleton/README.md (GitLab wording)
  - docs/governance registers, PLAN-005, CURRENT-STATE.yaml ADR counter only

files_expected:
  - docs/platform/engineering/GITLAB-REPO-STRUCTURE.md
  - docs/platform/architecture-review/ADR-020-gitlab-ce-backend-monorepo.md
  - docs/governance/plans/PLAN-005-gitlab-ce-backend-monorepo.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/governance/registers/PARKED-BACKLOG.md
  - docs/governance/registers/DECISION-REGISTER.md
  - docs/governance/state/CURRENT-STATE.yaml
  - docs/platform/architecture-review/ADR-019-microservice-skeleton-scaffold.md
  - docs/platform/engineering/backend-service-catalog.yaml
  - templates/microservice-skeleton/README.md
  - docs/platform/engineering/MICROSERVICE-SCAFFOLD-TEMPLATE.md
  - docs/context/DOC-MAP.yaml

data_changes: none
api_changes: none
security_impact: none
compliance_impact: "audit — records a no-GitHub-integration cutover; no runtime control change"
backward_compatibility: "compatible for runtime; supersedes the per-service GitLab project map in GITLAB-REPO-STRUCTURE.md"
performance_impact: none
operational_impact: "handoff only — DevOps still authors GitLab CI; no runner or env change in this PR"

testing:
  unit: []
  integration: []
  other:
    - "python3 scripts/context/build-doc-map.py && python3 scripts/context/context-load.py validate"
    - "java scripts/governance/FreshnessCheck.java (exit 0 or 1 warn only)"
    - "python3 scripts/governance/ci-checks.py"

rollback: >
  Revert the documentation commit. No service, database, GitLab project or GitHub
  remote is changed by this PR.

dependencies:
  - "ADR-016 / SUG-20260825-arb (GitLab CI/CD is the bank pipeline)"
  - "ADR-019 (skeletons exist in the Gradle monorepo)"
  - "GATE-S08-G1 G2 G9 G10"

acceptance_criteria:
  - "AC-1 GITLAB-REPO-STRUCTURE.md maps to Insurance/{platform,frontend,backend} and forbids per-service projects at H0"
  - "AC-2 ADR-020 is Proposed, not accepted, and does not claim T4"
  - "AC-3 Per-service extract is in PARKED-BACKLOG with target stage and unpark trigger"
  - "AC-4 CURRENT-STATE.yaml stage fields unchanged; ADR counter advanced for ADR-020"
  - "AC-5 No .gitlab-ci.yml and no Gradle module split in this change"
  - "AC-6 Wave 1 is all six libs plus 1sb-integration-service and bank-persistence-service; later copies are one mature service into nip-backend; per-service GitLab projects remain parked"

out_of_scope:
  - "git push to gitlab-ce.au.bank.in"
  - "Creating or deleting GitLab projects"
  - "Authoring production .gitlab-ci.yml"
  - "Splitting Gradle modules into repositories"
  - "Human T4 signatures"
  - "Editing current_phase or GATE-S08"
  - "Moving AIGEM into nip-governance"

assumptions: []

risks:
  - risk: "DevOps may still create per-service projects from the old GITLAB-REPO-STRUCTURE.md map"
    mitigation: "This change rewrites that file; ADR-020 forbids those projects at H0"
  - risk: "FreshnessCheck WARN on state_as_of (30d) is pre-existing"
    mitigation: "This plan does not refresh stage state; disclose WARN; do not admit further work if the check later exits 2"
```
