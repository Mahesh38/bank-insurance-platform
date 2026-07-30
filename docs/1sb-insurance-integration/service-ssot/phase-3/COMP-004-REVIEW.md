# Phase 3 — COMP-004 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | COMP-004-ASSIGNMENT.md |
| Implement | Dev | `211ba80` |
| Review | TL + QA Lead | **APPROVE** |
| Close | TL | **Done** → PR |

**Verdict:** **APPROVE**

**AC acceptance:** AC-1…AC-4 **Accepted**. Quote audit `distributorId` from `SecretProvider`, ArchUnit C-008, agent gate + spoof strip + attribution audit proofs (`@Tag("COMP-004")`) **pass**. Phase 3 backlog complete.

---

## AC checklist

| AC | Verdict | Proof |
|----|---------|-------|
| AC-1 `distributorId` only from `SecretProvider` / config — never public bank API DTOs | **Accepted** | No `distributorId` field on `api/dto`; `SubmitProposalRequest` / `SubmitProposalCommand` document absence; `TermProposalHandler` strips client spoof + injects `distributorID` from secrets; ArchUnit C-008 |
| AC-2 `agentId` required on proposal → `422 AGENT_ATTRIBUTION_MISSING` | **Accepted** | `ProposalService.submit` gate (reused from FUNC-005); `ProposalServiceTest.submit_missingAgentId_*` dual-tagged COMP-004 — no 1SB / no job |
| AC-3 Relevant audits include attribution | **Accepted** | Proposal `PROPOSAL_SUBMITTED`: both `agentId` + `distributorId`; Quote `QUOTE_CREATED`: `.distributorId(secretProvider.getDistributorId())` + agent when present; Payment / Status: `distributorId` from secrets (agentId deferred per C-006) |
| AC-4 ArchUnit C-008 — no `distributorId` field outside `..adapter.onesb..` | **Accepted** | `ArchitectureTest.noDistributorIdFieldOutsideOneSbAdapter` `@Tag("COMP-004")`; TODO-TRACKER C-008 ticked via COMP-004 |

---

## Strict gates (TL)

| Gate | Result | Notes |
|------|--------|-------|
| QuoteService audit `distributorId` | Pass | `publishQuoteCreated` always sets `secretProvider.getDistributorId()`; agentId from distribution when present |
| No `distributorId` on public DTOs | Pass | Grep of `api/dto` — comments only; domain command intentionally omits field |
| Spoof strip on proposal payload | Pass | `TermProposalHandler` removes root/`distributor` client keys; secrets win |
| Agent gate (reuse FUNC-005) | Pass | Dual-tagged; no re-implementation drift |
| Payment / status `distributorId` on audit | Pass | Both services + unit asserts `TEST_DIST` |
| ArchUnit C-008 | Pass | `noFields().haveName("distributorId")` outside `adapter.onesb`; scoped to `com.bank.insurance.onesb` |
| `@Tag("COMP-004")` | Pass | ArchUnit + Quote / Proposal / Payment / Status / TermProposalHandler proofs |
| Do not expand payment/status for agentId | Pass | Out of scope respected |

---

## Findings

### Major

_None._

### Minor (non-blocking)

1. **Quote agent-absent audit path** — Happy-path unit asserts both fields with agent present; code always stamps `distributorId` when agent is null. Optional dedicated null-agent quote audit assertion later.
2. **ArchUnit `allowEmptyShould(true)`** — Currently no `distributorId` Java fields inside `adapter.onesb` either (payload uses map key `distributorID`). Rule still fails if a field is added outside the adapter package — intent met.

---

## Test evidence notes

Re-run (reviewer):

```text
./gradlew :services:1sb-integration-service:test \
  --tests 'com.bank.insurance.onesb.architecture.ArchitectureTest.noDistributorIdFieldOutsideOneSbAdapter' \
  --tests 'com.bank.insurance.onesb.application.QuoteServiceTest' \
  --tests 'com.bank.insurance.onesb.application.ProposalServiceTest' \
  --tests 'com.bank.insurance.onesb.application.PaymentServiceTest' \
  --tests 'com.bank.insurance.onesb.application.StatusServiceTest' \
  --tests 'com.bank.insurance.onesb.lob.life.term.TermProposalHandlerTest'
→ BUILD SUCCESSFUL
```

| Suite | Tests | Failures |
|-------|------:|---------:|
| `ArchitectureTest` (C-008 method) | 1 | 0 |
| `QuoteServiceTest` | 6 | 0 |
| `ProposalServiceTest` | 9 | 0 |
| `PaymentServiceTest` | 6 | 0 |
| `StatusServiceTest` | 5 | 0 |
| `TermProposalHandlerTest` | 5 | 0 |
| **Total** | **32** | **0** |

`@Tag("COMP-004")` methods covered in the above suites: ArchUnit C-008; QuoteService audit distributor+agent; Proposal missing-agent gate + PROPOSAL_SUBMITTED both fields; Payment/Status distributorId; TermProposalHandler spoof ignored. Jacoco report task ran with the test run (`jacocoTestReport`).

---

## Dual approval

| Role | Verdict | Date | Notes |
|------|---------|------|-------|
| **Tech Lead** | **APPROVE** | 2026-07-30 | AC-1…4 Accepted; QuoteService gap closed; C-008 permanent; FUNC-005 reuse OK |
| **QA Lead** | **APPROVE** | 2026-07-30 | See QA section below |

---

## QA Lead notes

**QA verdict: APPROVE**

| AC | Coverage | QA note |
|----|----------|---------|
| AC-1 Secret-only distributor | Strong | Spoof unit + ArchUnit + DTO absence; adapter inject from SecretProvider |
| AC-2 Agent gate 422 | Strong | Dual-tag with FUNC-005; no upstream / no job |
| AC-3 Audit attribution | Strong | Quote / Proposal both fields; Payment / Status distributorId asserted |
| AC-4 ArchUnit C-008 | Strong | Dedicated tagged method green on review re-run |
| Tags / regression | Pass | COMP-004 tags on required suites; 32/32 green; jacoco report produced |

**QA soft (non-blocking):** Optional quote audit case with null agent still asserting `distributorId` only — code path is unconditional; not required to close DoD.
