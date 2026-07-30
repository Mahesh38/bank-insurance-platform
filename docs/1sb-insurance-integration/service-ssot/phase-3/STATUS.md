# Phase 3 — Status

**Branch:** `cursor/func-007-payment-url-c259`  
**Assignment:** [FUNC-007-ASSIGNMENT.md](./FUNC-007-ASSIGNMENT.md)  
**Review:** [FUNC-007-REVIEW.md](./FUNC-007-REVIEW.md)  
**Prior:** [FUNC-006-ASSIGNMENT.md](./FUNC-006-ASSIGNMENT.md) · [FUNC-006-REVIEW.md](./FUNC-006-REVIEW.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| FUNC-002 Term quote create | Dev A | **Done** | `af33e65` (fixes) / `66bfa4c` (feature) |
| FUNC-003 Get quote job result | Dev A | **Done** | `e25d7ff` |
| FUNC-004 Get proposal schema | Dev A | **Done** | `52cd397` |
| FUNC-005 Submit Term proposal | Dev A | **Done** | `56242ca` (R6 IT) / `2a64388` (feature) |
| FUNC-006 Get proposal job result | Dev A | **Done** | `42dd011` |
| FUNC-007 Create payment session / URL | Dev A | **Done** | `968aeab` |

## Notes

- FUNC-002: TL+QA Lead APPROVE (Case 2, WireMock AC proof, QUOTE_COMPLETED, PARTIAL, R6).
- FUNC-003: TL+QA Lead APPROVE — status + offers; TIMEOUT → 200; 404 RESOURCE_NOT_FOUND.
- FUNC-004: TL+QA Lead APPROVE — schema pass-through + cache; 410 QUOTE_EXPIRED; upstream 5xx → UPSTREAM_UNAVAILABLE retryable.
- FUNC-005: TL+QA Lead APPROVE — AC-1…AC-5 + R6 `r6_missingIdempotencyKey_returns400` on `ProposalSubmitIT`.
- FUNC-006: TL+QA Lead APPROVE — applicationNumber when available; no fabricate in-progress; 404 RESOURCE_NOT_FOUND; persist + poll wiring.
- FUNC-007: TL+QA Lead APPROVE — HTTPS paymentUrl + ref/expiry; no URL in logs; 409 PROPOSAL_NOT_PAYABLE; PAYMENT_URL_RETRIEVED; R6.
