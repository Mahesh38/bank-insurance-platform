# Identity Authorization Service

Business source of truth and policy-decision service for workforce identities. It owns branch/insurer scope, hierarchy, roles, permissions, certification, explicit grants/denials, and maker-checker partner-user administration.

See [`docs/authentication-authorization/README.md`](../../docs/authentication-authorization/README.md).

```bash
./gradlew :services:identity-authorization-service:test
./gradlew :services:identity-authorization-service:bootRun
```

The service listens on port `8083` and uses H2 in PostgreSQL compatibility mode by default. Deployed profiles must supply a dedicated PostgreSQL datasource.
