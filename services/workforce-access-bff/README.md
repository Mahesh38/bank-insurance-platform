# Workforce Access BFF

Token-hiding BFF for the workforce client (**NIP-APP**). NIP-APP source is **not** in this
repository (`DOC-006`). Browser clients receive an HttpOnly session cookie; native clients
exchange a one-time completion code for an opaque session handle. Provider tokens remain
encrypted in the server-side session vault.

Evaluate this contract against [`docs/figma/`](../../docs/figma/README.md) and the
[Login BFF vs Figma evaluation](../../docs/au-bank-insurance-platform/requirements/LOGIN-BFF-FIGMA-EVALUATION.md).
Behaviour SSOT is the [Login BRD v3.1](../../docs/au-bank-insurance-platform/requirements/brd-detailed/Login_Module_BRD_Detailed_CONTEXT.md). User-visible copy is the design text in that file. `POST /api/v1/auth/login` remains the IAM-001 Bank RM AD-verify path and still creates a session immediately. The v3.1 journey is separate: `POST /api/v1/auth/captcha`, `/sign-in`, `/otp/verify`, `/otp/resend`, `/unlock`, `/forgot-password`, `/password` and `/password-expired/send-otp`. A `WORKFORCE_SESSION` cookie (or native `sessionHandle`) is set only after a successful login OTP. Unlock, Forgot Password and expiry do not log the user in. Captcha text and OTP codes are hashed in memory on this instance and are not logged. There is no SMS, email or image-Captcha provider in this repository; a fixed `WORKFORCE_LOGIN_OTP_CODE` is refused under `prod` and `uat`. Challenge state is single-instance until a shared store exists.

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md).

```bash
./gradlew :services:workforce-access-bff:test
./gradlew :services:workforce-access-bff:bootRun
```

The service listens on port `8084`. Obtain a CSRF token from `GET /api/v1/auth/csrf` before POST requests. Local tests use the in-memory encrypted store; the identity Compose stack sets `WORKFORCE_SESSION_STORE=redis`.

Bank RM login (`IAM-001`):

```http
POST /api/v1/auth/login
{"clientType":"WEB","identitySource":"BANK_AD","employeeId":"EMP001","password":"local-stub-password"}
```

Local AD-verify is stubbed in `identity-provider-adapter-service` (`EMP001` / `local-stub-password` is active). The BFF forwards credentials on `POST /internal/v1/auth/ad-verify` with `X-Internal-Service-Key` (`IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET`; production has no default). It never persists the password and never returns an OAuth token. The BFF returns `{ "authenticated": true }` and an HttpOnly `WORKFORCE_SESSION` cookie. Native clients receive `sessionHandle` instead of a cookie. Partner login still returns an authorization URI.

v3.1 sign-in (`BANK_RM` or `INSURANCE_RM`) returns design-text `MESSAGE` outcomes, or `OTP_SENT` with masked mobile and email. The OTP value is not in the response. `WEB` receives the session cookie only from `POST /api/v1/auth/otp/verify` when `outcome` is `AUTHENTICATED`.
