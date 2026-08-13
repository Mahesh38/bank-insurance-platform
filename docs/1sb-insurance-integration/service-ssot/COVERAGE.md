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

## Thresholds (enforced)

| Module group | Line | Branch | Notes |
|--------------|------|--------|-------|
| `libs/*` | **≥ 80%** | **≥ 70%** | Strategy §7 — enforced immediately |
| `services:1sb-integration-service` | **≥ 90%** | **≥ 70%** | Raised 2026-08-04 — measured ~91.9% line / ~76.5% branch |
| `services:bank-persistence-service` | **≥ 90%** | **≥ 70%** | Raised 2026-08-13 on QA-001 closure — measured ~99.0% line / ~81.0% branch |
| WS-2 services | **≥ 50%** (interim) | *not gated* | `identity-*`, `workforce-access-bff`. Different workstream at IAM Phase 1; strategy §7 defines no package floors for them. Raising this belongs to the WS-2 gate, not QA-001 |

### Service package gates — **enforced** since QA-001 closure (2026-08-13)

Strategy §7 package floors, live as JaCoCo `PACKAGE` rules in `build.gradle.kts`. A module-wide
average can hide a thin adapter, which is precisely what these catch:

| Package | Line | Branch | Measured at closure |
|---------|------|--------|---------------------|
| `…onesb.application` | 80% | 70% | 92.9 / 75.7 |
| `…onesb.lob` | 80% | 70% | 100 / 100 |
| `…onesb.lob.life.term` | 80% | 70% | 94.9 / 74.4 |
| `…onesb.adapter.onesb.*` | 70% | 60% | lowest package 88.4 / 71.4 |
| `…onesb.adapter.idempotency` | 80% | 70% | 95.9 / 80.8 |
| `com.bank.persistence.api.internal.v1` | 70% | 60% | 99.6 / 80.0 |

**`packageFloorGuard`** runs before verification and fails the build if any glob above stops
matching a real package — otherwise renaming a package would silently remove its gate rather
than fail. Add a package to the table in `build.gradle.kts` and to strategy §7 together.

**QA-002 note (2026-07-30):** `bank-persistence-service` measured **~96% line / ~75% branch** after jobs/offers/payments/audit API tests; `api.internal.v1` package at **100% line**. Monorepo services interim floor raised **35% → 50%** (both services measured ≥55%). Package-level rules remain deferred to QA-003 / strategy §7.

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
| QA-001 | **Closed** (2026-08-13) | Package floors enforced; interim service floor retired ahead of its 2026-08-30 expiry. Closes WS-1 gate criterion 4.7. Pending TL + QA Lead counter-signature |
| QA-002 | **Done** | Persistence API tests landed; `com.bank.persistence.api.internal.v1` package gate now enforced under QA-001 closure |
| QA-003 | **Done** | IT-I template `OneSbConnectivityIT` (WireMock 1SB + persistence); package-level JaCoCo floors landed with QA-001 closure |

Do not lower lib gates without TL + QA Lead co-approval and a TECH-DEBT id + expiry.
