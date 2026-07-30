# Phase 3 — Status

**Branch:** `cursor/func-002-term-quote-create-c259`  
**Assignment:** [FUNC-002-ASSIGNMENT.md](./FUNC-002-ASSIGNMENT.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| FUNC-002 Term quote create | Dev A | In review | `1d0ba96` |

## Dev A notes

- **FUNC-002:** `QuoteService` + `TermQuoteHandler` + `LobQuoteHandlerRegistry` + `OneSbQuoteAdapter`; `POST /v1/quotes` → 202 `{jobId, status}`; `AsyncJobPoller.scheduleQuotePoll` completes with offers / `POLL_TIMEOUT`; `getQuoteResult` maps `TIMEOUT` → `QUOTE_TIMEOUT` retryable; ErrorCodes `QUOTE_TIMEOUT` / `UNSUPPORTED_LOB`.
- Tests tagged `@Tag("FUNC-002")`: QuoteServiceTest, QuoteControllerTest, TermQuoteHandlerTest, OneSbQuoteAdapterTest, AsyncJobPollerQuoteTest.
