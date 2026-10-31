# Story — WS2-LOGIN-S16 Complete login after the login OTP

```yaml
story:
  id: WS2-LOGIN-S16
  title: "Complete login after the login OTP"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: S
  owner_persona: "Amit / Engineering"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

The BFF needs the business user updated after a correct login OTP, so that the failure count clears and last successful login moves, and a first-login flag is not cleared by this call alone.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/login/complete`. Body: `businessUserId`. Called only after purpose `LOGIN` verifies.

## 2. Acceptance criteria

```gherkin
AC1
  Given a correct LOGIN OTP
  When complete is called
  Then failed-password count is reset
  And last successful login is now
  And the BFF may then create the workforce session

AC2
  Given a user who was inactivity-locked and later unlocked
  When complete runs after the next successful login OTP
  Then the inactivity baseline moves because last successful login moved

AC3
  Given an unlock OTP or a forgot OTP
  When the BFF finishes that challenge
  Then this operation is not called
```

- [x] AC1
- [x] AC2
- [x] AC3


## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Route and counter rules | Amit |
| Component | Service key and privilege | Amit |
| Integration | BFF calls this only on the path named above | Swapnali |
| E2E | Covered through the public story, not by a direct app call | Swapnali |

- [x] `PlatformLoginServiceTest.inactivityLocksAndSuccessfulLoginResetsItOnlyAfterComplete`
- [x] `LoginJourneyServiceTest.loginOtpCreatesOneSessionAndASecondSubmitDoesNot`

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
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §4, §5 |
| Service test | Unit | `PlatformLoginServiceTest` |

## 8. Notes

This call does not clear `first_login` by itself. First-time activation is S18 or, for Insurance RM, S20.
