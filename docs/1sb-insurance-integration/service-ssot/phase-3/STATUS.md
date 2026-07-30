# Phase 3 — Status

**Branch:** `cursor/func-005-submit-proposal-c259`  
**Assignment:** [FUNC-005-ASSIGNMENT.md](./FUNC-005-ASSIGNMENT.md)  
**Review:** [FUNC-005-REVIEW.md](./FUNC-005-REVIEW.md)  
**Prior:** [FUNC-004-ASSIGNMENT.md](./FUNC-004-ASSIGNMENT.md) · [FUNC-004-REVIEW.md](./FUNC-004-REVIEW.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| FUNC-002 Term quote create | Dev A | **Done** | `af33e65` (fixes) / `66bfa4c` (feature) |
| FUNC-003 Get quote job result | Dev A | **Done** | `e25d7ff` |
| FUNC-004 Get proposal schema | Dev A | **Done** | `52cd397` |
| FUNC-005 Submit Term proposal | Dev A | **CHANGES_REQUESTED** | `2a64388` — R6 missing Idempotency-Key IT |

## Notes

- FUNC-002: TL+QA Lead APPROVE (Case 2, WireMock AC proof, QUOTE_COMPLETED, PARTIAL, R6).
- FUNC-003: TL+QA Lead APPROVE — status + offers; TIMEOUT → 200; 404 RESOURCE_NOT_FOUND.
- FUNC-004: TL+QA Lead APPROVE — schema pass-through + cache; 410 QUOTE_EXPIRED; upstream 5xx → UPSTREAM_UNAVAILABLE retryable.
- FUNC-005: TL+QA Lead **CHANGES_REQUESTED** — AC-1…AC-5 pass; add `r6_missingIdempotencyKey_returns400` on `ProposalSubmitIT` (mirror QuoteCreateIT).
