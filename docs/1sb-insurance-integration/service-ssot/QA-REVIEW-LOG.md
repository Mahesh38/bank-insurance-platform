# Baseline QA backlog — dual review log

**Developer:** QA test-resolution specialist  
**Branch:** `cursor/phase1-foundations-c259`  
**Process:** Implement item → Tech Lead + QA Lead review → fix until both APPROVE

| ID | Dev delivery | TL | QA Lead | Final |
|----|--------------|-----|---------|-------|
| QA-001 | `008ec04` JaCoCo + gates | **APPROVE** | **APPROVE** | Approved 2026-07-30 (non-blocking doc polish applied) |
| QA-002 | Persistence API tests + services gate 50% | Pending | **CHANGES_REQUESTED** | Blocking: missing `GlobalExceptionHandler` validation 400 (`MethodArgumentNotValidException` → `ErrorCodes.VALIDATION_ERROR` / field `MISSING_REQUIRED_FIELD`). Happy-path jobs/offers/payments/audit + 404/`RESOURCE_NOT_FOUND` OK; synthetic data + tags/naming OK; no assertNotNull-only. |
| QA-003 | Pending | — | — | — |
| QA-006 | Pending | — | — | — |
