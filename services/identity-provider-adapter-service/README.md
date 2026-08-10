# Identity Provider Adapter Service

Private Spring Boot adapter that keeps Keycloak/Cognito/provider-specific APIs out of the BFF and business authorization service. Keycloak is the first implementation.

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md) for the accepted architecture and security invariants.

```bash
./gradlew :services:identity-provider-adapter-service:test
./gradlew :services:identity-provider-adapter-service:bootRun
```

The service listens on port `8082`. `/internal/v1/**` must be private and service-authenticated in deployed environments.
