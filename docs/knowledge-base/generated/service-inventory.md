# Module & Service Inventory

> **Generated, committed view.** CI regenerates this file from `settings.gradle.kts` and fails if it drifts. A registered module is not proof that its feature or lifecycle stage is complete.

**Registered modules:** 27 · **Services:** 21 · **Shared libraries:** 6

## Services

| Gradle module | Repository path |
|---|---|
| `services:1sb-integration-service` | `services/1sb-integration-service/` |
| `services:administration-config-service` | `services/administration-config-service/` |
| `services:audit-compliance-service` | `services/audit-compliance-service/` |
| `services:bank-persistence-service` | `services/bank-persistence-service/` |
| `services:consent-service` | `services/consent-service/` |
| `services:customer-service` | `services/customer-service/` |
| `services:direct-insurer-adapter-service` | `services/direct-insurer-adapter-service/` |
| `services:identity-authorization-service` | `services/identity-authorization-service/` |
| `services:identity-provider-adapter-service` | `services/identity-provider-adapter-service/` |
| `services:integration-hub-service` | `services/integration-hub-service/` |
| `services:journey-orchestration-service` | `services/journey-orchestration-service/` |
| `services:lead-service` | `services/lead-service/` |
| `services:notification-service` | `services/notification-service/` |
| `services:payment-service` | `services/payment-service/` |
| `services:policy-issuance-service` | `services/policy-issuance-service/` |
| `services:product-catalogue-service` | `services/product-catalogue-service/` |
| `services:proposal-service` | `services/proposal-service/` |
| `services:quotation-service` | `services/quotation-service/` |
| `services:reporting-mis-service` | `services/reporting-mis-service/` |
| `services:suitability-service` | `services/suitability-service/` |
| `services:workforce-access-bff` | `services/workforce-access-bff/` |

## Shared libraries

| Gradle module | Repository path |
|---|---|
| `libs:bank-common-error` | `libs/bank-common-error/` |
| `libs:bank-common-domain` | `libs/bank-common-domain/` |
| `libs:bank-common-security` | `libs/bank-common-security/` |
| `libs:bank-common-audit` | `libs/bank-common-audit/` |
| `libs:bank-common-observability` | `libs/bank-common-observability/` |
| `libs:bank-common-secrets` | `libs/bank-common-secrets/` |

## Read this correctly

- **Registered** means the module participates in the Gradle build.
- **Implemented** requires code plus tests/acceptance evidence.
- **READY/DONE** is a governance/backlog state, not inferred from module presence.
- **Stage complete** requires the relevant gate evidence and human sign-off where required.

For intended R0 boundaries use the [R0 HLD](../../architecture/R0-HLD.md). For 1SB adapter contracts use the [1SB service SSOT](../../1sb-insurance-integration/service-ssot/README.md).
