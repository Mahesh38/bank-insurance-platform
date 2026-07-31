# Phase 3 — FUNC-006 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | FUNC-006-ASSIGNMENT.md |
| Implement | Dev | `42dd011` |
| Review | TL + QA Lead | **APPROVE** |
| Close | TL | Done → PR |

**AC acceptance:** AC-1…AC-3 **accepted**.

| AC | Verdict | Proof |
|----|---------|-------|
| AC-1 applicationNo when available + status normalised | Pass | `ProposalGetControllerTest.ac1_*` — `COMPLETED` + `applicationNumber`; poll extract `OneSbHttpClientPollAdapterTest`; persist `HttpJobStoreAdapterTest.completeJob_withApplicationNumber_*` / `findQuoteJob_mapsApplicationNumber`; `AsyncJobPoller` → `completeJob(..., applicationNumber)` |
| AC-2 404 unknown jobId | Pass | `ProposalGetControllerTest.ac2_*` + `ProposalServiceTest.getProposalResult_unknown_*` — `RESOURCE_NOT_FOUND` |
| AC-3 In-progress no fabricated applicationNo | Pass | `ac3_inProgress_*` + `ac3_pending_neverFabricatesApplicationNumber_evenIfStoreHadOne` — field omitted; `offers: []` |

**Soft notes (non-blocking)**

- No `jobType=PROPOSAL` guard on GET (same bar as FUNC-003 quote GET).
- Slice/unit proof only — no dedicated WireMock GET IT (mirrors FUNC-003).
- Richer architecture fields (`requirements`, insurer substatus) deferred to FUNC-009.
