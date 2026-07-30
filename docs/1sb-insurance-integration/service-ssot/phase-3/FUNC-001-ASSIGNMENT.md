# Phase 3 — FUNC-001 Assignment (Team Lead)

**Backlog ID:** FUNC-001  
**Title:** Master lookup API  
**Branch:** `cursor/func-001-master-lookup-c259`  
**Base:** `main`  
**Owner (Dev):** Dev A  
**QA cycle:** Yes (functional P0)  
**Assigned:** 2026-07-30  
**Workflow:** [WORK-SEQUENCE.md](../WORK-SEQUENCE.md) · [ROLE-GUIDELINES-AND-DOD.md](../ROLE-GUIDELINES-AND-DOD.md)

---

## Story

As a bank app, I want `POST /v1/master-data/lookup` so I can fill enums without calling 1SB.

## Acceptance Criteria (PRODUCT-BACKLOG)

| # | AC | Must prove with test |
|---|-----|----------------------|
| AC-1 | Given `lob=TERM` + entityIds, When called, Then returns normalised enum lists | MockMvc + WireMock 1SB |
| AC-2 | Cache hit within TTL → no 1SB call | WireMock `verify(0, …)` on second call |
| AC-3 | 1SB down + stale cache → return stale with header/flag; no cache → 503 | WireMock fault after warm cache; cold → 503 |

## Technical design (binding)

| Layer | Deliverable |
|-------|-------------|
| API | `MasterDataController` → `POST /v1/master-data/lookup` |
| Application | `MasterDataService` — cache + port orchestration |
| Port | `OneSbMasterDataPort` |
| Adapter | `OneSbMasterDataAdapter` via `OneSbHttpClient.post("/v1/master/lookup", …)` |
| Cache | In-process ConcurrentHashMap + TTL (`insurance.masters.cache-ttl-seconds`, default 14400 = 4h). Redis later (TD-010). |
| Idempotency | **Exempt** `/v1/master-data/**` from IdempotencyFilter (read-like POST) |
| Errors | Validation → 422; upstream down no cache → 503 `UPSTREAM_UNAVAILABLE` |

### Bank request (canonical)

```json
{
  "lob": "TERM",
  "lookUpCategory": "quote",
  "entityIds": ["GENDER", "OCC", "TITLE"],
  "manufacturerId": null
}
```

### Bank response (normalised)

```json
{
  "lob": "TERM",
  "lookups": {
    "GENDER": [{"code": "M", "label": "M"}, {"code": "F", "label": "F"}],
    "OCC": [{"code": "SALARIED", "label": "SALARIED"}]
  },
  "cache": { "hit": false, "stale": false }
}
```

Stale success: HTTP 200 + response `cache.stale=true` + header `X-Master-Cache: STALE`.

## Out of scope

- Redis cache
- Vehicle master chain (Motor)
- Saving/Health LOB-specific masters beyond passing `lob` through

## DoD (Developer)

Per ROLE D-1…D-8 + TESTING-RULES R2/R5 + QA-004 spirit (unit + slice/WireMock).

## Review gates

1. Dev delivery → TL review  
2. Fix loop if CHANGES_REQUESTED  
3. QA scenarios on SHA  
4. QA Lead sign-off  
5. TL marks Done + opens PR
