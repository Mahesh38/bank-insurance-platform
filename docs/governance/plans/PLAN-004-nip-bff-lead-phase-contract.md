# PLAN-004 — NIP BFF lead landing and create contract pack

```yaml
# schema: implementation-plan
id: PLAN-004
work_item: EPIC-003
origin: SUG-20260907-ldc
workstream: WS-3
risk_tier: T3
author: "agent:cursor-grok (persona: Mahesh)"
date: "2026-09-07"

objective: >
  After this change NIP-APP can be implemented against a written R0 contract for
  SCR-02..SCR-05 (own-lead inbox, ETB search, Term create/resume) without reading
  Figma as SoT, and HLD §5.1 no longer publishes /opportunities as the create path.

problem: >
  R0-HLD §5.1 is an unpaginated seam sketch that still uses /opportunities after
  CR-013 / ADR-014 made Lead the spoken path. S05 already specifies SCR-02..SCR-05
  and S03 specifies AC-CUST-010 and AC-LEAD-010-1. Documentation canon records NIP
  BFF OpenAPI as 1SB-only. FF-15 (S08) cannot test a seam that has no consumer
  contract. Evidence: R0-HLD.md §5.1; 05-DOCUMENTATION-CANON.md S07 API contracts
  row; S05-experience-evidence.md §4.3.

proposed_solution: >
  Publish an LLD, OpenAPI 3.0.3 file and Java record sketches constrained to R0
  Term / ETB / own-RM inbox. Cursor-paginate the pipeline; cap search at 20;
  mask PII at the BFF; parallelise only cacheable catalogue + active-leads;
  keep search/create synchronous (no polling). Repair HLD §5.1 and S-20 labels
  to /leads. Park ULIP/Savings/Health tabs and meeting scheduling as SUG-20260907-fig.
  Do not implement NIP BFF or Lead services. Do not claim T4.

alternatives:
  - option: "Wait until GATE-S10/S11 to write any BFF contract"
    rejected_because: >
      AP-5 is contract-first; FF-15 is assigned to S08; producing documents is not
      S11 feature breadth. Waiting would freeze /opportunities drift into S11.
  - option: "Treat Figma as the API spec (three tabs, ULIP, meeting, Health)"
    rejected_because: "A11 Figma is reference only; BOOT and S11 exclude those products."
  - option: "One bulk GET /workspace/bootstrap that returns leads + prospects + catalogue + CBS recents"
    rejected_because: "Unbounded PII, no pagination, couples four contexts, fails INV-LED-05 scoping."
  - option: "Implement NIP BFF in this change"
    rejected_because: "S08 output is not a feature; S11-E02/E06 own the runtime."

affected_components:
  - docs/platform/ws3-platform (LLD, OpenAPI, Java sketches, work items)
  - docs/architecture/R0-HLD.md
  - docs/platform/ws3-platform/03-solution-architecture-r0.md
  - docs/platform/ws3-platform/06-architecture-justification-and-review-answers.md
  - docs/governance registers and PLAN-004
  - docs/governance/state/CURRENT-STATE.yaml (EPIC counter only)

files_expected:
  - docs/platform/ws3-platform/07-nip-bff-lead-phase-api-lld.md
  - docs/platform/ws3-platform/08-nip-bff-lead-phase-java-records.md
  - docs/platform/ws3-platform/nip-bff-lead-phase.openapi.yaml
  - docs/platform/ws3-platform/EPIC-003.work-item.yaml
  - docs/platform/ws3-platform/ARCH-023.work-item.yaml
  - docs/platform/ws3-platform/DOC-021.work-item.yaml
  - docs/governance/plans/PLAN-004-nip-bff-lead-phase-contract.md
  - docs/architecture/R0-HLD.md
  - docs/architecture/README.md
  - docs/platform/ws3-platform/03-solution-architecture-r0.md
  - docs/platform/ws3-platform/06-architecture-justification-and-review-answers.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/governance/registers/PARKED-BACKLOG.md
  - docs/governance/state/CURRENT-STATE.yaml
  - docs/context/DOC-MAP.yaml

data_changes: none
api_changes: "additive documentation of unpublished NIP BFF paths; HLD sketch /opportunities → /leads for this slice (breaking vs the sketch, not vs a published consumer)"
security_impact: "PII — specifies BFF masking and forbid-list for Flutter; no runtime exposure change"
compliance_impact: "audit — search and create remain material actions (BR-SEC-030); no consent change"
backward_compatibility: "compatible for runtime (no callers); HLD sketch paths renamed to match ADR-014"
performance_impact: "expected — list caps and cursor pagination; no production load"
operational_impact: none

testing:
  unit: []
  integration: []
  other:
    - "python3 scripts/context/build-doc-map.py && python3 scripts/context/context-load.py validate"
    - "java scripts/governance/FreshnessCheck.java (exit 0 or 1 warn only)"
    - "python3 scripts/governance/ci-checks.py"
    - "OpenAPI parses; paths include /workspace/pipeline, /customers:search, /leads"

rollback: >
  Revert the documentation commit. No service, database or published consumer is affected.

dependencies:
  - "ADR-005"
  - "ADR-014"
  - "ADR-015"
  - "ADR-017"
  - "S05 SCR-02..SCR-05"
  - "AC-CUST-010"
  - "AC-LEAD-010-1"

acceptance_criteria:
  - "AC-1 Pipeline API is cursor-paginated in OpenAPI"
  - "AC-2 Search/confirm schemas have no cifNumber or PAN"
  - "AC-3 POST /leads returns leadId and journeyId"
  - "AC-4 HLD §5.1 create path is /leads"
  - "AC-5 CURRENT-STATE.yaml stage fields unchanged"
  - "AC-6 ULIP/meeting parked as SUG-20260907-fig"

out_of_scope:
  - "NIP BFF / Lead / Customer / Flutter implementation"
  - "SCR-06+ journey APIs"
  - "Meeting scheduler and ULIP/Savings/Health pickers"
  - "Human T4 signatures"
  - "Editing current_phase or GATE-S08"
  - "S12 portal publication"
  - "Adding a new context-index capsule (CTX-7)"

assumptions: []

risks:
  - risk: "Rajal rejects by=NAME"
    mitigation: "OPEN-LEAD-NAME; BFF can 400 NAME without a schema break if Product drops it before S11"
  - risk: "Readers treat this OpenAPI as production-published"
    mitigation: "Status AI-DRAFTED on every artefact; S12 remains the portal gate"
  - risk: "HLD two-step journey-from-QUALIFIED vs AC-LEAD-010-1"
    mitigation: "LLD records Product/BA AC as winning for R0 assisted create; QUALIFIED stays a later lead state"

estimate: M
reviews: []
```
