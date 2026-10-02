# PLAN-008 — Workforce authn/authz LLD and Keycloak-collapse decision

```yaml
# schema: implementation-plan
id: PLAN-008
work_item: ARCH-029
origin: SUG-20261002-iap
workstream: WS-2
risk_tier: T3
author: "agent:cursor (persona: Mahesh)"
date: "2026-10-02"

objective: >
  After this change the delivery team has one Confluence-ready LLD that answers
  "do we still need the identity adapter and the PDP if Keycloak is there?",
  forbids the Keycloak-only collapse in ADR-022, and gives OpenAPI + ordered
  GATE-IAM-P1 slices against the Java that already exists.

problem: >
  WS-2 Phase 1 objective is provider-neutral workforce identity: token-hiding BFF,
  Keycloak behind an adapter, business PDP. GATE-IAM-P1 A.2 and A.3 are OPEN.
  The three services are scaffolds. The incoming request asked whether Keycloak
  presence makes those two services removable. Implementing or deleting them
  without a written options analysis would either violate standing constraints
  or stall IAM-P1.

proposed_solution: >
  Admit ARCH-029 on the WS-2 Mahesh lane. Publish AUTHN-AUTHZ-LLD.md, three
  OpenAPI files, and ADR-022 (PROPOSED, A3_JOINT_REVIEW with Deepali). Keep
  README.md as invariant SSOT; the LLD is the implementation pack. Record
  SUG-20261002-psr (PARTNER_SR seed grants proposal.submit) as a slice A.3
  defect — do not fix seed in this change. Do not implement runtime. Do not
  claim T4.

alternatives:
  - option: "Delete adapter and PDP and use Keycloak only"
    rejected_because: >
      Violates standing constraint, ARCH-018/020/021, ID-04/ID-06, GATE A.2/A.3.
      Production IdP is deliberately deferred. Not good practice for regulated
      multi-tenant authorization. ADR-022 records the reject.
  - option: "Wait until Phase 2 IdP product is chosen, then write the LLD"
    rejected_because: >
      The adapter exists so Phase 2 can be deferred. Waiting inverts the design.
      GATE-IAM-P1 is the current gate.
  - option: "Implement remaining Java in the same change as the LLD"
    rejected_because: >
      AP-5 is contract-first; this request asked for design LLD and justification.
      Runtime is slices A.1–A.6 after the pack is reviewed.

affected_components:
  - docs/platform/authentication-authorization
  - docs/platform/architecture-review/08-architecture-decision-log.md
  - docs/governance registers, plan, CURRENT-STATE id_allocation only
  - docs/context AGENT-CONTEXT-INDEX then_only_if + generated DOC-MAP / BOOT

files_expected:
  - docs/platform/authentication-authorization/AUTHN-AUTHZ-LLD.md
  - docs/platform/authentication-authorization/workforce-access-bff.openapi.yaml
  - docs/platform/authentication-authorization/identity-provider-adapter.openapi.yaml
  - docs/platform/authentication-authorization/identity-authorization.openapi.yaml
  - docs/platform/authentication-authorization/ARCH-029.work-item.yaml
  - docs/platform/authentication-authorization/README.md
  - docs/governance/plans/PLAN-008-authn-authz-lld.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/governance/registers/DECISION-REGISTER.md
  - docs/platform/architecture-review/08-architecture-decision-log.md
  - docs/governance/state/CURRENT-STATE.yaml
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/context/BOOT.md
  - docs/context/DOC-MAP.yaml
  - docs/platform/README.md

data_changes: none
api_changes: "additive documentation of existing and gap paths; no runtime"
security_impact: >
  Documents trust boundaries, token-hiding, PDP fail-closed and Keycloak isolation.
  Does not change enforcement. Collapse is REJECTED. Deepali still owns ID-11 and
  Board 4 verdict.
compliance_impact: "PDP policyVersion / maker-checker / 7-year events restated; no retention change"
backward_compatibility: "compatible — additive docs; SSOT README unchanged in invariants"
performance_impact: none
operational_impact: none

testing:
  unit: []
  integration: []
  other:
    - "python3 scripts/context/build-doc-map.py after new docs"
    - "python3 scripts/context/build-boot-capsule.py after ADR counter advance"
    - "python3 scripts/context/context-load.py validate"
    - "java scripts/governance/FreshnessCheck.java"
    - "LLD §0 answers the Keycloak question; OpenAPI operation set matches LLD tables"
    - "No CURRENT-STATE stage field edit"

rollback: >
  Revert the documentation commit. No runtime or data impact.

dependencies:
  - ARCH-018
  - ARCH-019
  - ARCH-020
  - ARCH-021
  - ADR-020
  - UC-05
assumptions: []
risks:
  - risk: "Readers treat this AI-DRAFTED pack as a signed T4 architecture/security approval"
    mitigation: "Status banner + ADR-022 approvals list + Mahesh NA on manufacturing T4"
  - risk: "PARTNER_SR seed continues to grant regulated actions until A.3"
    mitigation: "SUG-20261002-psr recorded; LLD §7.7 / §10.4 name the defect"

acceptance_criteria:
  - "AC-1 Keycloak-only collapse answered and REJECTED with how-to-remove"
  - "AC-2 three OpenAPI files cover BFF, adapter, PDP"
  - "AC-3 slices A.1–A.6 map to GATE-IAM-P1"
  - "AC-4 ADR-022 forbids Keycloak as business SoT and BFF→Keycloak"
  - "AC-5 ADR counter only in CURRENT-STATE"

out_of_scope:
  - "Runtime Java / Flyway / Helm"
  - "Human T4 signatures"
  - "Production IdP selection"
  - "ID-11 password ceremony"
  - "Customer identity"
  - "Fixing PARTNER_SR seed in this PR"

estimate: L

reviews: []
variance_log: []
```

## Board notes (agent self-review — not T4)

| Board | Provisional | Note |
|---|---|---|
| Architecture | self_review | Smallest structural answer: keep accepted boundaries; ADR forbids collapse; LLD is contract-first. RG-9 considered (OPEN gate + no G1–G10 delta) — still T3 because ADR-022 forbids collapse; G1 did not fire (RG-6) |
| Product | advisory | Login BRD remains behaviour SSOT; no password field added to match Figma |
| Security | needed at T3 human | Authn/authz restated, not changed. Deepali may escalate G1 to T4 (RG-6). ID-11 stays open |
| Compliance | notify | Maker-checker, policyVersion, 7-year events restated; Shailja owns A.5 confirmation and ID-21 |
| QA | advisory | §11 is the GATE evidence matrix for later slices |
| SRE | N/A for this delta | NetworkPolicy / Keycloak ops named as A.2 apply, not this PR |
| Engineering | advisory | OpenAPI + package map + gap list are the scaffold input |
