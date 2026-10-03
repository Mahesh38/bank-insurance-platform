# PLAN-007 — Lead module (#5) R0 design pack

```yaml
# schema: implementation-plan
id: PLAN-007
work_item: EPIC-005
origin: SUG-20260930-lmd
workstream: WS-3
risk_tier: T3
author: "agent:cursor (persona: Mahesh)"
date: "2026-09-30"

objective: >
  After this change engineers have a BRD-traced Lead (#5) design pack — module HLD,
  sequences, internal API LLD/OpenAPI, flows and algorithms — constrained to R0,
  reusing EPIC-003 as the public BFF edge, with BRD↔domain conflicts named OPEN for Rajal.

problem: >
  Architect was asked to design the Lead Management module from the Lead BRD
  (DOC-005). EPIC-003 already covers NIP BFF SCR-02..SCR-05. Without a Lead-service
  design pack, S11-E02 would invent ownership, dedupe, stage mapping and internal
  seams. Evidence: Lead_Module_BRD_Detailed_CONTEXT.md; CURRENT-STATE Lead #5 in_scope;
  01-domain-model §4.1; EPIC-003 completion_definition excludes Lead service code.

proposed_solution: >
  Admit EPIC-005 on SF5 lane mahesh-lead-domain-design. Publish docs under
  docs/platform/ws3-platform/10..13 plus lead-service-internal.openapi.yaml.
  Map BRD §4.1 to R0 IN / DEFER / OPEN. Do not rewrite EPIC-003. Do not implement
  services. Do not claim T4. Do not resolve OPEN-LEAD-ACTOR or OPEN-LEAD-STAGE.

alternatives:
  - option: "Redesign Lead from scratch including a new public BFF contract"
    rejected_because: "EPIC-003 is IN-FLIGHT and already the admitted consumer contract; forking breaks FF-15."
  - option: "Wait until S11 to write Lead-service design"
    rejected_because: "AP-5 is contract-first; Lead is Wave 1; BRD/ingest already exists (DOC-005)."
  - option: "Implement the full BRD including campaign, bulk and meeting completion"
    rejected_because: "BOOT out_of_scope_now and BRD §4.2 / SUG-20260907-fig park those."
  - option: "Silently adopt BRD SP/Non-SP/Insurance-RM create over INV-LED-04"
    rejected_because: "DOC-005 conflict rule + Mahesh NA on Product semantics; raise OPEN-LEAD-ACTOR."

affected_components:
  - docs/platform/ws3-platform (HLD, sequences, API LLD, algorithms, OpenAPI, work items)
  - docs/architecture/README.md (index)
  - docs/governance plans and registers
  - docs/governance/state/CURRENT-STATE.yaml (EPIC counter only)

files_expected:
  - docs/platform/ws3-platform/10-lead-module-hld.md
  - docs/platform/ws3-platform/11-lead-module-sequences.md
  - docs/platform/ws3-platform/12-lead-module-api-lld.md
  - docs/platform/ws3-platform/13-lead-module-flows-and-algorithms.md
  - docs/platform/ws3-platform/lead-service-internal.openapi.yaml
  - docs/platform/ws3-platform/EPIC-005.work-item.yaml
  - docs/platform/ws3-platform/ARCH-026.work-item.yaml
  - docs/platform/ws3-platform/ARCH-027.work-item.yaml
  - docs/platform/ws3-platform/ARCH-028.work-item.yaml
  - docs/platform/ws3-platform/DOC-023.work-item.yaml
  - docs/governance/plans/PLAN-007-lead-module-design-pack.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/governance/state/CURRENT-STATE.yaml
  - docs/context/DOC-MAP.yaml

data_changes: none
api_changes: "additive documentation of unpublished Lead-service internal paths; no runtime"
security_impact: "documents PDP checks and PII masking inheritance from EPIC-003; no runtime exposure change"
compliance_impact: "audit events listed per BRD §22; no consent change"
backward_compatibility: "compatible — additive docs; EPIC-003 unchanged"
performance_impact: none
operational_impact: none

testing:
  unit: []
  integration: []
  other:
    - "Cross-doc consistency: HLD R0 cut ↔ algorithms ↔ OpenAPI operation set"
    - "Every BRD §4.1 row classified IN / DEFER / OPEN"
    - "python3 scripts/context/build-doc-map.py after new docs"
    - "No CURRENT-STATE stage field edit"

rollback: >
  Revert the documentation commit. No runtime or data impact.

dependencies:
  - EPIC-003 (soft — public edge reuse)
  - ADR-005
  - ADR-014
  - ADR-017
assumptions: []
risks:
  - risk: "Insurance RM create may be solicitation (ADR-005 prior rejection)"
    mitigation: "D-018 / ADR-021 admit design; OPEN-COMP-LEAD-IPR-CREATE blocks IPR runtime until Board 6"
  - risk: "BRD stage names diverge from domain state machine"
    mitigation: "OPEN-LEAD-STAGE mapping table; Journey owns post-quote progression facts"

acceptance_criteria:
  - "AC-1 HLD boundaries + R0 cut + OPEN conflicts"
  - "AC-2 sequences for create/dedupe/resume/convert/archive"
  - "AC-3 Lead-service OpenAPI operations present"
  - "AC-4 algorithms trace to BR-* ids"
  - "AC-5 EPIC counter only in CURRENT-STATE"

out_of_scope:
  - "Runtime Lead / BFF / Flutter code"
  - "Campaign / bulk / meeting-completion"
  - "Physical schema"
  - "Human T4 signatures"
  - "Silent Product decisions"

estimate: L

reviews: []
variance_log: []
```

## Board notes (agent self-review — not T4)

| Board | Provisional | Note |
|---|---|---|
| Architecture | self_review | Smallest additive pack; reuses EPIC-003; conflicts raised not resolved |
| Product | needed | OPEN-LEAD-ACTOR / OPEN-LEAD-STAGE / OPEN-D1 are Product |
| Security | needed at T3 human | PDP grants and PII masking inheritance |
| Compliance | needed at T3 human | Audit + attribution (BR-OWN / BR-AUDIT) |
| QA | advisory | Algorithms become AC sources for S11 |
| SRE | N/A | Docs only |
| Engineering | advisory | Internal OpenAPI is the S11 scaffold input |
