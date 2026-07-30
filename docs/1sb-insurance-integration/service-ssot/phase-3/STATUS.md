# Phase 3 — Status

**Branch:** `cursor/func-004-proposal-schema-c259`  
**Assignment:** [FUNC-004-ASSIGNMENT.md](./FUNC-004-ASSIGNMENT.md)  
**Prior:** [FUNC-003-ASSIGNMENT.md](./FUNC-003-ASSIGNMENT.md) · [FUNC-003-REVIEW.md](./FUNC-003-REVIEW.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| FUNC-002 Term quote create | Dev A | **Done** | `af33e65` (fixes) / `66bfa4c` (feature) |
| FUNC-003 Get quote job result | Dev A | **Done** | `e25d7ff` |
| FUNC-004 Get proposal schema | Dev A | **Done** | `52cd397` |

## Notes

- FUNC-002: TL+QA Lead APPROVE (Case 2, WireMock AC proof, QUOTE_COMPLETED, PARTIAL, R6).
- FUNC-003: TL+QA Lead APPROVE — status + offers; TIMEOUT → 200; 404 RESOURCE_NOT_FOUND.
- FUNC-004: TL+QA Lead APPROVE — schema pass-through + cache; 410 QUOTE_EXPIRED; upstream 5xx → UPSTREAM_UNAVAILABLE retryable.
