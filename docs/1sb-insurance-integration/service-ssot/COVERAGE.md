# Coverage (JaCoCo) — QA-001

**Authority:** [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md) §7 · [TESTING-RULES.md](./TESTING-RULES.md) R7 · [TEST-BACKLOG.md](./TEST-BACKLOG.md)

## How to run

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

`check` also depends on `jacocoTestCoverageVerification`.

**CI contract:** `./gradlew test` alone generates reports but does **not** fail on thresholds. CI must run `jacocoTestCoverageVerification` (or `check` / `build`). PR XML annotation remains **QA-005**.

## Report paths (per module)

| Artefact | Path |
|----------|------|
| HTML | `<module>/build/reports/jacoco/test/html/index.html` |
| XML (CI) | `<module>/build/reports/jacoco/test/jacocoTestReport.xml` |

Examples:

- `libs/bank-common-error/build/reports/jacoco/test/html/index.html`
- `services/1sb-integration-service/build/reports/jacoco/test/jacocoTestReport.xml`

## Push-time / PR evidence (on the branch)

Floors below are the **merge bar**. They apply to **touched modules on the branch being pushed**,
not to “the repo as a whole later”. Process and reviewer duties:
[CODE-REVIEW-STANDARD.md](../../governance/CODE-REVIEW-STANDARD.md) §1.

Before push / in the PR body the author publishes, for each touched module:

| Field | Required |
|-------|----------|
| Test count | JUnit summary |
| Line % | Jacoco `COVEREDRATIO` (measured, not claimed) |
| Branch % | Measured even when the module has no branch gate |
| Command | `./gradlew :<module>:test jacocoTestCoverageVerification` |

`./gradlew test` alone does **not** fail floors. CI must run `jacocoTestCoverageVerification`
(or `check` / `build`). A PR without these numbers is not ready for review.

Same-PR tests for new/changed behaviour remain [TESTING-RULES.md](./TESTING-RULES.md) R2 — a
green module floor does not excuse an untested security or error branch on the diff.

## Thresholds (enforced)

| Module group | Line | Branch | Notes |
|--------------|------|--------|-------|
| `libs/*` | **≥ 80%** | **≥ 70%** | Strategy §7 — enforced immediately |
| `services:1sb-integration-service` | **≥ 90%** | **≥ 70%** | Raised 2026-08-04 — measured ~91.7% line / ~71.8% branch (2026-09-13 re-measure) |
| `services:bank-persistence-service` | **≥ 90%** | **≥ 70%** | Raised 2026-09-13 (QA-001 close) — measured ~99.0% line / ~85.0% branch |
| `services/*` (scaffold) | **≥ 50%** | *not gated* | **Ratified scaffold module floor** (Swapnali 2026-09-13) — no longer an open interim; package floors track as **QA-014** |

## Module maturity ladder (EPIC-007 / QA-015)

Coverage floors rise with the module's maturity. The same CODING-STANDARD and ArchUnit
baseline apply at every rung; only the Jacoco numbers change.

| Maturity | When a module is here | Line / branch floor | Graduation trigger |
|----------|----------------------|---------------------|--------------------|
| **H0 scaffold** | Generated skeleton: Application, `/info`, ArchUnit, no real domain APIs | **≥ 50%** line; branch ungated (publish measured %) | First story adds a domain type, application service, or public/internal API beyond `/info` |
| **Implementing** | In-scope R0/R1 stories landing behaviour in that module | Keep **≥ 50%** line until Phase-1 hardened criteria apply; same-PR tests (R2) for every behaviour change | Module is named a Phase-1 deployable **or** TL + QA Lead raise its floor in this table |
| **Phase-1 hardened** | Today: `1sb-integration-service`, `bank-persistence-service` | **≥ 90%** line / **≥ 70%** branch | New hardened module: TL + QA Lead + row in this table (do not silently raise scaffolds) |
| **Library** | Any `libs/*` module from the first commit | **≥ 80%** line / **≥ 70%** branch | Always — no scaffold rung for shared libs |

Do **not** demand Phase-1 90/70 on an empty H0 scaffold. Do **not** leave an implementing
module without same-PR tests because the scaffold floor is still 50%. Package-level strategy
§7 floors remain **QA-014** (separate).

### Planned service package gates (QA-014)

Per strategy §7 (not yet enforced as package-level rules — follow-up **QA-014**):

| Package | Line | Branch |
|---------|------|--------|
| `…onesb.application.*` / `…lob.*` | 80% | 70% |
| `…adapter.onesb.*` | 70% | 60% |
| `…adapter.idempotency.*` | 80% | 70% |
| `com.bank.persistence.api.*` | 70% | 60% |

**QA-002 note (2026-07-30):** `bank-persistence-service` measured **~96% line / ~75% branch** after jobs/offers/payments/audit API tests; `api.internal.v1` package at **100% line**. Monorepo services floor raised **35% → 50%**.

**QA-001 close (2026-09-13, Swapnali / QA):** Mechanism + Phase-1 module floors are the S08 exit bar. Strategy §7 *package* floors split to **QA-014** (expiry 2026-10-31). Compensating control: module `jacocoTestCoverageVerification` remains on every CI `check`.

### IT-I template (QA-003)

**Done (2026-07-30):** `OneSbConnectivityIT` in `1sb-integration-service` — `@SpringBootTest` + `@Tag("integration")` with two dynamic-port WireMocks (1SB + bank-persistence), properties bound via `@DynamicPropertySource`. Happy path: `OneSbHttpClient.get(/v1/probe)` → 200; `JobStorePort.createJob` → 201 stub. Run: `./gradlew :services:1sb-integration-service:test --tests '*IT'`.

## Exclusions (always)

Applied to report + verification class sets:

- `*Application` / `*Application.class`
- `package-info`
- Spring wiring: `*Config`, `*Configuration`, `*Properties`

## Waivers / TECH-DEBT

| ID | Status | Note |
|----|--------|------|
| QA-001 | **Closed** | JaCoCo + CI gates delivered; Phase-1 services at 90/70; scaffold floor ratified at 50% line (2026-09-13) |
| QA-002 | **Done** | Persistence API tests landed; services floor → 50%; package-level `com.bank.persistence.api.*` gate still not enforced (strategy §7 → QA-014) |
| QA-003 | **Done** | IT-I template `OneSbConnectivityIT` (WireMock 1SB + persistence) |
| QA-014 | **Open** | Package-level strategy §7 JaCoCo floors — expiry **2026-10-31**; compensating control = module gates in CI |

Do not lower lib or Phase-1 gates without TL + QA Lead co-approval and a TECH-DEBT id + expiry.
