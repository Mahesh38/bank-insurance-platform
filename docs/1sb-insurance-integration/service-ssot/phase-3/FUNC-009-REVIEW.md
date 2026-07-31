# Phase 3 — FUNC-009 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | FUNC-009-ASSIGNMENT.md |
| Implement | Dev | `98e1a3f` |
| Review | TL + QA Lead | **APPROVE** |
| Close | TL | **Done** → PR |

**Verdict:** **APPROVE**

**AC acceptance:** AC-1…AC-4 **Accepted**. Strict gates (Case 2, SecretProvider, 404, no `onesbStatus` in API, BankStage map, `@Tag("FUNC-009")`, audit) **pass**.

---

## AC checklist

| AC | Verdict | Proof |
|----|---------|-------|
| AC-1 Maps 1SB `applicationStatus` → `BankStage` | **Accepted** | `StatusNormaliser` covers all assignment buckets incl. `PROPOSAL_ERROR`→`CLOSED` before `PROPOSAL_*`; `StatusNormaliserTest` (40 cases); IT `ac1_*` → `UW_REQUIREMENTS` |
| AC-2 Returns manufacturer substatus for RM | **Accepted** | `ApplicationStatusResponse.manufacturerAppStatus` / `Desc`; IT `ac1_*` asserts both; adapter nested + flat parse tests |
| AC-3 `404` + `RESOURCE_NOT_FOUND` when not found / empty | **Accepted** | Adapter empty manufacturer / missing status; service null/blank `onesbStatus`; IT `ac3_*` + unit |
| AC-4 Audit `APPLICATION_STATUS_CHECKED` on successful check | **Accepted** | `StatusService.publishStatusChecked`; IT `ac4_*` + `StatusServiceTest` — SUCCESS, resourceId=appNo, `bankStatus` metadata; no audit on 404 |

---

## Strict gates (TL)

| Gate | Result | Notes |
|------|--------|-------|
| Case 2 layering | Pass | `StatusController` → `StatusUseCase`/`StatusService` → `OneSbStatusPort`/`OneSbStatusAdapter`; normalise + audit in application; no persistence (per DoD) |
| `distributorId` via `SecretProvider` only | Pass | Adapter payload `distributorID` from `SecretProvider`; audit `distributorId` from same; no client/query field |
| `404` + `RESOURCE_NOT_FOUND` | Pass | Adapter + service; problem JSON `code` asserted in IT/unit |
| `onesbStatus` not in API response | Pass | Domain retains for internal use; DTO omits; IT `jsonPath("$.onesbStatus").doesNotExist()` |
| BankStage mapping completeness | Pass | Quoting / selected / proposal / UW req / UW in-progress / customer decision / payment / issued / closed / unknown; `PROPOSAL_ERROR` not swallowed by `PROPOSAL_*` |
| `@Tag("FUNC-009")` | Pass | All four test classes tagged |
| Audit `APPLICATION_STATUS_CHECKED` | Pass | Success path only (matches assignment “successful check”); audit failure swallowed |

---

## Findings

### Major

_None._

### Minor (non-blocking)

1. **IT distributor value** — `ApplicationStatusIT.ac1_*` asserts body contains `"distributorID"` key, not the configured `TEST_DIST` value. Unit adapter test already locks the value; optional IT strengthen later.
2. **Arch sample drift** — architecture JSON still shows illustrative `bankStatus: REQUIREMENTS_PENDING` / `rawStatus`; implementation correctly uses `BankStage` + omits raw/`onesbStatus` and surfaces manufacturer fields per assignment AC-2. Align arch sample in a docs pass.
3. **Adapter placeholder stage** — `OneSbStatusAdapter` stamps `BankStage.UNKNOWN` before `StatusService` normalises (same “adapter returns domain snapshot” pattern as peers). Fine; do not normalise twice in adapter.

---

## Test evidence notes

Re-run (reviewer):

```text
./gradlew :services:1sb-integration-service:test \
  --tests 'com.bank.insurance.onesb.application.StatusNormaliserTest' \
  --tests 'com.bank.insurance.onesb.application.StatusServiceTest' \
  --tests 'com.bank.insurance.onesb.adapter.onesb.status.OneSbStatusAdapterTest' \
  --tests 'com.bank.insurance.onesb.ApplicationStatusIT'
→ BUILD SUCCESSFUL
```

| Suite | Tests | Failures |
|-------|------:|---------:|
| `StatusNormaliserTest` | 40 | 0 |
| `StatusServiceTest` | 5 | 0 |
| `OneSbStatusAdapterTest` | 6 | 0 |
| `ApplicationStatusIT` | 3 | 0 |
| **Total** | **54** | **0** |

Assignment DoD coverage: normaliser buckets + service 404/audit + adapter parse + WireMock `POST /LifeTerm/prostat/` MockMvc IT — met. Jacoco report task ran with the test run (`jacocoTestReport`).

---

## Dual approval

| Role | Verdict | Date | Notes |
|------|---------|------|-------|
| **Tech Lead** | **APPROVE** | 2026-07-30 | AC-1…4 Accepted; Case 2 + COMP-004 distributor secret + response hygiene OK vs FUNC-007 patterns |
| **QA Lead** | **APPROVE** | 2026-07-30 | See QA section below |

---

## QA Lead notes

**QA verdict: APPROVE**

| AC | Coverage | QA note |
|----|----------|---------|
| AC-1 BankStage map | Strong | Parametrised unit across all assignment buckets + happy-path IT |
| AC-2 Manufacturer substatus | Strong | Nested prostat fixture in IT; adapter nested/flat unit |
| AC-3 404 `RESOURCE_NOT_FOUND` | Strong | Empty `manufacturer: []` IT + adapter/service unit paths |
| AC-4 `APPLICATION_STATUS_CHECKED` | Strong | IT + unit assert action/outcome/resourceId/metadata; 404 does not audit |
| Tags / regression | Pass | `@Tag("FUNC-009")` on unit + IT; 54/54 green on review re-run |

**QA soft (non-blocking):** No separate IT for UNKNOWN stage with manufacturer fields still returned (unit covers UNKNOWN mapping; service keeps manufacturer fields). Optional follow-up fixture only.
