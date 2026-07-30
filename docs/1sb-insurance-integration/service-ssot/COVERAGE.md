# Coverage (JaCoCo) — QA-001

**Authority:** [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md) §7 · [TESTING-RULES.md](./TESTING-RULES.md) R7 · [TEST-BACKLOG.md](./TEST-BACKLOG.md)

## How to run

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

`check` also depends on `jacocoTestCoverageVerification`.

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
| `services/*` | **≥ 35%** (interim) | *not gated* | QA-001 floor so monorepo stays green; **tighten in QA-002/003** |

### Planned service package gates (QA-002 / QA-003)

Per strategy §7 (not yet enforced as package-level rules):

| Package | Line | Branch |
|---------|------|--------|
| `…onesb.application.*` / `…lob.*` | 80% | 70% |
| `…adapter.onesb.*` | 70% | 60% |
| `…adapter.idempotency.*` | 80% | 70% |
| `com.bank.persistence.api.*` | 70% | 60% |

## Exclusions (always)

Applied to report + verification class sets:

- `*Application` / `*Application.class`
- `package-info`
- Spring wiring: `*Config`, `*Configuration`, `*Properties`

## Waivers / TECH-DEBT

| ID | Status | Note |
|----|--------|------|
| QA-001 | **Partial** | Libs at strategy thresholds; services use interim **35% line** only until adapter/api tests land (QA-002, QA-003) |

Do not lower lib gates without TL + QA Lead co-approval and a TECH-DEBT id + expiry.
