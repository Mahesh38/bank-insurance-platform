# Story — WS2-LOGIN-S11 Log out and destroy the session

```yaml
story:
  id: WS2-LOGIN-S11
  title: "Log out and destroy the workforce session"
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

> As a **Bank RM or Insurance RM**, I need to **log out**, so that **the opaque session can no longer call workforce APIs**.

`POST /api/v1/auth/logout`. Handle from `X-Session-Handle`, otherwise the session cookie.

## 2. Acceptance criteria

```gherkin
AC1
  Given a live Bank RM or Insurance RM session
  When POST /api/v1/auth/logout is called
  Then the response is 204
  And the session cookie is expired
  And a later read of that handle is rejected

AC2
  Given the session was created by the Login BRD OTP and holds no provider refresh token
  When logout runs
  Then the vault record is deleted
  And no Keycloak revoke call is required for that session

AC3
  Given logout is called without a handle
  When the BFF requires a handle
  Then the call fails as a missing session and does not create one
```

- [x] AC1
- [x] AC2
- [x] AC3

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Vault delete happens before any revoke | Amit |
| Component | Expired cookie attributes match the session cookie | Amit |
| Integration | Reuse of the handle after logout fails | Swapnali |
| E2E | Logout returns the user to the login screen | Swapnali |

- [x] LLD §3.4: delete the vault record first; revoke only when a refresh token exists
- [ ] A dedicated logout test for the BRD session (no provider token) is not named in `LoginJourneyServiceTest`

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

- [x] `204` and an expired cookie (`AuthenticationController.logout`)
- [x] Bank RM / Insurance RM login sessions do not put an AD password in the vault
- [ ] `SessionRevoked` audit event is not written by this handler (C8)

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| API page | Spec | `docs/platform/api-reference/services/workforce-access-bff.md` §2.7 |
| Scenario | Spec | `docs/platform/api-reference/scenarios/03-session-lifecycle-and-logout.md` |

## 8. Notes

Partner OIDC logout may also call adapter revoke. That revoke failure is logged and does not fail the `204`. The Login BRD session has no provider refresh token.
