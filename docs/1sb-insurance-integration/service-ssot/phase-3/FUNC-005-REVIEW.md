# Phase 3 — FUNC-005 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | FUNC-005-ASSIGNMENT.md |
| Implement | Dev | `2a64388` |
| Review #1 | TL + QA Lead | **CHANGES_REQUESTED** (R6 missing Idempotency-Key) |
| Fix | Dev | `56242ca` — R6 IT: `r6_missingIdempotencyKey_returns400` |
| Review #2 | TL + QA Lead | **APPROVE** |
| Close | TL | Done → PR |

**AC acceptance:** AC-1…AC-5 **accepted** (behaviour + proof present). R6 gate closed.

| AC | Verdict | Proof |
|----|---------|-------|
| AC-1 Missing `agentId` → 422 `AGENT_ATTRIBUTION_MISSING`, no 1SB | Pass | `ProposalSubmitIT.ac1_*` WireMock `verify(0)`; `ProposalServiceTest.submit_missingAgentId_*` |
| AC-2 Missing `consentRef` → WARN audit; still allow | Pass | IT + unit: `CONSENT_REF_MISSING` + `WARN`; submit continues → 201 |
| AC-3 Success → 201 + job id + status | Pass | `ProposalSubmitIT.ac3_*` — `COMPLETED` when `applicationNumber`; PENDING+poll when `reqId` only |
| AC-4 Same Idempotency-Key → original result, no dup 1SB | Pass | `ProposalSubmitIT.ac4_*` — replay 201, WireMock `exactly(1)` |
| AC-5 1SB business reject → 422 `PROPOSAL_REJECTED` + audit | Pass | Adapter remap + IT `ac5_*` + service audit `REJECTED` |
| R6 Missing Idempotency-Key → 400 `MISSING_IDEMPOTENCY_KEY`, no 1SB | Pass | `ProposalSubmitIT.r6_missingIdempotencyKey_returns400` — WireMock `verify(0)` |

## Review #1 — CHANGES_REQUESTED (resolved)

| # | Gap | Fix |
|---|-----|-----|
| 1 | **R6** incomplete for new mutating `POST /v1/proposals` | Added `r6_missingIdempotencyKey_returns400` (assert `MISSING_IDEMPOTENCY_KEY`, WireMock `verify(0)`) — mirrors `QuoteCreateIT` |

## Soft notes (non-blocking)

- Reject path creates PROPOSAL job then leaves it non-terminal after `PROPOSAL_REJECTED` — consider `failJob` in a follow-up; not in FUNC-005 AC.
- Body-hash conflict covered by `IdempotencyFilterTest`; endpoint-level conflict IT optional (same bar as FUNC-002).
