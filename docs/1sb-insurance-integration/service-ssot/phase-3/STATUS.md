# Phase 3 — Status

<<<<<<< HEAD
**Branch:** `cursor/func-002-term-quote-create-c259`  
**Assignment:** [FUNC-002-ASSIGNMENT.md](./FUNC-002-ASSIGNMENT.md)  
**Review:** [FUNC-002-REVIEW.md](./FUNC-002-REVIEW.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| FUNC-002 Term quote create | Dev A | **Done** | `af33e65` (fixes) / `66bfa4c` (feature) |

## Notes

- TL+QA Lead APPROVE after review loop (Case 2, WireMock AC proof, QUOTE_COMPLETED, PARTIAL, R6).
- AC-1…AC-7 accepted.
- Thin `GET /v1/quotes/{jobId}` included for timeout mapping; FUNC-003 will harden GET AC.
=======
| Story | Status | Owner | Notes |
|-------|--------|-------|-------|
| FUNC-001 Master lookup API | **In review** | Dev A | Branch `cursor/func-001-master-lookup-c259`. `POST /v1/master-data/lookup` with in-process TTL cache, stale fallback, WireMock AC coverage. |

## FUNC-001 delivery summary

- API: `MasterDataController` + DTOs; validation → 422 `VALIDATION_ERROR`
- Application: `MasterDataService` ConcurrentHashMap cache (`insurance.masters.cache-ttl-seconds`, default 14400)
- Port: `OneSbMasterDataPort` / adapter `OneSbMasterDataAdapter` → `POST /v1/master/lookup`
- Idempotency: `/v1/master-data/**` exempt
- Header: `X-Master-Cache: HIT|MISS|STALE`
- Tests: `@Tag("FUNC-001")` unit + MockMvc + WireMock IT (AC-1/2/3)

## Tech debt

- TD-010: Redis-backed master cache (in-process for now)
>>>>>>> 98dc187 (feat(func-001): POST /v1/master-data/lookup with TTL cache and stale fallback)
