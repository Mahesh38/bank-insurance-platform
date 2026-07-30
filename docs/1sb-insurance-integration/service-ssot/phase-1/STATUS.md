# Phase 1 — Status

**Branch:** `cursor/phase1-foundations-c259`

---

## Dev A — Shared Libraries

| Ticket     | Module                      | Status      | Notes |
|------------|-----------------------------|-------------|-------|
| SHARED-001 | `libs/bank-common-error`    | ✅ Done     | `ServiceError`, `ServiceErrorResponse` (RFC7807), `ServiceException`, `ErrorCodes`; unit tests pass |
| SHARED-002 | `libs/bank-common-security` | ✅ Done     | `BankPrincipal`, `Role`, `BankPrincipalExtractor`, `SecurityProperties`, `JwtClaims`, `@RequiresRole`; unit tests pass |
| SHARED-003 | `libs/bank-common-audit`    | ✅ Done     | `AuditEvent`, `AuditEventPublisher`, `AuditActions`, `AuditOutcomes`; unit tests pass |
| SHARED-004 | `libs/bank-common-observability` | ✅ Done | `MdcKeys`, `MetricNames`, `TraceHeaders`, `MdcContext`; unit tests pass |

### Root Gradle setup
- `settings.gradle.kts` — includes all four libs and `services:1sb-integration-service`
- `build.gradle.kts` — Java 21 toolchain, Spring Boot 3.3.5 BOM (via Gradle `platform()`)
- `gradle.properties` — parallel build, UTF-8
- `gradlew` / `gradle/wrapper/` — Gradle 8.8 wrapper

### How to depend on the libs (Dev B)

In `services/1sb-integration-service/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":libs:bank-common-error"))
    implementation(project(":libs:bank-common-security"))
    implementation(project(":libs:bank-common-audit"))
    implementation(project(":libs:bank-common-observability"))
}
```

---

## Dev B — Integration Service Scaffold

| Ticket   | Task                              | Status    | Notes |
|----------|-----------------------------------|-----------|-------|
| TECH-001 | Service scaffold + health         | ⏳ Pending | |
| TECH-002 | Secrets & config                  | ⏳ Pending | |
| TECH-003 | DB migrations (Flyway)            | ⏳ Pending | |

---

## Exit criteria checklist

- [x] Shared libs compile — `./gradlew :libs:bank-common-error:build :libs:bank-common-security:build :libs:bank-common-audit:build :libs:bank-common-observability:build`
- [x] Shared lib unit tests pass
- [ ] `./gradlew build` full project passes (pending service scaffold)
- [ ] Service boots; `/actuator/health` UP
- [ ] Flyway migrations present
- [ ] Phase 1 status fully green
