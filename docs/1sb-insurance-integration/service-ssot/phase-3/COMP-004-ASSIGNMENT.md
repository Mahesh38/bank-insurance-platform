# Phase 3 — COMP-004 Assignment (Team Lead)

**Backlog ID:** COMP-004  
**Title:** Agent & distributor attribution  
**Branch:** `cursor/comp-004-agent-distributor-attribution-c259`  
**Depends on:** COMP-003 (stack tip)

## AC (PRODUCT-BACKLOG + CONFIRM-03 D6/D7 + C-008)

1. `distributorId` **only** from `SecretProvider` / config — never from public bank API DTOs
2. `agentId` **required** on proposal submit → `422 AGENT_ATTRIBUTION_MISSING` if missing
3. Relevant audits include attribution:
   - Proposal events: **both** `agentId` + `distributorId`
   - Quote `QUOTE_CREATED`: **both** when agent present; always `distributorId`
   - Payment / status: at least `distributorId` from secrets (agentId deferred to JWT claim mapping — CONFIRM-03 C-006)
4. ArchUnit C-008: no Java field named `distributorId` outside `..adapter.onesb..`

## Design (mostly close gaps — KISS)

Much of AC-1/AC-2 landed in FUNC-005. This backlog **closes proof + remaining gaps**:

| Change | Why |
|--------|-----|
| `QuoteService` audit `.distributorId(secretProvider.getDistributorId())` | Quote audits must carry distributor |
| ArchUnit rule in `ArchitectureTest` | Enforce D7 permanently |
| `@Tag("COMP-004")` tests asserting audit fields + spoof strip + agent gate | QA proof |
| Docs STATUS + tick C-008 in TODO-TRACKER | SSOT |

### Do not

- Add `distributorId` to any `api/dto`
- Expand payment/status APIs for agentId (out of scope)
- Re-implement FUNC-005 proposal gate (reuse; dual-tag tests)

## Tests

- `@Tag("COMP-004")` on new/retag assertions
- ArchUnit method tagged COMP-004
- QuoteServiceTest updated for SecretProvider + distributorId on audit
- ProposalServiceTest: both fields on PROPOSAL_SUBMITTED
- PaymentServiceTest / StatusServiceTest: distributorId asserted
- TermProposalHandlerTest client distributor spoof ignored (tag COMP-004)

## DoD

TL + QA Lead dual APPROVE; jacoco green; PR with AC table.
