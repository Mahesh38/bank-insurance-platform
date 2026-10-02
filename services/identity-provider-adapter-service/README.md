# Identity Provider Adapter Service

Private Spring Boot adapter that keeps Keycloak/Cognito/provider-specific APIs out of the BFF and business authorization service. Keycloak is the first implementation.

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md) for the accepted architecture and security invariants, and [`AUTHN-AUTHZ-LLD.md`](../../docs/platform/authentication-authorization/AUTHN-AUTHZ-LLD.md) for the implementation pack (`ADR-022` — do not collapse this service into Keycloak).

```bash
./gradlew :services:identity-provider-adapter-service:test
./gradlew :services:identity-provider-adapter-service:bootRun
```

The service listens on port `8082`. `/internal/v1/**` must be private and service-authenticated in deployed environments.
