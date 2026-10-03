# FUNC-028 — AIGEM seven-board review (PLAN-008)

**Work item:** FUNC-028 · **Origin:** `SUG-20261003-lvr` · **Tier:** T2  
**Stage:** WS-1 L7 Hardening + WS-3 R0 Life (CR-015)  
**Freshness:** FRESH 2026-10-03 ([BOOT.md](../../../context/BOOT.md) §5)  
**Self-review:** agent that implemented FUNC-028 also sat the boards (`self_review: true`, [11 §2](../../../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board)). T2 permits a full agent gate. No T4 human seat is claimed.

T4 triggers ([11 §3](../../../governance/11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory) RG-5) did **not** fire: no trust-boundary, authn/authz, crypto, or exposure change. Fail-closed validation stays inside `1sb-integration-service`.

Mandatory at T2: Technical, Product, QA. Architecture ran because bank contracts moved. Security ran because input validation / fail-closed is `security_impact ≠ none` in substance (plan records `none` for *new* exposure). Risk & Compliance ran because `consentRef` is now enforced. Operations screened `NOT_APPLICABLE`.

---

## Board 1 — Architecture (Mahesh)

**Question:** Does this belong here, shaped like this?  
**Severity:** `A3` (field-guide drift) · **Authority class:** `A1_AUTONOMOUS` (T2 agent)

```yaml
# schema: review-verdict
board: ARCHITECTURE
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
  - "A1: validators live in application.validation; 1SB types remain under adapter.onesb.* — ArchUnit standing constraint held"
  - "A2: LifeQuoteValidator / DynamicFormValidator sit on bank commands; SavingQuoteHandler only maps after validation"
  - "A3: no new service, no new persistence, no new event bus; coupling is directional (application → catalog → adapter)"
  - "A4: bank apps still never call 1SB or DB; no Flyway/JPA in 1sb-integration-service; no PII in logs"
  - "A5: no ADR — no new boundary or provider-route decision; CR-015 / ADR-015 unchanged"
  - "A6: no infrastructure"
  - "A7: breaking bank contract is local and reversible by revert; no data migration"
  - "A8: WS-1 L7 + R0 Life fail-before-1SB is on-stage; questionnaire engine correctly parked (SUG-20261003-svg)"
  - "A9: smallest structural change — four validators + adapter classify fallback"
  - "A10: replace cost is the four Java types; 1SB path names stay in the adapter"
  - "RG-5 G6 did not fire: consent capture/retention/deletion unchanged; presence-check only"
  - "RG-5 G9 did not fire: GATE-P4 4.3 BLOCKED — no bank caller has consumed the contract"
notes: >
  Authority class A1_AUTONOMOUS (T2 agent). Severity A3 for field-guide drift,
  owned by Product's condition. 1SB remains a provider route, not a domain
  dependency. No T4 Architecture signature is manufactured.
```

---

## Board 2 — Technical (Amit)

**Question:** Will this work, and can we live with it?

```yaml
# schema: review-verdict
board: TECHNICAL
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
  - "T1: Java 21 / Spring Boot / existing error catalogue; compile + 357 tests green"
  - "T2: error paths are first-class — MISSING_REQUIRED_FIELD, VALIDATION_ERROR, SCHEMA_INVALID, CONSENT_REQUIRED, UPSTREAM_BAD_RESPONSE, UNRECOGNIZED_STATUS"
  - "T3: no new transactions; job-store remains HTTP; no concurrency model change"
  - "T4: backward_compatibility is breaking and stated on PLAN-008 — Saving omit and reqId-only eligibility no longer succeed"
  - "T5: closed enums in LifeContractCatalog; product-specific lists stay out"
  - "T6: ProposalFormValidator.missingMandatory removed in favour of visibility-aware DynamicFormValidator — no second form engine"
  - "T7: files_expected matches the as-built tree (validators, ProposalService, adapters, tests, registers)"
  - "T8: revert is sufficient; no Flyway, no published external OpenAPI freeze in this repo"
notes: >
  Checkstyle UnusedImports and JaCoCo 90/70 were CI defects of this change and
  are closed on b1df87d. Coverage measured 91.4% line / 72.5% branch.
```

---

## Board 3 — Product (Rajal)

**Question:** Is this the thing we asked for — and only that?

```yaml
# schema: review-verdict
board: PRODUCT
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
  - "Correct docs/1sb-insurance-integration/field-guides/savings-quote.md so bank SAVING POST /v1/quotes requires preferences.savingsProductType; silent ULIP default applies only after validation (ULIP LOB omit, or SavingQuoteHandler mapping of a valid value)"
should_fix: []
evidence:
  - "P1: UAT-ready Life validation for Term/ULIP/Savings is the admitted SUG-20261003-lvr outcome and matches WS-3 R0 / CR-015"
  - "P2: fail-before-1SB matches the Saving readiness guide's hub checks; live 1SB remains looser — hub is correctly stricter"
  - "P3: no DIY, Health, Motor, or questionnaire engine bundled"
  - "P4: PRODUCT-BACKLOG FUNC-028 AC is observable (codes + no 1SB POST); remainder named SUG-20261003-svg"
  - "P5: intended experience change — Saving callers must send savingsProductType; eligibility can now surface UNRECOGNIZED_STATUS"
  - "P6: config-blocked work parked, not silently built"
  - "P7: out_of_scope on PLAN-008 matches the parked svg row"
  - "P8: actor is bank/hub; LOB TERM/SAVING/ULIP; no new journey state"
  - "P9: bank commands keep bank names; 1SB wire names stay in adapter.onesb.*"
  - "P10: P1-at-UAT remainder is parked, not bundled"
  - "P11: fail-closed codes defined; complete proposal/Gate POST left as parked exception outcomes"
  - "P12: evidence path is the FUNC-028 test suite + CI green — not a live UAT sign-off"
notes: >
  This board does not declare GATE-P4 4.3 (bank caller in UAT) met. That remains
  BLOCKED on DEP-001/002. Product does not waive Compliance on consent.
```

