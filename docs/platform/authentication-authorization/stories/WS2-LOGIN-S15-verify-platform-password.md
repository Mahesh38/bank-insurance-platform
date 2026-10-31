# Story — WS2-LOGIN-S15 Verify an Insurance RM platform password

```yaml
story:
  id: WS2-LOGIN-S15
  title: "Verify an Insurance RM platform password"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: M
  owner_persona: "Amit / Engineering"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

The BFF needs the stored platform password checked for an Insurance RM, so that a match can start the login OTP and an expired match can start the expiry path without counting as a failure.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/login/verify-platform-password`. Body: `businessUserId`, `password`. The password is omitted from `toString`.

## 2. Acceptance criteria

```gherkin
AC1
  Given a READY Insurance RM and a matching PBKDF2 hash inside 60 days
  When verify is called
  Then accepted is true

AC2
  Given a wrong password
  When verify is called
  Then accepted is false and the counter increments toward the third-failure lock

AC3
  Given a matching hash and password_changed_at plus 60 days is not after now
  When verify is called
  Then the route is PASSWORD_EXPIRED and accepted is false
  And the failure counter does not move

AC4
  Given a Bank RM sign-in
  When the password is checked
  Then this operation is not called
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4


## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Route and counter rules | Amit |
| Component | Service key and privilege | Amit |
| Integration | BFF calls this only on the path named above | Swapnali |
| E2E | Covered through the public story, not by a direct app call | Swapnali |

- [x] `PlatformLoginServiceTest.insurancePasswordFailuresLockOnTheThirdAndUnlockResetsTheCounter`
- [x] `PlatformLoginServiceTest.expiredPasswordDoesNotCountAsAFailureAndMustDifferFromThePrevious`

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

- [x] App cannot call this path
- [x] Password or network credential is not logged
- [ ] §19 audit event is not written by the login handlers (C8)
- [x] Contract row is in the service LLD

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| LLD | Spec | `identity-authorization-service/LLD.md` §2 |
| Service test | Unit | `PlatformLoginServiceTest` |

## 8. Notes

Hash format is `pbkdf2$iterations$salt$hash` (PBKDF2-HMAC-SHA256, 120000 iterations). A null `password_changed_at` is not expiry.
