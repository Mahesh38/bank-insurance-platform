# Identity Authorization Service

Business source of truth and policy-decision service for workforce identities. It owns branch/insurer scope, hierarchy, roles, permissions, certification, explicit grants/denials, and partner-user administration (created directly by a bank admin and provisioned at creation — DECISIONS-2026-10-22 E5; maker-checker only when `identity.partner-onboarding.approval-required=true`).

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md).

```bash
./gradlew :services:identity-authorization-service:test
./gradlew :services:identity-authorization-service:bootRun
./gradlew :services:identity-authorization-service:bootRun --args='--spring.profiles.active=dev'
```

The service listens on port `8083` and uses H2 in PostgreSQL compatibility mode by default. Deployed profiles must supply a dedicated PostgreSQL datasource. Provider provisioning calls `identity-provider-adapter-service` with `X-Internal-Service-Key` (`IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET`; production has no default).

Login BRD v3.1 account state lives here (Flyway `V6`): first login, failed-password count, lock reason (`PASSWORD` or `INACTIVITY`), last successful platform login, and the Insurance RM platform password (PBKDF2-HMAC-SHA256). Captcha and OTP stay on `workforce-access-bff`. `POST /internal/v1/identities/resolve` is unchanged and still creates the session path used by IAM-001. The v3.1 routes are `POST /internal/v1/login/preview`, `bank-password-result`, `verify-platform-password`, `complete`, `unlock/assess`, `unlock/complete`, `forgot/assess` and `password`. A locked Insurance RM cannot change the password until Unlock User clears the lock. Unlock does not record a successful login. Inactivity is measured from the later of the last successful login and the last lock clearance, so an unlock is not immediately re-locked and the stored last-login time is not rewritten.
