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
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/CustomerLookupController.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/BffSessionInterceptor.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/BffWebMvcConfig.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/application/LeadFacade.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/application/CountryCodeCatalog.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/application/OrganisationDirectory.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/lead/LeadGateway.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/lead/HttpLeadGateway.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/lead/StubLeadGateway.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/customer/CustomerGateway.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/customer/HttpCustomerGateway.java
  - services/workforce-access-bff/src/main/java/com/bank/workforce/bff/customer/StubCustomerGateway.java
  - services/lead-service/src/main/java/com/bank/platform/lead/domain/Ulid.java
  - services/lead-service/src/main/java/com/bank/platform/lead/application/LeadApplicationService.java
  - services/lead-service/src/main/java/com/bank/platform/lead/config/LeadClockConfig.java
  - services/customer-service/src/main/java/com/bank/platform/customer/adapter/apigee/ApigeeAccessTokenHolder.java
  - services/workforce-access-bff/src/test/java/com/bank/workforce/bff/api/LeadModuleApiTest.java
  - services/workforce-access-bff/src/test/java/com/bank/workforce/bff/application/LeadFacadeTest.java
  - services/workforce-access-bff/src/test/java/com/bank/workforce/bff/application/CountryCodeCatalogTest.java
  - services/lead-service/src/test/java/com/bank/platform/lead/application/LeadApplicationServiceTest.java
  - services/lead-service/src/test/java/com/bank/platform/lead/api/LeadControllerTest.java
  - services/customer-service/src/test/java/com/bank/platform/customer/adapter/apigee/ApigeeAccessTokenHolderTest.java
  - docs/platform/ws3-platform/nip-bff-lead-phase.openapi.yaml
  - docs/platform/ws3-platform/EPIC-006.work-item.yaml
  - docs/platform/ws3-platform/EPIC-006-REVIEW.md
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
  - "AC-7 (board, pending human close) Unauthenticated Lead/reference APIs remain 401"
  - "AC-8 (board, pending human close) Stub/in-memory is not SoR, audit, or IRDAI evidence"

out_of_scope:
  - "Live CBS / Apigee onboarding"
  - "Flyway Lead physical schema"
  - "Campaign / bulk / meeting SMS"
  - "Quote / proposal / payment"
  - "Human T4 signatures"
  - "GATE-S08 stage field edits"

estimate: L
variance_log:
  - date: "2026-10-05"
    change: "Seven-board review recorded; AC-7/AC-8 added as pending-human conditions"
    reason: "T3 self-review cannot close APPROVED without a human board (11 §2)"
    re_review: "docs/platform/ws3-platform/EPIC-006-REVIEW.md round 1"
  - date: "2026-10-07"
    change: "files_expected lists as-built gateways, facade, catalogues and AC test classes; Spotless applied"
    reason: "Round-1 TECHNICAL condition plus ENG-6 merge bar"
    re_review: "docs/platform/ws3-platform/EPIC-006-REVIEW.md round 2"
```

## Board notes (agent self-review — not T4)

Seven-board review: [`EPIC-006-REVIEW.md`](../../platform/ws3-platform/EPIC-006-REVIEW.md). Gate **ESCALATED** for T3 human Board 4. No T4 signature.

| Board | Provisional | Note |
|---|---|---|
| Architecture | APPROVED_WITH_CONDITIONS | In-memory/stub not SoR |
| Product | APPROVED_WITH_CONDITIONS | Human countersign D-020/D-021 |
| Technical | APPROVED_WITH_CONDITIONS | files_expected updated to as-built |
| Security | APPROVED_WITH_CONDITIONS | Human Board 4 required; 401 fail-closed |
| Compliance | APPROVED_WITH_CONDITIONS | Stub is not IRDAI/audit evidence |
| QA | APPROVED_WITH_CONDITIONS | Stub tests only |
| SRE | APPROVED_WITH_CONDITIONS | http mode is not a promotion |
