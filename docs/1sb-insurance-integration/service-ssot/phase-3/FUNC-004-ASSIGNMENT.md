# Phase 3 — FUNC-004 Assignment (Team Lead)

**Backlog ID:** FUNC-004  
**Title:** Get proposal schema (`GET /v1/proposals/schema`)  
**Branch:** `cursor/func-004-proposal-schema-c259`  
**Depends on:** FUNC-002/003 (Term quote stack)  
**Owner:** Dev A  
**QA cycle:** Yes  

## AC

| # | AC | Proof |
|---|-----|-------|
| AC-1 | Returns dynamic schema for product/manufacturer/version | MockMvc + WireMock |
| AC-2 | Quote expired (if detectable) → 410 or appropriate error | Unit/MockMvc |
| AC-3 | Upstream 5xx → 502 retryable | WireMock 503 → UPSTREAM_UNAVAILABLE retryable |

## Design

```text
GET /v1/proposals/schema?lob=TERM&productCode=&manufacturerId=&version=
  → ProposalController
  → ProposalService.getSchema
  → TermProposalHandler / OneSbProposalPort
  → OneSbHttpClient GET Term proposal form path
```

- In-process schema cache keyed by lob+product+manufacturer+version (TTL `insurance.proposals.schema-cache-ttl-seconds`, default 3600)
- Domain `ProposalSchema` = pass-through wrapper (fields JsonNode or Map) — do not interpret insurer semantics
- 1SB path (Term): use documented get-proposal-form style path — e.g. `/insurance/lifeterm/v1/proposal/form` with query params (confirm in adapter javadoc; WireMock stub in tests)
- Expired quote: if query includes `quoteJobId` and job is TIMEOUT/FAILED/missing offers → 410 with code `QUOTE_EXPIRED` (add ErrorCodes)
- No Idempotency-Key on GET

## DoD

`@Tag("FUNC-004")` tests; TL+QA APPROVE; PR.
