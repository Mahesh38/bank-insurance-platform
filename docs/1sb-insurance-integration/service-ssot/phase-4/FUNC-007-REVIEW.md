# Phase 4 — FUNC-007 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | FUNC-007-ASSIGNMENT.md |
| Implement | Dev | `PaymentController` / `PaymentService` / `OneSbPaymentAdapter` / `HttpPaymentSessionStoreAdapter` |
| Review (iter 1) | TL | **CHANGES_REQUESTED** |
| Fix | Dev | `agentId` vs `rmEmployeeId` separated |
| Review (iter 2) | TL | **APPROVE** |

## Iteration 1 — CHANGES_REQUESTED

**Must-fix:**

1. **`agentID` conflated with `rmEmployeeId`.** `OneSbPaymentAdapter.buildPayload` sent
   `command.rmEmployeeId()` straight through as the 1SB `Distributor.agentID` field. Architecture
   §8.5 treats `agentID` (insurer-facing SP/POSP/BQP code) and `rmEmployeeId` (bank internal RM id)
   as distinct identifiers requiring separate resolution — exactly the distinction
   `TermProposalHandler.resolveAgentId` already respects for proposals (`agentId` vs
   `distribution.rmEmployeeId`, never substituting one for the other). Sending the bank employee id
   to the insurer as if it were their own agent code is wrong and would corrupt attribution once a
   real agent-mapping table exists. **Fix:** add a distinct `agentId` on
   `CreatePaymentRequest.DistributionRequest` (mirroring `SubmitProposalRequest.DistributionRequest`
   shape) and `CreatePaymentCommand`; adapter uses `command.agentId()` for `Distributor.agentID`;
   `rmEmployeeId` stays audit-only.

**Non-blocking (soft notes, tracked not fixed):**

- `PAYABLE_JOB_STATUSES` includes `PARTIAL`, which never actually occurs for `PROPOSAL` jobs
  today (`HttpJobStoreAdapter.resolveCompleteStatus` only returns `PARTIAL` when the job has a mix
  of successful/failed *offers*, and proposal completion always posts an empty offer list). Harmless
  defensive superset — left as-is, matches the same non-blocking style as FUNC-006-REVIEW's soft
  notes.
- Payability is only checked when the caller supplies `jobId`; when omitted, the caller's own
  `applicationNumber` is trusted (architecture's `POST /v1/payments` sample doesn't carry `jobId`
  either). This was called out as an accepted design tradeoff in FUNC-007-ASSIGNMENT.md — not new
  scope for this review.

## Iteration 2 — APPROVE

Must-fix #1 addressed (`4d2f8c1`-equivalent working-tree fix — see commit). Re-ran
`./gradlew :services:1sb-integration-service:test :libs:bank-common-error:test` — green, including
`ArchitectureTest`, `jacocoTestCoverageVerification`.

**AC acceptance:**

| AC | Verdict | Proof |
|----|---------|-------|
| Payable application → HTTPS `paymentUrl` + expiry/ref | Pass | `PaymentCreateIT.ac1_happyPath_*`, `PaymentServiceTest.createPaymentSession_happyPath_*`, `OneSbPaymentAdapterTest.createPaymentUrl_happyPath_*` |
| `paymentUrl` not logged (ref only) | Pass | `PaymentServiceTest.createPaymentSession_happyPath_*` asserts audit metadata never contains the url; no log statement anywhere touches `paymentUrl` |
| Non-payable status → 409 `PROPOSAL_NOT_PAYABLE` | Pass | `PaymentServiceTest.createPaymentSession_jobNotCompleted_*` / `_jobIdUnknown_*`; `PaymentCreateIT.ac2_jobNotPayable_*`; `PaymentControllerTest.createPaymentSession_notPayable_*` |
| Audit `PAYMENT_URL_RETRIEVED` | Pass | `PaymentServiceTest.createPaymentSession_happyPath_*`, `PaymentCreateIT.ac1_*` |

Additional coverage beyond the AC text: non-HTTPS upstream URL → `502 UPSTREAM_BAD_RESPONSE`
retryable (`FUNCTIONAL-NFR-COMPLIANCE-MAP.md` transport-security row); missing `Idempotency-Key` →
`400` (existing global filter, verified end-to-end here too); validation (missing `lob`/`payer`) →
`422`.

**Deferred / out of scope (unchanged from assignment):** FUNC-008 payment intimation
(`OneSbPaymentPort.sendPaymentIntimation` throws `UnsupportedOperationException`, tracked TD-022).
