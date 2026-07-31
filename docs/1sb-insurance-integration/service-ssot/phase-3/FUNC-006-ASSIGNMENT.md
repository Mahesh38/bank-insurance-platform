# Phase 3 — FUNC-006 Assignment (Team Lead)

**Backlog ID:** FUNC-006  
**Title:** Get proposal job result  
**Branch:** `cursor/func-006-get-proposal-job-c259`  
**AC:** Poll result includes applicationNo when available; status normalised. 404 unknown. In-progress no fabricated applicationNo.

## Design
`GET /v1/proposals/{jobId}` → `{ jobId, status, applicationNumber?, failureReason?, offers?/empty }`  
Mirror FUNC-003 patterns for proposal jobs (jobType PROPOSAL).

## DoD
`@Tag("FUNC-006")`; TL+QA APPROVE.
