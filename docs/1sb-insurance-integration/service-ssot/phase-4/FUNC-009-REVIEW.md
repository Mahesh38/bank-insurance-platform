# Phase 4 — FUNC-009 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | FUNC-009-ASSIGNMENT.md |
| Implement | Dev | `StatusController` / `StatusService` / `OneSbStatusAdapter` |
| Review | TL | **APPROVE** |

**AC acceptance:**

| AC | Verdict | Proof |
|----|---------|-------|
| Maps 1SB `applicationStatus` → bank stage enum | Pass | `StatusServiceTest.normalise_mapsPerFieldGuideTable` (19-case parameterised, full mapping table from field guide); `ApplicationStatusIT.ac1_*` |
| Returns manufacturer substatus for RM display | Pass | `StatusServiceTest.getStatus_happyPath_*`, `StatusControllerTest.getStatus_happyPath_*`, `OneSbStatusAdapterTest.getStatus_happyPath_*` all assert `manufacturerSubStatus` |
| 404 if not found | Pass | `OneSbStatusAdapterTest.getStatus_emptyManufacturerArray_returnsEmpty`, `StatusServiceTest.getStatus_notFound_*`, `ApplicationStatusIT.ac2_notFound_*` |

**Compliance check:** `ApplicationStatusResponse` has no `rawStatus` field at all (not just
null-suppressed) — §8.1 "not surfaced in the response" is structurally enforced, verified by
`jsonPath("$.rawStatus").doesNotExist()` in both `StatusControllerTest` and `ApplicationStatusIT`.
`rawStatus` still flows into the `APPLICATION_STATUS_CHECKED` audit event as required by §8.3.

**Soft notes (non-blocking):**

- 1SB status is treated as "not found" only via the field-guide's documented 200-with-empty-array
  shape. A genuine 4xx from 1SB would still be normalised to `422 UPSTREAM_BUSINESS_ERROR` by the
  shared `OneSbErrorNormaliser` (not remapped to 404) — this is existing systemic behaviour shared
  by quote/proposal/payment/status alike, not something introduced or special-cased here.
- No status-snapshot persistence (architecture §6.4's optional `JS` upsert) — scope decision
  recorded in FUNC-009-ASSIGNMENT.md; AC does not require it and no `status_snapshot` table exists
  in `bank-persistence-service`.
- `@RequestParam String insurerCode` with no value → Spring's default 400 (not RFC7807-shaped),
  same as the pre-existing `@RequestParam Lob lob` behaviour on `ProposalController.getSchema` —
  consistent with the rest of the codebase, not a new gap.

No must-fix items. `./gradlew :services:1sb-integration-service:test` — green, including
`ArchitectureTest` and `jacocoTestCoverageVerification`.
