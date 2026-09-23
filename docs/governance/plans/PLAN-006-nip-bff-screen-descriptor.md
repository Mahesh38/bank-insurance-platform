# PLAN-006 — NIP BFF screen descriptor contract

```yaml
# schema: implementation-plan
id: PLAN-006
work_item: ARCH-026
origin: SUG-20260923-sdu
workstream: WS-3
risk_tier: T3
author: "agent:cursor-grok (persona: Mahesh)"
date: "2026-09-23"

objective: >
  After this change NIP-APP can implement one renderer for FORM / LIST / CARD /
  CAROUSEL against a written contract: nested X→Y→Z reveals, visibleWhen
  predicates, and field validation (required, format, length, min/max, regex).

problem: >
  SUG-20260923-sdu admitted ARCH-026 without the field/surface grammar frontend
  needs. Nested dependence (dropdown/checkbox/radio revealing another widget)
  and free-text formats were unspecified. Evidence: ARCH-026 AC-2; stakeholder
  follow-up 2026-09-23.

proposed_solution: >
  Publish ADR-021, 10-nip-bff-screen-descriptor.md and
  nip-bff-screen-descriptor.openapi.yaml. Closed widget and surface enums;
  reveals max depth 3; same validation object on client and server. Do not
  implement Flutter or BFF runtime. Do not claim T4. Do not rewrite ARCH-025
  SearchPage.

alternatives:
  - option: "Leave nested rules as prose in the triage record"
    rejected_because: "Frontend cannot generate models or share one evaluator"
  - option: "Copy 1SB proposal schema onto NIP BFF"
    rejected_because: "UI must not speak 1SB"
  - option: "Implement the Flutter renderer in this change"
    rejected_because: "FUNC-021 / S11 owns runtime; this item is the contract"

affected_components:
  - docs/platform/ws3-platform (LLD, OpenAPI, ARCH-026)
  - docs/platform/architecture-review (ADR-021)
  - docs/architecture/README.md
  - docs/governance registers, PLAN-006, CURRENT-STATE id_allocation only

files_expected:
  - docs/platform/ws3-platform/10-nip-bff-screen-descriptor.md
  - docs/platform/ws3-platform/nip-bff-screen-descriptor.openapi.yaml
  - docs/platform/architecture-review/ADR-021-nip-screen-descriptor.md
  - docs/platform/ws3-platform/ARCH-026.work-item.yaml
  - docs/governance/plans/PLAN-006-nip-bff-screen-descriptor.md
  - docs/governance/registers/DECISION-REGISTER.md
  - docs/governance/state/CURRENT-STATE.yaml
  - docs/architecture/README.md
  - docs/context/DOC-MAP.yaml

data_changes: none
api_changes: "additive documentation of unpublished /screens/{screenId} and submissions; no runtime caller"
security_impact: "PII — iconUrl CDN-only; PAN format not echoed; no runtime exposure change"
compliance_impact: "audit — assignment submit remains a material action; no consent change"
backward_compatibility: "compatible for runtime (no callers); does not rewrite SearchPage or PipelinePage"
performance_impact: "none — documentation"
operational_impact: none

testing:
  unit: []
  integration: []
  other:
    - "python3 scripts/context/build-doc-map.py && python3 scripts/context/context-load.py validate"
    - "java scripts/governance/FreshnessCheck.java (exit 0 or 1 warn only)"
    - "python3 scripts/governance/ci-checks.py"
    - "OpenAPI parses; paths include /screens/{screenId} and /screens/{screenId}/submissions"

rollback: >
  Revert the documentation commit. No service, database or published consumer is affected.

dependencies:
  - "ADR-015"
  - "ADR-017"
  - "ARCH-026"
  - "SUG-20260923-sdu"
  - "CR-015"

acceptance_criteria:
  - "AC-1 ScreenDocument defines FORM, LIST, CARD, CAROUSEL"
  - "AC-2 Field supports reveals (max depth 3), visibleWhen, and FieldValidation"
  - "AC-3 Widget and format enums are closed in OpenAPI"
  - "AC-4 CURRENT-STATE.yaml stage fields unchanged; ADR counter advanced to 22"
  - "AC-5 No Flutter or BFF runtime code"

out_of_scope:
  - "Flutter / iOS / Android / Web renderer"
  - "Rewriting SCR-03 SearchPage"
  - "Meeting scheduler fields"
  - "Raw S3 hosting"
  - "Human T4 signatures"
  - "Editing current_phase or GATE-S08"

assumptions: []

risks:
  - risk: "Readers treat OpenAPI as production-published"
    mitigation: "AI-DRAFTED on every artefact; S12 remains the portal gate"
  - risk: "Product needs a fourth nest level"
    mitigation: "ADR revalidation trigger; split the screen rather than deepen the tree"

estimate: M
reviews: []
```
