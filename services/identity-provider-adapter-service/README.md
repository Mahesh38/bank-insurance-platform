# Identity Provider Adapter Service

Private Spring Boot adapter that keeps Keycloak/Cognito/provider-specific APIs out of the BFF and business authorization service. Keycloak is the first implementation.

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md) for the accepted architecture and security invariants.

```bash
./gradlew :services:identity-provider-adapter-service:test
./gradlew :services:identity-provider-adapter-service:bootRun
```

The service listens on port `8082`. `/internal/v1/**` must be private and service-authenticated in deployed environments.

Bank AD-verify (`IAM-001`): `POST /internal/v1/auth/ad-verify` with `{ "employeeId", "password" }` returns `{ "authenticated", "accountActive" }`. Local/test default is `identity.ad-verify.mode=stub`. Production must use `mode=http` and `BANK_AD_VERIFY_BASE_URL` (Apigee private). Stub mode is refused under the `prod` profile. LDAP from this service to AD is forbidden.
