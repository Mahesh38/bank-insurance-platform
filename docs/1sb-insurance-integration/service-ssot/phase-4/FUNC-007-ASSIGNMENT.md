# Phase 4 — FUNC-007 Assignment (Team Lead)

**Backlog ID:** FUNC-007
**Title:** Create payment session / URL
**Branch:** `claude/1sb-integration-phase-4-p8s0bo`
**Depends on:** Phase 3 complete (FUNC-001…006) — Done

**AC (from PRODUCT-BACKLOG.md):**
- Given payable application, When `POST /v1/payments` called, Then returns HTTPS `paymentUrl` + expiry/ref.
- `paymentUrl` not written to logs (ref only).
- Non-payable status → 409 `PROPOSAL_NOT_PAYABLE`.
- Audit `PAYMENT_URL_RETRIEVED`.

## Design

`OneSbPaymentPort` and the `PaymentSession` / `PaymentStatus` domain models already exist (scaffolded ahead of TD-009). This story implements the missing pieces:

- `PaymentUseCase` (inbound port) + `PaymentService` (application layer, no LOB handler — payment is
  LOB-agnostic per architecture §2 component diagram: `PayC → PayS → PayPt` directly).
- `OneSbPaymentPort.createPaymentUrl(CreatePaymentCommand)` → `OneSbPaymentAdapter` calling
  `POST /v1/payment/url` (field guide: `docs/.../field-guides/payment.md`). `distributorID` injected
  from `SecretProvider` only (never client-supplied), matching COMP-004.
- New `PaymentSessionStorePort` → `HttpPaymentSessionStoreAdapter` against
  `bank-persistence-service` `/internal/v1/payment-sessions` (controller + entity already exist there).
- Payability check: when the request carries `jobId` (originating proposal job), `PaymentService`
  checks `JobStorePort.findQuoteJob(jobId)` — anything other than `COMPLETED`/`PARTIAL` with a stored
  `applicationNumber` → `409 PROPOSAL_NOT_PAYABLE`, no 1SB call.
- Response `paymentUrl` must be HTTPS; a non-HTTPS upstream URL is rejected as
  `UPSTREAM_BAD_RESPONSE` (502, retryable) per `FUNCTIONAL-NFR-COMPLIANCE-MAP.md` transport-security row.
- `paymentUrl` never appears in the outbound audit metadata or application logs — only
  `paymentSessionId` / `applicationNumber` are logged/audited (ref only, per AC).
- FUNC-008 (payment intimation) stays out of scope — P1 per backlog. `OneSbPaymentPort.sendPaymentIntimation`
  is intentionally left as a documented stub (`UnsupportedOperationException`) so the port compiles
  without pulling in FUNC-008's full 1SB payload; tracked as TD-022.

## DoD

`@Tag("FUNC-007")` on tests; unit tests for happy path + validation + non-payable + upstream non-HTTPS
url; WireMock IT for the 1SB call; TL review (this phase has no dedicated QA Lead pass — Team Lead
review only, consistent with WORK-SEQUENCE §3 "Infra/no bank API behaviour change" short path is **not**
used here since this is a bank-facing API change — full functional review applies).
