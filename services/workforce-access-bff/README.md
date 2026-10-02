# Workforce Access BFF

Token-hiding BFF for the workforce client (**NIP-APP**). NIP-APP source is **not** in this
repository (`DOC-006`). Browser clients receive an HttpOnly session cookie; native clients
exchange a one-time completion code for an opaque session handle. Provider tokens remain
encrypted in the server-side session vault.

Evaluate this contract against [`docs/figma/`](../../docs/figma/README.md) and the
[Login BFF vs Figma evaluation](../../docs/au-bank-insurance-platform/requirements/LOGIN-BFF-FIGMA-EVALUATION.md).
Behaviour SSOT is the [Login BRD](../../docs/au-bank-insurance-platform/requirements/brd-detailed/Login_Module_BRD_Detailed_CONTEXT.md).

See [`docs/platform/authentication-authorization/README.md`](../../docs/platform/authentication-authorization/README.md) and the [LLD pack](../../docs/platform/authentication-authorization/AUTHN-AUTHZ-LLD.md).

```bash
./gradlew :services:workforce-access-bff:test
./gradlew :services:workforce-access-bff:bootRun
```

The service listens on port `8084`. Obtain a CSRF token from `GET /api/v1/auth/csrf` before POST requests. Local tests use the in-memory encrypted store; the identity Compose stack sets `WORKFORCE_SESSION_STORE=redis`.
