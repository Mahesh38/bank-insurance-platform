# Identity Authorization Service

Business source of truth and policy-decision service for workforce identities. It owns branch/insurer scope, hierarchy, roles, permissions, certification, explicit grants/denials, and partner-user administration (created directly by a bank admin and provisioned at creation — DECISIONS-2026-10-22 E5; maker-checker only when `identity.partner-onboarding.approval-required=true`).

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md).

```bash
./gradlew :services:identity-authorization-service:test
./gradlew :services:identity-authorization-service:bootRun
```

The service listens on port `8083` and uses H2 in PostgreSQL compatibility mode by default. Deployed profiles must supply a dedicated PostgreSQL datasource. Provider provisioning calls `identity-provider-adapter-service` with `X-Internal-Service-Key` (`IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET`; production has no default).
