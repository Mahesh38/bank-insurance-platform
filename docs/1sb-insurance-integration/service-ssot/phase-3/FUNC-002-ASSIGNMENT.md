# Phase 3 — FUNC-002 Assignment (Team Lead)

**Backlog ID:** FUNC-002  
**Title:** Create Term quote job (`POST /v1/quotes` with `lob=TERM`)  
**Branch:** `cursor/func-002-term-quote-create-c259`  
**Base:** `main`  
**Depends on:** Phase 2 connectivity (Done on main). Masters (FUNC-001) parallel OK — not a hard code dependency.  
**Owner:** Dev A  
**QA cycle:** Yes (functional P0)  
**Workflow:** WORK-SEQUENCE.md

---

## Acceptance Criteria (PRODUCT-BACKLOG)

| # | AC | Test proof required |
|---|-----|---------------------|
| AC-1 | Valid Term multi-quote → job created + 1SB Term quote via `QuoteService` → `TermQuoteHandler` | MockMvc + WireMock + JobStore mock/WireMock |
| AC-2 | Missing required fields → 422, **no** 1SB call | WireMock `verify(0)` |
| AC-3 | 1SB pending → internal poll completes with offers | Poller + WireMock pending→complete (may reuse AsyncJobPoller) |
| AC-4 | Poll timeout → `QUOTE_TIMEOUT` retryable | Unit/IT |
| AC-5 | Per-insurer errors with some success → `PARTIAL` | Mapping test |
| AC-6 | Audit `QUOTE_CREATED` / completion | Mock publisher verify |
| AC-7 | Response primary id = bank `jobId` only (not raw 1SB `reqId`) | Assert JSON |

## Design (binding)

```text
POST /v1/quotes + Idempotency-Key
  → QuoteController
  → QuoteService (application)
       → JobStorePort.createJob
       → LobQuoteHandlerRegistry → TermQuoteHandler
       → OneSbQuotePort / handler posts via OneSbHttpClient
            POST /insurance/lifeterm/v1/quote
       → JobStorePort.updateJobPolling(reqId)
       → AsyncJobPoller.schedulePoll
  ← 202 { jobId, status: PENDING|RUNNING }
```

- LOB discriminator: request `lob` must be `TERM` for this story (reject others with 422 for now or unsupported).
- Saving handler remains stub / unsupported.
- Offer normalisation: map insurer product premium fields into `QuoteOffer` domain; on poll complete call `completeJob`.
- Error code: add `QUOTE_TIMEOUT` to ErrorCodes if missing.
- AuditActions: add QUOTE_CREATED / QUOTE_COMPLETED if missing.

## Minimum request shape (bank)

```json
{
  "lob": "TERM",
  "journeyId": "…",
  "sumAssured": 5000000,
  "members": [{ "dob": "1990-01-15", "gender": "M" }],
  "distribution": { "agentId": "109337" }
}
```

(Keep KISS — validate lob + members non-empty + sumAssured; map to a minimal 1SB JSON body in TermQuoteHandler.)

## DoD

D-1…D-8, R2/R5, `@Tag("FUNC-002")`, `./gradlew test` green.

## Out of scope

- FUNC-003 GET (separate backlog — may add thin GET if needed for poller demos, but prefer separate PR)
- Health/Motor handlers
- Real sandbox credentials in tests
