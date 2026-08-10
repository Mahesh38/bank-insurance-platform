# Baseline QA backlog — dual review log

**Developer:** QA test-resolution specialist  
**Branch:** `cursor/phase1-foundations-c259`  
**Process:** Implement item → Tech Lead + QA Lead review → fix until both APPROVE

| ID | Dev delivery | TL | QA Lead | Final |
|----|--------------|-----|---------|-------|
| QA-001 | `008ec04` JaCoCo + gates | **APPROVE** | **APPROVE** | Approved 2026-07-30 (non-blocking doc polish applied) |
| QA-002 | Persistence API tests + services gate 50% + validation 400 fix | **APPROVE** | **APPROVE** | Approved 2026-07-30. TL re-review: `GlobalExceptionHandler` → 400/`VALIDATION_ERROR`/`MISSING_REQUIRED_FIELD`; `JobApiTest.createJob_missingRequiredField_returns400_withValidationError` green. |
| QA-003 | `OneSbConnectivityIT` (IT-I dual WireMock) `5d9ee76` | **APPROVE** | **APPROVE** | Approved 2026-07-30. TL: hex OK (prod unchanged; `JobStorePort` + connectivity via client); no real network (dual dynamic WireMock + `@DynamicPropertySource`); ArchUnit green; Phase 3 template ready (extend via API/ports for FUNC-002). |
| QA-004 | — | — | — | **Open — no review cycle recorded.** Directive: *"Phase 3 definition of done includes unit+slice+IT for quote path"* ([QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md) §10.4). Phase 3 shipped with dual TL+QA Lead APPROVE per story ([phase-3/STATUS.md](./phase-3/STATUS.md)), but no QA-004 item was reviewed and signed off in its own right. |
| QA-005 | — | — | — | **Open — no review cycle recorded.** Directive: *"CI badge/report for coverage on PR"* ([QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md) §10.5). Still tracked as outstanding in [COVERAGE.md](./COVERAGE.md) — "PR XML annotation remains QA-005". |
| QA-006 | Delivered — unit tests for idempotency store, poll adapter, secrets validator `177620e` | **APPROVE** | **APPROVE** | Approved 2026-07-30. QA Lead: all three classes covered (`InMemoryIdempotencyStoreTest`, `OneSbHttpClientPollAdapterTest`, `SecretsStartupValidatorTest`); R1 pure unit + mocks; R3 `*Test` + `behaviour_outcome` + `@Tag("unit"|"QA-006")`; R5 no assertNotNull-only, exception type/message/cause, skip verifies `never()` secret lookup; gradle green. |

## Open items

`QA-004` and `QA-005` are directives in
[QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md) §10 that have **never been
through the dual TL + QA Lead review cycle**. They are listed above with empty verdict
columns rather than omitted, so the gap is visible in the audit trail instead of silent.

Closing either one requires the normal process: implement → TL + QA Lead review → both
APPROVE → fill in the row.
