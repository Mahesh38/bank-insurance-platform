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
| `services/*` | **≥ 50%** (interim) | *not gated* | Raised in QA-002 after persistence API coverage (~96% line measured); package floors still pending QA-003 |

### Planned service package gates (QA-002 / QA-003)

Per strategy §7 (not yet enforced as package-level rules):

| Package | Line | Branch |
|---------|------|--------|
| `…onesb.application.*` / `…lob.*` | 80% | 70% |
| `…adapter.onesb.*` | 70% | 60% |
| `…adapter.idempotency.*` | 80% | 70% |
| `com.bank.persistence.api.*` | 70% | 60% |

**QA-002 note (2026-07-30):** `bank-persistence-service` measured **~96% line / ~75% branch** after jobs/offers/payments/audit API tests; `api.internal.v1` package at **100% line**. Monorepo services interim floor raised **35% → 50%** (both services measured ≥55%). Package-level rules remain deferred to QA-003 / strategy §7.

## Exclusions (always)

Applied to report + verification class sets:

- `*Application` / `*Application.class`
- `package-info`
- Spring wiring: `*Config`, `*Configuration`, `*Properties`

## Waivers / TECH-DEBT

| ID | Status | Note |
|----|--------|------|
| QA-001 | **Partial** | Libs at strategy thresholds; services interim raised **35% → 50%** line after QA-002 (persistence ~96%); package floors still pending QA-003 |
| QA-002 | **Done** | Persistence API tests landed; services floor → 50%; package-level `com.bank.persistence.api.*` gate still not enforced (QA-003 / strategy §7) |

Do not lower lib gates without TL + QA Lead co-approval and a TECH-DEBT id + expiry.
