# Story — WS2-LOGIN-S01 Issue the login CSRF token

```yaml
story:
  id: WS2-LOGIN-S01
  title: "Issue the login CSRF token"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: S
  owner_persona: "Rajal / Product"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

> As a **Bank RM or Insurance RM on the web app**, I need a **CSRF token before any login POST**, so that **another site cannot submit the login journey with my browser cookies**.

`GET /api/v1/auth/csrf` on `workforce-access-bff`. No session. No downstream call.

## 2. Acceptance criteria

```gherkin
AC1
  Given the web app has not signed in
  When it calls GET /api/v1/auth/csrf
  Then the response is 200 with headerName and token
  And the response sets cookie WORKFORCE-XSRF

AC2
  Given a web POST to /api/v1/auth/** without X-XSRF-TOKEN
  When the BFF checks CSRF
  Then Spring Security refuses the request
  And the problem+json code CSRF_REJECTED is not implemented yet

AC3
  Given a request that already carries X-Session-Handle or Authorization Bearer
  When CSRF is evaluated
  Then CSRF is skipped for that request
```

- [x] AC1
- [ ] AC2 — refusal is the Spring Security default; `403 CSRF_REJECTED` is fix-pending on the BFF API page
- [x] AC3

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | CSRF skip when a session handle or bearer is present | Amit |
| Component | Cookie and header names | Amit |
| Integration | Web login POST refused without the token | Swapnali |
| E2E | NIP login screen obtains the token before captcha | Swapnali |

- [x] `BffSessionCredentialsTest.csrfSkipRequiresBearerOrSessionHandleWithoutCookie`
- [ ] Browser journey against a running BFF is not in this repository (NIP-APP is DOC-006)

## 4. Compliance and security impact

```yaml
security_impact: high
compliance_impact: medium
controls_touched: [C5, C8, C10]
pii_involved: false
review_tier: T3
```

C5: passwords, OTP codes, and captcha answers are redacted from request `toString` and are not
logged. C8: these handlers do not yet emit the §19 audit events; that gap stays on the flow
document and is not closed by this story. C10: the browser session cookie is `HttpOnly`,
`Secure`, `SameSite=Strict`. No OAuth token is returned to the app. A T4 Security signature is
not claimed here.

## 5. Definition of Ready

- [x] Traces to Login BRD v3.1 and `SUG-20261028-1lg`
- [x] Acceptance criteria observable and testable
- [x] Security and compliance impact stated
- [x] Dependencies identified (BFF, and the internal operation named in the story)
- [x] Test approach names the existing test class
- [x] Test data is synthetic (employee id, corporate email, no production credential)
- [x] Sized (not XL)

## 6. Definition of Done

- [x] AC1 and AC3 match `AuthenticationController.csrf` and `BffSecurityConfig`
- [ ] AC2 catalogue code `CSRF_REJECTED` is still fix-pending
- [x] No password, PAN, Aadhaar, phone, or email is logged
- [ ] Audit event for CSRF issuance is not required; login success audit is still open (C8)
- [x] Documentation: this story and the BFF LLD §3.1

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Controller | Code | `services/workforce-access-bff/.../api/AuthenticationController.java` |
| CSRF skip test | Unit | `.../api/BffSessionCredentialsTest.java` |

## 8. Notes

Native clients that send `X-Session-Handle` do not use this cookie. The first web login POST still does.
