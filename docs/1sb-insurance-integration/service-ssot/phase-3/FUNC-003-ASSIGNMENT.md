# Phase 3 — FUNC-003 Assignment (Team Lead)

**Backlog ID:** FUNC-003  
**Title:** Get quote job result (`GET /v1/quotes/{jobId}`)  
**Branch:** `cursor/func-003-get-quote-job-c259`  
**Base / depends on:** `cursor/func-002-term-quote-create-c259` (FUNC-002 Done)  
**Owner:** Dev A  
**QA cycle:** Yes  

## AC (PRODUCT-BACKLOG)

| # | AC | Proof |
|---|-----|-------|
| AC-1 | Returns status + offers when complete | MockMvc / IT |
| AC-2 | 404 unknown jobId | ErrorCodes.RESOURCE_NOT_FOUND |
| AC-3 | In-progress returns status **without fabricating offers** | Assert offers empty/absent while PENDING/RUNNING |

## Design

Harden existing thin GET from FUNC-002:

- Response DTO: `{ jobId, status, failureReason?, offers: [...] }`
- COMPLETED/PARTIAL → include offers from JobStore
- PENDING/RUNNING → `offers: []` (never invent)
- TIMEOUT/FAILED → status + failureReason; offers []
- Unknown → 404 RESOURCE_NOT_FOUND
- No Idempotency-Key on GET

## DoD

Tests `@Tag("FUNC-003")`; TL+QA APPROVE; PR with AC table.
