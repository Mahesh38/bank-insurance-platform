# Phase 3 — FUNC-007 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | FUNC-007-ASSIGNMENT.md |
| Implement | Dev | `968aeab` |
| Review | TL + QA Lead | **APPROVE** |
| Close | TL | Done → PR |

**AC acceptance:** AC-1…AC-4 **accepted**. R6 gate closed.

| AC | Verdict | Proof |
|----|---------|-------|
| AC-1 Payable → HTTPS `paymentUrl` + expiry/ref | Pass | `PaymentCreateIT.ac1_*` / `ac1b_*` — 201 `paymentRef` + HTTPS URL + `expiresAt`; WireMock `POST /v1/payment/url` + persist session |
| AC-2 `paymentUrl` not in logs (ref only) | Pass | IT `ac2_*` + `PaymentServiceTest` ListAppender — `paymentRef` present; secret token / full URL absent; audit metadata scrubbed |
| AC-3 Non-payable → 409 `PROPOSAL_NOT_PAYABLE` | Pass | IT `ac3_*` (PENDING job) + unit — WireMock `verify(0)` on 1SB; no persist |
| AC-4 Audit `PAYMENT_URL_RETRIEVED` | Pass | IT `ac4_*` + unit — SUCCESS; resourceId = session ref; no URL in metadata |
| R6 Missing Idempotency-Key → 400 | Pass | `PaymentCreateIT.r6_*` — `MISSING_IDEMPOTENCY_KEY`; no 1SB call |

**Soft notes (non-blocking)**

- Response uses `paymentRef` (AC “ref”); arch sample names `paymentSessionId` and includes `status` — align contract docs later if desired.
- Payable gate runs only when `proposalJobId` is supplied (arch allows appNo-only create).
- 1SB request body is minimal (`applicationNo` + `redirectUrl` + distributor) per arch sequence; richer field-guide fields deferred.
- No `jobType=PROPOSAL` guard on optional job lookup (same bar as FUNC-006).
