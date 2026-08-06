# Workforce Access BFF

Token-hiding BFF for the Phase 1 Flutter workforce application. Browser clients receive an HttpOnly session cookie; native clients exchange a one-time completion code for an opaque session handle. Provider tokens remain encrypted in the server-side session vault.

See [`docs/authentication-authorization/README.md`](../../docs/authentication-authorization/README.md).

```bash
./gradlew :services:workforce-access-bff:test
./gradlew :services:workforce-access-bff:bootRun
```

The service listens on port `8084`. Obtain a CSRF token from `GET /api/v1/auth/csrf` before POST requests. Local tests use the in-memory encrypted store; the identity Compose stack sets `WORKFORCE_SESSION_STORE=redis`.