---

## Board 4 — Security (Deepali)

**Question:** What does this expose, what can be abused, and are the controls enough?  
**Depth:** SECURITY-L1 · **Severity:** `S3` · **Veto:** none  
T4 human Security sign-off is **not** claimed.

```yaml
# schema: review-verdict
board: SECURITY
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
  - "S1: no authn/authz change; same BFF/bank callers"
  - "S2: no new PII fields persisted or logged; existing PiiMasker / no-PII-in-logs constraint unchanged"
  - "S3: no new secrets; distributorId still from SecretProvider"
  - "S4: no crypto change"
  - "S5: untrusted quote/proposal/gate input validated at the application boundary before 1SB"
  - "S6: attack surface does not grow; one fewer incomplete outbound 1SB POST"
  - "S7: injection/HTML-200 confusion addressed by UpstreamResponseGuard; no new SSRF or object-level authz path"
  - "S8: existing audit events remain; CONSENT_REQUIRED path already audited"
  - "S9: no new dependencies"
  - "S10: fail-closed on schema GET failure, empty/unusable schema, unrecognized eligibility"
  - "S11: 1SB trust contract unchanged (Basic auth, existing HTTP client, no 401 retry)"
  - "S12: blast radius remains this service's Life quote/proposal/eligibility paths"
notes: >
  Agent T2 verdict only. Residual risk is caller breakage, not a new exposure.
```

---

## Board 5 — QA (Swapnali)

**Question:** How will we know it works — and know when it breaks?

```yaml
# schema: review-verdict
board: QA
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
  - "Q1: each FUNC-028 AC maps to a named test (LifeQuoteValidatorTest, DynamicFormValidatorTest, ProposalServiceTest, OneSbEligibilityAdapterTest, QuoteCriteriaIT, ProposalSubmitIT)"
  - "Q2: unit + WireMock IT; live 1SB Gate POST / complete Proposal is explicitly not claimed (parked svg)"
  - "Q3: negatives cover omit/invalid savings type, HTML 200, empty schema, hidden mandatory, schema-fetch fail, quote-job mismatch, unrecognized eligibility"
  - "Q4: jacocoTestCoverageVerification passed — 91.4% line / 72.5% branch vs 90/70 (COVERAGE.md)"
  - "Q5: Term/ULIP/Saving regression ITs updated; IT fixtures stub usable schema GET and recognized eligible"
  - "Q6: fixtures use synthetic DOB/pincode/income; no production PII"
  - "Q7: demonstrable via POST /v1/quotes 422 on omit savingsProductType and eligibility UNRECOGNIZED_STATUS"
  - "Q8: tests tagged FUNC-028 / unit; no assumed unexecuted live E2E"
notes: >
  Unexecuted live Gate POST and complete Proposal POST stay PARTIAL / parked.
  That is not a Q0 hold on the admitted hub AC. GATE-P4 4.1 sandbox E2E is
  still BLOCKED and is not re-opened here.
```

---

## Board 6 — Risk & Compliance (Shailja S)

**Question:** Can we defend this to a regulator?  
**Severity:** `R3` · T4 human Risk & Compliance sign-off is **not** claimed.

```yaml
# schema: review-verdict
board: RISK_COMPLIANCE
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
  - "R2: consentRef presence-check fail-closed (CONSENT_REQUIRED); capture/retention/deletion unchanged; COMP-005 not closed"
  - "R3: existing audit publisher paths unchanged; actor/idempotency already on the command"
  - "R4: no retention/deletion change"
  - "R5: no payment, maker-checker, or reconciliation change"
  - "R6: operational risk is stricter local reject, not a silent accept of unrecognized eligibility"
  - "R7: SUG-20261003-lvr → FUNC-028 AC → named tests"
  - "R8: no new disclosure/reporting obligation"
notes: >
  Suitability-before-quote remains a standing constraint and is not implemented
  by this item. Agent T2 verdict only.
```

---

## Board 7 — Operations (Shivanshi)

**Question:** Can we run, observe, and recover this?

```yaml
# schema: review-verdict
board: OPERATIONS
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
notes: >
  CI minutes unchanged in kind. No scaling advice is offered because no load
  shape changed.
```

---

## Aggregation

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

**Gate result:** `APPROVED` (T2 agent, self-reviewed). Product's field-guide condition is folded into PLAN-008 `acceptance_criteria` ([11 §14](../../../governance/11-REVIEW_GATES.md#14-post-approval)). No board returned `REWORK` or `REJECTED`. No T4 human signature is manufactured. The field-guide correction is not implemented in this review turn.

**Not claimed:** GATE-P4 4.1 sandbox E2E · 4.3 bank-caller UAT · live Gate POST · complete Proposal E2E (`SUG-20261003-svg`).
