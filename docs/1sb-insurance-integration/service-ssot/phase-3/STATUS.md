# Phase 3 — Status

**Branch:** `cursor/func-005-submit-proposal-c259` (merged `main` for PR #19)  
**Assignment:** [FUNC-006-ASSIGNMENT.md](./FUNC-006-ASSIGNMENT.md)  
**Review:** [FUNC-006-REVIEW.md](./FUNC-006-REVIEW.md)  
**Prior:** [FUNC-001](./FUNC-001-ASSIGNMENT.md) · [FUNC-002](./FUNC-002-ASSIGNMENT.md) · [FUNC-005](./FUNC-005-ASSIGNMENT.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| FUNC-001 Master lookup API | Dev A | **Done** | on `main` (#8) |
| FUNC-002 Term quote create | Dev A | **Done** | `af33e65` / `66bfa4c` (#9) |
| FUNC-003 Get quote job result | Dev A | **Done** | `e25d7ff` |
| FUNC-004 Get proposal schema | Dev A | **Done** | `52cd397` |
| FUNC-005 Submit Term proposal | Dev A | **Done** | `56242ca` / `2a64388` |
| FUNC-006 Get proposal job result | Dev A | **Done** | `42dd011` |

## Notes

- FUNC-001: `POST /v1/master-data/lookup` with TTL cache, stale fallback; `/v1/master-data/**` idempotency exempt; `X-Master-Cache`. TD-010 Redis later.
- FUNC-002: TL+QA Lead APPROVE (Case 2, WireMock AC proof, QUOTE_COMPLETED, PARTIAL, R6).
- FUNC-003: TL+QA Lead APPROVE — status + offers; TIMEOUT → 200; 404 RESOURCE_NOT_FOUND.
- FUNC-004: TL+QA Lead APPROVE — schema pass-through + cache; 410 QUOTE_EXPIRED; upstream 5xx → UPSTREAM_UNAVAILABLE retryable.
- FUNC-005: TL+QA Lead APPROVE — AC-1…AC-5 + R6 `r6_missingIdempotencyKey_returns400` on `ProposalSubmitIT`.
- FUNC-006: TL+QA Lead APPROVE — applicationNumber when available; no fabricate in-progress; 404 RESOURCE_NOT_FOUND; persist + poll wiring.
