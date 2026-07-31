# Phase 3 — Status

**Branch:** `cursor/func-001-master-lookup-c259` (rebased onto `main` after FUNC-002)  
**Assignment:** [FUNC-001-ASSIGNMENT.md](./FUNC-001-ASSIGNMENT.md) · [FUNC-002-ASSIGNMENT.md](./FUNC-002-ASSIGNMENT.md)  
**Review:** [FUNC-001-REVIEW.md](./FUNC-001-REVIEW.md) · [FUNC-002-REVIEW.md](./FUNC-002-REVIEW.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| FUNC-001 Master lookup API | Dev A | **Done** | rebased onto main |
| FUNC-002 Term quote create | Dev A | **Done** | `af33e65` (fixes) / `66bfa4c` (feature) |

## Notes

- FUNC-001: `POST /v1/master-data/lookup` with in-process TTL cache, stale fallback, WireMock AC; `/v1/master-data/**` idempotency exempt; `X-Master-Cache: HIT|MISS|STALE`.
- FUNC-002: TL+QA Lead APPROVE (Case 2, WireMock AC proof, QUOTE_COMPLETED, PARTIAL, R6). Thin `GET /v1/quotes/{jobId}` included; FUNC-003 hardens GET AC.
- TD-010: Redis-backed master cache (in-process for now).
