# Story — WS2-LOGIN-S18 Finish unlock after the unlock OTP

```yaml
story:
  id: WS2-LOGIN-S18
  title: "Finish unlock after the unlock OTP"
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

The BFF needs unlock finished after a correct unlock OTP, so that a Bank RM is activated or unlocked and an Insurance RM who is first-time is sent to set a password, still without a session.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/login/unlock/complete`. Body: `businessUserId`.

## 2. Acceptance criteria

```gherkin
AC1
  Given a first-time Bank RM and a correct unlock OTP
  When complete is called
  Then the result is ACTIVATED and first_login is false
  And no platform password is created

AC2
  Given a first-time Insurance RM and a correct unlock OTP
  When complete is called
  Then the result is SET_PASSWORD and first_login stays true

AC3
  Given a password lock or an inactivity lock and a correct unlock OTP
  When complete is called
  Then the result is UNLOCKED
  And the failure counter is reset and lock_cleared_at is now
  And the password hash is unchanged
  And last successful login is unchanged

AC4
  Given the account is already usable
  When complete is called
  Then the result is NOT_REQUIRED or INACTIVE and nothing is changed
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

- [x] `PlatformLoginServiceTest.firstTimeBankActivatesWithoutAPasswordChangeAndInsuranceMustSetOne`
- [x] `PlatformLoginServiceTest.insurancePasswordFailuresLockOnTheThirdAndUnlockResetsTheCounter`

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

`lock_cleared_at` stops the same preview from locking the user again immediately. It is not a login.
