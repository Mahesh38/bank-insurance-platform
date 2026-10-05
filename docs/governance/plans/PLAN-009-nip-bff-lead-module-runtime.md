# PLAN-009 — NIP BFF Lead module runtime slice

```yaml
# schema: implementation-plan
id: PLAN-009
work_item: EPIC-006
origin: SUG-20261005-lbf
workstream: WS-3
risk_tier: T3
author: "agent:cursor-grok (persona: Amit; product closures as Rajal)"
date: "2026-10-05"

objective: >
  After this change NIP-APP can call REST Lead-module APIs on the workforce BFF
  (country-codes, organisation lookups, customer search, pipeline, create, assign
  with exceptionRequired) and leadId is a ULID with D-021 stage projection.

problem: >
  EPIC-003/005 published contracts. workforce-access-bff still deny-all except
  auth. Lead and Customer services were skeletons. Concurrent CBS calls that
  each fetch an Apigee token would race. Evidence: Lead BRD Screens 1/5/7;
  EPIC-003 OpenAPI; BOOT Lead #5 and RM Workspace BFF in_scope.

proposed_solution: >
  Admit EPIC-006 on SF5 lane amit-nip-bff-lead. Implement session-gated REST
  collections on /api/v1; Lead service in-memory ULID aggregate; Customer
  service stub directory plus lock-guarded ApigeeAccessTokenHolder. BFF default
  downstream mode is stub so Flutter can integrate without live CBS. Record
  D-020 and D-021. Do not apply Flyway Lead DDL. Do not claim T4.

alternatives:
  - option: "Wait until S11 / GAP-006 and GAP-007 close"
    rejected_because: "SF5 parallel-lane test passes; contracts already exist; NIP-APP is blocked on missing APIs."
  - option: "Put CBS tokens on the BFF"
    rejected_because: "ADR-015 / ADR-020 — Flutter never sees OAuth; Customer #4 owns CBS egress."
  - option: "Encode branch or product into leadId"
    rejected_because: "D-020 / ID-01 — identity must stay opaque; reassignment would invalidate the key."
  - option: "Copy BRD insurer-status ladder onto Lead state"
    rejected_because: "D-021 — Journey owns post-quote refs; Lead must not transition through UW queue."

affected_components:
  - services/workforce-access-bff
  - services/lead-service
  - services/customer-service
  - docs/platform/ws3-platform/nip-bff-lead-phase.openapi.yaml

files_expected:
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/LeadModuleController.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/OrganisationController.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/ReferenceDataController.java
  - services/lead-service/src/main/java/com/bank/platform/lead/domain/Ulid.java
  - services/lead-service/src/main/java/com/bank/platform/lead/application/LeadApplicationService.java
  - services/customer-service/src/main/java/com/bank/platform/customer/adapter/apigee/ApigeeAccessTokenHolder.java
  - docs/platform/ws3-platform/nip-bff-lead-phase.openapi.yaml
  - docs/platform/ws3-platform/EPIC-006.work-item.yaml
  - docs/governance/plans/PLAN-009-nip-bff-lead-module-runtime.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/au-bank-insurance-platform/DECISION-LOG.md

data_changes: none
api_changes: "additive public BFF paths under /api/v1; internal Lead and Customer search/assign paths"
security_impact: "attack-surface — new session-gated BFF endpoints; CBS tokens stay in Customer #4"
compliance_impact: "audit — assignment and create remain attributable; no consent change"
backward_compatibility: "compatible — additive; login paths unchanged"
performance_impact: "single-flight token refresh removes CBS auth stampede; org/country catalogues are in-process"
operational_impact: "stub mode default; set LEAD_DOWNSTREAM_MODE=http and CUSTOMER_DOWNSTREAM_MODE=http when services are up"

testing:
  unit:
    - "UlidTest — Crockford 26 and time order"
    - "DashboardStageTest — Lead does not own UW queue"
    - "LeadApplicationServiceTest — create/dedupe/onboard/assign"
    - "ApigeeAccessTokenHolderTest — one fetch under 16 threads"
    - "CountryCodeCatalogTest — India regex"
    - "LeadFacadeTest — evaluate-then-assign / hold short-circuit"
  integration:
    - "LeadModuleApiTest — MockMvc session, REST names, assign exceptionRequired"
    - "LeadControllerTest — internal create/onboard/assign"
  other:
    - "ArchUnit domain/application rules without allowEmptyShould"
    - "No CURRENT-STATE stage field edit"

rollback: >
  Revert the commit. In-memory stores hold no durable customer data. Public
  OpenAPI additions are additive and can be unpublished by revert.

dependencies:
  - EPIC-003
  - EPIC-005
  - D-018
  - D-019
  - ADR-015
  - ADR-017
  - ADR-020
assumptions: []
risks:
  - risk: "Stub org/CBS fixtures diverge from bank HR/CBS"
    mitigation: "Switch downstream mode to http when Apigee products exist; do not treat fixtures as SoR"
  - risk: "Session-only authz until WS-2 A.3 PDP"
    mitigation: "Lead book-scope still 404s foreign ids; PDP grants remain a named not_included"

acceptance_criteria:
  - "AC-1 GET /country-codes returns India validation rule"
  - "AC-2 Nested /branches/{id}/verticals and specified-persons"
  - "AC-3 Assign returns exceptionRequired"
  - "AC-4 leadId is ULID; dashboardStage NEW on create"
  - "AC-5 Apigee token fetch is single-flight"
  - "AC-6 GET /customers/{id} confirm sheet and /customers/{id}/active-leads duplicate check"

out_of_scope:
  - "Live CBS / Apigee onboarding"
  - "Flyway Lead physical schema"
  - "Campaign / bulk / meeting SMS"
  - "Quote / proposal / payment"
  - "Human T4 signatures"
  - "GATE-S08 stage field edits"

estimate: L
variance_log: []
```

## Board notes (agent self-review — not T4)

| Board | Provisional | Note |
|---|---|---|
| Architecture | self_review | Reuses EPIC-003; BFF does not hold CBS tokens; REST nesting |
| Product | self_review | D-020 / D-021 recorded as Rajal behaviour, not technology |
| Technical | self_review | Java 21, ports, tests in the same change |
| Security | needed at T3 human | New public endpoints; session interceptor; no token to Flutter |
| Compliance | advisory | Attribution fields retained; IPR create still gated |
| QA | advisory | MockMvc + unit evidence; live CBS not claimed |
| SRE | N/A | Stub default; no new runtime platform |
