# Workforce Access BFF

Token-hiding BFF for the workforce client (**NIP-APP**). NIP-APP source is **not** in this
repository (`DOC-006`). Browser clients receive an HttpOnly session cookie; native clients
exchange a one-time completion code for an opaque session handle. Provider tokens remain
encrypted in the server-side session vault.

Evaluate this contract against [`docs/figma/`](../../docs/figma/README.md) and the
[Login BFF vs Figma evaluation](../../docs/au-bank-insurance-platform/requirements/LOGIN-BFF-FIGMA-EVALUATION.md).
Behaviour SSOT is the [Login BRD v3.1](../../docs/au-bank-insurance-platform/requirements/brd-detailed/Login_Module_BRD_Detailed_CONTEXT.md). User-visible copy is the design text in that file.

**One password-login surface (`SUG-20261028-1lg`):** `POST /api/v1/auth/captcha`, `/login`,
`/otp/verify`, `/otp/resend`, `/unlock`, `/forgot-password`, `/password` and
`/password-expired/send-otp`. A `WORKFORCE_SESSION` cookie (or native `sessionHandle`) is set
only after a successful login OTP. Unlock, Forgot Password and expiry do not log the user in.
IAM-001 immediate-session `POST /login` with `identitySource` and `/auth/sign-in` are retired.
Keycloak partner OIDC begins at `POST /api/v1/auth/partner/authorization`.

Captcha text and OTP codes are hashed in memory on this instance and are not logged. There is no
SMS, email or image-Captcha provider in this repository; a fixed `WORKFORCE_LOGIN_OTP_CODE` is
refused under `prod` and `uat`. Challenge state is single-instance until a shared store exists.

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md).

```bash
./gradlew :services:workforce-access-bff:test
./gradlew :services:workforce-access-bff:bootRun
```

The service listens on port `8084`. Obtain a CSRF token from `GET /api/v1/auth/csrf` before POST requests. Local tests use the in-memory encrypted store; the identity Compose stack sets `WORKFORCE_SESSION_STORE=redis`.

Login BRD v3.1 (`BANK_RM` or `INSURANCE_RM`):

```http
POST /api/v1/auth/captcha
POST /api/v1/auth/login
{"clientType":"WEB","userType":"BANK_RM","loginId":"EMP001","password":"local-stub-password","captchaId":"…","captchaValue":"…"}
POST /api/v1/auth/otp/verify
{"challengeId":"…","otp":"482913"}
```

Local AD-verify is stubbed in `identity-provider-adapter-service` (`EMP001` / `local-stub-password` is active). The BFF forwards Bank RM credentials on `POST /internal/v1/auth/ad-verify` with `X-Internal-Service-Key` (`IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET`; production has no default). It never persists the password and never returns an OAuth token. The session cookie is set only when OTP verify returns `AUTHENTICATED`. Insurance RM / FLS use `userType: INSURANCE_RM` and a platform password.

Partner OIDC:

```http
POST /api/v1/auth/partner/authorization
{"clientType":"WEB","returnUri":"http://localhost:3000/auth/complete"}
```

Dev profile (`--spring.profiles.active=dev`) sets the OTP to `482913` so the journey can be finished locally. `prod` and `uat` refuse to start with that pin. Start the adapter (8082), then identity-authorization with the same profile (8083, which loads `EMP001`, Insurance RM/FLS `fls.rm@example.test`, and first-time `fls.new@example.test`), then this service. Bank RM password reset is not a platform API.
