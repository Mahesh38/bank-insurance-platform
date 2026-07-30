# Baseline QA backlog — dual review log

**Developer:** QA test-resolution specialist  
**Branch:** `cursor/phase1-foundations-c259`  
**Process:** Implement item → Tech Lead + QA Lead review → fix until both APPROVE

| ID | Dev delivery | TL | QA Lead | Final |
|----|--------------|-----|---------|-------|
| QA-001 | `008ec04` JaCoCo + gates | **APPROVE** | **APPROVE** | Approved 2026-07-30 (non-blocking doc polish applied) |
| QA-002 | Persistence API tests + services gate 50% + validation 400 fix | Re-review | **APPROVE** | Blocking closed: `JobApiTest.createJob_missingRequiredField_returns400_withValidationError` asserts `problem+json`, `VALIDATION_ERROR`, `MISSING_REQUIRED_FIELD`, field `lob` (400). Happy-path + 404 coverage previously accepted. |
| QA-003 | Pending | — | — | — |
| QA-006 | Pending | — | — | — |
