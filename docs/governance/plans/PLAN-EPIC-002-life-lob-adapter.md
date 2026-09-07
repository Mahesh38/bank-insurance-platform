# schema: implementation-plan
id: PLAN-EPIC-002
work_item: EPIC-002
origin: SUG-20260903-lif
workstream: WS-1
risk_tier: T3
author: "agent:composer"
date: "2026-09-03"
change_request: CR-014

objective: >
  Complete Life LOB coverage (Term + Savings + ULIP) in the single 1SB integration
  service, move bank-owned models out of that service, replace Map-built JSON with
  typed payloads, and make poll/retry/circuit-breaker resilience explicit.

problem: >
  Only Term handlers are implemented; Savings is a scaffold; ULIP has no package.
  Handlers assemble 1SB bodies via LinkedHashMap.put. Bank domain models sit inside
  the 1SB app service. Circuit breaker is parked at Phase 5.5 while stakeholders now
  require Life LOB completeness and resilience in the current workstream.

proposed_solution: >
  Execute EPIC-002 story order: field guides first; extract bank models to a shared
  lib (e.g. libs/bank-common-domain or bank-canonical-model); introduce
  adapter.onesb.*.dto typed request/response records with ObjectMapper; implement
  Savings then ULIP handlers behind existing Lob* registries; document and wire
  poll max-attempts/backoff/stop and Resilience4j (or equivalent already approved)
  circuit breaker around OneSbHttpClient; prove with QA-012.

alternatives:
  - option: "Park Savings/ULIP until Phase 6+ / Health+Motor stable"
    rejected_because: "Stakeholder ADMIT-BYPASS via CR-014 withdraws SF3 parking for Life LOB adapter work"
  - option: "Keep Map payloads until second LOB lands"
    rejected_because: "Stakeholder requires typed models now; Map assembly is the defect being fixed"
  - option: "Expand WS-3 R0 catalogue to Savings/ULIP in the same CR"
    rejected_because: "Supplier adapter readiness ≠ R0 journey admission; WS-3 stays R1 for those product classes"

affected_components:
  - 1sb-integration-service
  - new or extended shared lib for bank-owned models
  - service-ssot field guides and PRODUCT-BACKLOG

files_expected:
  - docs/1sb-insurance-integration/field-guides/savings-quote.md
  - docs/1sb-insurance-integration/field-guides/ulip-quote.md
  - libs/... bank model types moved from services/1sb-integration-service/.../domain/model
  - adapter.onesb quote/proposal typed DTO classes
  - lob/life/saving/*Handler.java
  - lob/life/ulip/*Handler.java
  - Resilience / CB config under adapter.onesb.config
  - docs for poll/retry policy (service-ssot)
  - unit + WireMock tests for Savings/ULIP and CB
  - ArchUnit updates if package roots change

out_of_scope:
  - Health / Motor handlers
  - Annuity / Pension handlers
  - WS-3 R0 Savings/ULIP journey or catalogue
  - Redis idempotency
  - Editing CURRENT-STATE stage fields

data_changes: none
api_changes: >
  Public bank API keeps lob discriminator; new lob=SAVING and lob=ULIP become
  supported rather than LOB_NOT_SUPPORTED. No breaking change to Term contracts.
security_impact: >
  Outbound CB must not retry 401; no PII in CB/metrics tags; secrets unchanged.
compliance_impact: >
  Same consent/audit obligations as Term when Savings/ULIP proposal paths are enabled;
  no silent waiver of consentRef rules already scheduled for COMP-005.
backward_compatibility: compatible
performance_impact: "CB and typed mapping overhead expected negligible vs upstream RTT"
operational_impact: >
  New config keys for CB failure rate / wait duration; existing ONESB_POLL_* retained;
  runbook update for open-breaker 503.

testing:
  unit:
    - Typed payload serialisation golden fixtures for Term (refactor non-regression)
    - Savings and ULIP handler mapping tests
    - CB opens after consecutive failures; 401 never retried
  integration:
    - WireMock Life paths for Term + Savings + ULIP poll completion and timeout exhaustion
  evidence:
    - QA-012 report under module build/reports

review_boards_required:
  - Board 1 Architecture (packaging / anti-corruption) — human T4
  - Board 2 Engineering
  - Board 7 Operations (resilience policy)
  - Board 4 Security if egress/CB behaviour changes auth failure handling — human T4

drift_contract:
  files_expected_is_ceiling: true
  note: "Do not absorb Health/Motor or WS-3 catalogue work into this plan"
