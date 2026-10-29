# Identity Provider Adapter Service

Private Spring Boot adapter that keeps Keycloak/Cognito/provider-specific APIs out of the BFF and business authorization service. Keycloak is the first implementation.

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md) for the accepted architecture and security invariants.

```bash
./gradlew :services:identity-provider-adapter-service:test
./gradlew :services:identity-provider-adapter-service:bootRun
```

The service listens on port `8082`. `/internal/v1/**` is private and requires `X-Internal-Service-Key` matching `IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET` (local default; production fail-closed, no default). Health probes stay unauthenticated.

Bank AD-verify (`IAM-001`): `POST /internal/v1/auth/ad-verify` with `{ "employeeId", "password" }` returns `{ "authenticated", "accountActive" }`. Callers (workforce-access-bff, identity-authorization-service) send the same shared key. Local/test default is `identity.ad-verify.mode=stub`. Production must use `mode=http` and `BANK_AD_VERIFY_BASE_URL` (Apigee private). Stub mode is refused under the `prod` profile. LDAP from this service to AD is forbidden.

Insurance password (`SUG-20261029-iu2`): `POST /internal/v1/auth/keycloak-password` with `{ "username", "password" }` returns `{ "authenticated", "accountActive" }`. HTTP mode uses the confidential `workforce-bff` client password grant and revokes the refresh token before returning, so the workforce app never sees a Keycloak token. `POST /internal/v1/auth/keycloak-password/reset` writes the new password after the BFF has accepted a set-password. Local/test default is `identity.keycloak-password.mode=stub` (user `fls.rm@example.test`). Production sets `mode=http` and refuses the stub. Direct access grants stay off on the adapter's own admin client.
