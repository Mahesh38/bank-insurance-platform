# Phase 4 — FUNC-009 Assignment (Team Lead)

**Backlog ID:** FUNC-009
**Title:** Application status
**Branch:** `claude/1sb-integration-phase-4-p8s0bo`
**Depends on:** Phase 3 complete (FUNC-001…006) — Done. Richer status fields were explicitly deferred
here by FUNC-006-REVIEW.md ("Richer architecture fields (`requirements`, insurer substatus) deferred
to FUNC-009").

**AC (from PRODUCT-BACKLOG.md):**
- Maps 1SB `applicationStatus` values to the bank stage enum (field-guide normalisation table).
- Returns manufacturer substatus for RM display.
- 404 if not found.

## Design

`GET /v1/status/{applicationNumber}?lob=TERM&insurerCode=HDFC&productCode=T1` (`insurerCode` required —
1SB's status API requires `insuranceCompanyCode`; `productCode` optional/situational per field guide).

- `StatusUseCase` (inbound port) + `StatusService` (application layer) — calls `OneSbStatusPort` then
  normalises the raw 1SB `applicationStatus` string to `BankApplicationStatus`
  (`QUOTING | PROPOSAL | UW_REQUIREMENTS | UW_IN_PROGRESS | CUSTOMER_DECISION | PAYMENT | ISSUED | CLOSED | UNKNOWN`)
  per `field-guides/application-status.md` mapping table. Normalisation lives in `StatusService` (pure
  function, no adapter/Spring dependency) exactly as architecture §6.4 sequence shows.
- `OneSbStatusPort` → `OneSbStatusAdapter` calling `POST /LifeTerm/prostat/` and parsing
  `manufacturer[0].product[0].applicationStatus` / `.policyDetails`. Empty/absent `manufacturer[]` →
  `Optional.empty()` → service throws `404 RESOURCE_NOT_FOUND` (1SB returns 200 with an empty result
  for an unknown `applicationNo`, not a 4xx, per field guide).
- Response omits raw 1SB status text (compliance §8.1 — "rawStatus... not surfaced in the response");
  only `bankStatus` (normalised) and `manufacturerSubStatus` (for RM display) are returned. Raw status
  is still an audit metadata field (`APPLICATION_STATUS_CHECKED`).
- No status-snapshot persistence (architecture §6.4 shows an optional `JS` upsert) — out of scope for
  this story: no `status_snapshot` table exists in `bank-persistence-service`, latency table §7.2 marks
  status as "not cached" already, and the AC does not require persistence. Live call each time (KISS).
  Deferred as a documented gap, not silently dropped.

## DoD

`@Tag("FUNC-009")` on tests; unit tests for happy path + validation + not-found + upstream 5xx; WireMock
IT; TL review.
