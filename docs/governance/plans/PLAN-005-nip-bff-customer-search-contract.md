# PLAN-005 — NIP BFF SCR-03 customer search unified contract

```yaml
# schema: implementation-plan
id: PLAN-005
work_item: ARCH-025
origin: SUG-20260923-scs
workstream: WS-3
risk_tier: T3
author: "agent:cursor-grok (persona: Mahesh)"
date: "2026-09-23"

objective: >
  After this change every seat implements or reviews SCR-03 from one written
  contract: lead-first search by Customer ID / PAN / mobile, else CBS via
  Apigee, with a public schema that can render the shipped screen.

problem: >
  Human screen + hop (lead exist? else CBS via Apigee) is not what ARCH-023
  published: CBS-first, no CIF-shaped field, three artefacts for one screen.
  ADR-020 already requires Apigee for internal bank APIs. Evidence: 07-nip-bff
  §4.2; information model §4.1; 2026-09-14 Apigee direction; S05 SCR-03.

proposed_solution: >
  Publish 09-nip-bff-customer-search-contract.md as the SSOT. Align 07 LLD
  search sequence, OpenAPI CustomerSummary/search, Java sketches and HLD §5.1
  search row. Record SUG-20260923-scs and ARCH-025 under EPIC-003. Do not
  implement services. Do not claim T4. Do not add a second role-specific doc.

alternatives:
  - option: "Treat this as a duplicate of SUG-20260907-ldc and only add a paragraph to 07-LLD"
    rejected_because: >
      The hop and the screen fields change the public schema (source, maskedCif,
      existingLead). A paragraph would leave OpenAPI and Flutter generators wrong.
  - option: "Write separate Product, Architecture, QA and OpenAPI documents"
    rejected_because: "The intake forbids multiple docs for the same screen."
  - option: "Implement NIP BFF search in this change"
    rejected_because: "S08 output is the contract; S11-E02/E06 own the runtime."
  - option: "Flutter calls Apigee / CBS"
    rejected_because: "Standing constraint and ADR-020 ingress/egress split."

affected_components:
  - docs/platform/ws3-platform (unified contract, LLD, OpenAPI, Java sketches, work items)
  - docs/architecture/R0-HLD.md
  - docs/architecture/README.md
  - docs/governance registers and PLAN-005

files_expected:
  - docs/platform/ws3-platform/09-nip-bff-customer-search-contract.md
  - docs/platform/ws3-platform/ARCH-025.work-item.yaml
  - docs/platform/ws3-platform/EPIC-003.work-item.yaml
  - docs/platform/ws3-platform/07-nip-bff-lead-phase-api-lld.md
  - docs/platform/ws3-platform/08-nip-bff-lead-phase-java-records.md
  - docs/platform/ws3-platform/nip-bff-lead-phase.openapi.yaml
  - docs/governance/plans/PLAN-005-nip-bff-customer-search-contract.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/R0-HLD.md
  - docs/architecture/README.md
  - docs/context/DOC-MAP.yaml

data_changes: none
api_changes: "additive documentation of GET /customers:search orchestration and public fields (source, maskedCif, existingLead); no runtime caller"
security_impact: "PII — specifies lead-first, BFF masking including last-4 CIF, forbid-list, audit hash of q; no runtime exposure change"
compliance_impact: "audit — search remains a material action; OPEN-SEARCH-CIF-MASK left to Board 6"
backward_compatibility: "compatible for runtime (no callers); documentation breaks the 2026-09-07 CBS-first sequence on purpose"
performance_impact: "expected — CBS hop skipped when an own lead exists; no production load"
operational_impact: "documents 503 collapse and CBS-as-bottleneck; no new dashboard shipped"

testing:
  unit: []
  integration: []
  other:
    - "python3 scripts/context/build-doc-map.py && python3 scripts/context/context-load.py validate"
    - "java scripts/governance/FreshnessCheck.java (exit 0 or 1 warn only)"
    - "python3 scripts/governance/ci-checks.py"
    - "OpenAPI still parses; /customers:search documents lead-first and maskedCif"

rollback: >
  Revert the documentation commit. No service, database or published consumer is affected.

dependencies:
  - "ADR-014"
  - "ADR-015"
  - "ADR-017"
  - "ADR-020"
  - "ARCH-023"
  - "S05 SCR-03"
  - "AC-CUST-010"
  - "AC-EXC-10"

acceptance_criteria:
  - "AC-1 One file specifies GET /customers:search for CUSTOMER_ID, PAN, MOBILE with lead-first then CBS via Apigee"
  - "AC-2 Public schema has no cifNumber or PAN; maskedCif and source are defined"
  - "AC-3 07-LLD §4.2 no longer claims CBS-first as the only hop"
  - "AC-4 CURRENT-STATE.yaml stage fields unchanged"
  - "AC-5 No second Product/QA/OpenAPI file is created for the same screen"

out_of_scope:
  - "NIP BFF / Lead / Customer / Flutter / Apigee implementation"
  - "Exact CBS verb (OPEN-SEARCH-CBS-OP)"
  - "Human T4 signatures"
  - "Editing current_phase or GATE-S08"
  - "Customer BFF"
  - "POST /leads Savings/ULIP create-new"

assumptions:
  - "ASM-016"

risks:
  - risk: "Board 6 rejects last-4 CIF on the device"
    mitigation: "OPEN-SEARCH-CIF-MASK; field is optional on the wire; drop without a version bump"
  - risk: "Readers treat the OpenAPI as production-published"
    mitigation: "Status AI-DRAFTED; S12 remains the portal gate"
  - risk: "Guessed CBS path becomes a fake dependency"
    mitigation: "Capability table only; configurable Apigee base URL"

estimate: M
reviews: []
```
