# Phase 3 — FUNC-005 Assignment (Team Lead)

**Backlog ID:** FUNC-005  
**Title:** Submit Term proposal (`POST /v1/proposals`)  
**Branch:** `cursor/func-005-submit-proposal-c259`  
**Depends on:** FUNC-004  
**Owner:** Dev A  
**QA cycle:** Yes  

## AC

| # | AC | Proof |
|---|-----|-------|
| AC-1 | Missing `agentId` → 422 `AGENT_ATTRIBUTION_MISSING`, no 1SB | WireMock verify(0) |
| AC-2 | Missing `consentRef` → WARN audit; still allow submit | Audit verify + success path |
| AC-3 | Success → 201 with bank proposal/job id + normalised status | MockMvc + WireMock |
| AC-4 | Same Idempotency-Key → original result, no duplicate 1SB | Filter + verify(1) total |
| AC-5 | 1SB business reject → 422 `PROPOSAL_REJECTED` + audit | WireMock 422 body |

## Design

```text
POST /v1/proposals + Idempotency-Key
  → ProposalController
  → ProposalService.submit
       → require agentId (from body or distribution)
       → if no consentRef: audit WARN CONSENT_REF_MISSING (add AuditActions)
       → JobStore create PROPOSAL job
       → TermProposalHandler build payload from schema values map
       → OneSbProposalPort.submit → POST Term proposal
       → updateJobPolling; schedule poll if async
  ← 201 { proposalJobId, status }
```

- Add ErrorCodes: `AGENT_ATTRIBUTION_MISSING`, `PROPOSAL_REJECTED`
- DistributorId from SecretProvider only (COMP-004 light touch — do not trust client distributorId)
- KISS: if 1SB returns immediately with applicationNumber, complete job; else schedule poll like quotes

## DoD

`@Tag("FUNC-005")`; TL+QA APPROVE.
