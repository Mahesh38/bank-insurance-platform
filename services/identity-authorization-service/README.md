# Identity Authorization Service

Business source of truth and policy-decision service for workforce identities. It owns branch/insurer scope, hierarchy, roles, permissions, certification, explicit grants/denials, and maker-checker partner-user administration.

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md) (invariants) and [`AUTHN-AUTHZ-LLD.md`](../../docs/platform/authentication-authorization/AUTHN-AUTHZ-LLD.md) (implementation pack).

```bash
./gradlew :services:identity-authorization-service:test
./gradlew :services:identity-authorization-service:bootRun
```

The service listens on port `8083` and uses H2 in PostgreSQL compatibility mode by default. Deployed profiles must supply a dedicated PostgreSQL datasource.
