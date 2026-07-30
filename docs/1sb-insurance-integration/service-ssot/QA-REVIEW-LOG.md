# Baseline QA backlog — dual review log

**Developer:** QA test-resolution specialist  
**Branch:** `cursor/phase1-foundations-c259`  
**Process:** Implement item → Tech Lead + QA Lead review → fix until both APPROVE

| ID | Dev delivery | TL | QA Lead | Final |
|----|--------------|-----|---------|-------|
| QA-001 | `008ec04` JaCoCo + gates | **APPROVE** | **APPROVE** | Approved 2026-07-30 (non-blocking doc polish applied) |
| QA-002 | Persistence API tests + services gate 50% + validation 400 fix | **APPROVE** | **APPROVE** | Approved 2026-07-30. TL re-review: `GlobalExceptionHandler` → 400/`VALIDATION_ERROR`/`MISSING_REQUIRED_FIELD`; `JobApiTest.createJob_missingRequiredField_returns400_withValidationError` green. |
| QA-003 | `OneSbConnectivityIT` (IT-I dual WireMock) | — | **APPROVE** | QA Lead 2026-07-30: R3 `*IT`+`@Tag("integration")`; R4 no sandbox (WireMock 1SB+persistence + `@DynamicPropertySource`); AssertJ + WireMock `verify(exactly(1))`/Basic; `application-test.yml` stub secrets + validator skip on `test`. Awaiting TL. |
| QA-006 | Pending | — | — | — |
