# PLAN-008 — FUNC-028 Life journey fail-before-1SB validation

As-built plan for the seven-board review. FUNC-028 was admitted under
`SUG-20261003-lvr` and implemented before this plan existed; `variance_log`
records that. Boards review this artefact ([11 §1](../11-REVIEW_GATES.md#1-the-board)).

```yaml
# schema: implementation-plan
id: PLAN-008
work_item: FUNC-028
origin: SUG-20261003-lvr
workstream: WS-1
risk_tier: T2
author: "agent:cursor-grok"
date: "2026-10-03"

objective: >
  1sb-integration-service rejects incomplete or unrecognized Term, Saving and
  ULIP quote, gate and proposal contracts locally so UAT can exercise Life
  journeys without posting skeletons to 1SB.

problem: >
  The Saving Service Readiness guide and the 1SB Insurance Gateway contract
  require typed quote fields, JSON (not HTML) 200s, usable gate/proposal
  schemas, visible-answer checks, consent on proposal submit, and recognized
  eligibility statuses. Before FUNC-028 the hub forwarded incomplete Saving
  quotes, treated reqId-only eligibility as ACCEPTED, and could POST a
  proposal when schema GET failed.

proposed_solution: >
  Fail-before-1SB LifeQuoteValidator plus LifeContractCatalog closed enums;
  UpstreamResponseGuard rejects non-JSON success; DynamicFormValidator
  usability and visibility-aware answers; ProposalService requires consentRef,
  fails closed on schema GET failure, and binds optional quoteJobId to a
  selectable offer; OneSbEligibilityAdapter last fallback is
  UNRECOGNIZED_STATUS. Config-blocked remainder stays parked as
  SUG-20261003-svg.

alternatives:
  - option: "Forward incomplete payloads and let 1SB reject"
    rejected_because: "UAT cannot distinguish hub defects from 1SB demo gaps; fail-closed is the standing WS-1 rule."
  - option: "Invent a complete Saving proposal and questionnaire engine"
    rejected_because: "Needs 1SB working product configuration — parked as SUG-20261003-svg."

affected_components:
  - services/1sb-integration-service application validation
  - services/1sb-integration-service proposal and eligibility adapters
  - bank POST /v1/quotes and POST /v1/proposals contracts

files_expected:
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/application/validation/LifeQuoteValidator.java
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/application/validation/LifeContractCatalog.java
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/application/validation/DynamicFormValidator.java
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/application/validation/UpstreamResponseGuard.java
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/application/ProposalService.java
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/application/QuoteService.java
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/adapter/onesb/eligibility/OneSbEligibilityAdapter.java
  - services/1sb-integration-service/src/main/java/com/bank/insurance/onesb/lob/life/saving/SavingQuoteHandler.java
  - docs/1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/governance/plans/PLAN-008-func-028-life-journey-validation.md
  - docs/1sb-insurance-integration/service-ssot/phase-3/FUNC-028-REVIEW.md

data_changes: none
api_changes: >
  breaking for bank callers — SAVING POST /v1/quotes requires
  preferences.savingsProductType; eligibility without a recognized flag is
  UNRECOGNIZED_STATUS not silent ACCEPTED; proposal submit requires consentRef
  and optional quoteJobId product identity
security_impact: none
compliance_impact: consent
backward_compatibility: >
  breaking — callers that omitted savingsProductType or treated reqId-only
  eligibility as accepted must send the field and handle UNRECOGNIZED_STATUS
performance_impact: "local validation only; no extra 1SB hop on the happy path; schema GET already required on proposal submit"
operational_impact: none

testing:
  unit:
    - "LifeQuoteValidatorTest — Term/Saving/ULIP pass; money, member, DOB, savings-list rejects"
    - "DynamicFormValidatorTest — empty schema, enum options, visible answers, type mismatches"
    - "ProposalServiceTest — empty schema, hidden mandatory, schema-fetch fail, quote-job mismatch, null LOB"
    - "OneSbEligibilityAdapterTest — UNRECOGNIZED_STATUS not ACCEPTED"
    - "SavingQuoteHandlerTest — list map and unknown-string default"
    - "UpstreamResponseGuardTest — HTML and empty body"
  integration:
    - "LifeLobRegressionIT / LifeLobJourneyCaptureIT — Saving prefs"
    - "ProposalSubmitIT / QuoteCriteriaIT — usable schema GET; recognized eligible flag"
  e2e: []
  other:
    - "./gradlew :services:1sb-integration-service:test jacocoTestCoverageVerification — 357 tests, 91.4% line / 72.5% branch"
    - "Application CI + Governance CI green on b1df87d"

rollback: >
  Revert the FUNC-028 commits. No schema or data migration. Bank callers that
  already send savingsProductType and handle UNRECOGNIZED_STATUS keep working.

dependencies:
  - "SUG-20261003-svg remains PARKED — 1SB working Saving configuration"

assumptions:
  - "R0 already includes Term and Savings/ULIP (CR-015); this plan does not add a LOB."
  - "1SB types stay in adapter.onesb.*; validators use bank commands only."

risks:
  - risk: "Bank callers of Saving quotes break until they send savingsProductType."
    mitigation: "Documented on FUNC-028 AC and this plan's api_changes; field-guide drift is a Product condition."
  - risk: "Live Gate POST and complete Proposal POST remain unverified on 1SB demo."
    mitigation: "Parked as SUG-20261003-svg; do not invent a complete proposal payload."

acceptance_criteria:
  - "Missing/invalid Life quote fields fail locally with VALIDATION_ERROR / MISSING_REQUIRED_FIELD before any 1SB POST."
  - "SAVING quotes require preferences.savingsProductType in {nonParticipating, Participating, ULIP}."
  - "Non-JSON HTTP 200 from 1SB is UPSTREAM_BAD_RESPONSE."
  - "Incomplete quote offers are dropped; unknown offer status is not exposed as selectable."
  - "Gate Criteria and proposal GET/submit reject unusable schemas (SCHEMA_INVALID) and invalid visible answers."
  - "Proposal submit without consentRef is CONSENT_REQUIRED; schema GET failure does not POST to 1SB."
  - "When quoteJobId is bound, productCode + manufacturerId must match a selectable offer."
  - "Eligibility without a recognized eligible/status flag is UNRECOGNIZED_STATUS, not ACCEPTED."
  - "Tests cover TERM, SAVING and ULIP; module JaCoCo stays ≥ 90% line / 70% branch."
  - "Field guide savings-quote.md states that bank SAVING POST /v1/quotes requires preferences.savingsProductType; silent ULIP default applies only after validation (PRODUCT condition, 11 §14)"

out_of_scope:
  - "Product UI min/max/increments at quote time (SUG-20261003-svg)"
  - "Habits/medical/occupation/avocation questionnaire engine (SUG-20261003-svg)"
  - "Authoritative 1SB enum inventory / master refresh (SUG-20260913-hms)"
  - "Live Gate Criteria POST and complete Proposal POST/poll on 1SB demo (SUG-20261003-svg)"
  - "Health or Motor LOB validation"
  - "Editing CURRENT-STATE.yaml stage fields or manufacturing T4 sign-off"

estimate: M
reviews:
  - board: ARCHITECTURE
    plan: PLAN-008
    work_item: FUNC-028
    reviewer: "Mahesh — Principal Insurance Platform Architect (agent)"
    reviewer_type: AGENT
    self_review: true
    round: 1
    date: "2026-10-03"
    decision: APPROVED
    must_fix: []
    conditions: []
    should_fix: []
    evidence:
      - "A1: validators live in application.validation; 1SB types remain under adapter.onesb.*"
      - "A2: LifeQuoteValidator / DynamicFormValidator sit on bank commands"
      - "A3: no new service, persistence, or event bus"
      - "A4: standing constraints held — no Flyway/JPA, no bank→1SB/DB, no PII in logs"
      - "A5: no ADR; CR-015 / ADR-015 unchanged"
      - "A6: no infrastructure"
      - "A7: breaking bank contract is revertible; no data migration"
      - "A8: WS-1 L7 + R0 Life fail-before-1SB is on-stage; svg remainder parked"
      - "A9: four validators + adapter classify fallback"
      - "A10: replace cost is the four Java types"
      - "RG-5 G6 did not fire: consent capture/retention/deletion unchanged; presence-check only"
      - "RG-5 G9 did not fire: GATE-P4 4.3 BLOCKED — no bank caller has consumed the contract"
    notes: >
      Authority class A1_AUTONOMOUS (T2 agent). Severity A3 for field-guide drift,
      owned by Product's condition. No T4 Architecture signature is manufactured.
  - board: TECHNICAL
    plan: PLAN-008
    work_item: FUNC-028
    reviewer: "Amit — Technical Head (agent)"
    reviewer_type: AGENT
    self_review: true
    round: 1
    date: "2026-10-03"
    decision: APPROVED
    must_fix: []
    conditions: []
    should_fix: []
    evidence:
      - "T1: Java 21 / Spring Boot / existing error catalogue; 357 tests green"
      - "T2: MISSING_REQUIRED_FIELD, VALIDATION_ERROR, SCHEMA_INVALID, CONSENT_REQUIRED, UPSTREAM_BAD_RESPONSE, UNRECOGNIZED_STATUS"
      - "T3: no new transactions; job-store remains HTTP"
      - "T4: breaking compat stated — Saving omit and reqId-only eligibility no longer succeed"
      - "T5: closed enums in LifeContractCatalog"
      - "T6: visibility-aware DynamicFormValidator replaces ProposalFormValidator.missingMandatory"
      - "T7: files_expected matches the as-built tree"
      - "T8: revert is sufficient; no Flyway"
    notes: "Checkstyle UnusedImports and JaCoCo 90/70 closed on b1df87d (91.4% line / 72.5% branch)."
  - board: PRODUCT
    plan: PLAN-008
    work_item: FUNC-028
    reviewer: "Rajal — Product Owner (agent)"
    reviewer_type: AGENT
    self_review: true
    round: 1
    date: "2026-10-03"
    decision: APPROVED_WITH_CONDITIONS
    must_fix: []
    conditions:
      - "Correct docs/1sb-insurance-integration/field-guides/savings-quote.md so bank SAVING POST /v1/quotes requires preferences.savingsProductType; silent ULIP default applies only after validation"
    should_fix: []
    evidence:
      - "P1: admitted SUG-20261003-lvr / WS-3 R0 / CR-015"
      - "P2: hub is correctly stricter than live 1SB"
      - "P3: no DIY, Health, Motor, or questionnaire engine"
      - "P4: FUNC-028 AC observable; remainder SUG-20261003-svg"
      - "P5: intended caller break on savingsProductType and UNRECOGNIZED_STATUS"
      - "P6: config-blocked work parked"
      - "P7: out_of_scope matches parked svg"
      - "P8: actor is bank/hub; LOB TERM/SAVING/ULIP"
      - "P9: bank names on commands; 1SB wire names in adapter.onesb.*"
      - "P10: P1-at-UAT remainder parked"
      - "P11: fail-closed codes defined"
      - "P12: evidence is the FUNC-028 suite + CI — not live UAT sign-off"
    notes: "Does not declare GATE-P4 4.3 met. Does not waive Compliance on consent. Does not close COMP-005."
  - board: SECURITY
    plan: PLAN-008
    work_item: FUNC-028
    reviewer: "Deepali — Security Architect (agent)"
    reviewer_type: AGENT
    self_review: true
    round: 1
    date: "2026-10-03"
    decision: APPROVED
    must_fix: []
    conditions: []
    should_fix: []
    evidence:
      - "S1: no authn/authz change"
      - "S2: no new PII persisted or logged"
      - "S3: no new secrets"
      - "S4: no crypto change"
      - "S5: untrusted quote/proposal/gate input validated before 1SB"
      - "S6: attack surface does not grow"
      - "S7: HTML-200 confusion addressed by UpstreamResponseGuard"
      - "S8: existing audit events remain"
      - "S9: no new dependencies"
      - "S10: fail-closed on schema GET failure and unrecognized eligibility"
      - "S11: 1SB trust contract unchanged"
      - "S12: blast radius is this service's Life quote/proposal/eligibility paths"
    notes: "SECURITY-L1. Plan security_impact is none for new exposure; board still sat S1–S12. T4 human sign-off not claimed."
  - board: QA
    plan: PLAN-008
    work_item: FUNC-028
    reviewer: "Swapnali — QA Lead (agent)"
    reviewer_type: AGENT
    self_review: true
    round: 1
    date: "2026-10-03"
    decision: APPROVED
    must_fix: []
    conditions: []
    should_fix: []
    evidence:
      - "Q1: each FUNC-028 AC maps to a named test"
      - "Q2: unit + WireMock IT; live Gate/Proposal POST not claimed"
      - "Q3: negatives cover omit/invalid type, HTML 200, empty schema, hidden mandatory, schema-fetch fail, quote-job mismatch, unrecognized eligibility"
      - "Q4: JaCoCo 91.4% line / 72.5% branch vs 90/70"
      - "Q5: Term/ULIP/Saving regression ITs updated"
      - "Q6: synthetic fixtures; no production PII"
      - "Q7: demonstrable via POST /v1/quotes 422 and UNRECOGNIZED_STATUS"
      - "Q8: no assumed unexecuted live E2E"
    notes: "GATE-P4 4.1 remains BLOCKED and is not re-opened."
  - board: RISK_COMPLIANCE
    plan: PLAN-008
    work_item: FUNC-028
    reviewer: "Shailja S — Compliance & Risk (agent)"
    reviewer_type: AGENT
    self_review: true
    round: 1
    date: "2026-10-03"
    decision: APPROVED
    must_fix: []
    conditions: []
    should_fix: []
    evidence:
      - "R1: no new IRDAI/PII obligation; LOB set already in CR-015"
      - "R2: consentRef presence-check fail-closed; capture/retention/deletion unchanged; COMP-005 not closed"
      - "R3: existing audit publisher paths unchanged"
      - "R4: no retention/deletion change"
      - "R5: no payment, maker-checker, or reconciliation change"
      - "R6: operational risk is stricter local reject"
      - "R7: SUG-20261003-lvr → FUNC-028 AC → named tests"
      - "R8: no new disclosure/reporting obligation"
    notes: "Agent T2 verdict only. Suitability-before-quote remains a standing constraint and is not implemented by this item."
  - board: OPERATIONS
    plan: PLAN-008
    work_item: FUNC-028
    reviewer: "Shivanshi — SRE / Operations (agent)"
    reviewer_type: AGENT
    self_review: true
    round: 1
    date: "2026-10-03"
    decision: NOT_APPLICABLE
    reason: "No deploy, config, secret, alert, runbook, capacity or rollback-surface change; revert of application code is sufficient"
    must_fix: []
    conditions: []
    evidence:
      - "O1–O8 screened: no new env vars, migrations, SLOs, pages, or scale recommendation"
    notes: "No scaling advice — no load shape changed."
variance_log:
  - date: "2026-10-03"
    change: "Plan recorded after implementation so the seven-board gate has a subject"
    reason: "FUNC-028 was admitted and coded under SUG-20261003-lvr without PLAN-00N; 11 §1 reviews the plan, not the raw diff"
    re_review: "FUNC-028-REVIEW.md round 1"
```

```yaml
# schema: approval-gate
plan: PLAN-008
work_item: FUNC-028
risk_tier: T2
round: 1
verdicts:
  ARCHITECTURE:
    decision: APPROVED
    reviewer_type: AGENT
    reviewer: "Mahesh — Principal Insurance Platform Architect (agent)"
    self_review: true
    evidence:
      - "A1–A10 plus RG-5 G6/G9 did not fire"
  TECHNICAL:
    decision: APPROVED
    reviewer_type: AGENT
    reviewer: "Amit — Technical Head (agent)"
    self_review: true
    evidence:
      - "T1–T8: 357 tests; fail-closed codes; breaking compat stated; revert sufficient"
  PRODUCT:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Rajal — Product Owner (agent)"
    self_review: true
    conditions:
      - "Correct savings-quote.md: bank SAVING quotes require savingsProductType"
    evidence:
      - "P1–P12: admitted UAT validation only; svg remainder parked; intended caller break"
  SECURITY:
    decision: APPROVED
    reviewer_type: AGENT
    reviewer: "Deepali — Security Architect (agent)"
    self_review: true
    evidence:
      - "S1–S12 SECURITY-L1: no new exposure; fail-closed input validation; T4 human sign-off not claimed"
  QA:
    decision: APPROVED
    reviewer_type: AGENT
    reviewer: "Swapnali — QA Lead (agent)"
    self_review: true
    evidence:
      - "Q1–Q8: AC-to-test map; JaCoCo 91.4/72.5; live Gate/Proposal POST not claimed"
  RISK_COMPLIANCE:
    decision: APPROVED
    reviewer_type: AGENT
    reviewer: "Shailja S — Compliance & Risk (agent)"
    self_review: true
    evidence:
      - "R1–R8: consentRef presence-check; COMP-005 not closed; T4 human sign-off not claimed"
  OPERATIONS:
    decision: NOT_APPLICABLE
    reviewer_type: AGENT
    reviewer: "Shivanshi — SRE / Operations (agent)"
    reason: "No deploy, alert, runbook or capacity change"
result: APPROVED
conditions_folded_into_ac:
  - "Correct savings-quote.md so bank SAVING POST /v1/quotes requires preferences.savingsProductType (PRODUCT)"
should_fix_registered_as: []
vetoes: none
human_signoffs: []
approved_on: "2026-10-03"
expires: "2026-11-02 or sooner if GATE-P4 / CR-015 / standing constraints change (11 §14)"
```
