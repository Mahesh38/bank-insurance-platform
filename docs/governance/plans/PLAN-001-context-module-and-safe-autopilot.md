# PLAN-001 — Reusable Context Module and Safe Autopilot

```yaml
# schema: implementation-plan
id: PLAN-001
work_item: SUG-20260816-ap1
origin: SUG-20260816-ap1
workstream: cross-cutting
risk_tier: T3
author: "Codex"
date: "2026-08-16"

objective: >
  The repository has one portable context contract and an evidence-driven automation loop that
  continues safe READY work while preserving mandatory human stage and risk decisions.

problem: >
  Mechanical governance checks pass while workstream routes contain nonexistent paths, semantic
  documents conflict, the proposed lifecycle has no reconciliation controller, application CI is
  absent, and persona compatibility files repeat canonical content.

proposed_solution: >
  Add a manifest-driven context framework with validation/scaffolding, make routing
  workstream-aware, add structured gate evidence and a proposal-only autopilot controller,
  expand CI semantic coverage, establish application CI, and collapse compatibility documents
  into redirects after canonical references are established.

alternatives:
  - option: "Fully autonomous stage transitions"
    rejected_because: "Would violate mandatory human approval and regulated-risk boundaries."
  - option: "Keep project-specific personas as the reusable framework"
    rejected_because: "Couples the module to bancassurance names, terminology and regulatory context."

affected_components:
  - documentation context module
  - AIGEM governance state and validation
  - lifecycle generation checks
  - GitHub Actions CI

files_expected:
  - docs/context/README.md
  - docs/context/context-manifest.yaml
  - docs/context/schemas/context-manifest.schema.json
  - docs/context/framework/README.md
  - docs/context/framework/CONTEXT-MODEL.md
  - docs/context/framework/LOADING-PROTOCOL.md
  - docs/context/framework/templates/project/README.md
  - docs/context/framework/templates/project/context-manifest.yaml
  - docs/context/framework/templates/project/problem-statement.md
  - scripts/context/validate-context.py
  - scripts/context/new-project-context.py
  - docs/governance/state/CURRENT-STATE.yaml
  - docs/governance/state/GATE-EVIDENCE.yaml
  - docs/governance/schemas/current-state.schema.json
  - docs/governance/schemas/gate-evidence.schema.json
  - scripts/governance/ci-checks.py
  - scripts/governance/autopilot.py
  - .github/workflows/governance.yml
  - .github/workflows/application-ci.yml
  - docs/application-lifecycle-bible/09-JIRA-MODEL.md
  - docs/application-lifecycle-bible/backlog/README.md
  - docs/context/roles/rajal-product-owner.md
  - docs/context/roles/rajal-product-owner-agentic-ai-evolution.md
  - docs/context/roles/principal-insurance-platform-architect/README.md
  - docs/README.md
  - AGENTS.md

data_changes: "governance/context YAML schemas only"
api_changes: none
security_impact: "repository automation permissions; no production credentials or runtime path"
compliance_impact: "preserves mandatory approval, waiver and evidence controls"
backward_compatibility: "compatible; stable context entry paths retained as redirect files"
performance_impact: "small CI-time increase"
operational_impact: "new CI jobs and proposal-only autopilot status command"

testing:
  unit:
    - "context manifest validation fixtures"
    - "autopilot refuses incomplete evidence and never emits PASSED"
  integration:
    - "governance semantic checks validate all configured paths and workstream routes"
    - "lifecycle backlog regeneration produces no diff"
  e2e:
    - "GitHub application workflow runs Gradle test and coverage tasks"
  other:
    - "freshness, full internal links, schemas and repository status"

rollback: >
  Revert the branch. No runtime data, external tracker state or production configuration is changed.

dependencies: []
assumptions:
  - "CR-010 can be reviewed before any proposed stage or WS-3 transition is made binding."
risks:
  - risk: "Automation is mistaken for approval authority."
    mitigation: "Controller is proposal-only and hard-rejects PASSED mutation."
  - risk: "Documentation cleanup removes a live entrypoint."
    mitigation: "Retain compact redirect files and run a repository-wide link check."

acceptance_criteria:
  - "A new project/domain context can be scaffolded and validated without bancassurance-specific framework edits."
  - "Every active workstream and routing destination resolves to an existing path."
  - "Gate evidence records owners, verifier, evidence, blockers and human approvals separately."
  - "Blocked work causes selection of another READY item rather than a default approval."
  - "No automation path can mark PASSED or waive mandatory human decisions."
  - "Lifecycle generated files are reproducible and checked in CI."
  - "Application CI exists and runs tests plus coverage gates."
  - "Compatibility files contain redirects rather than duplicate persona policy."

out_of_scope:
  - "actual stage transition or CR-010 ratification"
  - "Jira import or two-way Jira/YAML synchronization"
  - "production deployment, secrets or cloud infrastructure"
  - "rewriting domain-specific persona packages"

estimate: XL
reviews: []
variance_log:
  - date: "2026-08-16"
    change: >
      Added the CR, intake/backlog/register records, autopilot contract and safety tests outside
      the initial files_expected list.
    reason: >
      Binding AIGEM traceability and executable safety verification were required once the
      proposal-only controller moved from design to implementation.
    re_review: "Included in the still-pending CR-010 human review package."
  - date: "2026-08-16"
    change: >
      Reconciled stage-gate, lifecycle, WIP, dependency, risk, schema-index and documentation-map
      narratives discovered by the repository-wide semantic scan.
    reason: >
      Leaving those files unchanged would preserve the exact contradictions and blocking
      ambiguity that PLAN-001 is intended to remove.
    re_review: "Included in the still-pending CR-010 human review package."
```
