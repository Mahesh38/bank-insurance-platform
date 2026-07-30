# Phase 3 — FUNC-009 Assignment (Team Lead)

**Backlog ID:** FUNC-009  
**Title:** Application status (`GET /v1/status/{applicationNumber}`)  
**Branch:** `cursor/func-009-application-status-c259`  
**Depends on:** FUNC-007 (payment URL)  

## AC (PRODUCT-BACKLOG)

1. Maps 1SB `applicationStatus` values to bank stage enum (`BankStage`)
2. Returns manufacturer substatus (`manufacturerAppStatus` / desc) for RM display
3. `404` + `RESOURCE_NOT_FOUND` when application is not found / empty upstream result
4. Audit `APPLICATION_STATUS_CHECKED` on every successful check

## Design (Case 2 + KISS)

```text
StatusController → StatusService → OneSbStatusPort (OneSbStatusAdapter)
                                      → POST /LifeTerm/prostat/
StatusService → StatusNormaliser (1SB enum → BankStage)
StatusService → AuditEventPublisher (APPLICATION_STATUS_CHECKED)
```

- Query: `lob` (required), `insuranceCompanyCode` (required — 1SB prostat contract)
- Optional query: `policyNo` for post-issuance lookup
- `distributorID` from `SecretProvider` only (COMP-004 alignment)
- Do **not** persist status snapshot in this backlog (no persistence API yet) — call 1SB + normalise + return
- Do **not** implement requirements list (FUNC-010)
- Surface bank stage + manufacturer substatus; do not dump full raw 1SB payload in API response

### BankStage mapping (field guide)

| 1SB applicationStatus prefix / values | BankStage |
|---------------------------------------|-----------|
| QUOTE_CREATED, QUOTE_UPDATED | QUOTING |
| QUOTE_SELECTED | QUOTE_SELECTED |
| PROPOSAL_* (except ERROR) | PROPOSAL |
| REQUIREMENTS_*, DOCUMENT_*, OTP_*, KYC_*, INSPECTION_*, REQUIREMENT_VERIFICATION | UW_REQUIREMENTS |
| UNDERWRITING, SCRUTINY, PRE_CONVERSION | UW_IN_PROGRESS |
| COUNTER_OFFER, AWAITING_CLIENT_APPROVAL, ACCEPTED | CUSTOMER_DECISION |
| PAYMENT_* | PAYMENT |
| POLICY_ISSUED | ISSUED |
| REJECTED, CANCELLED, PROPOSAL_ERROR | CLOSED |
| Unknown | UNKNOWN (still return manufacturer fields) |

## Tests

- `@Tag("FUNC-009")` on unit + IT
- Unit: StatusNormaliser (all mapping buckets), StatusService (404, audit), OneSbStatusAdapter parse
- IT: WireMock `POST /LifeTerm/prostat/` → MockMvc GET status; missing app → 404

## DoD

TL + QA Lead dual APPROVE; jacoco verification green; PR with AC table.
