# Story — WS2-LOGIN-S10 Read the workforce session

```yaml
story:
  id: WS2-LOGIN-S10
  title: "Read the workforce session"
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
    - IAM-P1
```

## 1. Story

> As a **Bank RM or Insurance RM who has completed the login OTP**, I need to **read who the session belongs to**, so that **the app can show my workspace without receiving a provider token**.

`GET /api/v1/auth/session`. Handle from `X-Session-Handle`, otherwise cookie `WORKFORCE_SESSION`.

## 2. Acceptance criteria

```gherkin
AC1
  Given a live session created by a successful login OTP
  When GET /api/v1/auth/session is called with that handle
  Then the response is 200 with businessUserId, username, userType, status, policyVersion, and insurerCode
  And the body contains no access token or refresh token

AC2
  Given no handle
  When the session is read
  Then the response is 400 SCHEMA_INVALID

AC3
  Given a handle that is unknown or expired
  When the session is read
  Then the response is 400 SCHEMA_INVALID
  And the call does not contact the identity provider or the authorization service
```

- [x] AC1
- [x] AC2
- [x] AC3

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Missing handle is rejected | Amit |
| Component | Vault read decrypts without a downstream call | Amit |
| Integration | Cookie and native header both resolve | Swapnali |
| E2E | App restores the workspace from the session | Swapnali |

- [x] `BffSessionAuthenticationFilterTest` covers a present, missing, and undecryptable handle on protected routes
- [ ] This `/auth/session` path maps an unknown handle to `400 SCHEMA_INVALID`, which is a different code from `401 SESSION_EXPIRED` on business routes

## 4. Compliance and security impact

```yaml
security_impact: high
compliance_impact: medium
controls_touched: [C5, C8, C10]
pii_involved: true
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

- [x] Response fields match `AuthenticationController.SessionResponse`
- [x] No provider token in the body
- [ ] Session idle lifetime by role is still an open decision in the auth SSOT §15

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| API page | Spec | `docs/platform/api-reference/services/workforce-access-bff.md` §2.6 |
| Controller | Code | `AuthenticationController.session` |

## 8. Notes

Bank RM `userType` on the session is the business identity type `BANK_EMPLOYEE`. Insurance RM is `INSURER_REPRESENTATIVE`. That is the stored type, not the login toggle name.
