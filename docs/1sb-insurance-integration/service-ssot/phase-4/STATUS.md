# Phase 4 — Status

**Branch:** `claude/1sb-integration-phase-4-p8s0bo`
**Scope:** Remaining P0 Term-path FUNC stories not yet built after Phase 3
(`FUNC-006-REVIEW.md` explicitly deferred insurer substatus to FUNC-009; `TECH-DEBT.md` TD-009
tracked the missing payment ports).

| Task | Owner | Status | Notes |
|------|-------|--------|-------|
| FUNC-007 Create payment session / URL | Dev | **Done** | TL review found 1 must-fix (`agentID` vs `rmEmployeeId` conflation) — fixed, re-reviewed, APPROVE |
| FUNC-009 Application status | Dev | **Done** | TL APPROVE (no must-fix) |

## Notes

- FUNC-007: `POST /v1/payments` → `OneSbPaymentAdapter` (`POST /v1/payment/url`) → persisted via
  new `HttpPaymentSessionStoreAdapter` against `bank-persistence-service`'s existing
  `PaymentSessionController`. Payability check via `JobStorePort` when `jobId` supplied → 409
  `PROPOSAL_NOT_PAYABLE`. Non-HTTPS upstream `paymentUrl` → 502 `UPSTREAM_BAD_RESPONSE` retryable.
  `paymentUrl` never logged/audited — only session/application refs. FUNC-008 (intimation) stays
  out of scope — port method stubbed, tracked as TD-022.
- FUNC-009: `GET /v1/status/{applicationNumber}` → `OneSbStatusAdapter`
  (`POST /LifeTerm/prostat/`) → `StatusService` normalises to `BankApplicationStatus` per the
  field-guide mapping table. Raw 1SB status never leaves the service (audit-only); 404 when 1SB
  has no manufacturer/product data for the applicationNo. No status-snapshot persistence (documented
  scope decision, not silently dropped).
- Both stories: no QA Engineer / QA Lead cycle run this phase (single-agent branch) — TL review
  only, per WORK-SEQUENCE §3 this is a variance from the full Functional P0 sequence; flagging here
  rather than silently skipping the step. `./gradlew :services:1sb-integration-service:test` and
  `:libs:bank-common-error:test` green, including `ArchitectureTest` and coverage gates, for every
  commit on this branch.
- Backlog error code `PROPOSAL_NOT_PAYABLE` added to shared `bank-common-error`; `TECH-DEBT.md`
  TD-022 opened for deferred FUNC-008.
