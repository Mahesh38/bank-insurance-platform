# Phase 3 — FUNC-004 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | FUNC-004-ASSIGNMENT.md |
| Implement | Dev | `52cd397` |
| Review | TL + QA Lead | **APPROVE** |
| Close | TL | Done → PR |

**AC acceptance:** AC-1…AC-3 **accepted**.

| AC | Verdict | Proof |
|----|---------|-------|
| AC-1 Schema returned | Pass | `ProposalSchemaIT.ac1_*` — 200 + pass-through `fields`; cache hit skips 2nd 1SB GET |
| AC-2 Quote expired → 410 | Pass | TIMEOUT / FAILED / missing job → 410 `QUOTE_EXPIRED` (IT + `ProposalService`); empty offers gated in service |
| AC-3 Upstream 5xx retryable | Pass | WireMock 503 → `UPSTREAM_UNAVAILABLE` `retryable=true` (HTTP **503** via `bank-common-error`, not 502 — platform contract) |

**Soft note (non-blocking):** Assignment AC text says “502”; shared factory maps `UPSTREAM_UNAVAILABLE` to **503**. Align assignment wording later if desired.
