# AGENTS.md

Guidance for cloud agents working in this repository.

## Repository status

Multi-module Gradle (Kotlin DSL) monorepo for the **1SB insurance platform**:

- **Java 21** / **Spring Boot 3.3.4**
- Shared libs under `libs/`
- Services under `services/` (`1sb-integration-service`; persistence service added in Phase 1 remediation)

## Cursor Cloud specific instructions

### Services

| Service | Required? | Notes |
|---------|-----------|-------|
| `1sb-integration-service` | Yes (Phase 1+) | Boot app; profile `local` / `test` / `uat` / `prod` |
| PostgreSQL / H2 | Profile-dependent | Local/test use H2; uat+prod use PostgreSQL (persistence ownership evolving — see TECH-DEBT) |

### System tooling (VM)

- **JDK 21** (required for build)
- **Git** 2.43+
- Node.js / Python / Go may also be present but are not required for this Java platform

Docker is not required for unit tests.

### Lint / test / run

```bash
./gradlew test
./gradlew :services:1sb-integration-service:bootRun --args='--spring.profiles.active=local'
```

Targeted shared-lib verification:

```bash
./gradlew :libs:bank-common-error:test :libs:bank-common-security:test \
  :libs:bank-common-audit:test :libs:bank-common-secrets:test
```

### Update script

After cloning, no separate install step beyond a Gradle build (wrapper downloads the toolchain/deps).
